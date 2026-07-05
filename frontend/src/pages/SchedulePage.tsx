import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { Button, Card, Empty, Input } from "../components/ui";
import type { Slot } from "../types";

export function SchedulePage() {
  const [slots, setSlots] = useState<Slot[]>([]);
  const [message, setMessage] = useState("");
  const load = useCallback(() => api<Slot[]>("/doctors/me/availability").then(setSlots), []);
  useEffect(() => { void load(); }, [load]);
  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    try {
      await api("/doctors/me/availability", { method: "POST", body: JSON.stringify({
        startAt: new Date(String(f.get("startAt"))).toISOString(),
        endAt: new Date(String(f.get("endAt"))).toISOString(),
      }) });
      e.currentTarget.reset(); setMessage("Availability block added."); await load();
    } catch (x) { setMessage(x instanceof Error ? x.message : "Could not add schedule block"); }
  }
  async function remove(id?: string) { if (!id) return; await api(`/doctors/me/availability/${id}`, { method: "DELETE" }); await load(); }
  return <div><h1 className="text-3xl font-black">Doctor schedule</h1><p className="mt-2 text-slate-600">Availability blocks drive appointment slot generation and overlap prevention.</p>
    {message && <p className="mt-4 rounded-xl bg-cyan-50 p-3 text-sm text-cyan-900">{message}</p>}
    <Card className="mt-6"><h2 className="font-bold">Add availability block</h2><form onSubmit={create} className="mt-4 grid gap-3 sm:grid-cols-2"><Input name="startAt" type="datetime-local" required /><Input name="endAt" type="datetime-local" required /><Button className="sm:col-span-2">Add availability</Button></form></Card>
    <div className="mt-6 space-y-3">{slots.length === 0 && <Empty>No custom availability configured. The system will use weekday defaults.</Empty>}{slots.map((s) => <Card key={s.id ?? s.startAt}><div className="flex items-center justify-between gap-3"><div><p className="font-semibold">{new Date(s.startAt).toLocaleString()}</p><p className="text-sm text-slate-500">to {new Date(s.endAt).toLocaleString()}</p></div>{s.id && <button className="rounded-xl border px-4 py-2 text-sm" onClick={() => void remove(s.id)}>Remove</button>}</div></Card>)}</div>
  </div>;
}
