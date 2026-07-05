import type { SessionResponse } from "../types";
const BASE = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
let accessToken: string | null = null;
export const tokenStore = {
  set(value: string | null) {
    accessToken = value;
  },
  get() {
    return accessToken;
  },
};

async function refresh(): Promise<boolean> {
  const response = await fetch(`${BASE}/auth/refresh`, {
    method: "POST",
    credentials: "include",
  });
  if (!response.ok) {
    accessToken = null;
    return false;
  }
  const session = (await response.json()) as SessionResponse;
  accessToken = session.accessToken;
  return true;
}

async function request(path: string, options: RequestInit, retry: boolean) {
  const headers = new Headers(options.headers);
  const isFormData = options.body instanceof FormData;
  if (options.body && !isFormData && !headers.has("Content-Type"))
    headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  const response = await fetch(`${BASE}${path}`, {
    ...options,
    headers,
    credentials: "include",
  });
  if (response.status === 401 && retry && (await refresh()))
    return request(path, options, false);
  return response;
}

export async function api<T>(
  path: string,
  options: RequestInit = {},
  retry = true,
): Promise<T> {
  const response = await request(path, options, retry);
  if (!response.ok) {
    const body = await response
      .json()
      .catch(() => ({ message: "Request failed" }));
    throw new Error(body.message ?? `Request failed (${response.status})`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export async function apiBlob(
  path: string,
  options: RequestInit = {},
): Promise<{ blob: Blob; filename?: string }> {
  const response = await request(path, options, true);
  if (!response.ok) {
    const body = await response
      .json()
      .catch(() => ({ message: "Download failed" }));
    throw new Error(body.message ?? `Download failed (${response.status})`);
  }
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^"';]+)|filename=["']?([^"';]+)/i);
  return {
    blob: await response.blob(),
    filename: decodeURIComponent(match?.[1] ?? match?.[2] ?? ""),
  };
}

export function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export { BASE };
