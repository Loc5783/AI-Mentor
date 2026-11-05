import base64
import json
import time
from types import SimpleNamespace
import uuid

import httpx
import pytest
from fastapi import HTTPException

from aimentor.supabase_admin_auth import SupabaseAdminAuth

UID = '11111111-1111-4111-8111-111111111111'
SID = '1555b422-274d-40bb-a17a-3534b063923b'
ORIGIN = 'https://group.supabase.co'


def token(**changes):
    claims = dict(sub=UID, session_id=SID, iss=ORIGIN + '/auth/v1', aud='authenticated',
                  role='authenticated', exp=time.time() + 3600,
                  app_metadata={'mentor_role': 'ADMIN'})
    claims.update(changes)
    def encode(value):
        return base64.urlsafe_b64encode(json.dumps(value).encode()).decode().rstrip('=')
    return encode({'alg': 'ES256'}) + '.' + encode(claims) + '.mock-server-validated-signature'


@pytest.fixture
def auth(monkeypatch):
    state = dict(role='ADMIN', live=True, verified=True, requests=[], outage=False)
    original = httpx.Client
    def handle(request):
        state['requests'].append(request)
        if state['outage']:
            raise httpx.ConnectError('private internal address', request=request)
        if state.get('provider_error'):
            return httpx.Response(400, json={'code': 400, 'error_code': state['provider_error']})
        if request.url.path.endswith('/token'):
            state['grant'] = request.url.params.get('grant_type')
            state['body'] = json.loads(request.content)
            return httpx.Response(200, json=dict(access_token=token(), refresh_token='new-refresh', expires_in=3600))
        if request.url.path.endswith('/user'):
            if not state['verified']:
                return httpx.Response(401, json={'code': 'bad_jwt'})
            return httpx.Response(200, json=dict(id=UID, user_metadata={'role': 'ADMIN'}))
        if request.url.path.endswith('/mentor_admin_identity'):
            assert 'authorization' not in request.headers  # Opaque server key is only apikey.
            payload = json.loads(request.content)
            assert payload == dict(p_user_id=UID, p_session_id=SID)
            return httpx.Response(200, json=dict(id=UID, email='admin@group.example',
                username='admin@group.example', role=state['role']) if state['live'] else None)
        if request.url.path.endswith('/logout'):
            state['logout_scope'] = request.url.params.get('scope')
            return httpx.Response(204)
        raise AssertionError(str(request.url))
    monkeypatch.setattr(httpx, 'Client', lambda **kw: original(transport=httpx.MockTransport(handle), **kw))
    return SupabaseAdminAuth(ORIGIN, 'sb_secret_mock'), state


def test_password_login_and_refresh_use_group_auth(auth):
    a, s = auth
    result = a.signin(SimpleNamespace(identifier='admin@group.example', password='not-a-real-password'))
    assert result['userId'] == UID and result['role'] == 'ADMIN'
    assert s['grant'] == 'password' and s['body']['email'] == 'admin@group.example'
    assert a.refresh('old-refresh')['refreshToken'] == 'new-refresh'
    assert s['grant'] == 'refresh_token' and s['body'] == {'refresh_token': 'old-refresh'}


def test_local_username_not_accepted_by_cloud_login(auth):
    a, s = auth
    with pytest.raises(HTTPException) as e:
        a.signin(SimpleNamespace(identifier='admin', password='admin123'))
    assert e.value.status_code == 400
    assert not s['requests']


def test_online_verification_required_before_using_claims(auth):
    a, s = auth
    s['verified'] = False
    with pytest.raises(HTTPException) as e:
        a.current(token())
    assert e.value.status_code == 401
    assert len(s['requests']) == 1


@pytest.mark.parametrize('claims', [dict(iss='https://other.supabase.co/auth/v1'),
    dict(exp=1), dict(aud='service_role'), dict(role='service_role'),
    dict(session_id=None), dict(sub=str(uuid.uuid4()))])
def test_wrong_project_expiry_audience_role_and_identity_rejected(auth, claims):
    a, s = auth
    with pytest.raises(HTTPException) as e:
        a.current(token(**claims))
    assert e.value.status_code == 401
    assert len(s['requests']) == 1


def test_live_revocation_and_current_role_override_stale_claims(auth):
    a, s = auth
    assert a.current(token())['role'] == 'ADMIN'
    s['role'] = 'STUDENT'  # user_metadata and token's stale ADMIN cannot grant access.
    with pytest.raises(HTTPException) as e:
        a.current(token())
    assert e.value.status_code == 403
    s['role'] = 'ADMIN'
    s['live'] = False
    with pytest.raises(HTTPException) as e:
        a.current(token())
    assert e.value.status_code == 401


def test_unprivileged_login_discards_only_new_session(auth):
    a, s = auth
    s['role'] = 'STUDENT'
    with pytest.raises(HTTPException) as e:
        a.signin(SimpleNamespace(identifier='student@group.example', password='not-a-real-password'))
    assert e.value.status_code == 403
    assert s['logout_scope'] == 'local'


def test_network_failure_denies_access_without_secret_in_error(auth):
    a, s = auth
    s['outage'] = True
    with pytest.raises(HTTPException) as e:
        a.current(token())
    assert e.value.status_code == 503
    assert e.value.detail == {'code': 'SUPABASE_AUTH_UNAVAILABLE'}


def test_unconfirmed_email_uses_provider_error_code(auth):
    a, s = auth
    s['provider_error'] = 'email_not_confirmed'
    with pytest.raises(HTTPException) as e:
        a.signin(SimpleNamespace(identifier='unconfirmed@group.example', password='not-a-real-password'))
    assert e.value.status_code == 403
    assert e.value.detail == {'code': 'EMAIL_NOT_CONFIRMED'}


def test_admin_routes_never_fall_back_to_local_admin(tmp_path, monkeypatch):
    from fastapi.testclient import TestClient
    from aimentor.config import Settings
    from aimentor.main import create_app
    import aimentor.supabase_admin_auth as module
    class CloudAuth:
        def current(self, access):
            if access != 'verified-cloud-access':
                raise HTTPException(401, detail={'code': 'INVALID_SUPABASE_SESSION'})
            return dict(id=UID, username='admin@group.example', email='admin@group.example', role='ADMIN')
        def signin(self, body):
            return dict(accessToken='verified-cloud-access', refreshToken='cloud-refresh', userId=UID)
        def refresh(self, value):
            assert value == 'cloud-refresh'
            return dict(accessToken='verified-cloud-access', refreshToken='rotated-cloud-refresh', userId=UID)
        def signout(self, access):
            return {'accepted': True}
    monkeypatch.setattr(module, 'SupabaseAdminAuth', lambda *args: CloudAuth())
    app = create_app(Settings(tmp_path, admin_auth_provider='supabase',
                              supabase_url=ORIGIN, supabase_server_key='sb_secret_mock'))
    with TestClient(app) as c:
        local = c.post('/auth/sign-up', json=dict(displayName='Local', email='local@test.example',
            password='Mentor01!2026', acceptedTermsVersion='2026-10')).json()
        with app.state.db.transaction() as db:
            db.execute("UPDATE users SET role='ADMIN',verified=1 WHERE email='local@test.example'")
        assert c.get('/admin/questions', headers={'Authorization': 'Bearer '+local['accessToken']}).status_code == 401
        result = c.post('/admin/auth/sign-in', json=dict(identifier='admin@group.example',password='unused')).json()
        headers = {'Authorization': 'Bearer ' + result['accessToken']}
        identity = c.get('/admin/auth/me', headers=headers)
        assert identity.status_code == 200 and identity.json()['userId'] == UID
        assert set(identity.json()) == {'userId','username','email','role'}
        assert c.get('/admin/questions', headers=headers).status_code == 200
        assert c.get('/admin/auth/config').json() == {'provider': 'supabase'}
        assert c.post('/admin/auth/refresh', json={'refreshToken': 'cloud-refresh'}).json()['refreshToken'] == 'rotated-cloud-refresh'
        assert c.post('/admin/auth/sign-out', headers=headers,json={'refreshToken':'cloud-refresh'}).json()['accepted']
