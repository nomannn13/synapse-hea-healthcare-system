import { useEffect, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { Button, Card, Input } from "../components/ui";
interface Profile {
  firstName: string;
  lastName: string;
  phone?: string;
  patient?: {
    dateOfBirth?: string;
    bloodGroup?: string;
    allergies?: string;
    emergencyContact?: string;
    address?: string;
  };
}
export function ProfilePage() {
  const [p, setP] = useState<Profile | null>(null);
  useEffect(() => {
    api<Profile>("/profile/me").then(setP);
  }, []);
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const body = Object.fromEntries(new FormData(e.currentTarget));
    setP(
      await api<Profile>("/profile/me", {
        method: "PATCH",
        body: JSON.stringify(body),
      }),
    );
  }
  if (!p) return <p>Loading…</p>;
  return (
    <div>
      <h1 className="text-3xl font-black">Profile</h1>
      <Card className="mt-6">
        <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
          <label>
            First name
            <Input name="firstName" defaultValue={p.firstName} />
          </label>
          <label>
            Last name
            <Input name="lastName" defaultValue={p.lastName} />
          </label>
          <label>
            Phone
            <Input name="phone" defaultValue={p.phone} />
          </label>
          <label>
            Blood group
            <Input name="bloodGroup" defaultValue={p.patient?.bloodGroup} />
          </label>
          <label className="sm:col-span-2">
            Address
            <Input name="address" defaultValue={p.patient?.address} />
          </label>
          <input
            type="hidden"
            name="dateOfBirth"
            value={p.patient?.dateOfBirth ?? ""}
          />
          <input
            type="hidden"
            name="allergies"
            value={p.patient?.allergies ?? ""}
          />
          <input
            type="hidden"
            name="emergencyContact"
            value={p.patient?.emergencyContact ?? ""}
          />
          <Button className="sm:col-span-2">Save changes</Button>
        </form>
      </Card>
    </div>
  );
}
