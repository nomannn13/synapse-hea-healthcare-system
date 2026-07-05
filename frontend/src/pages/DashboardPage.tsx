import { useEffect, useState } from "react";
import { api } from "../api/client";
import { Card } from "../components/ui";
import { useAuth } from "../auth/auth-context";
export function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<Record<string, unknown>>({});
  useEffect(() => {
    api<Record<string, unknown>>("/dashboard")
      .then(setData)
      .catch(() => setData({}));
  }, []);
  return (
    <div>
      <p className="text-sm font-semibold text-cyan-700">
        {new Date().toLocaleDateString(undefined, {
          weekday: "long",
          month: "long",
          day: "numeric",
        })}
      </p>
      <h1 className="mt-1 text-3xl font-black">
        Good to see you, {user?.firstName}
      </h1>
      <p className="mt-2 text-slate-600">
        Here is the latest operational view for your role.
      </p>
      <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Object.entries(data)
          .filter(([, v]) => typeof v === "number")
          .map(([k, v]) => (
            <Card key={k}>
              <p className="text-sm text-slate-500">{human(k)}</p>
              <p className="mt-2 text-3xl font-black">{String(v)}</p>
            </Card>
          ))}
      </div>
      <Card className="mt-6">
        <h2 className="font-bold">Engineering note</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          This dashboard is backed by role-specific API aggregation. It does not
          fetch every table into the browser and calculate totals client-side.
        </p>
      </Card>
    </div>
  );
}
function human(s: string) {
  return s.replace(/([A-Z])/g, " $1").replace(/^./, (c) => c.toUpperCase());
}
