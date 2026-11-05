import io
import json
import urllib.error
from unittest.mock import patch

import pytest

from aimentor.ai_practice import AiProviderError, GeminiAiProvider
from aimentor.gemini_failover import RotatingGeminiProvider


def test_rate_auth_network_and_server_failover_to_fourth_key():
    calls=[]
    failures=[AiProviderError('AI_RATE_LIMIT','quota',429),
              AiProviderError('AI_AUTH_FAILURE','auth'),
              AiProviderError('AI_UPSTREAM_ERROR','server',upstream_status=503)]
    def invoke(provider,*args):
        calls.append(provider.api_key)
        if len(calls)<=3:raise failures[len(calls)-1]
        return 'OK'
    p=RotatingGeminiProvider(['slot1','slot2','slot3','slot4'],'model')
    with patch.object(GeminiAiProvider,'_call_gemini_api',invoke):
        assert p._call_gemini_api('question')=='OK'
        assert p._call_gemini_api('next')=='OK'
    assert calls==['slot1','slot2','slot3','slot4','slot4']


@pytest.mark.parametrize('code',['AI_SAFETY_BLOCKED','AI_MALFORMED_OUTPUT','AI_UPSTREAM_ERROR'])
def test_safety_malformed_and_bad_request_do_not_rotate(code):
    p=RotatingGeminiProvider(['one','two'],'model')
    with patch.object(GeminiAiProvider,'_call_gemini_api',side_effect=AiProviderError(code,'safe')) as call:
        with pytest.raises(AiProviderError):p._call_gemini_api('question')
    assert call.call_count==1


def test_all_failed_is_bounded_and_subsequent_calls_respect_cooldown():
    now=[100.0]
    p=RotatingGeminiProvider(['one','two','one','three','four'],'model',clock=lambda:now[0])
    with patch.object(GeminiAiProvider,'_call_gemini_api',side_effect=AiProviderError('AI_RATE_LIMIT','quota',429)) as call:
        with pytest.raises(AiProviderError) as e:p._call_gemini_api('question')
        assert e.value.code=='AI_RATE_LIMIT'
        assert call.call_count==4
        with pytest.raises(AiProviderError):p._call_gemini_api('question')
        assert call.call_count==4
        now[0]+=61
        with pytest.raises(AiProviderError):p._call_gemini_api('question')
        assert call.call_count==8


def test_retry_after_and_total_deadline_respected():
    now=[100.0]
    p=RotatingGeminiProvider(['one','two'],'model',timeout_seconds=15,clock=lambda:now[0])
    def fail(*args):
        now[0]+=20
        raise AiProviderError('AI_RATE_LIMIT','quota',429,retry_after=120)
    with patch.object(GeminiAiProvider,'_call_gemini_api',side_effect=fail) as call:
        with pytest.raises(AiProviderError):p._call_gemini_api('question')
        assert call.call_count==1
        assert p._cooldown[0]>=240


def test_google_invalid_key_400_is_auth_failure_not_generic_bad_request():
    body=json.dumps({'error':{'details':[{'reason':'API_KEY_INVALID'}]}}).encode()
    error=urllib.error.HTTPError('https://provider',400,'Bad request',{},io.BytesIO(body))
    with patch('urllib.request.urlopen',side_effect=error):
        with pytest.raises(AiProviderError) as e:GeminiAiProvider('fake','model')._call_gemini_api('question')
    assert e.value.code=='AI_AUTH_FAILURE'
    assert e.value.upstream_status==400


def test_factory_uses_private_settings_without_putting_keys_in_repr(tmp_path,monkeypatch):
    from aimentor.config import Settings
    from aimentor.main import create_app
    for name in ('GEMINI_API_KEY','GEMINI_API_KEYS','GEMINI_MODEL','AI_PROVIDER_TYPE'):
        monkeypatch.delenv(name,raising=False)
    settings=Settings(tmp_path,gemini_api_keys=('slot-secret-one','slot-secret-two'),gemini_model='model')
    assert 'slot-secret' not in repr(settings)
    assert isinstance(create_app(settings).state.ai_practice.provider,RotatingGeminiProvider)
