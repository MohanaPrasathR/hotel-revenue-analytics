# StayPulse AI Service

Python (FastAPI + scikit-learn + statsmodels) microservice that adds machine-learning insights to the
StayPulse hotel revenue platform. It reads bookings from the Spring Boot API (`GET /api/bookings`),
trains its models in memory, and serves the results to the Angular **AI Insights** page.

| Endpoint | What it does | Model |
| --- | --- | --- |
| `GET /api/ai/forecast?days=30` | Daily revenue and rooms-sold forecast with a 95% band and what is already booked | Holt-Winters (weekly seasonality) blended with a booking-pickup model |
| `GET /api/ai/anomalies` | Bookings worth a manual review, each with a plain-English reason | Isolation Forest + robust z-score + room-capacity rules |
| `GET /api/ai/cancellation-risk` | Upcoming bookings most likely to cancel, and expected revenue at risk | Random Forest classifier (class-balanced) |
| `GET /api/ai/pricing` | Recommended nightly rate per room type for the next 14 days | Demand index = expected rooms ÷ same-weekday baseline, clamped to −15% / +25% |
| `POST /api/ai/ask` | "Ask your data" questions in plain English | Rule-based intent engine; optional LLM that answers only from the computed metrics |
| `GET /api/ai/overview` | Summary metrics and model quality | — |
| `POST /api/ai/retrain` | Reload data and retrain now (otherwise every 10 minutes) | — |

## How the models are evaluated

The forecast is re-run **as if it were 28 days ago**: only the bookings made by then are visible.
It's then compared with what actually happened. On the demo dataset:

| Method | MAPE (lower is better) |
| --- | --- |
| Seasonal naive (same weekday last week) | ~13.5% |
| Holt-Winters only | ~10.4% |
| **Holt-Winters + pickup blend (used)** | **~2.6%** |

The cancellation model reports ROC-AUC, precision and recall on a stratified 25% hold-out set
(~0.68 ROC-AUC on demo data, with lead time the strongest signal, as in published hotel
cancellation studies).

> **About the data.** The Spring seeder creates only ~23 bookings, which is too few to train a model.
> Until the API has `MIN_ROWS_FOR_ML` (300) bookings, the service uses a **synthetic** two-year
> booking history (`app/demo_data.py`) and the dashboard shows a "Demo data" banner. The generator
> models weekend and festive-season demand and lead-time-driven cancellations, and it plants a few
> data-entry errors for the anomaly detector to find. Synthetic data is cleaner than real data, so
> expect real-world accuracy to be lower than these figures.

## Run it

```bash
cd ai-service
python -m venv .venv && source .venv/bin/activate      # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000              # docs at http://localhost:8000/docs
```

| Variable | Default | Purpose |
| --- | --- | --- |
| `BOOKINGS_API_URL` | `http://localhost:8080/api/bookings` | Spring Boot bookings endpoint |
| `DATA_SOURCE` | `auto` | `auto` (API, else demo), `api` (fail if API is down), `demo` |
| `MIN_ROWS_FOR_ML` | `300` | Bookings needed before live data replaces demo data |
| `CACHE_TTL_SECONDS` | `600` | How long trained models are reused |
| `ANTHROPIC_API_KEY`, `AI_MODEL` | unset | Optional: LLM answers in "Ask your data" |

## Tests

```bash
pytest -q        # 13 tests: feature engineering, each model, API contract, input validation
```
