"""Loads bookings from the Spring Boot API and derives model-ready features."""
from __future__ import annotations

import logging
import os
from dataclasses import dataclass
from datetime import datetime, timezone

import httpx
import numpy as np
import pandas as pd

from .demo_data import generate_bookings

log = logging.getLogger(__name__)

BOOKINGS_API_URL = os.getenv("BOOKINGS_API_URL", "http://localhost:8080/api/bookings")
DATA_SOURCE = os.getenv("DATA_SOURCE", "auto").lower()  # auto | api | demo
MIN_ROWS_FOR_ML = int(os.getenv("MIN_ROWS_FOR_ML", "300"))
ROOM_TYPES = ["SINGLE", "DOUBLE", "DELUXE", "SUITE", "PRESIDENTIAL"]


@dataclass
class Dataset:
    bookings: pd.DataFrame
    source: str          # "api" or "demo"
    loaded_at: datetime
    note: str = ""


def fetch_from_api(timeout: float = 5.0) -> pd.DataFrame:
    resp = httpx.get(BOOKINGS_API_URL, timeout=timeout)
    resp.raise_for_status()
    payload = resp.json()
    if isinstance(payload, dict):  # tolerate paged or wrapped responses
        payload = payload.get("content") or payload.get("data") or []
    return pd.DataFrame(payload)


def load_dataset() -> Dataset:
    now = datetime.now(timezone.utc)
    if DATA_SOURCE == "demo":
        return Dataset(prepare(generate_bookings()), "demo", now, "DATA_SOURCE=demo")
    try:
        raw = fetch_from_api()
        if DATA_SOURCE == "api" or len(raw) >= MIN_ROWS_FOR_ML:
            return Dataset(prepare(raw), "api", now, f"{len(raw)} bookings from {BOOKINGS_API_URL}")
        note = (f"Live API returned {len(raw)} bookings (< {MIN_ROWS_FOR_ML} needed for reliable models); "
                "using synthetic demo history instead.")
    except Exception as exc:  # API down: stay useful in demo mode
        if DATA_SOURCE == "api":
            raise
        note = f"Bookings API unreachable ({exc.__class__.__name__}); using synthetic demo history."
    log.warning(note)
    return Dataset(prepare(generate_bookings()), "demo", now, note)


def prepare(raw: pd.DataFrame) -> pd.DataFrame:
    """Normalise types and add derived columns used across models."""
    df = raw.copy()
    df["checkInDate"] = pd.to_datetime(df["checkInDate"])
    df["checkOutDate"] = pd.to_datetime(df["checkOutDate"])
    df["createdAt"] = pd.to_datetime(df["createdAt"], errors="coerce").dt.tz_localize(None)
    df["createdAt"] = df["createdAt"].fillna(df["checkInDate"])
    df["totalRevenue"] = pd.to_numeric(df["totalRevenue"], errors="coerce").fillna(0.0)
    df["guests"] = pd.to_numeric(df["guests"], errors="coerce").fillna(1).astype(int)
    df["nights"] = (df["checkOutDate"] - df["checkInDate"]).dt.days.clip(lower=1)
    df["adr"] = df["totalRevenue"] / df["nights"]
    df["leadTime"] = (df["checkInDate"] - df["createdAt"].dt.normalize()).dt.days.clip(lower=0)
    df["checkInWeekday"] = df["checkInDate"].dt.weekday
    df["checkInMonth"] = df["checkInDate"].dt.month
    df["isCancelled"] = (df["bookingStatus"] == "CANCELLED").astype(int)
    df["roomTypeCode"] = df["roomType"].map({r: i for i, r in enumerate(ROOM_TYPES)}).fillna(1).astype(int)
    return df.sort_values("checkInDate").reset_index(drop=True)


def daily_series(df: pd.DataFrame) -> pd.DataFrame:
    """Spread each non-cancelled booking across its nights -> daily revenue and rooms sold."""
    live = df[df["isCancelled"] == 0]
    if live.empty:
        return pd.DataFrame(columns=["revenue", "roomsSold"])
    nights = live["nights"].to_numpy()
    starts = live["checkInDate"].to_numpy()
    idx = np.repeat(np.arange(len(live)), nights)
    offsets = np.concatenate([np.arange(n) for n in nights])
    days = pd.to_datetime(starts[idx]) + pd.to_timedelta(offsets, unit="D")
    per_night = (live["totalRevenue"] / live["nights"]).to_numpy()[idx]
    daily = pd.DataFrame({"day": days, "revenue": per_night, "roomsSold": 1})
    out = daily.groupby("day").agg(revenue=("revenue", "sum"), roomsSold=("roomsSold", "sum"))
    full = pd.date_range(out.index.min(), out.index.max(), freq="D")
    return out.reindex(full, fill_value=0.0)
