import pytest
from test_api import signup, headers, key, api

def test_qr_code_security_isolation_and_no_leak_in_public_endpoints(api):
    client, _ = api
    first, second = signup(client, "StudentA"), signup(client, "StudentB")

    # 1. User B gets their own QR token
    qr_b = client.get("/me/qr-code", headers=headers(second))
    assert qr_b.status_code == 200
    token_b = qr_b.json()["qrCodeToken"]
    assert token_b.startswith("qr_")

    # 2. User A views User B's public profile -> MUST NOT leak qrCodeToken!
    pub_b = client.get(f"/profiles/{second['userId']}", headers=headers(first))
    assert pub_b.status_code == 200
    assert "qrCodeToken" not in pub_b.json()

    # 3. Public rankings MUST NOT leak qrCodeToken!
    rankings = client.get("/rankings?metric=DAILY_XP&scope=GLOBAL", headers=headers(first))
    assert rankings.status_code == 200
    for entry in rankings.json()["entries"]:
        assert "qrCodeToken" not in entry["profile"]

    # 4. User A's own public profile MUST NOT expose qrCodeToken
    pub_me = client.get("/me/public-profile", headers=headers(first))
    assert pub_me.status_code == 200
    assert "qrCodeToken" not in pub_me.json()

    # 5. User A connects to B using B's valid token -> Creates PENDING request
    conn = client.post("/friends/qr-connect", headers=headers(first, **{"Idempotency-Key": key()}), json={"qrToken": token_b})
    assert conn.status_code == 201
    assert conn.json()["state"] == "PENDING"
    friendship_id = conn.json()["id"]

    # 6. Recipient (User B) accepts the friend request
    accept_res = client.patch(f"/friendships/{friendship_id}", headers=headers(second, **{"If-Match": "1"}), json={"decision": "ACCEPT"})
    assert accept_res.status_code == 200
    assert accept_res.json()["state"] == "ACCEPTED"

    # 7. Connecting again returns existing friendship without duplicate creation
    conn_again = client.post("/friends/qr-connect", headers=headers(first, **{"Idempotency-Key": key()}), json={"qrToken": token_b})
    assert conn_again.status_code == 201
    assert conn_again.json()["id"] == friendship_id

def test_qr_code_cannot_friend_self_and_unauthenticated_blocked(api):
    client, _ = api
    first = signup(client, "SelfFriend")

    # Unauthenticated request fails
    assert client.get("/me/qr-code").status_code == 401
    assert client.post("/friends/qr-connect", json={"qrToken": "qr_fake"}).status_code == 401

    # User cannot friend self using their own token
    token_me = client.get("/me/qr-code", headers=headers(first)).json()["qrCodeToken"]
    self_conn = client.post("/friends/qr-connect", headers=headers(first, **{"Idempotency-Key": key()}), json={"qrToken": token_me})
    assert self_conn.status_code == 400

    # Invalid token fails
    invalid_conn = client.post("/friends/qr-connect", headers=headers(first, **{"Idempotency-Key": key()}), json={"qrToken": "qr_nonexistent_12345"})
    assert invalid_conn.status_code == 404

def test_qr_code_revocation_invalidates_old_token(api):
    client, _ = api
    first, second = signup(client, "Revoker"), signup(client, "Scanner")

    old_token = client.get("/me/qr-code", headers=headers(first)).json()["qrCodeToken"]
    assert old_token.startswith("qr_")

    # Revoke/Refresh token
    revoked_res = client.post("/me/qr-code/revoke", headers=headers(first))
    assert revoked_res.status_code == 200
    new_token = revoked_res.json()["qrCodeToken"]
    assert new_token != old_token

    # Scanning old token fails
    scan_old = client.post("/friends/qr-connect", headers=headers(second, **{"Idempotency-Key": key()}), json={"qrToken": old_token})
    assert scan_old.status_code == 404

    # Scanning new token succeeds (creates PENDING)
    scan_new = client.post("/friends/qr-connect", headers=headers(second, **{"Idempotency-Key": key()}), json={"qrToken": new_token})
    assert scan_new.status_code == 201
    assert scan_new.json()["state"] == "PENDING"

def test_badges_and_streaks_endpoints(api):
    client, _ = api
    user = signup(client, "Learner")
    badges = client.get("/me/badges", headers=headers(user))
    assert badges.status_code == 200
    assert "badges" in badges.json()

    streaks = client.get("/friends/streaks", headers=headers(user))
    assert streaks.status_code == 200
    assert "streaks" in streaks.json()

def test_rank_tier_in_dashboard_and_consistency(api):
    client, _ = api
    user = signup(client, "RankStudent")
    profile = client.get("/me/public-profile", headers=headers(user))
    assert profile.status_code == 200
    assert profile.json()["rankTier"] == "BRONZE"

    dashboard = client.get("/me/dashboard?grade=10", headers=headers(user))
    assert dashboard.status_code == 200
    assert dashboard.json()["rankTier"] == "BRONZE"
