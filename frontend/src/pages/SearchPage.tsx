import { useState, type FormEvent } from "react";
import { api } from "../api/client";
import { Button, Card, Empty, Input } from "../components/ui";
import type { GlobalSearchResult } from "../types";

const empty: GlobalSearchResult = { doctors: [], departments: [], patients: [] };
export function SearchPage() {
  const [results, setResults] = useState(empty);
  const [searched, setSearched] = useState(false);
  async function search(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); const q = new FormData(e.currentTarget).get("q");
    setResults(await api<GlobalSearchResult>(`/search?q=${encodeURIComponent(String(q))}`)); setSearched(true);
  }
  const total = results.doctors.length + results.departments.length + results.patients.length;
  return <div><h1 className="text-3xl font-black">Global search</h1><p className="mt-2 text-slate-600">Role-aware search across doctors, departments, and the patient directory.</p>
    <form onSubmit={search} className="mt-6 flex gap-2"><Input name="q" minLength={2} placeholder="Search by name, email, specialization, or department" required /><Button>Search</Button></form>
    {searched && total === 0 && <div className="mt-6"><Empty>No matching records.</Empty></div>}
    <div className="mt-6 grid gap-4 lg:grid-cols-3">{results.doctors.length > 0 && <Card><h2 className="font-bold">Doctors</h2><div className="mt-3 space-y-3">{results.doctors.map((x) => <div key={x.id}><p className="font-semibold">{x.name}</p><p className="text-sm text-slate-500">{x.specialization} · {x.department}</p></div>)}</div></Card>}{results.departments.length > 0 && <Card><h2 className="font-bold">Departments</h2><div className="mt-3 space-y-3">{results.departments.map((x) => <div key={x.id}><p className="font-semibold">{x.name}</p><p className="text-sm text-slate-500">{x.code}</p></div>)}</div></Card>}{results.patients.length > 0 && <Card><h2 className="font-bold">Patients</h2><div className="mt-3 space-y-3">{results.patients.map((x) => <div key={x.id}><p className="font-semibold">{x.name}</p><p className="text-sm text-slate-500">{x.email}{x.bloodGroup ? ` · ${x.bloodGroup}` : ""}</p></div>)}</div></Card>}</div>
  </div>;
}
