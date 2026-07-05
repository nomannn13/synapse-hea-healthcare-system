import { useEffect, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { Button, Card, Input } from "../components/ui";
import type { PageResponse, PatientDirectory } from "../types";
export function ClinicalPage() {
  const [patients, setPatients] = useState<PatientDirectory[]>([]);
  const [message, setMessage] = useState("");
  useEffect(() => {
    api<PageResponse<PatientDirectory>>("/patients").then((r) =>
      setPatients(r.items),
    );
  }, []);
  async function record(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    try {
      await api("/medical-records", {
        method: "POST",
        body: JSON.stringify({
          patientId: f.get("patientId"),
          diagnosis: f.get("diagnosis"),
          treatment: f.get("treatment"),
          prescription: f.get("prescription"),
          clinicalNotes: f.get("clinicalNotes"),
        }),
      });
      setMessage("Medical record created.");
      e.currentTarget.reset();
    } catch (x) {
      setMessage(x instanceof Error ? x.message : "Could not create record");
    }
  }
  async function urgent(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const number = (name: string) => {
      const v = String(f.get(name) || "");
      return v ? Number(v) : null;
    };
    try {
      await api("/urgent-cases", {
        method: "POST",
        body: JSON.stringify({
          patientId: f.get("patientId"),
          heartRate: number("heartRate"),
          oxygenSaturation: number("oxygenSaturation"),
          systolicPressure: number("systolicPressure"),
          temperature: number("temperature"),
          symptomSeverity: number("symptomSeverity"),
          reason: f.get("reason"),
        }),
      });
      setMessage("Urgent case assessed and recorded.");
      e.currentTarget.reset();
    } catch (x) {
      setMessage(
        x instanceof Error ? x.message : "Could not create urgent case",
      );
    }
  }
  return (
    <div>
      <h1 className="text-3xl font-black">Clinical workspace</h1>
      <p className="mt-2 text-slate-600">
        Create records and operational triage entries. Triage priority is not a
        diagnosis.
      </p>
      {message && (
        <p className="mt-4 rounded-xl bg-cyan-50 p-3 text-sm text-cyan-900">
          {message}
        </p>
      )}
      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <Card>
          <h2 className="font-bold">New medical record</h2>
          <form onSubmit={record} className="mt-4 space-y-3">
            <PatientSelect patients={patients} />
            <Input name="diagnosis" placeholder="Diagnosis" required />
            <Input name="treatment" placeholder="Treatment" />
            <Input name="prescription" placeholder="Prescription" />
            <textarea
              name="clinicalNotes"
              placeholder="Clinical notes"
              className="min-h-28 w-full rounded-xl border border-slate-200 p-3"
            />
            <Button className="w-full">Save medical record</Button>
          </form>
        </Card>
        <Card>
          <h2 className="font-bold">Urgent-case prioritization</h2>
          <form onSubmit={urgent} className="mt-4 grid gap-3 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <PatientSelect patients={patients} />
            </div>
            <Input name="heartRate" type="number" placeholder="Heart rate" />
            <Input
              name="oxygenSaturation"
              type="number"
              placeholder="Oxygen saturation"
            />
            <Input
              name="systolicPressure"
              type="number"
              placeholder="Systolic pressure"
            />
            <Input
              name="temperature"
              type="number"
              step="0.1"
              placeholder="Temperature"
            />
            <Input
              name="symptomSeverity"
              type="number"
              min="0"
              max="10"
              placeholder="Severity 0–10"
            />
            <Input name="reason" placeholder="Reason / symptoms" required />
            <Button className="sm:col-span-2">Assess and save</Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
function PatientSelect({ patients }: { patients: PatientDirectory[] }) {
  return (
    <select
      name="patientId"
      required
      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5"
    >
      <option value="">Select patient</option>
      {patients.map((p) => (
        <option value={p.id} key={p.id}>
          {p.name} — {p.email}
        </option>
      ))}
    </select>
  );
}
