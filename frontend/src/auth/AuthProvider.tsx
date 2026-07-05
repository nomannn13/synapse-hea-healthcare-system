import { useEffect, useRef, useState, type ReactNode } from "react";
import { api, tokenStore } from "../api/client";
import type { SessionResponse, User } from "../types";
import { AuthContext, type AuthValue } from "./auth-context";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);
  const bootstrapped = useRef(false);

  useEffect(() => {
    if (bootstrapped.current) return;
    bootstrapped.current = true;

    api<SessionResponse>("/auth/refresh", { method: "POST" }, false)
      .then((session) => {
        tokenStore.set(session.accessToken);
        setUser(session.user);
      })
      .catch(() => null)
      .finally(() => setReady(true));
  }, []);

  async function applySession(path: string, body: unknown): Promise<void> {
    const session = await api<SessionResponse>(
      path,
      { method: "POST", body: JSON.stringify(body) },
      false,
    );
    tokenStore.set(session.accessToken);
    setUser(session.user);
  }

  const value: AuthValue = {
    user,
    ready,
    login: (email, password) =>
      applySession("/auth/login", { email, password }),
    register: (data) => applySession("/auth/register", data),
    logout: async () => {
      await api("/auth/logout", { method: "POST" });
      tokenStore.set(null);
      setUser(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
