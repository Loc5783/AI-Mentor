import {environmentKeys,uniqueKeys,ProviderFailure} from './key_pool.mjs';

export function createConfigLoader({get,fetchImpl=fetch,clock=Date.now}) {
  let cached=null;let expires=0;let flight=null;
  return async function load() {
    if(cached && clock()<expires)return cached;
    if(flight)return flight;
    flight=(async()=>{
      let value=null;
      let service=get('SUPABASE_SERVICE_ROLE_KEY');
      if(!service) {
        try {service=JSON.parse(get('SUPABASE_SECRET_KEYS')??'{}').default;}catch{}
      }
      if(service) {
        const headers={apikey:service,'Content-Type':'application/json'};
        if(service.startsWith('eyJ'))headers.Authorization='Bearer '+service;
        let response;
        try {response=await fetchImpl(get('SUPABASE_URL')+'/rest/v1/rpc/mentor_ai_server_config',{
          method:'POST',headers,body:'{}',signal:AbortSignal.timeout(8000)
        });}catch{throw new ProviderFailure(503,'NOT_CONFIGURED');}
        if(response.ok)value=await response.json();
        else if(response.status!==404)throw new ProviderFailure(503,'NOT_CONFIGURED');
      }
      const keys=value?uniqueKeys(value.keys):environmentKeys(get);
      const model=value?.model??get('GEMINI_MODEL')??'gemini-3.1-flash-lite';
      if(!keys.length || !/^[a-zA-Z0-9._-]+$/.test(model))throw new ProviderFailure(503,'NOT_CONFIGURED');
      cached={keys,model};expires=clock()+60000;return cached;
    })();
    try{return await flight;}finally{flight=null;}
  };
}
