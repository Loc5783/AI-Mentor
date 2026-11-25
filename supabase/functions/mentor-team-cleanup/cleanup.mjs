const headers = key => ({apikey: key, Authorization: `Bearer ${key}`});

export async function cleanTeamFiles(base, key, fetcher = fetch) {
  const root = base.replace(/\/$/, '');
  const jobsResponse = await fetcher(`${root}/rest/v1/mentor_team_file_cleanup?select=project_id,file_paths&order=created_at.asc&limit=100`, {headers: headers(key)});
  if (!jobsResponse.ok) throw new Error(`LIST_JOBS_${jobsResponse.status}`);
  const jobs = await jobsResponse.json();
  let completed = 0;
  let failed = 0;
  for (const job of jobs) {
    try {
      const project = encodeURIComponent(job.project_id);
      const projectResponse = await fetcher(`${root}/rest/v1/mentor_team_projects?id=eq.${project}&select=id`, {headers: headers(key)});
      if (!projectResponse.ok) throw new Error(`READ_PROJECT_${projectResponse.status}`);
      if ((await projectResponse.json()).length) continue;
      if (!Array.isArray(job.file_paths)) throw new Error('INVALID_FILE_PATHS');
      for (const path of job.file_paths) {
        if (typeof path !== 'string' || !path.startsWith(`${job.project_id}/`) || path.includes('..')) throw new Error('INVALID_FILE_PATH');
        const objectPath = path.split('/').map(encodeURIComponent).join('/');
        const deleted = await fetcher(`${root}/storage/v1/object/mentor-team-files/${objectPath}`, {method: 'DELETE', headers: headers(key)});
        if (!deleted.ok && deleted.status !== 404) throw new Error(`DELETE_FILE_${deleted.status}`);
      }
      const cleared = await fetcher(`${root}/rest/v1/mentor_team_file_cleanup?project_id=eq.${project}`, {method: 'DELETE', headers: headers(key)});
      if (!cleared.ok) throw new Error(`DELETE_JOB_${cleared.status}`);
      completed++;
    } catch {
      failed++;
    }
  }
  return {completed, failed};
}
