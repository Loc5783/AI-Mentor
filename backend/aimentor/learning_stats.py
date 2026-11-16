"""Canonical learning-day/reward summary for completion, dashboard and public rankings."""
from datetime import date, datetime, timedelta, timezone

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


def shared_streak_summary(days, now):
    """Count consecutive Vietnam learning days shared by both friends, through today/yesterday."""
    today = learning_day(now)
    shared_days = set()
    for value in days:
        if isinstance(value, datetime):
            day = value.date()
        elif isinstance(value, date):
            day = value
        else:
            day = date.fromisoformat(str(value))
        if day <= today:
            shared_days.add(day)
    if not shared_days:
        return {"streakDays": 0, "lastSharedDay": None}

    latest = max(shared_days)
    streak = 0
    if latest >= today - timedelta(days=1):
        current = latest
        while current in shared_days:
            streak += 1
            current -= timedelta(days=1)
    return {"streakDays": streak, "lastSharedDay": latest.isoformat()}
