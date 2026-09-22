
import os
import hashlib
from datetime import datetime, timedelta
from typing import Optional
from jose import JWTError, jwt
import bcrypt

# Configuration
SECRET_KEY = os.environ.get("JWT_SECRET_KEY", "your-super-secret-key-change-this-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24 * 7  # 1 week

PASSWORD_HASH_PREFIX = "sha256-bcrypt$"


def _password_digest(password: str) -> bytes:
    return hashlib.sha256(password.encode("utf-8")).digest()


def _normalize_passlib_bcrypt_sha256(hash_value: str) -> str | None:
    try:
        _, scheme, params, salt, checksum = hash_value.split("$", 4)
    except ValueError:
        return None

    if scheme != "bcrypt-sha256":
        return None

    options = {}
    for pair in params.split(","):
        if "=" not in pair:
            continue
        key, value = pair.split("=", 1)
        options[key] = value

    ident = options.get("t")
    rounds = options.get("r")
    if not ident or not rounds:
        return None

    try:
        cost = int(rounds)
    except ValueError:
        return None

    return f"${ident}${cost:02d}${salt}{checksum}"

def verify_password(plain_password, hashed_password):
    password_bytes = _password_digest(plain_password)

    if hashed_password.startswith(PASSWORD_HASH_PREFIX):
        return bcrypt.checkpw(password_bytes, hashed_password[len(PASSWORD_HASH_PREFIX):].encode("utf-8"))

    if hashed_password.startswith("$bcrypt-sha256$"):
        normalized_hash = _normalize_passlib_bcrypt_sha256(hashed_password)
        if normalized_hash:
            return bcrypt.checkpw(password_bytes, normalized_hash.encode("utf-8"))

    try:
        return bcrypt.checkpw(plain_password.encode("utf-8"), hashed_password.encode("utf-8"))
    except ValueError:
        return False

def get_password_hash(password):
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(_password_digest(password), salt).decode("utf-8")
    return f"{PASSWORD_HASH_PREFIX}{hashed}"

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=15)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def decode_access_token(token: str):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except JWTError:
        return None


 