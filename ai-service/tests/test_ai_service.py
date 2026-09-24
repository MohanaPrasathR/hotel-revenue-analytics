import os

os.environ["DATA_SOURCE"] = "demo"

import pandas as pd
import pytest
from fastapi.testclient import TestClient

from app import assistant, models
from app.data import daily_series, prepare
from app.demo_data import generate_bookings
from app.main import app


@pytest.fixture(scope="module")
def df():
    return prepare(generate_bookings(days=500, seed=3))


@pytest.fixture(scope="module")
def client():
    return TestClient(app)


def test_prepare_derives_features(df):
    assert {"nights", "adr", "leadTime", "isCancelled"} <= set(df.columns)
    assert (df["nights"] >= 1).all()
    row = df.iloc[0]
    assert row["adr"] == pytest.approx(row["totalRevenue"] / row["nights"])


def test_daily_series_excludes_cancelled_and_spreads_nights():
    raw = pd.DataFrame([
        {"id": 1, "hotelName": "H", "guestName": "A", "checkInDate": "2026-01-01", "checkOutDate": "2026-01-03",
         "guests": 1, "roomType": "DOUBLE", "bookingStatus": "CHECKED_OUT", "totalRevenue": 200.0,
         "createdAt": "2025-12-01T10:00:00"},
        {"id": 2, "hotelName": "H", "guestName": "B", "checkInDate": "2026-01-01", "checkOutDate": "2026-01-02",
         "guests": 1, "roomType": "DOUBLE", "bookingStatus": "CANCELLED", "totalRevenue": 999.0,
         "createdAt": "2025-12-01T10:00:00"},
    ])
    s = daily_series(prepare(raw))
    assert list(s["revenue"]) == [100.0, 100.0]
    assert list(s["roomsSold"]) == [1, 1]


def test_forecast_beats_or_matches_seasonal_naive(df):
    f = models.forecast_revenue(df, horizon=30)
    assert len(f["points"]) == 30
    assert all(p["lower"] <= p["forecastRevenue"] <= p["upper"] for p in f["points"])
    assert f["backtest"]["modelMAPE"] <= f["backtest"]["seasonalNaiveMAPE"] * 1.15


def test_forecast_needs_history():
    tiny = prepare(generate_bookings(days=30, seed=1))
    with pytest.raises(ValueError):
        models.forecast_revenue(tiny, today=tiny["checkInDate"].max())


def test_anomalies_find_injected_errors(df):
    a = models.detect_anomalies(df, top=10)
    reasons = " ".join(i["reason"] for i in a["items"])
    assert "exceeds the single room capacity" in reasons
    assert "Nightly rate" in reasons


def test_cancellation_model_learns_signal(df):
    r = models.cancellation_risk(df)
    assert r["metrics"]["rocAuc"] > 0.6
    assert r["featureImportance"][0]["feature"] == "leadTime"
    risks = [i["cancellationRisk"] for i in r["items"]]
    assert risks == sorted(risks, reverse=True)


def test_rate_recommendations_are_clamped(df):
    f = models.forecast_revenue(df)
    p = models.rate_recommendations(df, f)
    for day in p["days"]:
        for room, rate in day["rates"].items():
            assert 0.8 * p["baseAdr"][room] <= rate <= 1.3 * p["baseAdr"][room]


@pytest.mark.parametrize("question,expected", [
    ("What is the revenue forecast for next month?", "forecast"),
    ("Which bookings are likely to cancel?", "cancellation rate"),
    ("Show me anomalies", "flagged"),
    ("Which hotel is the best?", "leads the last 90 days"),
])
def test_rule_assistant_routes_intents(df, question, expected):
    f = models.forecast_revenue(df)
    ctx = assistant.build_context(df, f, models.detect_anomalies(df), models.cancellation_risk(df))
    out = assistant.answer(question, ctx)
    assert out["engine"] == "rules"
    assert expected in out["answer"]


def test_api_endpoints(client):
    assert client.get("/health").json() == {"status": "UP"}
    ov = client.get("/api/ai/overview").json()
    assert ov["meta"]["dataSource"] == "demo"
    assert len(client.get("/api/ai/forecast?days=14").json()["points"]) == 14
    assert client.get("/api/ai/anomalies?limit=5").status_code == 200
    assert client.get("/api/ai/cancellation-risk").status_code == 200
    assert len(client.get("/api/ai/pricing").json()["days"]) == 14
    ans = client.post("/api/ai/ask", json={"question": "forecast next month"}).json()
    assert "forecast" in ans["answer"].lower()


def test_ask_validates_input(client):
    assert client.post("/api/ai/ask", json={"question": "?"}).status_code == 422
