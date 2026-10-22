from datetime import datetime, timezone
import hashlib
import json
import unicodedata
from fastapi import HTTPException
from .database import dump

ALL_SUBJECTS=("math","literature","english","history","geography","chemistry","physics","biology","economics_law")
ACTIVE_SUBJECTS=ALL_SUBJECTS

def error(code: str,status=400):
    raise HTTPException(status,detail={"code":code})

def iso(value: float):
    return datetime.fromtimestamp(value,timezone.utc).isoformat().replace("+00:00","Z")

def check_revision(row,header):
    if header is None:
        error("REVISION_REQUIRED",428)
    try:
        expected=int(header.strip('"'))
    except (ValueError,TypeError):
        error("INVALID_REVISION")
    if row["revision"]!=expected:
        error("REVISION_CONFLICT",412)

def normalized(value):
    return "".join(c for c in unicodedata.normalize("NFD",value.casefold().replace("đ","d")) if not unicodedata.combining(c)).strip()

def idem(db,owner,operation,key,body,action):
    if not key or len(key)>128:
        error("IDEMPOTENCY_KEY_REQUIRED")
    fingerprint=hashlib.sha256(dump(body).encode()).hexdigest()
    cached=db.execute("SELECT request_hash,response FROM idempotency WHERE owner=? AND operation=? AND key=?",(owner,operation,key)).fetchone()
    if cached:
        if cached["request_hash"]!=fingerprint:
            error("IDEMPOTENCY_CONFLICT",409)
        return json.loads(cached["response"])
    result=action()
    db.execute("INSERT INTO idempotency VALUES(?,?,?,?,?)",(owner,operation,key,fingerprint,dump(result)))
    return result

def owned_resource(db,owner,kind,resource_id):
    row=db.execute("SELECT * FROM resources WHERE id=? AND kind=? AND user_id=? AND deleted_at IS NULL",(resource_id,kind,owner)).fetchone()
    if not row:
        error("RESOURCE_NOT_FOUND",404)
    return row

def resource(row):
    return {"id":row["id"],"revision":row["revision"],"updatedAt":iso(row["updated_at"]),**json.loads(row["body"])}

