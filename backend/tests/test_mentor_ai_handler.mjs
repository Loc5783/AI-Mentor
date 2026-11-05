import test from 'node:test';
import assert from 'node:assert/strict';

let handler;let calls=[];let quotaStatus=200;let statuses=[429,503,400,200];
let contentMode='ok';
const previousFetch=globalThis.fetch;
const env={SUPABASE_URL:'https://test.supabase.co',SUPABASE_ANON_KEY:'public-mock',SUPABASE_SERVICE_ROLE_KEY:'eyJ.mock-service'};
globalThis.Deno={env:{get:k=>env[k]},serve:fn=>{handler=fn;}};
globalThis.fetch=async(url,options={})=>{
 calls.push({url,options});
 if(url.endsWith('/auth/v1/user'))return options.headers.Authorization==='Bearer test-user'?Response.json({id:'mock-user'}):new Response('{}',{status:401});
 if(url.endsWith('/mentor_ai_server_config'))return Response.json({keys:['slot-one','slot-two','slot-three','slot-four'],model:'model'});
 if(url.includes('/mentor_preferences?'))return Response.json([{grade:10,language:'vi'}]);
 if(url.endsWith('/mentor_reserve_ai_request'))return new Response('{}',{status:quotaStatus});
 if(url.startsWith('https://generativelanguage.googleapis.com/')){
   const status=statuses.shift()??200;
   if(status===400)return Response.json({error:{details:[{reason:'API_KEY_INVALID'}]}},{status});
   if(status!==200)return new Response('{}',{status});
   return Response.json(contentMode==='ok'?{candidates:[{content:{parts:[{text:'OK'}]},finishReason:'STOP'}]}:{promptFeedback:{blockReason:'SAFETY'}});
 }
 throw Error('Unexpected endpoint');
};
await import('../../supabase/functions/mentor-ai/index.ts');
const request=()=>new Request('https://test/functions/v1/mentor-ai',{method:'POST',headers:{Authorization:'Bearer test-user'},
 body:JSON.stringify({turns:[{role:'user',text:'Hello',files:[]}]})});

test('unauthorized requests cannot read Vault or call provider',async()=>{
 calls=[];
 const r=await handler(new Request('https://test/functions/v1/mentor-ai',{method:'POST',body:'{}'}));
 assert.equal(r.status,401);assert.equal(calls.length,0);
});

test('handler fails over four keys but reserves user quota only once',async()=>{
 calls=[];
 const r=await handler(request());assert.equal(r.status,200);assert.deepEqual(await r.json(),{text:'OK',truncated:false});
 assert.equal(calls.filter(c=>c.url.endsWith('/mentor_reserve_ai_request')).length,1);
 const provider=calls.filter(c=>c.url.startsWith('https://generativelanguage.googleapis.com/'));
 assert.deepEqual(provider.map(c=>c.options.headers['x-goog-api-key']),['slot-one','slot-two','slot-three','slot-four']);
});

test('invalid bearer rejected before secret loading',async()=>{
 calls=[];
 const r=await handler(new Request('https://test/functions/v1/mentor-ai',{method:'POST',headers:{Authorization:'Bearer invalid'},body:'{}'}));
 assert.equal(r.status,401);assert.equal(calls.length,1);assert(calls[0].url.endsWith('/auth/v1/user'));
});

test('user quota denial cannot be bypassed by key switching',async()=>{
 calls=[];quotaStatus=429;
 const r=await handler(request());assert.equal(r.status,429);
 assert.equal(calls.filter(c=>c.url.startsWith('https://generativelanguage.googleapis.com/')).length,0);
 quotaStatus=200;
});

test('safety empty response never tries other keys',async()=>{
 calls=[];contentMode='safety';
 const r=await handler(request());assert.equal(r.status,502);
 assert.equal(calls.filter(c=>c.url.startsWith('https://generativelanguage.googleapis.com/')).length,1);
 assert.equal((await r.json()).error_code,'EMPTY_RESPONSE');
 globalThis.fetch=previousFetch;
});
