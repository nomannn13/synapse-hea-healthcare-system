import type { ReactNode } from "react";
import { ArrowRight, ShieldCheck, Stethoscope } from "lucide-react";
import { Link } from "react-router-dom";
import { BASE } from "../api/client";

const apiDocsHref = (() => {
  try {
    return new URL("/swagger-ui.html", BASE).toString();
  } catch {
    return "/swagger-ui.html";
  }
})();

export function LandingPage() {
  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,#cffafe,transparent_35%),linear-gradient(#fff,#f8fafc)]">
      <nav className="mx-auto flex max-w-7xl items-center justify-between px-6 py-6">
        <b className="text-xl">Synapse HEA</b>
        <Link
          to="/login"
          className="rounded-xl bg-slate-950 px-4 py-2 text-white"
        >
          Sign in
        </Link>
      </nav>
      <section className="mx-auto grid max-w-7xl items-center gap-12 px-6 py-24 lg:grid-cols-2">
        <div>
          <span className="rounded-full bg-cyan-100 px-3 py-1 text-sm font-semibold text-cyan-900">
            Java 21 · Spring Boot · React
          </span>
          <h1 className="mt-6 text-5xl font-black leading-tight tracking-tight text-slate-950 sm:text-7xl">
            Hospital operations, without the paperwork maze.
          </h1>
          <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">
            A secure, role-based system for appointments, medical records,
            resources, notifications, and operational reporting.
          </p>
          <div className="mt-8 flex gap-3">
            <Link
              to="/register"
              className="flex items-center gap-2 rounded-xl bg-cyan-600 px-5 py-3 font-semibold text-white"
            >
              Create patient account <ArrowRight size={18} />
            </Link>
            <a
              href={apiDocsHref}
              className="rounded-xl border border-slate-300 px-5 py-3 font-semibold"
            >
              API docs
            </a>
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Feature
            icon={<Stethoscope />}
            title="Care workflow"
            text="Appointments, availability and medical history in one traceable flow."
          />
          <Feature
            icon={<ShieldCheck />}
            title="Security by design"
            text="JWT access, rotating refresh tokens, RBAC and audit logging."
          />
          <Feature
            icon={<ArrowRight />}
            title="Concurrency safe"
            text="Transactional overlap checks stop double-booking races."
          />
          <Feature
            icon={<ShieldCheck />}
            title="Clear clinical boundaries"
            text="Transparent triage rules support prioritization without claiming automated diagnosis."
          />
        </div>
      </section>
    </main>
  );
}
function Feature({
  icon,
  title,
  text,
}: {
  icon: ReactNode;
  title: string;
  text: string;
}) {
  return (
    <div className="rounded-3xl border border-white bg-white/80 p-6 shadow-sm backdrop-blur">
      <div className="text-cyan-700">{icon}</div>
      <h2 className="mt-5 font-bold">{title}</h2>
      <p className="mt-2 text-sm leading-6 text-slate-600">{text}</p>
    </div>
  );
}
