export type Role = "PATIENT" | "DOCTOR" | "ADMIN";
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: Role;
  status: string;
}
export interface SessionResponse {
  user: User;
  accessToken: string;
  accessTokenExpiresAt: string;
}
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
export interface Department {
  id: string;
  code: string;
  name: string;
  description?: string;
  active?: boolean;
}
export interface Doctor {
  id: string;
  name: string;
  specialization: string;
  departmentId: string;
  department: string;
  biography?: string;
  consultationMinutes: number;
}
export interface Slot {
  id?: string;
  startAt: string;
  endAt: string;
  active?: boolean;
}
export interface Appointment {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  department: string;
  startAt: string;
  endAt: string;
  status: string;
  reason: string;
  notes?: string;
  cancellationReason?: string;
}
export interface MedicalRecord {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  appointmentId?: string;
  diagnosis: string;
  treatment?: string;
  prescription?: string;
  clinicalNotes?: string;
  recordedAt: string;
}

export interface PatientDirectory {
  id: string;
  name: string;
  email: string;
  phone?: string;
  dateOfBirth?: string;
  bloodGroup?: string;
}
export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: string;
  createdAt: string;
  readAt?: string;
}
export interface ResourceItem {
  id: string;
  departmentId: string;
  department: string;
  type: string;
  code: string;
  name: string;
  status: string;
  notes?: string;
}

export interface InvoiceItem {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}
export interface Invoice {
  id: string;
  invoiceNumber: string;
  patientId: string;
  patientName: string;
  appointmentId?: string;
  status: "DRAFT" | "ISSUED" | "PAID" | "CANCELLED" | "OVERDUE";
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  currency: string;
  dueDate?: string;
  issuedAt?: string;
  paidAt?: string;
  notes?: string;
  createdAt: string;
  items: InvoiceItem[];
}
export interface PrescriptionItem {
  id: string;
  medicationName: string;
  dosage: string;
  frequency: string;
  durationDays?: number;
  instructions?: string;
}
export interface Prescription {
  id: string;
  prescriptionNumber: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  appointmentId?: string;
  status: "DRAFT" | "ISSUED" | "CANCELLED";
  notes?: string;
  issuedAt?: string;
  createdAt: string;
  items: PrescriptionItem[];
}
export interface ClinicalDocument {
  id: string;
  patientId: string;
  patientName: string;
  uploadedById: string;
  uploadedByName: string;
  category: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  notes?: string;
  createdAt: string;
}
export interface AuditLog {
  id: string;
  actorId?: string;
  action: string;
  entityType: string;
  entityId?: string;
  details?: string;
  createdAt: string;
}
export interface GlobalSearchResult {
  doctors: Array<{ id: string; name: string; specialization: string; department: string }>;
  departments: Array<{ id: string; code: string; name: string }>;
  patients: Array<{ id: string; name: string; email: string; bloodGroup?: string }>;
}
