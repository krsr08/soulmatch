from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, PlainTextResponse
import json
import time
import os
from app.routes.matches import router as matches_router
from app.routes.interests import router as interests_router
app = FastAPI(title="SoulMatch Matching Service", version="1.0.0")

REQUEST_COUNTS = {}
REQUEST_DURATIONS = {}
BUCKETS = [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10]
STARTED_AT = time.time()


def cors_origins():
    raw = os.getenv("CORS_ORIGINS") or os.getenv("ALLOWED_ORIGINS") or ""
    origins = [origin.strip() for origin in raw.split(",") if origin.strip()]
    if origins:
        return origins
    if os.getenv("ENVIRONMENT") != "production":
        return ["http://localhost:3000", "http://127.0.0.1:3000"]
    return []


app.add_middleware(CORSMiddleware, allow_origins=cors_origins(), allow_methods=["*"], allow_headers=["*"])


def metric_labels(**labels):
    return ",".join([f'{key}="{str(value).replace(chr(10), " ").replace(chr(34), chr(92) + chr(34))}"' for key, value in labels.items()])


@app.middleware("http")
async def observe_requests(request, call_next):
    started = time.perf_counter()
    response = await call_next(request)
    duration = time.perf_counter() - started
    route = getattr(request.scope.get("route"), "path", request.url.path)
    labels = {
        "service": "matching-service",
        "method": request.method,
        "route": route,
        "status": str(response.status_code),
        "status_class": f"{response.status_code // 100}xx"
    }
    key = tuple(labels.items())
    REQUEST_COUNTS[key] = REQUEST_COUNTS.get(key, 0) + 1
    metric = REQUEST_DURATIONS.get(key, {"count": 0, "sum": 0.0, "buckets": {bucket: 0 for bucket in BUCKETS}})
    metric["count"] += 1
    metric["sum"] += duration
    for bucket in BUCKETS:
        if duration <= bucket:
            metric["buckets"][bucket] += 1
    REQUEST_DURATIONS[key] = metric
    print(json.dumps({
        "level": "info",
        "type": "http_request",
        "service": "matching-service",
        "method": request.method,
        "route": route,
        "status": response.status_code,
        "durationMs": round(duration * 1000)
    }))
    return response


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


@app.get("/metrics")
def metrics():
    lines = [
        "# HELP soulmatch_http_requests_total Total HTTP requests by service, method, route, and status.",
        "# TYPE soulmatch_http_requests_total counter"
    ]
    for labels_tuple, count in REQUEST_COUNTS.items():
        labels = dict(labels_tuple)
        lines.append(f"soulmatch_http_requests_total{{{metric_labels(**labels)}}} {count}")
    lines.extend([
        "# HELP soulmatch_http_request_duration_seconds HTTP request duration histogram.",
        "# TYPE soulmatch_http_request_duration_seconds histogram"
    ])
    for labels_tuple, metric in REQUEST_DURATIONS.items():
        labels = dict(labels_tuple)
        for bucket in BUCKETS:
            lines.append(f"soulmatch_http_request_duration_seconds_bucket{{{metric_labels(**labels, le=bucket)}}} {metric['buckets'][bucket]}")
        lines.append(f"soulmatch_http_request_duration_seconds_bucket{{{metric_labels(**labels, le='+Inf')}}} {metric['count']}")
        lines.append(f"soulmatch_http_request_duration_seconds_sum{{{metric_labels(**labels)}}} {metric['sum']:.6f}")
        lines.append(f"soulmatch_http_request_duration_seconds_count{{{metric_labels(**labels)}}} {metric['count']}")
    lines.extend([
        "# HELP soulmatch_process_uptime_seconds Process uptime in seconds.",
        "# TYPE soulmatch_process_uptime_seconds gauge",
        f"soulmatch_process_uptime_seconds{{service=\"matching-service\"}} {int(time.time() - STARTED_AT)}"
    ])
    return PlainTextResponse("\n".join(lines) + "\n")
