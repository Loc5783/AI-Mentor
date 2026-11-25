import {cleanTeamFiles} from './cleanup.mjs';

Deno.serve(async request => {
  if (request.method !== 'POST') return new Response('Method not allowed', {status: 405});
  const secret = Deno.env.get('TEAM_CLEANUP_SECRET');
  if (!secret || request.headers.get('x-team-cleanup-secret') !== secret) return new Response('Unauthorized', {status: 401});
  const base = Deno.env.get('SUPABASE_URL');
  const key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');
  if (!base || !key) return new Response('Not configured', {status: 503});
  try {
    const result = await cleanTeamFiles(base, key);
    return Response.json(result, {status: result.failed ? 503 : 200});
  } catch {
    return Response.json({error: 'CLEANUP_FAILED'}, {status: 503});
  }
});
