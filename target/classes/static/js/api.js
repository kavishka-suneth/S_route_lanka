(function () {
  class ApiError extends Error {
    constructor(message, status, data) {
      super(message);
      this.name = "ApiError";
      this.status = status;
      this.data = data;
      this.errors = data && data.errors ? data.errors : null;
    }
  }

  function toQuery(params) {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
      if (value === undefined || value === null || value === "") return;
      query.set(key, value);
    });
    const text = query.toString();
    return text ? "?" + text : "";
  }

  function endpoint(path, params) {
    const base = window.SmartRouteConfig.apiBase;
    const normalized = path.startsWith("/api/") ? path : base + (path.startsWith("/") ? path : "/" + path);
    return normalized + toQuery(params);
  }

  async function parseResponse(response) {
    if (response.status === 204) return null;
    const type = response.headers.get("content-type") || "";
    if (type.includes("application/json")) {
      try {
        return await response.json();
      } catch (error) {
        return null;
      }
    }
    const text = await response.text();
    return text || null;
  }

  function messageFrom(data, fallback) {
    if (!data) return fallback;
    if (data.message) {
      if (data.errors) {
        const fieldMessages = Object.entries(data.errors)
          .map(([field, message]) => field + ": " + message)
          .join("; ");
        return fieldMessages ? data.message + " - " + fieldMessages : data.message;
      }
      return data.message;
    }
    if (typeof data === "string") return data;
    return fallback;
  }

  async function request(path, options) {
    const opts = options || {};
    const headers = new Headers(opts.headers || {});
    const body = opts.body;

    if (body != null && !(body instanceof FormData) && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }

    const token = window.Auth.getToken();
    if (token && !headers.has("Authorization")) {
      headers.set("Authorization", "Bearer " + token);
    }

    const response = await fetch(endpoint(path, opts.params), {
      method: opts.method || "GET",
      headers,
      body: body == null || body instanceof FormData ? body : JSON.stringify(body)
    });
    const data = await parseResponse(response);

    if (!response.ok) {
      if (response.status === 401 && opts.redirectOnUnauthorized !== false) {
        window.Auth.clearSession();
        const current = window.location.pathname + window.location.search;
        window.location.assign("/login.html?redirect=" + encodeURIComponent(current));
      }
      throw new ApiError(messageFrom(data, response.statusText || "Request failed"), response.status, data);
    }
    return data;
  }

  window.Api = {
    ApiError,
    request,
    get: (path, params, options) => request(path, { ...(options || {}), method: "GET", params }),
    post: (path, body, options) => request(path, { ...(options || {}), method: "POST", body }),
    put: (path, body, options) => request(path, { ...(options || {}), method: "PUT", body }),
    patch: (path, body, options) => request(path, { ...(options || {}), method: "PATCH", body }),
    delete: (path, options) => request(path, { ...(options || {}), method: "DELETE" }),
    toQuery
  };
})();
