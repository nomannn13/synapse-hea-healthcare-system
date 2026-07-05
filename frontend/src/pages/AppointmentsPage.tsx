import { useEffect, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { useAuth } from "../auth/auth-context";
import { Button, Card, Empty, Input } from "../components/ui";
import type { Appointment, Doctor, PageResponse } from "../types";
export function AppointmentsPage() {
  const { user } = useAuth();
  const [items, setItems] = useState<Appointment[]>([]);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [error, setError] = useState("");
  const load = () =>
    api<PageResponse<Appointment>>("/appointments").then((r) =>
      setItems(r.items),
    );
  useEffect(() => {
    void load();
    if (user?.role === "PATIENT")
      api<PageResponse<Doctor>>("/doctors").then((r) => setDoctors(r.items));
  }, [user?.role]);
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    try {
      await api("/appointments", {
        method: "POST",
        body: JSON.stringify({
          doctorId: f.get("doctorId"),
          startAt: new Date(String(f.get("startAt"))).toISOString(),
          reason: f.get("reason"),
        }),
      });
      e.currentTarget.reset();
      await load();
    } catch (x) {
      setError(x instanceof Error ? x.message : "Booking failed");
    }
  }
  async function status(id: string, value: string) {
    try {
      await api(`/appointments/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: value, notes: "" }),
      });
      await load();
    } catch (x) {
      setError(x instanceof Error ? x.message : "Update failed");
    }
  }
  return (
    <div>
      <h1 className="text-3xl font-black">Appointments</h1>
      {user?.role === "PATIENT" && (
        <Card className="mt-6">
          <h2 className="font-bold">Book an appointment</h2>
          <form onSubmit={submit} className="mt-4 grid gap-3 md:grid-cols-4">
            <select
              name="doctorId"
              required
              className="rounded-xl border border-slate-200 px-3 py-2.5"
            >
              <option value="">Choose doctor</option>
              {doctors.map((d) => (
                <option value={d.id} key={d.id}>
                  {d.name} — {d.specialization}
                </option>
              ))}
            </select>
            <Input name="startAt" type="datetime-local" required />
            <Input name="reason" placeholder="Reason" required />
            <Button>Book</Button>
          </form>
        </Card>
      )}
      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      <div className="mt-6 space-y-3">
        {items.map((a) => (
          <Card key={a.id}>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="font-bold">
                  {user?.role === "DOCTOR" ? a.patientName : a.doctorName}
                </h2>
                <p className="text-sm text-slate-500">
                  {a.department} · {new Date(a.startAt).toLocaleString()}
                </p>
              </div>
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold">
                {a.status}
              </span>
            </div>
            <p className="mt-3 text-sm">{a.reason}</p>
            {user?.role === "DOCTOR" &&
              a.status !== "COMPLETED" &&
              a.status !== "CANCELLED" && (
                <div className="mt-4 flex gap-2">
                  <Button onClick={() => void status(a.id, "CONFIRMED")}>
                    Confirm
                  </Button>
                  <Button
                    onClick={() => void status(a.id, "COMPLETED")}
                    className="bg-cyan-700"
                  >
                    Complete
                  </Button>
                </div>
              )}
          </Card>
        ))}
        {!items.length && <Empty>No appointments yet.</Empty>}
      </div>
    </div>
  );
}
