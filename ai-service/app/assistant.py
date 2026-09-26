"""'Ask your data' assistant.

Answers are always grounded in numbers computed by this service. If an LLM key is
configured (ANTHROPIC_API_KEY + AI_MODEL) the model writes the answer from that
numeric context only; otherwise a deterministic intent matcher answers directly, so
the feature works offline and in tests.
"""
from __future__ import annotations

import os
import re

import httpx
import pandas as pd


def build_context(df: pd.DataFrame, forecast: dict, anomalies: dict, risk: dict) -> dict:
    live = df[df["isCancelled"] == 0]
    today = pd.Timestamp.today().normalize()
    last90 = live[(live["checkInDate"] >= today - pd.Timedelta(days=90)) & (live["checkInDate"] < today)]
    by_hotel = last90.groupby("hotelName")["totalRevenue"].sum().sort_values(ascending=False)
    by_room = last90.groupby("roomType")["totalRevenue"].sum().sort_values(ascending=False)
    return {
        "totalBookings": int(len(df)),
        "cancellationRatePct": round(float(df["isCancelled"].mean() * 100), 1),
        "revenueLast90Days": round(float(last90["totalRevenue"].sum()), 2),
        "adrLast90Days": round(float(last90["totalRevenue"].sum() / max(1, last90["nights"].sum())), 2),
        "topHotelsLast90Days": {k: round(float(v), 2) for k, v in by_hotel.head(5).items()},
        "revenueByRoomTypeLast90Days": {k: round(float(v), 2) for k, v in by_room.items()},
        "forecastNext30DaysRevenue": round(sum(p["forecastRevenue"] for p in forecast["points"][:30]), 2),
        "forecastChangeVsLast30DaysPct": forecast.get("changeVsLast30DaysPct"),
        "forecastBacktestMAPE": forecast["backtest"]["modelMAPE"],
        "anomaliesFlagged": anomalies.get("count", 0),
        "topAnomalies": [a["reason"] + f" (booking #{a['bookingId']}, {a['hotelName']})"
                         for a in anomalies.get("items", [])[:3]],
        "upcomingBookings": risk.get("upcomingBookings", 0),
        "expectedRevenueAtRisk": risk.get("expectedRevenueAtRisk", 0),
        "highestRiskBookings": [f"#{b['bookingId']} {b['hotelName']} on {b['checkInDate']} "
                                f"({b['cancellationRisk']:.0%})" for b in risk.get("items", [])[:3]],
    }


def _inr(x: float) -> str:
    return f"₹{x:,.0f}"


def rule_answer(question: str, ctx: dict) -> str:
    q = question.lower()
    if re.search(r"forecast|next (month|30|week)|predict|expect|future", q):
        chg = ctx["forecastChangeVsLast30DaysPct"]
        trend = f", {abs(chg)}% {'up' if chg >= 0 else 'down'} on the last 30 days" if chg is not None else ""
        return (f"Revenue for the next 30 days is forecast at {_inr(ctx['forecastNext30DaysRevenue'])}{trend}. "
                f"In backtesting the model's average error was {ctx['forecastBacktestMAPE']}%.")
    if re.search(r"cancel|risk|no.?show", q):
        tops = "; ".join(ctx["highestRiskBookings"]) or "none"
        return (f"Overall cancellation rate is {ctx['cancellationRatePct']}%. Across {ctx['upcomingBookings']} upcoming "
                f"bookings, expected revenue at risk is {_inr(ctx['expectedRevenueAtRisk'])}. Highest risk: {tops}.")
    if re.search(r"anomal|unusual|fraud|suspicious|error|outlier", q):
        tops = "; ".join(ctx["topAnomalies"]) or "none"
        return f"{ctx['anomaliesFlagged']} bookings were flagged as unusual. Top findings: {tops}."
    if re.search(r"best|top|which hotel|highest", q):
        hotel, rev = next(iter(ctx["topHotelsLast90Days"].items()), ("n/a", 0))
        return f"{hotel} leads the last 90 days with {_inr(rev)} in revenue."
    if re.search(r"room|suite|deluxe", q):
        parts = ", ".join(f"{k.title()} {_inr(v)}" for k, v in ctx["revenueByRoomTypeLast90Days"].items())
        return f"Revenue by room type over the last 90 days: {parts}."
    if re.search(r"adr|average (daily )?rate|price", q):
        return f"Average daily rate over the last 90 days is {_inr(ctx['adrLast90Days'])}."
    return (f"Last 90 days: {_inr(ctx['revenueLast90Days'])} revenue at an ADR of {_inr(ctx['adrLast90Days'])}; "
            f"cancellation rate {ctx['cancellationRatePct']}%; next-30-day forecast "
            f"{_inr(ctx['forecastNext30DaysRevenue'])}. Try asking about forecasts, cancellations, anomalies or top hotels.")


def llm_answer(question: str, ctx: dict) -> str | None:
    key, model = os.getenv("ANTHROPIC_API_KEY"), os.getenv("AI_MODEL")
    if not key or not model:
        return None
    system = ("You are a hotel revenue analyst. Answer in at most 4 sentences using ONLY the JSON metrics "
              "provided. Use Indian rupees. If the metrics can't answer the question, say so.")
    try:
        resp = httpx.post(
            "https://api.anthropic.com/v1/messages",
            headers={"x-api-key": key, "anthropic-version": "2023-06-01", "content-type": "application/json"},
            json={"model": model, "max_tokens": 400, "system": system,
                  "messages": [{"role": "user", "content": f"Metrics: {ctx}\n\nQuestion: {question}"}]},
            timeout=20,
        )
        resp.raise_for_status()
        return "".join(b.get("text", "") for b in resp.json().get("content", [])).strip() or None
    except Exception:
        return None  # fall back to rules rather than failing the request


def answer(question: str, ctx: dict) -> dict:
    text = llm_answer(question, ctx)
    return {"answer": text, "engine": "llm"} if text else {"answer": rule_answer(question, ctx), "engine": "rules"}
