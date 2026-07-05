import { useEffect, useState } from "react";
import { api } from "../api/client";
import { Badge, Card, Empty, Input } from "../components/ui";
import type { Doctor, PageResponse } from "../types";
export function DoctorsPage() {
  const [q, setQ] = useState("");
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  useEffect(() => {
    const t = setTimeout(
      () =>
        api<PageResponse<Doctor>>(
          `/doctors?query=${encodeURIComponent(q)}`,
        ).then((r) => setDoctors(r.items)),
      250,
    );
    return () => clearTimeout(t);
  }, [q]);
  return (
    <div>
      <h1 className="text-3xl font-black">Find a doctor</h1>
      <Input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Search name or specialization"
        className="mt-5"
      />
      <div className="mt-6 grid gap-4 md:grid-cols-2">
        {doctors.map((d) => (
          <Card key={d.id}>
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-lg font-bold">{d.name}</h2>
                <p className="text-slate-600">{d.specialization}</p>
              </div>
              <Badge>{d.department}</Badge>
            </div>
            <p className="mt-4 text-sm leading-6 text-slate-500">
              {d.biography || "Professional profile available."}
            </p>
            <p className="mt-4 text-xs text-slate-400">
              {d.consultationMinutes}-minute appointments
            </p>
          </Card>
        ))}
        {!doctors.length && <Empty>No doctors match this search.</Empty>}
      </div>
    </div>
  );
}
