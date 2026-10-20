"""Canonical learning-day/reward summary for completion, dashboard and public rankings."""
from datetime import datetime, timedelta, timezone

ZONE = timezone(timedelta(hours=7))


def learning_day(now):
    return datetime.fromtimestamp(now, ZONE).date()


def reward_summary(awards, days, now):
    today = learning_day(now)
    current = today if today.isoformat() in days else today - timedelta(days=1)
    streak = 0
    while current.isoformat() in days:
        streak += 1
        current -= timedelta(days=1)
    return {"learningDay": today.isoformat(), "streakDays": streak,
            "totalXp": sum(row["xp"] for row in awards),
            "dailyXp": sum(row["xp"] for row in awards if row["day"] == today.isoformat())}
