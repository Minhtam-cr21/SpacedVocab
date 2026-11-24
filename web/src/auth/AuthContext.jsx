import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { getTokens, onSessionExpired, setTokens } from '../api/client.js';
import { authApi, meApi } from '../api/endpoints.js';

const AuthContext = createContext(null);

function keepSession(response) {
  setTokens({ accessToken: response.accessToken, refreshToken: response.refreshToken });
  return response.user;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(() => Boolean(getTokens()));

  useEffect(() => {
    onSessionExpired(() => setUser(null));
    if (!getTokens()) return;
    meApi.get()
      .then(setUser)
      .catch(() => setTokens(null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email, password) => {
    setUser(keepSession(await authApi.login({ email, password })));
  }, []);

  const register = useCallback(async (form) => {
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    setUser(keepSession(await authApi.register({ ...form, timezone })));
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = getTokens()?.refreshToken;
    setTokens(null);
    setUser(null);
    if (refreshToken) await authApi.logout(refreshToken).catch(() => {});
  }, []);

  const value = useMemo(() => ({ user, loading, login, register, logout }), [user, loading, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
