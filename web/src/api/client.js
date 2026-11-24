/**
 * Thin fetch wrapper for the SpacedVocab API (same origin: /api is proxied by nginx or the Vite dev server).
 * - Sends the access token; on 401 it rotates the refresh token once and retries.
 * - Every error becomes an ApiError carrying the backend's { code, message, field, errors }.
 */
const STORAGE_KEY = 'spacedvocab.tokens';
const NETWORK_MESSAGE = 'Không kết nối được tới máy chủ. Kiểm tra mạng rồi thử lại.';

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại sau.');
    this.status = status;
    this.code = body?.code ?? 'UNKNOWN';
    this.field = body?.field ?? null;
    this.errors = body?.errors ?? [];
  }

  /** Field → message map for VALIDATION_FAILED responses. */
  fieldErrors() {
    return Object.fromEntries(this.errors.map((e) => [e.field, e.message]));
  }
}

function readTokens() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY));
  } catch {
    return null;
  }
}

let tokens = readTokens();
let sessionExpiredListener = () => {};
let refreshInFlight = null;

export function setTokens(next) {
  tokens = next;
  try {
    if (next) localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // storage may be blocked (private mode); the session then lasts until the tab closes
  }
}

export const getTokens = () => tokens;

export function onSessionExpired(listener) {
  sessionExpiredListener = listener;
}

async function send(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth && tokens?.accessToken) headers.Authorization = `Bearer ${tokens.accessToken}`;
  try {
    return await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, { code: 'NETWORK', message: NETWORK_MESSAGE });
  }
}

/** Rotates the refresh token; concurrent 401s share one refresh call. */
function refreshTokens() {
  if (!tokens?.refreshToken) return Promise.resolve(false);
  refreshInFlight ??= (async () => {
    const res = await send('/api/auth/refresh', {
      method: 'POST',
      body: { refreshToken: tokens.refreshToken },
      auth: false,
    }).catch(() => null);
    if (!res?.ok) return false;
    const data = await res.json();
    setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
    return true;
  })().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

export async function api(path, options = {}) {
  const auth = options.auth ?? true;
  let res = await send(path, options);

  if (res.status === 401 && auth && tokens) {
    if (await refreshTokens()) {
      res = await send(path, options);
    }
    if (res.status === 401) {
      setTokens(null);
      sessionExpiredListener();
    }
  }

  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}
