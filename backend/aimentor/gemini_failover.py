"""Bounded, thread-safe Gemini failover; no automatic content/safety retries."""
import threading
import time

from .ai_practice import GeminiAiProvider, AiProviderError


class RotatingGeminiProvider(GeminiAiProvider):
    def __init__(self, api_keys, model, timeout_seconds=15.0, clock=time.monotonic):
        keys=list(dict.fromkeys(k.strip() for k in api_keys if k.strip()))[:16]
        super().__init__(keys[0] if keys else '', model, timeout_seconds)
        self._keys=keys
        self._clock=clock
        self._lock=threading.Lock()
        self._cursor=0
        self._cooldown={}

    def _call_gemini_api(self, prompt_text, response_mime_type='application/json'):
        visited=set()
        limit=min(4,len(self._keys))
        deadline=self._clock()+self.timeout_seconds
        last=AiProviderError('AI_KEYS_COOLDOWN','Các khóa AI đang tạm nghỉ. Vui lòng thử lại sau.',503)
        while len(visited)<limit:
            remaining=deadline-self._clock()
            if remaining<=0:break
            with self._lock:
                slot=next((i for n in range(len(self._keys))
                           if (i:=(self._cursor+n)%len(self._keys)) not in visited
                           and self._cooldown.get(i,0)<=self._clock()),None)
            if slot is None:break
            visited.add(slot)
            budget=max(0.05,remaining-(limit-len(visited)))
            provider=GeminiAiProvider(self._keys[slot],self.model,budget)
            try:
                result=provider._call_gemini_api(prompt_text,response_mime_type)
                with self._lock:
                    if self._cooldown.get(slot,0)<=self._clock():self._cursor=slot
                return result
            except AiProviderError as exc:
                upstream=getattr(exc,'upstream_status',None)
                if exc.code not in ('AI_AUTH_FAILURE','AI_RATE_LIMIT','AI_TIMEOUT') and upstream not in (500,502,503,504):
                    raise
                delay=300 if exc.code=='AI_AUTH_FAILURE' else 60 if exc.code=='AI_RATE_LIMIT' else 15
                delay=max(delay,getattr(exc,'retry_after',0))
                with self._lock:
                    self._cooldown[slot]=self._clock()+delay
                    self._cursor=(slot+1)%len(self._keys)
                last=exc
        raise last
