import test from 'node:test';
import assert from 'node:assert/strict';
import {GeminiKeyPool,environmentKeys} from '../../supabase/functions/mentor-ai/key_pool.mjs';
import {createConfigLoader} from '../../supabase/functions/mentor-ai/server_config.mjs';

const ok=()=>new Response(JSON.stringify({candidates:[{content:{parts:[{text:'OK'}]}}]}));

test('fourth key succeeds after quota/auth/server errors; stays on healthy key',async()=>{
 const seen=[];const statuses=[429,403,503,200,200];
 const pool=new GeminiKeyPool(['one','two','three','four'],{fetchImpl:async(url,opts)=>{
   seen.push(opts.headers['x-goog-api-key']);const status=statuses.shift();return status===200?ok():new Response('{}',{status});
 }});
 assert.equal((await pool.request('model',{})).status,200);
 assert.equal((await pool.request('model',{})).status,200);
 assert.deepEqual(seen,['one','two','three','four','four']);
});

test('network failure switches; bad request/model error does not',async()=>{
 let n=0;const pool=new GeminiKeyPool(['one','two'],{fetchImpl:async()=>{if(!n++)throw Error('Network');return ok();}});
 assert.equal((await pool.request('model',{})).status,200);assert.equal(n,2);
 for(const status of [400,404]) {
   let calls=0;const p=new GeminiKeyPool(['one','two'],{fetchImpl:async()=>{calls++;return new Response('{}',{status});}});
   await assert.rejects(p.request('model',{}));assert.equal(calls,1);
 }
});

test('invalid key 400 switches, arbitrary 400 does not',async()=>{
 let n=0;const pool=new GeminiKeyPool(['one','two'],{fetchImpl:async()=>n++?ok():new Response(JSON.stringify({error:{details:[{reason:'API_KEY_INVALID'}]}}),{status:400})});
 assert.equal((await pool.request('model',{})).status,200);assert.equal(n,2);
});

test('bounded attempts, cooldown and Retry-After',async()=>{
 let now=0,calls=0;const p=new GeminiKeyPool(['one','two','one','three','four'],{clock:()=>now,fetchImpl:async()=>{
   calls++;return new Response('{}',{status:429,headers:{'Retry-After':'120'}});
 }});
 await assert.rejects(p.request('model',{}),e=>e.status===429);assert.equal(calls,4);
 await assert.rejects(p.request('model',{}));assert.equal(calls,4);
 now=61000;await assert.rejects(p.request('model',{}));assert.equal(calls,4);
 now=121000;await assert.rejects(p.request('model',{}));assert.equal(calls,8);
});

test('total deadline and caller abort stop further attempts',async()=>{
 let now=0,calls=0;const p=new GeminiKeyPool(['one','two'],{clock:()=>now,budgetMs:100,fetchImpl:async()=>{calls++;now+=101;throw Error('timeout');}});
 await assert.rejects(p.request('model',{}));assert.equal(calls,1);
 const controller=new AbortController();controller.abort();
 await assert.rejects(p.request('model',{},controller.signal));assert.equal(calls,1);
});

test('environment supports four indexed keys and JSON without requiring old prefixes',()=>{
 const env={GEMINI_API_KEY_1:'AQ.fake-one',GEMINI_API_KEY_2:'AQ.fake-two',GEMINI_API_KEY_3:'AQ.fake-three',GEMINI_API_KEY_4:'AQ.fake-four'};
 assert.equal(environmentKeys(k=>env[k]).length,4);
 assert.deepEqual(environmentKeys(k=>({GEMINI_API_KEYS:'[" AQ.fake ","AQ.fake"]'})[k]),['AQ.fake']);
});

test('Vault has priority over old env keys, cached without exposing service bearer',async()=>{
 let calls=0;const get=k=>({SUPABASE_URL:'https://group.supabase.co',SUPABASE_SECRET_KEYS:'{"default":"sb_secret_fake"}',GEMINI_API_KEY_1:'old'})[k];
 const load=createConfigLoader({get,fetchImpl:async(url,opts)=>{calls++;assert.equal(opts.headers.apikey,'sb_secret_fake');assert.equal(opts.headers.Authorization,undefined);return new Response(JSON.stringify({keys:['one','two','three','four'],model:'model'}));}});
 const [a,b]=await Promise.all([load(),load()]);assert.equal(calls,1);assert.equal(a.keys.length,4);assert.deepEqual(a,b);
 await load();assert.equal(calls,1);
});

test('Vault failure never silently uses old keys',async()=>{
 const get=k=>({SUPABASE_URL:'https://group.supabase.co',SUPABASE_SERVICE_ROLE_KEY:'eyJ.fake',GEMINI_API_KEY_1:'old'})[k];
 const load=createConfigLoader({get,fetchImpl:async()=>new Response('{}',{status:503})});
 await assert.rejects(load(),e=>e.code==='NOT_CONFIGURED');
});
