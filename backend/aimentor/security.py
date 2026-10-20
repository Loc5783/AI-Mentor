import base64
import hashlib
import hmac
import secrets

def hash_password(password: str) -> str:
    salt = secrets.token_bytes(16)
    value = hashlib.scrypt(password.encode("utf-8"), salt=salt, n=16384, r=8, p=1, dklen=32, maxmem=64*1024*1024)
    return "scrypt$"+base64.b64encode(salt).decode()+"$"+base64.b64encode(value).decode()

def verify_password(password: str, encoded: str) -> bool:
    try:
        scheme,salt,value=encoded.split("$")
        if scheme!="scrypt":
            return False
        actual=hashlib.scrypt(password.encode("utf-8"),salt=base64.b64decode(salt),n=16384,r=8,p=1,dklen=32,maxmem=64*1024*1024)
        return hmac.compare_digest(actual,base64.b64decode(value))
    except (ValueError,TypeError):
        return False

def token():
    return secrets.token_urlsafe(40)

def digest(value: str):
    return hashlib.sha256(value.encode("utf-8")).hexdigest()

def valid_password(value: str):
    return 8<=len(value) and len(value.encode("utf-8"))<=72 and any(c.isupper() for c in value) and any(c.isdigit() for c in value)

