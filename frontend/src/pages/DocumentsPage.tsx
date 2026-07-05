import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api, apiBlob, saveBlob } from "../api/client";
import { useAuth } from "../auth/auth-context";
import { Button, Card, Empty, Input } from "../components/ui";
import type { ClinicalDocument, PageResponse, PatientDirectory } from "../types";

export function DocumentsPage() {
  const { user } = useAuth();
  const [documents, setDocuments] = useState<ClinicalDocument[]>([]);
  const [patients, setPatients] = useState<PatientDirectory[]>([]);
  const [patientId, setPatientId] = useState("");
  const [message, setMessage] = useState("");
  const staff = user?.role !== "PATIENT";

  const load = useCallback(async (target?: string) => {
    if (staff && !target) { setDocuments([]); return; }
    const suffix = staff ? `?patientId=${target}` : "";
    const result = await api<PageResponse<ClinicalDocument>>(`/documents${suffix}`);
    setDocuments(result.items);
  }, [staff]);

  useEffect(() => {
    let active = true;
    if (staff) {
      void api<PageResponse<PatientDirectory>>("/patients?size=100").then((r) => { if (active) setPatients(r.items); });
    } else {
      void api<PageResponse<ClinicalDocument>>("/documents").then((r) => { if (active) setDocuments(r.items); });
    }
    return () => { active = false; };
  }, [staff]);

  async function upload(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    if (staff) form.set("patientId", patientId);
    try {
      await api("/documents", { method: "POST", body: form });
      setMessage("Document uploaded securely.");
      e.currentTarget.reset();
      await load(patientId);
    } catch (x) { setMessage(x instanceof Error ? x.message : "Could not upload document"); }
  }

  async function download(item: ClinicalDocument) {
    const result = await apiBlob(`/documents/${item.id}/download`);
    saveBlob(result.blob, result.filename || item.originalName);
  }

  return <div>
    <h1 className="text-3xl font-black">Clinical documents</h1>
    <p className="mt-2 text-slate-600">Upload and retrieve PDF, JPEG, PNG, or TXT reports with access control and audit logging.</p>
    {message && <p className="mt-4 rounded-xl bg-cyan-50 p-3 text-sm text-cyan-900">{message}</p>}
    {staff && <div className="mt-6"><label className="mb-2 block text-sm font-semibold">Patient workspace</label><select value={patientId} onChange={(e) => { setPatientId(e.target.value); void load(e.target.value); }} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5"><option value="">Select patient</option>{patients.map((p) => <option key={p.id} value={p.id}>{p.name} — {p.email}</option>)}</select></div>}
    {(!staff || patientId) && <Card className="mt-6"><h2 className="font-bold">Upload document</h2><form onSubmit={upload} className="mt-4 grid gap-3 md:grid-cols-2">
      <select name="category" required className="rounded-xl border border-slate-200 bg-white px-3 py-2.5"><option value="LAB_REPORT">Lab report</option><option value="IMAGING">Imaging</option><option value="DISCHARGE_SUMMARY">Discharge summary</option><option value="REFERRAL">Referral</option><option value="INSURANCE">Insurance</option><option value="OTHER">Other</option></select>
      <Input name="file" type="file" accept=".pdf,.jpg,.jpeg,.png,.txt" required />
      <Input name="notes" placeholder="Document note" className="md:col-span-2" />
      <Button className="md:col-span-2">Upload securely</Button>
    </form></Card>}
    <div className="mt-6 space-y-3">{documents.length === 0 && <Empty>{staff && !patientId ? "Select a patient to view documents." : "No documents uploaded."}</Empty>}
      {documents.map((d) => <Card key={d.id}><div className="flex flex-wrap items-center justify-between gap-3"><div><p className="font-bold">{d.originalName}</p><p className="mt-1 text-sm text-slate-500">{d.category.replaceAll("_", " ")} · {(d.sizeBytes / 1024).toFixed(1)} KB · {new Date(d.createdAt).toLocaleString()}</p>{d.notes && <p className="mt-2 text-sm">{d.notes}</p>}</div><Button type="button" onClick={() => void download(d)}>Download</Button></div></Card>)}
    </div>
  </div>;
}
