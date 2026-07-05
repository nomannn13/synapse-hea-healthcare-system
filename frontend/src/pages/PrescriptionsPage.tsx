import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api, apiBlob, saveBlob } from "../api/client";
import { useAuth } from "../auth/auth-context";
import { Badge, Button, Card, Empty, Input } from "../components/ui";
import type { PageResponse, PatientDirectory, Prescription } from "../types";

export function PrescriptionsPage() {
  const { user } = useAuth();
  const [items, setItems] = useState<Prescription[]>([]);
  const [patients, setPatients] = useState<PatientDirectory[]>([]);
  const [message, setMessage] = useState("");
  const load = useCallback(() => api<PageResponse<Prescription>>("/prescriptions").then((r) => setItems(r.items)), []);
  useEffect(() => {
    void load();
    if (user?.role !== "PATIENT") void api<PageResponse<PatientDirectory>>("/patients?size=100").then((r) => setPatients(r.items));
  }, [load, user?.role]);

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    try {
      await api("/prescriptions", { method: "POST", body: JSON.stringify({
        patientId: f.get("patientId"), medication: undefined,
        items: [{ medicationName: f.get("medicationName"), dosage: f.get("dosage"),
          frequency: f.get("frequency"), durationDays: Number(f.get("durationDays")), instructions: f.get("instructions") }],
        notes: f.get("notes"),
      }) });
      e.currentTarget.reset(); setMessage("Draft prescription created."); await load();
    } catch (x) { setMessage(x instanceof Error ? x.message : "Could not create prescription"); }
  }
  async function status(id: string, value: "ISSUED" | "CANCELLED") {
    try { await api(`/prescriptions/${id}/status`, { method: "PATCH", body: JSON.stringify({ status: value }) }); await load(); }
    catch (x) { setMessage(x instanceof Error ? x.message : "Could not update prescription"); }
  }
  async function pdf(item: Prescription) {
    const result = await apiBlob(`/prescriptions/${item.id}/pdf`);
    saveBlob(result.blob, result.filename || `${item.prescriptionNumber}.pdf`);
  }
  return <div>
    <h1 className="text-3xl font-black">Prescriptions</h1>
    <p className="mt-2 text-slate-600">Structured medications, controlled issuing, patient notifications, and printable PDFs.</p>
    {message && <p className="mt-4 rounded-xl bg-cyan-50 p-3 text-sm text-cyan-900">{message}</p>}
    {user?.role === "DOCTOR" && <Card className="mt-6"><h2 className="font-bold">New prescription</h2>
      <form onSubmit={create} className="mt-4 grid gap-3 md:grid-cols-2">
        <select name="patientId" required className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 md:col-span-2"><option value="">Select patient</option>{patients.map((p) => <option key={p.id} value={p.id}>{p.name} — {p.email}</option>)}</select>
        <Input name="medicationName" placeholder="Medication" required /><Input name="dosage" placeholder="Dosage, e.g. 10 mg" required />
        <Input name="frequency" placeholder="Frequency, e.g. twice daily" required /><Input name="durationDays" type="number" min="1" max="3650" placeholder="Duration in days" required />
        <Input name="instructions" placeholder="Instructions" /><Input name="notes" placeholder="Prescription notes" />
        <Button className="md:col-span-2">Save draft</Button>
      </form></Card>}
    <div className="mt-6 space-y-4">{items.length === 0 && <Empty>No prescriptions yet.</Empty>}
      {items.map((p) => <Card key={p.id}><div className="flex flex-wrap justify-between gap-3"><div><div className="flex items-center gap-2"><h2 className="font-bold">{p.prescriptionNumber}</h2><Badge>{p.status}</Badge></div><p className="mt-1 text-sm text-slate-500">{p.patientName} · Dr. {p.doctorName}</p></div><p className="text-sm text-slate-500">{new Date(p.createdAt).toLocaleDateString()}</p></div>
        <div className="mt-4 space-y-2">{p.items.map((x) => <div key={x.id} className="rounded-xl bg-slate-50 p-3"><p className="font-semibold">{x.medicationName}</p><p className="text-sm text-slate-600">{x.dosage} · {x.frequency} · {x.durationDays ?? "As directed"} days</p>{x.instructions && <p className="mt-1 text-sm">{x.instructions}</p>}</div>)}</div>
        <div className="mt-4 flex flex-wrap gap-2"><Button type="button" onClick={() => void pdf(p)}>Download PDF</Button>{user?.role !== "PATIENT" && p.status === "DRAFT" && <Button type="button" onClick={() => void status(p.id, "ISSUED")}>Issue to patient</Button>}{user?.role !== "PATIENT" && p.status !== "CANCELLED" && <button className="rounded-xl border px-4 py-2 text-sm" onClick={() => void status(p.id, "CANCELLED")}>Cancel</button>}</div>
      </Card>)}</div>
  </div>;
}
