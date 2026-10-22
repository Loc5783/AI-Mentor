import contextlib
import json
import sqlite3
import time
import uuid
from pathlib import Path

class Database:
    def __init__(self, directory: Path):
        directory.mkdir(parents=True, exist_ok=True)
        self.path = directory / "aimentor.sqlite3"

    def connect(self):
        db = sqlite3.connect(self.path, timeout=15)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA foreign_keys=ON")
        return db

    @contextlib.contextmanager
    def transaction(self):
        db = self.connect()
        try:
            db.execute("BEGIN IMMEDIATE")
            yield db
            db.commit()
        except BaseException:
            db.rollback()
            raise
        finally:
            db.close()

    @contextlib.contextmanager
    def read(self):
        db = self.connect()
        try:
            yield db
        finally:
            db.close()

    def initialize(self):
        with self.read() as db:
            db.execute("PRAGMA journal_mode=WAL")
            db.executescript("""
            CREATE TABLE IF NOT EXISTS users(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              username TEXT NOT NULL UNIQUE COLLATE NOCASE,
              email TEXT NOT NULL UNIQUE COLLATE NOCASE,
              password_hash TEXT NOT NULL,
              nickname TEXT NOT NULL, bio TEXT NOT NULL DEFAULT '',
              avatar_id TEXT, grade INTEGER NOT NULL DEFAULT 0,
              verified INTEGER NOT NULL DEFAULT 0,
              revision INTEGER NOT NULL DEFAULT 1,
              created_at REAL NOT NULL, updated_at REAL NOT NULL);
            CREATE TABLE IF NOT EXISTS sessions(
              id TEXT PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              access_hash TEXT NOT NULL UNIQUE, refresh_hash TEXT NOT NULL UNIQUE,
              access_expires REAL NOT NULL, refresh_expires REAL NOT NULL, revoked INTEGER NOT NULL DEFAULT 0);
            CREATE TABLE IF NOT EXISTS resources(
              id TEXT PRIMARY KEY, kind TEXT NOT NULL,
              user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              body TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1,
              updated_at REAL NOT NULL, deleted_at REAL);
            CREATE INDEX IF NOT EXISTS resource_owner_kind ON resources(user_id,kind,updated_at);
            CREATE TABLE IF NOT EXISTS enrollments(
              id TEXT PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              subject_id TEXT NOT NULL, grade INTEGER NOT NULL CHECK(grade BETWEEN 10 AND 12),
              curriculum_version TEXT NOT NULL, placement TEXT,
              revision INTEGER NOT NULL DEFAULT 1, updated_at REAL NOT NULL,
              UNIQUE(user_id,subject_id,grade));
            CREATE TABLE IF NOT EXISTS attempts(
              id TEXT PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              enrollment_id TEXT NOT NULL REFERENCES enrollments(id),
              unit_id TEXT NOT NULL, kind TEXT NOT NULL,
              curriculum_version TEXT NOT NULL, step INTEGER NOT NULL DEFAULT 0,
              answers TEXT NOT NULL DEFAULT '[]', state TEXT NOT NULL DEFAULT 'IN_PROGRESS',
              revision INTEGER NOT NULL DEFAULT 1, updated_at REAL NOT NULL);
            CREATE TABLE IF NOT EXISTS completions(
              id TEXT PRIMARY KEY, attempt_id TEXT NOT NULL UNIQUE REFERENCES attempts(id),
              user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              xp INTEGER NOT NULL, day TEXT NOT NULL, result TEXT NOT NULL, completed_at REAL NOT NULL);
            CREATE TABLE IF NOT EXISTS awards(
              user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              subject_id TEXT NOT NULL, grade INTEGER NOT NULL, reward_unit_id TEXT NOT NULL,
              xp INTEGER NOT NULL, day TEXT NOT NULL, created_at REAL NOT NULL,
              PRIMARY KEY(user_id,subject_id,grade,reward_unit_id));
            CREATE TABLE IF NOT EXISTS learning_days(
              user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              day TEXT NOT NULL, PRIMARY KEY(user_id,day));
            CREATE TABLE IF NOT EXISTS friendships(
              id TEXT PRIMARY KEY, requester INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              recipient INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              state TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1, updated_at REAL NOT NULL,
              CHECK(requester<>recipient), UNIQUE(requester,recipient));
            CREATE TABLE IF NOT EXISTS events(
              seq INTEGER PRIMARY KEY AUTOINCREMENT, owner INTEGER,
              resource_type TEXT NOT NULL, resource_id TEXT NOT NULL,
              revision INTEGER NOT NULL, operation TEXT NOT NULL, created_at REAL NOT NULL);
            CREATE TABLE IF NOT EXISTS idempotency(
              owner INTEGER NOT NULL, operation TEXT NOT NULL, key TEXT NOT NULL,
              request_hash TEXT NOT NULL, response TEXT NOT NULL,
              PRIMARY KEY(owner,operation,key));
            CREATE TABLE IF NOT EXISTS recovery_tokens(
              token_hash TEXT PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              purpose TEXT NOT NULL, expires_at REAL NOT NULL, consumed INTEGER NOT NULL DEFAULT 0);
            CREATE TABLE IF NOT EXISTS rate_limits(
              bucket TEXT NOT NULL, slot INTEGER NOT NULL, count INTEGER NOT NULL,
              PRIMARY KEY(bucket,slot));
            PRAGMA user_version=1;
            """)
            cols = [r[1] for r in db.execute("PRAGMA table_info(users)").fetchall()]
            if "rank_tier" not in cols:
                db.execute("ALTER TABLE users ADD COLUMN rank_tier TEXT NOT NULL DEFAULT 'BRONZE'")
            if "qr_code_token" not in cols:
                db.execute("ALTER TABLE users ADD COLUMN qr_code_token TEXT")

            db.executescript("""
            CREATE TABLE IF NOT EXISTS badges(
              id TEXT PRIMARY KEY, code TEXT NOT NULL UNIQUE, title TEXT NOT NULL,
              description TEXT NOT NULL, icon_url TEXT NOT NULL, month_period TEXT NOT NULL, created_at REAL NOT NULL);
            CREATE TABLE IF NOT EXISTS user_badges(
              user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              badge_id TEXT NOT NULL REFERENCES badges(id),
              unlocked_at REAL NOT NULL, PRIMARY KEY(user_id,badge_id));
            CREATE TABLE IF NOT EXISTS friend_streaks(
              id TEXT PRIMARY KEY, user1_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              user2_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
              streak_days INTEGER NOT NULL DEFAULT 0, last_shared_day TEXT NOT NULL,
              updated_at REAL NOT NULL, UNIQUE(user1_id,user2_id));
            """)
            db.commit()

    @staticmethod
    def event(db, owner, kind, resource_id, revision=1, operation="UPSERT"):
        db.execute("INSERT INTO events(owner,resource_type,resource_id,revision,operation,created_at) VALUES(?,?,?,?,?,?)",
                   (owner,kind,str(resource_id),revision,operation,time.time()))

def identifier():
    return str(uuid.uuid4())

def dump(value):
    return json.dumps(value, ensure_ascii=False, separators=(",",":"), sort_keys=True)

