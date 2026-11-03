export async function api(path, { method = "GET", body, ...options } = {}) {
  // Fetch the current token before each mutation: authentication can rotate it.
  const csrf = method !== "GET" ? (await api("/auth/csrf")).token : null;
  const headers = { ...(method !== "GET" ? { "X-XSRF-TOKEN": csrf } : {}) };
  if (body && !(body instanceof FormData))
    headers["Content-Type"] = "application/json";
  const response = await fetch(`/api${path}`, {
    method,
    credentials: "same-origin",
    headers,
    body:
      body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
    ...options,
  });
  const data =
    response.status === 204 || response.headers.get("content-length") === "0"
      ? null
      : await response.text();
  let parsed;
  try {
    parsed = data ? JSON.parse(data) : null;
  } catch {
    parsed = null;
  }
  if (!response.ok) {
    throw new Error(
      parsed?.message || `Không thể thực hiện yêu cầu (${response.status})`,
    );
  }
  return parsed;
}
export async function getHealth(options) {
  return api("/health", options);
}
