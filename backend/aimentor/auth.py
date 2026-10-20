import sqlite3
import time
from . import security
from .common import error, iso
from .database import identifier, Database

class AuthService:
    def __init__(self,db,settings):
        self.db,self.settings=db,settings
        self.dummy=security.hash_password("NotARealAccount01!")

    def limit(self,bucket,maximum=10,window=60):
        slot=int(time.time())//window
        with self.db.transaction() as db:
            row=db.execute("SELECT count FROM rate_limits WHERE bucket=? AND slot=?",(bucket,slot)).fetchone()
            if row and row["count"]>=maximum:
                error("RATE_LIMITED",429)
            db.execute("INSERT INTO rate_limits(bucket,slot,count) VALUES(?,?,1) ON CONFLICT(bucket,slot) DO UPDATE SET count=count+1",(bucket,slot))
            db.execute("DELETE FROM rate_limits WHERE slot<? AND bucket=?",(slot-2,bucket))

    def mint(self,db,user_id):
        now=time.time()
        access,refresh=security.token(),security.token()
        db.execute("INSERT INTO sessions VALUES(?,?,?,?,?,?,0)",(identifier(),user_id,security.digest(access),security.digest(refresh),now+self.settings.access_seconds,now+self.settings.refresh_seconds))
        return {"accessToken":access,"refreshToken":refresh,"expiresAt":iso(now+self.settings.access_seconds),"userId":str(user_id)}

    def signup(self,body):
        if not security.valid_password(body.password):
            error("WEAK_PASSWORD")
        now=time.time()
        try:
            with self.db.transaction() as db:
                row=db.execute("INSERT INTO users(username,email,password_hash,nickname,created_at,updated_at) VALUES(?,?,?,?,?,?)",
                    (body.email,body.email,security.hash_password(body.password),body.displayName.strip(),now,now))
                owner=row.lastrowid
                Database.event(db,None,"PROFILE",owner)
                return self.mint(db,owner)
        except sqlite3.IntegrityError:
            error("ACCOUNT_ALREADY_EXISTS",409)

    def signin(self,body):
        with self.db.read() as db:
            user=db.execute("SELECT * FROM users WHERE username=? COLLATE NOCASE OR email=? COLLATE NOCASE",(body.identifier.strip(),body.identifier.strip())).fetchone()
        valid=security.verify_password(body.password,user["password_hash"] if user else self.dummy)
        if not user or not valid:
            error("INVALID_CREDENTIALS",401)
        with self.db.transaction() as db:
            return self.mint(db,user["id"])

    def current(self,token):
        if not token:error("AUTHENTICATION_REQUIRED",401)
        with self.db.read() as db:
            row=db.execute("SELECT users.* FROM sessions JOIN users ON users.id=sessions.user_id WHERE access_hash=? AND revoked=0 AND access_expires>?",
                           (security.digest(token),time.time())).fetchone()
        if not row:error("INVALID_SESSION",401)
        return dict(row)

    def refresh(self,value):
        now=time.time()
        with self.db.transaction() as db:
            row=db.execute("SELECT * FROM sessions WHERE refresh_hash=? AND revoked=0 AND refresh_expires>?",(security.digest(value),now)).fetchone()
            if not row:error("INVALID_SESSION",401)
            db.execute("UPDATE sessions SET revoked=1 WHERE id=?",(row["id"],))
            return self.mint(db,row["user_id"])

    def signout(self,owner,refresh):
        with self.db.transaction() as db:
            db.execute("UPDATE sessions SET revoked=1 WHERE user_id=? AND refresh_hash=?",(owner,security.digest(refresh)))
        return {"accepted":True}

    def change_password(self,owner,body):
        if not security.valid_password(body.newPassword):error("WEAK_PASSWORD")
        with self.db.transaction() as db:
            row=db.execute("SELECT password_hash FROM users WHERE id=?",(owner,)).fetchone()
            if not security.verify_password(body.currentPassword,row["password_hash"]):error("INVALID_CREDENTIALS",401)
            db.execute("UPDATE users SET password_hash=?,revision=revision+1,updated_at=? WHERE id=?",(security.hash_password(body.newPassword),time.time(),owner))
            db.execute("UPDATE sessions SET revoked=1 WHERE user_id=?",(owner,))
        return {"accepted":True}

    def seed_demo(self):
        if not self.settings.demo_accounts:return
        with self.db.transaction() as db:
            for number in range(1,4):
                username=f"student {number:02d}"
                if db.execute("SELECT 1 FROM users WHERE username=?",(username,)).fetchone():continue
                now=time.time()
                db.execute("INSERT INTO users(username,email,password_hash,nickname,grade,created_at,updated_at) VALUES(?,?,?,?,?,?,?)",
                           (username,f"student{number:02d}@demo.aimentor.invalid",security.hash_password("123123123"),f"Student {number:02d}",10,now,now))

