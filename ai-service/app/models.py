"""The four AI engines: forecasting, anomaly detection, cancellation risk and rate recommendations."""
from __future__ import annotations

import warnings
from datetime import timedelta

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest, RandomForestClassifier
from sklearn.metrics import roc_auc_score, precision_score, recall_score
from sklearn.model_selection import train_test_split

from .data import daily_series


def _mape(actual: np.ndarray, pred: np.ndarray) -> float:
    mask = actual > 0
    if not mask.any():
        return float("nan")
    return float(np.mean(np.abs((actual[mask] - pred[mask]) / actual[mask])) * 100)


# --------------------------------------------------------------------------- forecasting
def _stay_nights(df: pd.DataFrame) -> pd.DataFrame:
    """One row per occupied room-night of every non-cancelled booking, with when it was booked."""
    live = df[df["isCancelled"] == 0]
    nights = live["nights"].to_numpy()
    idx = np.repeat(np.arange(len(live)), nights)
    offsets = np.concatenate([np.arange(n) for n in nights]) if len(nights) else np.array([], dtype=int)
    day = pd.to_datetime(live["checkInDate"].to_numpy()[idx]) + pd.to_timedelta(offsets, unit="D")
    created = pd.to_datetime(live["createdAt"].to_numpy()[idx]).normalize()
    return pd.DataFrame({
        "day": day,
        "created": created,
        "revenue": (live["totalRevenue"] / live["nights"]).to_numpy()[idx],
        "rooms": 1.0,
    })


def _holt_winters(y: pd.Series, horizon: int) -> tuple[np.ndarray, float]:
    from statsmodels.tsa.holtwinters import ExponentialSmoothing
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        fit = ExponentialSmoothing(y, trend="add", damped_trend=True, seasonal="add", seasonal_periods=7,
                                   initialization_method="estimated").fit()
    return np.clip(np.asarray(fit.forecast(horizon)), 0, None), float(np.std(fit.resid))


def _forecast_at(nights: pd.DataFrame, cutoff: pd.Timestamp, horizon: int, col: str) -> dict:
    """Forecast `col` for `horizon` days from `cutoff`, using only what was known at `cutoff`.

    1. Holt-Winters on realised history (captures weekly pattern and trend).
    2. Pickup model: at k days out, a hotel has typically booked R_k of a night's final
       revenue, so on-the-books / R_k estimates the final figure.
    The two are blended with weight R_k: close-in dates trust the bookings already made,
    far-out dates trust the time-series model.
    """
    past = nights[nights["day"] < cutoff]
    full = pd.date_range(past["day"].min(), cutoff - pd.Timedelta(days=1), freq="D")
    hist = past.groupby("day")[col].sum().reindex(full, fill_value=0.0).iloc[-365:]
    if len(hist) < 56:
        raise ValueError("Need at least 8 weeks of history to forecast.")
    hw, resid_sd = _holt_winters(hist, horizon)

    # Pickup curve from the last 180 fully-realised days
    window = past[past["day"] >= cutoff - pd.Timedelta(days=180)]
    lead = (window["day"] - window["created"]).dt.days.clip(lower=0).to_numpy()
    values = window[col].to_numpy()
    total = values.sum() or 1.0
    booked_share = np.array([values[lead >= k].sum() / total for k in range(horizon)])

    future = nights[(nights["day"] >= cutoff) & (nights["day"] < cutoff + pd.Timedelta(days=horizon))
                    & (nights["created"] <= cutoff)]
    days = pd.date_range(cutoff, periods=horizon, freq="D")
    on_books = future.groupby("day")[col].sum().reindex(days, fill_value=0.0).to_numpy()
    k = np.arange(horizon)
    share = booked_share[k]
    pickup = np.where(share > 0.05, on_books / np.maximum(share, 0.05), hw)
    blended = np.maximum(share * pickup + (1 - share) * hw, on_books)
    return {"days": days, "forecast": blended, "hw": hw, "onBooks": on_books, "share": share, "residSd": resid_sd}


def forecast_revenue(df: pd.DataFrame, horizon: int = 30, today: pd.Timestamp | None = None) -> dict:
    """Blended Holt-Winters + booking-pickup forecast with an honest 28-day backtest.

    The backtest re-runs the whole method as if it were 28 days ago (only bookings made
    by then are visible) and compares against what actually happened, alongside the plain
    Holt-Winters model and a seasonal-naive baseline (same weekday last week).
    """
    today = (today or pd.Timestamp.today()).normalize()
    nights = _stay_nights(df)

    cutoff = today - pd.Timedelta(days=28)
    bt = _forecast_at(nights, cutoff, 28, "revenue")
    realised = nights[(nights["day"] >= cutoff) & (nights["day"] < today)]
    actual = realised.groupby("day")["revenue"].sum().reindex(bt["days"], fill_value=0.0).to_numpy()
    hist_all = nights[nights["day"] < today].groupby("day")["revenue"].sum()
    naive = hist_all.reindex(bt["days"] - pd.Timedelta(days=7), fill_value=0.0).to_numpy()
    backtest = {
        "days": 28,
        "modelMAPE": round(_mape(actual, bt["forecast"]), 2),
        "holtWintersOnlyMAPE": round(_mape(actual, bt["hw"]), 2),
        "seasonalNaiveMAPE": round(_mape(actual, naive), 2),
    }

    rev = _forecast_at(nights, today, horizon, "revenue")
    rooms = _forecast_at(nights, today, horizon, "rooms")
    band = 1.96 * rev["residSd"] * (1 - rev["share"])  # uncertainty shrinks as bookings firm up
    points = [{
        "date": d.date().isoformat(),
        "forecastRevenue": round(float(f), 2),
        "lower": round(float(max(on, f - b)), 2),
        "upper": round(float(f + b), 2),
        "forecastRoomsSold": round(float(r), 1),
        "onTheBooksRevenue": round(float(on), 2),
        "onTheBooksRoomsSold": int(ro),
        "bookedSharePct": round(float(sh) * 100, 1),
    } for d, f, b, r, on, ro, sh in zip(rev["days"], rev["forecast"], band, rooms["forecast"],
                                         rev["onBooks"], rooms["onBooks"], rev["share"])]

    last_30 = float(hist_all.iloc[-30:].sum())
    next_30 = float(rev["forecast"][:30].sum())
    return {
        "model": "Holt-Winters (weekly seasonality) blended with a booking-pickup model",
        "horizonDays": horizon,
        "points": points,
        "totalForecastRevenue": round(float(rev["forecast"].sum()), 2),
        "changeVsLast30DaysPct": round((next_30 - last_30) / last_30 * 100, 1) if last_30 else None,
        "backtest": backtest,
    }


# --------------------------------------------------------------------------- anomalies
Z_THRESHOLD = 6.0
# Maximum guests per room type: a hard business rule, checked like an application control.
ROOM_CAPACITY = {"SINGLE": 2, "DOUBLE": 3, "DELUXE": 3, "SUITE": 4, "PRESIDENTIAL": 6}
_ANOMALY_LABELS = {"adr": "nightly rate", "guests": "guest count", "nights": "length of stay",
                   "leadTime": "booking lead time"}


def detect_anomalies(df: pd.DataFrame, top: int = 15, contamination: float = 0.01) -> dict:
    """Isolation Forest over peer-normalised booking features, with a plain-language reason.

    Each feature is converted to a robust z-score (median / MAD) *within the same hotel and
    room type*, so a 6-guest single room or a rate 9x the suite median stands out even though
    those raw values would be normal for another room type. The reason shown is the feature
    with the largest deviation, which is what an auditor would check first.
    """
    live = df[df["isCancelled"] == 0].copy()
    if len(live) < 50:
        return {"model": "IsolationForest", "count": 0, "items": [], "note": "Not enough bookings"}
    live["logAdr"] = np.log1p(live["adr"])
    peers = live.groupby(["hotelName", "roomType"])
    zcols = []
    for col, src in [("adr", "logAdr"), ("guests", "guests"), ("nights", "nights"), ("leadTime", "leadTime")]:
        med = peers[src].transform("median")
        # MAD is 0 when most peers share one value (e.g. single rooms nearly always have 1 guest);
        # fall back to the standard deviation, then to 1, so real outliers are still measurable.
        scale = (1.4826 * peers[src].transform(lambda s: (s - s.median()).abs().median())).replace(0, np.nan)
        scale = scale.fillna(peers[src].transform("std")).replace(0, np.nan).fillna(1.0)
        live[f"z_{col}"] = ((live[src] - med) / scale).fillna(0.0).clip(-50, 50)
        live[f"med_{col}"] = peers[col].transform("median")
        zcols.append(f"z_{col}")

    # Isolation Forest catches unusual *combinations*; it can miss a single extreme value that
    # never appears in its 256-row training subsamples, so a robust-z rule (|z| >= 6) backs it up.
    forest = IsolationForest(n_estimators=300, contamination=contamination, random_state=7)
    X = live[zcols].to_numpy(dtype=float)
    iso = -forest.fit(X).score_samples(X)
    max_z = live[zcols].abs().max(axis=1)
    live["score"] = np.maximum(iso, np.minimum(max_z / 10, 5.0))
    over_capacity = live["guests"] > live["roomType"].map(ROOM_CAPACITY).fillna(99)
    live.loc[over_capacity, "score"] = 5.0
    live["flag"] = (forest.predict(X) == -1) | (max_z >= Z_THRESHOLD) | over_capacity

    items = []
    for _, r in live[live["flag"]].sort_values("score", ascending=False).head(top).iterrows():
        worst = max(_ANOMALY_LABELS, key=lambda c: abs(r[f"z_{c}"]))
        direction = "above" if r[f"z_{worst}"] > 0 else "below"
        value, median = float(r[worst]), float(r[f"med_{worst}"])
        reason = (f"{_ANOMALY_LABELS[worst].capitalize()} of {value:,.0f} is well {direction} the "
                  f"{str(r['roomType']).lower()} median of {median:,.0f} at this hotel")
        capacity = ROOM_CAPACITY.get(str(r["roomType"]))
        if capacity and r["guests"] > capacity:
            worst = "guests"
            reason = f"{int(r['guests'])} guests exceeds the {str(r['roomType']).lower()} room capacity of {capacity}"
        items.append({
            "bookingId": int(r["id"]),
            "hotelName": str(r["hotelName"]),
            "guestName": str(r["guestName"]),
            "checkInDate": r["checkInDate"].date().isoformat(),
            "roomType": str(r["roomType"]),
            "totalRevenue": round(float(r["totalRevenue"]), 2),
            "anomalyScore": round(float(r["score"]), 3),
            "field": worst,
            "reason": reason,
        })
    return {"model": "IsolationForest (300 trees) + robust z-score and room-capacity rules",
            "count": int(live["flag"].sum()), "items": items}


# --------------------------------------------------------------------------- cancellation risk
CANCEL_FEATURES = ["leadTime", "nights", "guests", "adr", "roomTypeCode", "checkInWeekday", "checkInMonth"]


def cancellation_risk(df: pd.DataFrame, today: pd.Timestamp | None = None, top: int = 20) -> dict:
    """Random Forest trained on resolved bookings; scores upcoming CONFIRMED bookings."""
    today = (today or pd.Timestamp.today()).normalize()
    resolved = df[(df["checkInDate"] < today) | (df["bookingStatus"] == "CANCELLED")]
    if resolved["isCancelled"].nunique() < 2 or len(resolved) < 200:
        return {"model": "RandomForestClassifier", "items": [], "note": "Not enough labelled history"}

    X, y = resolved[CANCEL_FEATURES], resolved["isCancelled"]
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.25, random_state=11, stratify=y)
    clf = RandomForestClassifier(n_estimators=250, max_depth=8, min_samples_leaf=20,
                                 class_weight="balanced", random_state=11, n_jobs=-1)
    clf.fit(X_tr, y_tr)
    proba_te = clf.predict_proba(X_te)[:, 1]
    pred_te = (proba_te >= 0.5).astype(int)
    metrics = {
        "rocAuc": round(float(roc_auc_score(y_te, proba_te)), 3),
        "precision": round(float(precision_score(y_te, pred_te, zero_division=0)), 3),
        "recall": round(float(recall_score(y_te, pred_te, zero_division=0)), 3),
        "baseCancellationRatePct": round(float(y.mean() * 100), 1),
        "testSize": int(len(y_te)),
    }
    importances = sorted(zip(CANCEL_FEATURES, clf.feature_importances_), key=lambda t: -t[1])

    upcoming = df[(df["bookingStatus"] == "CONFIRMED") & (df["checkInDate"] >= today)].copy()
    items = []
    if not upcoming.empty:
        upcoming["risk"] = clf.predict_proba(upcoming[CANCEL_FEATURES])[:, 1]
        for _, r in upcoming.sort_values("risk", ascending=False).head(top).iterrows():
            items.append({
                "bookingId": int(r["id"]), "hotelName": str(r["hotelName"]), "guestName": str(r["guestName"]),
                "checkInDate": r["checkInDate"].date().isoformat(), "roomType": str(r["roomType"]),
                "leadTimeDays": int(r["leadTime"]), "totalRevenue": round(float(r["totalRevenue"]), 2),
                "cancellationRisk": round(float(r["risk"]), 3),
            })
        revenue_at_risk = float((upcoming["risk"] * upcoming["totalRevenue"]).sum())
    else:
        revenue_at_risk = 0.0
    return {
        "model": "RandomForestClassifier (250 trees, class-balanced)",
        "metrics": metrics,
        "featureImportance": [{"feature": f, "importance": round(float(v), 3)} for f, v in importances],
        "upcomingBookings": int(len(upcoming)),
        "expectedRevenueAtRisk": round(revenue_at_risk, 2),
        "items": items,
    }


# --------------------------------------------------------------------------- pricing
def rate_recommendations(df: pd.DataFrame, forecast: dict, days: int = 14,
                         today: pd.Timestamp | None = None) -> dict:
    """Demand-based rate suggestions per room type.

    demand index = forecast rooms sold for the day / average rooms sold on the same weekday
    over the last 8 weeks. The recommended ADR scales the recent median ADR by that index,
    clamped to +-15/25% so suggestions stay within what a revenue manager would accept.
    """
    today = (today or pd.Timestamp.today()).normalize()
    series = daily_series(df).loc[: today - pd.Timedelta(days=1)].iloc[-56:]
    weekday_avg = series.groupby(series.index.weekday)["roomsSold"].mean()
    recent = df[(df["isCancelled"] == 0) & (df["checkInDate"] >= today - pd.Timedelta(days=90))
                & (df["checkInDate"] < today)]
    base_adr = recent.groupby("roomType")["adr"].median()

    out = []
    for p in forecast["points"][:days]:
        d = pd.Timestamp(p["date"])
        baseline = weekday_avg.get(d.weekday(), np.nan)
        # Rooms already booked are a floor on demand: never price below confirmed pickup.
        expected_rooms = max(p["forecastRoomsSold"], p.get("onTheBooksRoomsSold", 0))
        index = float(expected_rooms / baseline) if baseline and baseline > 0 else 1.0
        multiplier = float(np.clip(1 + 0.6 * (index - 1), 0.85, 1.25))
        out.append({
            "date": p["date"],
            "demandIndex": round(index, 2),
            "action": "raise" if multiplier > 1.03 else "discount" if multiplier < 0.97 else "hold",
            "rates": {str(rt): round(float(adr * multiplier), -1) for rt, adr in base_adr.items()},
        })
    return {"method": "Demand index from forecast vs. same-weekday baseline", "baseAdr":
            {str(k): round(float(v), -1) for k, v in base_adr.items()}, "days": out}
