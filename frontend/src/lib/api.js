/** Use relative /api paths: Vite proxies locally; nginx proxies in Docker. */
export async function getHealth({ signal } = {}) {
  const response = await fetch('/api/health', { signal })
  if (!response.ok) throw new Error(`Health request failed (${response.status})`)
  return response.json()
}
