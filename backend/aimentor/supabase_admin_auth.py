"""Supabase authentication for the separate administrative HTTP surface."""
import base64
import json
import time
import uuid

from fastapi import HTTPException
import httpx

from .supabase_catalog import CatalogTransport


class SupabaseAdminAuth:
    def __init__(self, url, key):
        # Reuse the strict origin/server-key validation from the catalog adapter.
        validated = CatalogTransport(url, key)
        self.url = url.rstrip('/')
        self._headers = dict(validated._headers)

    def _request(self, method, path, body=None, token=None):
        headers = dict(self._headers)
        if token:
            headers['Authorization'] = 'Bearer ' + token
        try:
            with httpx.Client(timeout=20, follow_redirects=False, trust_env=False) as client:
                r = client.request(method, self.url + path, headers=headers, json=body)
        except httpx.HTTPError:
            raise HTTPException(503, detail={'code': 'SUPABASE_AUTH_UNAVAILABLE'}) from None
        if r.status_code == 429:
            raise HTTPException(429, detail={'code': 'RATE_LIMITED'})
        if r.is_error:
            if r.status_code >= 500:
                raise HTTPException(503, detail={'code': 'SUPABASE_AUTH_UNAVAILABLE'})
            try:
                error = r.json()
                code = error.get('error_code') or error.get('code', '')
            except ValueError:
                code = ''
            if code == 'email_not_confirmed':
                raise HTTPException(403, detail={'code': 'EMAIL_NOT_CONFIRMED'})
            if path.startswith('/rest/v1/rpc/'):
                raise HTTPException(503, detail={'code': 'SUPABASE_AUTH_UNAVAILABLE'})
            raise HTTPException(401, detail={'code': 'INVALID_SUPABASE_SESSION'})
        if r.status_code == 204 or not r.content:
            return {}
        try:
            return r.json()
        except ValueError:
            raise HTTPException(503, detail={'code': 'SUPABASE_AUTH_INVALID_RESPONSE'}) from None

    def current(self, token):
        if not isinstance(token, str) or len(token) > 8192 or len(token.split('.')) != 3:
            raise HTTPException(401, detail={'code': 'INVALID_SUPABASE_SESSION'})
        # Signature and project verification happen at the selected Supabase Auth server.
        user = self._request('GET', '/auth/v1/user', token=token)
        try:
            raw = token.split('.')[1]
            claims = json.loads(base64.urlsafe_b64decode(raw + '=' * (-len(raw) % 4)))
            uid = str(uuid.UUID(claims['sub']))
            sid = str(uuid.UUID(claims['session_id']))
            audience = claims.get('aud')
            if (uid != user['id'] or claims.get('iss') != self.url + '/auth/v1'
                    or claims.get('role') != 'authenticated'
                    or not (audience == 'authenticated' or isinstance(audience, list) and 'authenticated' in audience)
                    or float(claims['exp']) <= time.time()):
                raise ValueError('Invalid claims')
        except (ValueError, KeyError, TypeError, UnicodeError):
            raise HTTPException(401, detail={'code': 'INVALID_SUPABASE_SESSION'}) from None
        # Server-key-only lookup checks session revocation and fresh app_metadata.
        # Never authorize from user_metadata or stale role claims in the token.
        identity = self._request('POST', '/rest/v1/rpc/mentor_admin_identity',
                                 dict(p_user_id=uid, p_session_id=sid))
        if not isinstance(identity, dict) or identity.get('id') != uid:
            raise HTTPException(401, detail={'code': 'INVALID_SUPABASE_SESSION'})
        if identity.get('role') not in ('ADMIN', 'TEACHER'):
            raise HTTPException(403, detail={'code': 'ADMIN_REQUIRED'})
        return identity

    def _session(self, response):
        access = response.get('access_token')
        refresh = response.get('refresh_token')
        if not access or not refresh:
            raise HTTPException(503, detail={'code': 'SUPABASE_AUTH_INVALID_RESPONSE'})
        try:
            identity = self.current(access)
        except HTTPException:
            # Discard only this newly created session, without signing out other devices.
            try:
                self.signout(access)
            except HTTPException:
                pass
            raise
        return dict(accessToken=access, refreshToken=refresh, userId=identity['id'],
                    expiresIn=response.get('expires_in'), role=identity['role'])

    def signin(self, body):
        email = body.identifier.strip()
        if '@' not in email or len(email) > 320:
            raise HTTPException(400, detail={'code': 'SUPABASE_EMAIL_REQUIRED'})
        return self._session(self._request('POST', '/auth/v1/token?grant_type=password',
                                           dict(email=email, password=body.password)))

    def refresh(self, value):
        return self._session(self._request('POST', '/auth/v1/token?grant_type=refresh_token',
                                           dict(refresh_token=value)))

    def signout(self, token):
        self._request('POST', '/auth/v1/logout?scope=local', token=token)
        return {'accepted': True}
