import { useEffect, useState } from "react";
import { api } from "../api/client";
import { Card, Empty } from "../components/ui";
import type { NotificationItem } from "../types";
export function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const load = () => api<NotificationItem[]>("/notifications").then(setItems);
  useEffect(() => {
    void load();
  }, []);
  async function read(n: NotificationItem) {
    if (!n.readAt) {
      await api(`/notifications/${n.id}/read`, { method: "PATCH" });
      await load();
    }
  }
  return (
    <div>
      <h1 className="text-3xl font-black">Notifications</h1>
      <div className="mt-6 space-y-3">
        {items.map((n) => (
          <button
            key={n.id}
            onClick={() => void read(n)}
            className="block w-full text-left"
          >
            <Card className={!n.readAt ? "border-cyan-300 bg-cyan-50/40" : ""}>
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h2 className="font-bold">{n.title}</h2>
                  <p className="mt-1 text-sm text-slate-600">{n.message}</p>
                </div>
                <span className="text-xs text-slate-400">
                  {new Date(n.createdAt).toLocaleString()}
                </span>
              </div>
            </Card>
          </button>
        ))}
        {!items.length && <Empty>You are all caught up.</Empty>}
      </div>
    </div>
  );
}
