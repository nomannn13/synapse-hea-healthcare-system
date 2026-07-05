import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { Button, Card, Input } from "../components/ui";
import type { AuditLog, Department, PageResponse, User } from "../types";

export function AdminPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [report, setReport] = useState<Record<string, number>>({});
  const [departments, setDepartments] = useState<Department[]>([]);
  const [audits, setAudits] = useState<AuditLog[]>([]);
  const [message, setMessage] = useState("");

  const load = useCallback(async () => {
    const [userPage, summary, deps, auditPage] = await Promise.all([
      api<PageResponse<User>>("/admin/users?size=50"),
      api<Record<string, number>>("/admin/reports/summary"),
      api<Department[]>("/departments"),
      api<PageResponse<AuditLog>>("/admin/audit-logs?size=20"),
    ]);
    setUsers(userPage.items); setReport(summary); setDepartments(deps); setAudits(auditPage.items);
  }, []);
  useEffect(() => {
    let active = true;
    Promise.all([
      api<PageResponse<User>>("/admin/users?size=50"),
      api<Record<string, number>>("/admin/reports/summary"),
      api<Department[]>("/departments"),
      api<PageResponse<AuditLog>>("/admin/audit-logs?size=20"),
    ]).then(([userPage, summary, deps, auditPage]) => {
      if (!active) return;
      setUsers(userPage.items); setReport(summary); setDepartments(deps); setAudits(auditPage.items);
    });
    return () => { active = false; };
  }, []);

  async function createDepartment(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); const f = new FormData(e.currentTarget);
    try {
      await api("/departments", { method: "POST", body: JSON.stringify({ code: f.get("code"), name: f.get("name"), description: f.get("description") }) });
      e.currentTarget.reset(); setMessage("Department created."); await load();
    } catch (x) { setMessage(x instanceof Error ? x.message : "Could not create department"); }
  }

  return <div>
    <h1 className="text-3xl font-black">Administration</h1>
    <p className="mt-2 text-slate-600">Operational analytics, departments, accounts, resources, and auditable activity.</p>
    {message && <p className="mt-4 rounded-xl bg-cyan-50 p-3 text-sm text-cyan-900">{message}</p>}
    <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{Object.entries(report).map(([k, v]) => <Card key={k}><p className="text-xs uppercase tracking-wide text-slate-500">{human(k)}</p><p className="mt-2 text-2xl font-black">{typeof v === "number" ? v.toLocaleString() : String(v)}</p></Card>)}</div>
    <div className="mt-6 grid gap-6 xl:grid-cols-2">
      <Card><h2 className="font-bold">Create department</h2><form onSubmit={createDepartment} className="mt-4 space-y-3"><Input name="code" placeholder="Code, e.g. ORTH" required /><Input name="name" placeholder="Department name" required /><Input name="description" placeholder="Description" /><Button className="w-full">Create department</Button></form>
        <div className="mt-5 space-y-2">{departments.map((d) => <div key={d.id} className="rounded-xl bg-slate-50 p-3"><p className="font-semibold">{d.name}</p><p className="text-sm text-slate-500">{d.code} · {d.description}</p></div>)}</div></Card>
      <Card className="overflow-x-auto"><h2 className="mb-4 font-bold">Users</h2><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="py-2">Name</th><th>Email</th><th>Role</th><th>Status</th></tr></thead><tbody>{users.map((u) => <tr key={u.id} className="border-b last:border-0"><td className="py-3">{u.firstName} {u.lastName}</td><td>{u.email}</td><td>{u.role}</td><td>{u.status}</td></tr>)}</tbody></table></Card>
    </div>
    <Card className="mt-6 overflow-x-auto"><h2 className="mb-4 font-bold">Recent audit trail</h2><table className="w-full text-left text-sm"><thead><tr className="border-b"><th className="py-2">Time</th><th>Action</th><th>Entity</th><th>Details</th></tr></thead><tbody>{audits.map((a) => <tr key={a.id} className="border-b last:border-0"><td className="py-3 whitespace-nowrap">{new Date(a.createdAt).toLocaleString()}</td><td>{a.action}</td><td>{a.entityType}</td><td>{a.details}</td></tr>)}</tbody></table></Card>
  </div>;
}
function human(s: string) { return s.replace(/([A-Z])/g, " $1").replace(/^./, (c) => c.toUpperCase()); }
