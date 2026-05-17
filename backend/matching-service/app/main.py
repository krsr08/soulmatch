from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import os
from app.routes.matches import router as matches_router
from app.routes.interests import router as interests_router
app = FastAPI(title="SoulMatch Matching Service", version="1.0.0")


def cors_origins():
    raw = os.getenv("CORS_ORIGINS") or os.getenv("ALLOWED_ORIGINS") or ""
    origins = [origin.strip() for origin in raw.split(",") if origin.strip()]
    if origins:
        return origins
    if os.getenv("ENVIRONMENT") != "production":
        return ["http://localhost:3000", "http://127.0.0.1:3000"]
    return []


app.add_middleware(CORSMiddleware, allow_origins=cors_origins(), allow_methods=["*"], allow_headers=["*"])
app.include_router(matches_router, prefix="/api/v1/matches")
app.include_router(interests_router, prefix="/api/v1/interests")


@app.exception_handler(ValueError)
async def handle_value_error(_, exc: ValueError):
    return JSONResponse(
        status_code=400,
        content={
            "success": False,
            "error": {
                "code": "VALIDATION_ERROR",
                "message": str(exc)
            }
        }
    )


@app.get("/health")
def health():
    return {"status": "ok", "service": "matching-service"}
