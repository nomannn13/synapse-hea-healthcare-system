import {
  Activity, Bell, CalendarDays, ClipboardPlus, Clock3, FileHeart, FolderOpen,
  LayoutDashboard, LogOut, Pill, Search, Stethoscope, UserRound, Users, WalletCards,
} from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/auth-context";

export function AppLayout() {
  const { user, logout } = useAuth();
  const links: Array<[string, string, typeof LayoutDashboard]> = [
    ["/app", "Dashboard", LayoutDashboard],
    ["/app/search", "Search", Search],
    ["/app/doctors", "Doctors", Stethoscope],
    ["/app/appointments", "Appointments", CalendarDays],
    ["/app/prescriptions", "Prescriptions", Pill],
    ["/app/billing", "Billing", WalletCards],
    ["/app/documents", "Documents", FolderOpen],
    ["/app/notifications", "Notifications", Bell],
    ["/app/profile", "Profile", UserRound],
  ];
  if (user?.role === "PATIENT") links.splice(4, 0, ["/app/records", "Medical records", FileHeart]);
  if (user?.role === "DOCTOR") {
    links.splice(4, 0, ["/app/clinical", "Clinical workspace", ClipboardPlus]);
    links.splice(5, 0, ["/app/schedule", "My schedule", Clock3]);
  }
  if (user?.role === "ADMIN") links.splice(1, 0, ["/app/admin", "Administration", Users]);
  return <div className="min-h-screen bg-slate-50">
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
        <NavLink to="/app" className="flex items-center gap-2 font-black tracking-tight">
          <span className="grid size-9 place-items-center rounded-xl bg-cyan-600 text-white"><Activity size={20} /></span>Synapse HEA
        </NavLink>
        <div className="flex items-center gap-3 text-sm"><span className="hidden sm:block">{user?.firstName} · {user?.role}</span><button onClick={() => void logout()} aria-label="Log out"><LogOut size={19} /></button></div>
      </div>
    </header>
    <div className="mx-auto grid max-w-7xl gap-6 px-4 py-6 lg:grid-cols-[230px_1fr]">
      <nav className="flex gap-2 overflow-x-auto lg:flex-col">{links.map(([to, label, Icon]) => <NavLink key={to} to={to} end={to === "/app"} className={({ isActive }) => `flex shrink-0 items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium ${isActive ? "bg-slate-950 text-white" : "text-slate-600 hover:bg-white"}`}><Icon size={18} />{label}</NavLink>)}</nav>
      <main><Outlet /></main>
    </div>
  </div>;
}
