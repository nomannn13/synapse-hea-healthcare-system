import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/auth-context";
import { AppLayout } from "./layouts/AppLayout";
import { AdminPage } from "./pages/AdminPage";
import { AppointmentsPage } from "./pages/AppointmentsPage";
import { LoginPage, RegisterPage } from "./pages/AuthPages";
import { BillingPage } from "./pages/BillingPage";
import { ClinicalPage } from "./pages/ClinicalPage";
import { DashboardPage } from "./pages/DashboardPage";
import { DoctorsPage } from "./pages/DoctorsPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { LandingPage } from "./pages/LandingPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { PrescriptionsPage } from "./pages/PrescriptionsPage";
import { ProfilePage } from "./pages/ProfilePage";
import { RecordsPage } from "./pages/RecordsPage";
import { SchedulePage } from "./pages/SchedulePage";
import { SearchPage } from "./pages/SearchPage";

function Protected() {
  const { user, ready } = useAuth();
  if (!ready) return <div className="grid min-h-screen place-items-center">Loading Synapse…</div>;
  return user ? <AppLayout /> : <Navigate to="/login" replace />;
}
function AdminOnly() {
  const { user } = useAuth();
  return user?.role === "ADMIN" ? <AdminPage /> : <Navigate to="/app" replace />;
}
function DoctorOnly({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  return user?.role === "DOCTOR" ? children : <Navigate to="/app" replace />;
}
function PatientOnly() {
  const { user } = useAuth();
  return user?.role === "PATIENT" ? <RecordsPage /> : <Navigate to="/app" replace />;
}

export default function App() {
  return <Routes>
    <Route path="/" element={<LandingPage />} />
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/app" element={<Protected />}>
      <Route index element={<DashboardPage />} />
      <Route path="search" element={<SearchPage />} />
      <Route path="doctors" element={<DoctorsPage />} />
      <Route path="appointments" element={<AppointmentsPage />} />
      <Route path="records" element={<PatientOnly />} />
      <Route path="prescriptions" element={<PrescriptionsPage />} />
      <Route path="billing" element={<BillingPage />} />
      <Route path="documents" element={<DocumentsPage />} />
      <Route path="profile" element={<ProfilePage />} />
      <Route path="notifications" element={<NotificationsPage />} />
      <Route path="clinical" element={<DoctorOnly><ClinicalPage /></DoctorOnly>} />
      <Route path="schedule" element={<DoctorOnly><SchedulePage /></DoctorOnly>} />
      <Route path="admin" element={<AdminOnly />} />
    </Route>
    <Route path="*" element={<Navigate to="/" />} />
  </Routes>;
}
