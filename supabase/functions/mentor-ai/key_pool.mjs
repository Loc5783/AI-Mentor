export class ProviderFailure extends Error {
  constructor(status, code) { super(code); this.status=status; this.code=code; }
}

export function uniqueKeys(keys) {
  if (!Array.isArray(keys) || keys.some(k => typeof k !== 'string')) throw new ProviderFailure(503,'NOT_CONFIGURED');
  return [...new Set(keys.map(k => k.trim()).filter(Boolean))].slice(0,16);
}

export function environmentKeys(get) {
  const packed=get('GEMINI_API_KEYS');
  if (packed) {
    try { return uniqueKeys(JSON.parse(packed)); }
    catch { throw new ProviderFailure(503,'NOT_CONFIGURED'); }
  }
  return uniqueKeys([get('GEMINI_API_KEY'), ...Array.from({length:16},(_,i)=>get('GEMINI_API_KEY_'+(i+1)))].filter(Boolean));
}

export class GeminiKeyPool {
  constructor(keys, {fetchImpl=fetch, clock=Date.now, budgetMs=45000}={}) {
    this.keys=uniqueKeys(keys); this.fetch=fetchImpl; this.clock=clock; this.budgetMs=budgetMs;
    this.cursor=0; this.cooldown=new Map();
  }

  async request(model,payload,signal) {
    const visited=new Set(); const deadline=this.clock()+this.budgetMs;
    let last=new ProviderFailure(503,'PROVIDER');
    const limit=Math.min(4,this.keys.length);
    if (!limit) throw new ProviderFailure(503,'NOT_CONFIGURED');
    while (visited.size<limit) {
      if (signal?.aborted) throw new ProviderFailure(502,'PROVIDER');
      const remaining=deadline-this.clock(); if(remaining<=0) break;
      let slot=-1;
      for(let i=0;i<this.keys.length;i++) {
        const candidate=(this.cursor+i)%this.keys.length;
        if(!visited.has(candidate) && (this.cooldown.get(candidate)??0)<=this.clock()) {slot=candidate;break;}
      }
      if(slot<0) break;
      visited.add(slot);
      const timeout=Math.max(1,Math.min(30000,remaining-(limit-visited.size)*1000));
      let response;
      try {
        const timeoutSignal=AbortSignal.timeout(Math.floor(timeout));
        response=await this.fetch('https://generativelanguage.googleapis.com/v1beta/models/'+model+':generateContent',{
          method:'POST',headers:{'Content-Type':'application/json','x-goog-api-key':this.keys[slot]},
          body:JSON.stringify(payload),signal:signal?AbortSignal.any([signal,timeoutSignal]):timeoutSignal
        });
      } catch {
        if(signal?.aborted) throw new ProviderFailure(502,'PROVIDER');
        this.cooldown.set(slot,this.clock()+15000); this.cursor=(slot+1)%this.keys.length;
        last=new ProviderFailure(502,'PROVIDER'); continue;
      }
      if(response.ok) {if((this.cooldown.get(slot)??0)<=this.clock())this.cursor=slot;return response;}
      let badKey=false;
      if(response.status===400) {
        const body=await response.clone().json().catch(()=>null);
        badKey=(body?.error?.details??[]).some(d=>['API_KEY_INVALID','API_KEY_EXPIRED','API_KEY_SERVICE_BLOCKED'].includes(d.reason));
      }
      const retry=badKey || [401,403,429,500,502,503,504].includes(response.status);
      if(!retry) {await response.body?.cancel();throw new ProviderFailure(502,'PROVIDER');}
      let wait=response.status===429?60000:badKey||[401,403].includes(response.status)?300000:15000;
      const retryAfter=response.headers.get('Retry-After');
      if(retryAfter) {
        const seconds=Number(retryAfter);
        const delay=Number.isFinite(seconds)?seconds*1000:Date.parse(retryAfter)-this.clock();
        if(Number.isFinite(delay))wait=Math.max(wait,delay);
      }
      await response.body?.cancel();
      this.cooldown.set(slot,this.clock()+wait);this.cursor=(slot+1)%this.keys.length;
      last=new ProviderFailure(response.status===429?429:502,response.status===429?'RATE_LIMITED':'PROVIDER');
    }
    throw last;
  }
}
