"""StayPulse AI service: forecasting, anomaly detection, cancellation risk,
rate recommendations and a natural-language assistant over hotel bookings."""
from __future__ import annotations

import os
import threading
import time

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from . import assistant, models
from .data import Dataset, load_dataset

CACHE_TTL_SECONDS = int(os.getenv("CACHE_TTL_SECONDS", "600"))

app = FastAPI(
    title="StayPulse AI Service",
    version="1.0.0",
    description="Machine-learning insights for the Hotel Revenue Analytics platform.",
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("CORS_ALLOWED_ORIGINS", "*").split(","),
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


class _Cache:
    """Loads data and trains models once per TTL; results are shared by all endpoints."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._at = 0.0
        self.data: Dataset | None = None
        self.results: dict = {}

    def get(self, force: bool = False) -> tuple[Dataset, dict]:
        with self._lock:
            if force or self.data is None or time.time() - self._at > CACHE_TTL_SECONDS:
                data = load_dataset()
                df = data.bookings
                forecast = models.forecast_revenue(df, horizon=30)
                anomalies = models.detect_anomalies(df)
                risk = models.cancellation_risk(df)
                pricing = models.rate_recommendations(df, forecast)
                self.data, self._at = data, time.time()
                self.results = {"forecast": forecast, "anomalies": anomalies, "risk": risk, "pricing": pricing,
                                "context": assistant.build_context(df, forecast, anomalies, risk)}
            return self.data, self.results


cache = _Cache()


def _meta(data: Dataset) -> dict:
    return {"dataSource": data.source, "note": data.note, "loadedAt": data.loaded_at.isoformat(),
            "bookings": int(len(data.bookings))}


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.get("/api/ai/overview")
def overview() -> dict:
    data, r = cache.get()
    return {"meta": _meta(data), "summary": r["context"],
            "forecastModel": r["forecast"]["model"], "backtest": r["forecast"]["backtest"],
            "cancellationModelMetrics": r["risk"].get("metrics")}


@app.get("/api/ai/forecast")
def forecast(days: int = Query(30, ge=7, le=30)) -> dict:
    data, r = cache.get()
    f = dict(r["forecast"])
    f["points"] = f["points"][:days]
    return {"meta": _meta(data), **f}


@app.get("/api/ai/anomalies")
def anomalies(limit: int = Query(15, ge=1, le=50)) -> dict:
    data, r = cache.get()
    a = dict(r["anomalies"])
    a["items"] = a["items"][:limit]
    return {"meta": _meta(data), **a}


@app.get("/api/ai/cancellation-risk")
def cancellation_risk(limit: int = Query(20, ge=1, le=50)) -> dict:
    data, r = cache.get()
    c = dict(r["risk"])
    c["items"] = c.get("items", [])[:limit]
    return {"meta": _meta(data), **c}


@app.get("/api/ai/pricing")
def pricing() -> dict:
    data, r = cache.get()
    return {"meta": _meta(data), **r["pricing"]}


class AskRequest(BaseModel):
    question: str = Field(..., min_length=3, max_length=500)


@app.post("/api/ai/ask")
def ask(body: AskRequest) -> dict:
    _, r = cache.get()
    return assistant.answer(body.question, r["context"])


@app.post("/api/ai/retrain")
def retrain() -> dict:
    try:
        data, _ = cache.get(force=True)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return {"status": "retrained", "meta": _meta(data)}
