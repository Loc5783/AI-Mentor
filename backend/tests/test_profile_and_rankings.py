import pytest
from test_api import signup, headers, key, api

def test_qr_code_and_qr_friend_connect(api):
    client, _ = api
    first, second = signup(client, "StudentOne"), signup(client, "StudentTwo")
    qr_res = client.get("/me/qr-code", headers=headers(first))
    assert qr_res.status_code == 200
    token = qr_res.json()["qrCodeToken"]
    assert token.startswith("qr_")

    conn = client.post("/friends/qr-connect", headers=headers(second, **{"Idempotency-Key": key()}), json={"qrToken": token})
    assert conn.status_code == 201
    assert conn.json()["state"] == "ACCEPTED"

def test_badges_and_streaks_endpoints(api):
    client, _ = api
    user = signup(client, "Learner")
    badges = client.get("/me/badges", headers=headers(user))
    assert badges.status_code == 200
    assert "badges" in badges.json()

    streaks = client.get("/friends/streaks", headers=headers(user))
    assert streaks.status_code == 200
    assert "streaks" in streaks.json()

def test_rank_tier_in_public_profile_and_dashboard(api):
    client, _ = api
    user = signup(client, "RankStudent")
    profile = client.get("/me/public-profile", headers=headers(user))
    assert profile.status_code == 200
    assert profile.json()["rankTier"] == "BRONZE"

    dashboard = client.get("/me/dashboard?grade=10", headers=headers(user))
    assert dashboard.status_code == 200
    assert dashboard.json()["rankTier"] == "BRONZE"
