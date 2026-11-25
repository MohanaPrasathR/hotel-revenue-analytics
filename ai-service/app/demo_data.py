"""Synthetic booking history for demo and test runs.

The Spring Boot seeder only creates ~23 bookings, which is far too little to
train or evaluate any model. When the live API has fewer rows than
``MIN_ROWS_FOR_ML`` the service falls back to this generator so every AI
feature can be demonstrated. The data is SYNTHETIC: patterns (weekend peaks,
festive-season demand, lead-time driven cancellations) are modelled on common
hotel industry behaviour, and a handful of anomalies are injected on purpose
so the anomaly detector has something real to find.
"""
from __future__ import annotations

from datetime import date, datetime, timedelta

import numpy as np
import pandas as pd

HOTELS = {
    # name: (rooms, base ADR in INR)
    "Grand Horizon Resort": (60, 7200.0),
    "Azure Bay Suites": (45, 9800.0),
    "Starlight Palace Hotel": (80, 5600.0),
    "Mountain Crest Lodge": (30, 4800.0),
}
ROOM_MULTIPLIER = {"SINGLE": 0.75, "DOUBLE": 1.0, "DELUXE": 1.35, "SUITE": 1.9, "PRESIDENTIAL": 3.4}
ROOM_WEIGHTS = {"SINGLE": 0.22, "DOUBLE": 0.38, "DELUXE": 0.24, "SUITE": 0.13, "PRESIDENTIAL": 0.03}
FIRST_NAMES = ["Aarav", "Diya", "Rohan", "Isha", "Karthik", "Meera", "Arjun", "Sneha", "Vikram", "Ananya",
               "Rahul", "Priya", "Nikhil", "Kavya", "Siddharth", "Lakshmi", "Farhan", "Neha", "Aditya", "Pooja"]
LAST_NAMES = ["Sharma", "Iyer", "Reddy", "Nair", "Gupta", "Menon", "Rao", "Khan", "Patel", "Das"]


def _season_factor(d: date) -> float:
    """Demand multiplier: Indian holiday season (Oct-Jan) and summer (May-Jun) peak."""
    month_factor = {1: 1.15, 2: 0.9, 3: 0.85, 4: 0.95, 5: 1.2, 6: 1.1, 7: 0.8,
                    8: 0.85, 9: 0.9, 10: 1.1, 11: 1.2, 12: 1.35}[d.month]
    weekend = 1.3 if d.weekday() >= 4 else 1.0  # Fri/Sat/Sun
    return month_factor * weekend


def generate_bookings(days: int = 730, end: date | None = None, seed: int = 42) -> pd.DataFrame:
    """Return a bookings DataFrame shaped like the Spring API's BookingResponse."""
    rng = np.random.default_rng(seed)
    end = end or date.today() + timedelta(days=45)
    start = end - timedelta(days=days)
    rows = []
    booking_id = 1
    for offset in range(days):
        check_in = start + timedelta(days=offset)
        season = _season_factor(check_in)
        for hotel, (rooms, base_adr) in HOTELS.items():
            expected = rooms * 0.12 * season  # new arrivals per day
            for _ in range(rng.poisson(expected)):
                room_type = rng.choice(list(ROOM_WEIGHTS), p=list(ROOM_WEIGHTS.values()))
                nights = int(min(14, max(1, rng.geometric(0.42))))
                lead_time = int(min(240, rng.gamma(2.0, 18.0)))
                created_at = datetime.combine(check_in - timedelta(days=lead_time), datetime.min.time()) \
                    + timedelta(hours=int(rng.integers(8, 23)), minutes=int(rng.integers(0, 60)))
                adr = base_adr * ROOM_MULTIPLIER[room_type] * (0.85 + 0.3 * season / 1.35) * rng.normal(1.0, 0.07)
                revenue = round(max(800.0, adr * nights), 2)
                capacity = {"SINGLE": 2, "DOUBLE": 3, "DELUXE": 3, "SUITE": 4, "PRESIDENTIAL": 6}[room_type]
                guests = int(min(capacity, max(1, rng.poisson(1.1 if room_type == "SINGLE" else 2.0))))

                if created_at > datetime.now():
                    continue  # a future stay booked "tomorrow" can't be on the books yet

                # Cancellation risk grows with lead time, long stays and premium rooms.
                p_cancel = (0.02 + 0.0032 * lead_time + 0.05 * (nights > 5) + 0.06 * (room_type in ("SUITE", "PRESIDENTIAL"))
                            + 0.04 * (check_in.month in (12, 5)))
                today = date.today()
                if rng.random() < min(p_cancel, 0.6):
                    status = "CANCELLED"
                elif check_in > today:
                    status = "CONFIRMED"
                elif check_in + timedelta(days=nights) > today:
                    status = "CHECKED_IN"
                else:
                    status = "CHECKED_OUT"

                rows.append({
                    "id": booking_id,
                    "hotelName": hotel,
                    "guestName": f"{rng.choice(FIRST_NAMES)} {rng.choice(LAST_NAMES)}",
                    "checkInDate": check_in.isoformat(),
                    "checkOutDate": (check_in + timedelta(days=nights)).isoformat(),
                    "guests": guests,
                    "roomType": room_type,
                    "bookingStatus": status,
                    "totalRevenue": revenue,
                    "createdAt": created_at.isoformat(),
                })
                booking_id += 1

    df = pd.DataFrame(rows)
    # Inject a few anomalies so the detector has real signal to find.
    anomaly_idx = rng.choice(df.index[df["bookingStatus"] != "CANCELLED"], size=6, replace=False)
    df.loc[anomaly_idx[:2], "totalRevenue"] = df.loc[anomaly_idx[:2], "totalRevenue"] * 9      # rate typed wrong
    df.loc[anomaly_idx[2:4], "totalRevenue"] = 120.0                                            # near-zero revenue
    df.loc[anomaly_idx[4:], "guests"] = 6                                                       # 6 guests in a single
    df.loc[anomaly_idx[4:], "roomType"] = "SINGLE"
    return df
