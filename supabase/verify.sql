-- Read-only verification after the migration. Expect 3 rows with RLS=true and policies>0.
select c.relname as table_name,c.relrowsecurity as rls_enabled,
 (select count(*) from pg_policies p where p.schemaname='public' and p.tablename=c.relname) as policies
from pg_class c join pg_namespace n on n.oid=c.relnamespace
where n.nspname='public' and c.relname in ('mentor_profiles','mentor_preferences','mentor_timetable');
