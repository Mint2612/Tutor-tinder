export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export async function api(path, { method = "GET", body, ...options } = {}) {
  method = method.toUpperCase();
  // Fetch the current token before each mutation: authentication can rotate it.
  const csrf =
    method !== "GET"
      ? (await api("/auth/csrf", { signal: options.signal })).token
      : null;
  const headers = { ...(method !== "GET" ? { "X-XSRF-TOKEN": csrf } : {}) };
  if (body && !(body instanceof FormData))
    headers["Content-Type"] = "application/json";
  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      credentials: "same-origin",
      headers,
      body:
        body instanceof FormData
          ? body
          : body
            ? JSON.stringify(body)
            : undefined,
      ...options,
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new ApiError(
      "Không thể kết nối. Vui lòng kiểm tra mạng và thử lại.",
      0,
    );
  }
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
    if (response.status === 401 && !path.startsWith("/auth/"))
      window.dispatchEvent(new Event("session-expired"));
    throw new ApiError(
      parsed?.message || `Không thể thực hiện yêu cầu (${response.status})`,
      response.status,
    );
  }
  return parsed;
}
export async function getHealth(options) {
  return api("/health", options);
}
