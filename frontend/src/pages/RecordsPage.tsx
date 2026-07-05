import { useEffect, useState } from "react";
import { api } from "../api/client";
import { Card, Empty } from "../components/ui";
import type { MedicalRecord, PageResponse } from "../types";
export function RecordsPage() {
  const [items, setItems] = useState<MedicalRecord[]>([]);
  useEffect(() => {
    api<PageResponse<MedicalRecord>>("/medical-records")
      .then((r) => setItems(r.items))
      .catch(() => setItems([]));
  }, []);
  return (
    <div>
      <h1 className="text-3xl font-black">Medical records</h1>
      <p className="mt-2 text-slate-600">
        Appointment history and clinical history are intentionally separate.
      </p>
      <div className="mt-6 space-y-4">
        {items.map((r) => (
          <Card key={r.id}>
            <div className="flex justify-between">
              <div>
                <h2 className="font-bold">{r.diagnosis}</h2>
                <p className="text-sm text-slate-500">
                  Dr. {r.doctorName} ·{" "}
                  {new Date(r.recordedAt).toLocaleDateString()}
                </p>
              </div>
            </div>
            {r.treatment && (
              <p className="mt-4 text-sm">
                <b>Treatment:</b> {r.treatment}
              </p>
            )}
            {r.prescription && (
              <p className="mt-2 text-sm">
                <b>Prescription:</b> {r.prescription}
              </p>
            )}
          </Card>
        ))}
        {!items.length && <Empty>No medical records are available.</Empty>}
      </div>
    </div>
  );
}
