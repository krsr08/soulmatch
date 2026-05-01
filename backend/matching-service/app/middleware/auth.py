from fastapi import Header, HTTPException
from jose import jwt, JWTError
import os
def get_current_user(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "): raise HTTPException(status_code=401, detail="Invalid authorization header")
    token = authorization.split(" ")[1]
    try: payload = jwt.decode(token, os.getenv("JWT_SECRET",""), algorithms=["HS256"]); return payload
    except JWTError: raise HTTPException(status_code=401, detail="Invalid or expired token")
