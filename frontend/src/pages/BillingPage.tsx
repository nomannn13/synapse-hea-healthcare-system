import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api, apiBlob, saveBlob } from "../api/client";
import { useAuth } from "../auth/auth-context";
import { Badge, Button, Card, Empty, Input } from "../components/ui";
import type { Invoice, PageResponse, PatientDirectory } from "../types";

export function BillingPage() {
  const { user } = useAuth();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [patients, setPatients] = useState<PatientDirectory[]>([]);
  const [message, setMessage] = useState("");
  const load = useCallback(() =>
    api<PageResponse<Invoice>>("/invoices").then((r) => setInvoices(r.items)), []);

  useEffect(() => {
    void load();
    if (user?.role === "ADMIN")
      void api<PageResponse<PatientDirectory>>("/patients?size=100").then((r) => setPatients(r.items));
  }, [load, user?.role]);

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    try {
      await api<Invoice>("/invoices", {
        method: "POST",
        body: JSON.stringify({
          patientId: f.get("patientId"),
          currency: "EUR",
          dueDate: f.get("dueDate") || null,
          items: [{
            description: f.get("description"),
            quantity: Number(f.get("quantity")),
            unitPrice: Number(f.get("unitPrice")),
          }],
          taxPercent: Number(f.get("taxPercent") || 0),
          discountAmount: Number(f.get("discountAmount") || 0),
          notes: f.get("notes"),
        }),
      });
      e.currentTarget.reset();
      setMessage("Draft invoice created.");
      await load();
    } catch (x) { setMessage(x instanceof Error ? x.message : "Could not create invoice"); }
  }

  async function changeStatus(id: string, status: "ISSUED" | "PAID" | "CANCELLED") {
    try {
      await api(`/invoices/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) });
      await load();
    } catch (x) { setMessage(x instanceof Error ? x.message : "Could not update invoice"); }
  }

  async function download(invoice: Invoice) {
    const result = await apiBlob(`/invoices/${invoice.id}/pdf`);
    saveBlob(result.blob, result.filename || `${invoice.invoiceNumber}.pdf`);
  }

  return <div>
    <h1 className="text-3xl font-black">Billing & invoices</h1>
    <p className="mt-2 text-slate-600">Traceable invoice lifecycle with server-calculated totals and PDF export.</p>
    {message && <p className="mt-4 rounded-xl bg-cyan-50 p-3 text-sm text-cyan-900">{message}</p>}
    {user?.role === "ADMIN" && <Card className="mt-6">
      <h2 className="font-bold">Create invoice</h2>
      <form onSubmit={create} className="mt-4 grid gap-3 md:grid-cols-3">
        <select name="patientId" required className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 md:col-span-2">
          <option value="">Select patient</option>
          {patients.map((p) => <option key={p.id} value={p.id}>{p.name} — {p.email}</option>)}
        </select>
        <Input name="dueDate" type="date" />
        <Input name="description" placeholder="Service description" required />
        <Input name="quantity" type="number" min="0.01" step="0.01" defaultValue="1" required />
        <Input name="unitPrice" type="number" min="0" step="0.01" placeholder="Unit price" required />
        <Input name="taxPercent" type="number" min="0" max="100" step="0.01" defaultValue="22" />
        <Input name="discountAmount" type="number" min="0" step="0.01" defaultValue="0" />
        <Input name="notes" placeholder="Internal or patient-facing note" />
        <Button className="md:col-span-3">Create draft invoice</Button>
      </form>
    </Card>}
    <div className="mt-6 space-y-4">
      {invoices.length === 0 && <Empty>No invoices yet.</Empty>}
      {invoices.map((invoice) => <Card key={invoice.id}>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div><div className="flex items-center gap-2"><h2 className="font-bold">{invoice.invoiceNumber}</h2><Badge>{invoice.status}</Badge></div>
            <p className="mt-1 text-sm text-slate-500">{invoice.patientName} · {new Date(invoice.createdAt).toLocaleDateString()}</p></div>
          <p className="text-2xl font-black">{invoice.currency} {Number(invoice.totalAmount).toFixed(2)}</p>
        </div>
        <div className="mt-4 divide-y text-sm">
          {invoice.items.map((item) => <div key={item.id} className="flex justify-between py-2"><span>{item.description} × {item.quantity}</span><span>{invoice.currency} {Number(item.lineTotal).toFixed(2)}</span></div>)}
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <Button type="button" onClick={() => void download(invoice)}>Download PDF</Button>
          {user?.role === "ADMIN" && invoice.status === "DRAFT" && <Button type="button" onClick={() => void changeStatus(invoice.id, "ISSUED")}>Issue</Button>}
          {user?.role === "ADMIN" && (invoice.status === "ISSUED" || invoice.status === "OVERDUE") && <Button type="button" onClick={() => void changeStatus(invoice.id, "PAID")}>Mark paid</Button>}
          {user?.role === "ADMIN" && invoice.status !== "PAID" && invoice.status !== "CANCELLED" && <button className="rounded-xl border px-4 py-2 text-sm" onClick={() => void changeStatus(invoice.id, "CANCELLED")}>Cancel</button>}
        </div>
      </Card>)}
    </div>
  </div>;
}
