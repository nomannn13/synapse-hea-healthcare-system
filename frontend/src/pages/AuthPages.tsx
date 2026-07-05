import { useState, type FormEvent, type ReactNode } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import { Button, Card, Input } from "../components/ui";
export function LoginPage() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [error, setError] = useState("");
  if (user) return <Navigate to="/app" />;
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    try {
      await login(String(f.get("email")), String(f.get("password")));
      nav("/app");
    } catch (x) {
      setError(x instanceof Error ? x.message : "Login failed");
    }
  }
  return (
    <AuthShell
      title="Welcome back"
      footer={
        <Link to="/register" className="text-cyan-700">
          Create an account
        </Link>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <label>
          Email
          <Input
            name="email"
            type="email"
            required
            defaultValue="patient@synapse.local"
          />
        </label>
        <label>
          Password
          <Input
            name="password"
            type="password"
            required
            defaultValue="Password123!"
          />
        </label>
        {error && (
          <p role="alert" className="text-sm text-red-600">
            {error}
          </p>
        )}
        <Button className="w-full">Sign in</Button>
      </form>
    </AuthShell>
  );
}
export function RegisterPage() {
  const { user, register } = useAuth();
  const nav = useNavigate();
  const [error, setError] = useState("");
  if (user) return <Navigate to="/app" />;
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    try {
      await register(f);
      nav("/app");
    } catch (x) {
      setError(x instanceof Error ? x.message : "Registration failed");
    }
  }
  return (
    <AuthShell
      title="Create patient account"
      footer={
        <Link to="/login" className="text-cyan-700">
          Already registered?
        </Link>
      }
    >
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <label>
          First name
          <Input name="firstName" required />
        </label>
        <label>
          Last name
          <Input name="lastName" required />
        </label>
        <label className="sm:col-span-2">
          Email
          <Input name="email" type="email" required />
        </label>
        <label>
          Phone
          <Input name="phone" />
        </label>
        <label>
          Date of birth
          <Input name="dateOfBirth" type="date" />
        </label>
        <label className="sm:col-span-2">
          Password
          <Input name="password" type="password" minLength={10} required />
        </label>
        {error && <p className="sm:col-span-2 text-sm text-red-600">{error}</p>}
        <Button className="sm:col-span-2">Create account</Button>
      </form>
    </AuthShell>
  );
}
function AuthShell({
  title,
  children,
  footer,
}: {
  title: string;
  children: ReactNode;
  footer: ReactNode;
}) {
  return (
    <main className="grid min-h-screen place-items-center bg-slate-100 p-4">
      <Card className="w-full max-w-lg">
        <Link to="/" className="text-sm text-slate-500">
          ← Synapse HEA
        </Link>
        <h1 className="my-6 text-3xl font-black">{title}</h1>
        {children}
        <div className="mt-6 text-center text-sm">{footer}</div>
      </Card>
    </main>
  );
}
