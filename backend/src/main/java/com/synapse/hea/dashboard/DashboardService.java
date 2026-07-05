package com.synapse.hea.dashboard;

import com.synapse.hea.admin.AdminService;
import com.synapse.hea.appointment.*;
import com.synapse.hea.billing.*;
import com.synapse.hea.prescription.*;
import com.synapse.hea.document.*;
import com.synapse.hea.doctor.*;
import com.synapse.hea.medical.*;
import com.synapse.hea.notification.*;
import com.synapse.hea.patient.*;
import com.synapse.hea.user.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  private final UserRepository users;
  private final PatientProfileRepository patients;
  private final DoctorProfileRepository doctors;
  private final AppointmentRepository appointments;
  private final MedicalRecordRepository records;
  private final NotificationService notifications;
  private final AdminService admin;
  private final AppointmentService appointmentService;
  private final InvoiceRepository invoices;
  private final PrescriptionRepository prescriptions;
  private final ClinicalDocumentRepository documents;

  public DashboardService(
    UserRepository u,
    PatientProfileRepository p,
    DoctorProfileRepository d,
    AppointmentRepository a,
    MedicalRecordRepository r,
    NotificationService n,
    AdminService ad,
    AppointmentService as,
    InvoiceRepository i,
    PrescriptionRepository pr,
    ClinicalDocumentRepository cd
  ) {
    users = u;
    patients = p;
    doctors = d;
    appointments = a;
    records = r;
    notifications = n;
    admin = ad;
    appointmentService = as;
    invoices = i;
    prescriptions = pr;
    documents = cd;
  }

  @Transactional(readOnly = true)
  public Object get(UUID userId) {
    User u = users.findById(userId).orElseThrow();
    if (u.getRole() == Role.ADMIN) return admin.summary();
    if (u.getRole() == Role.PATIENT) {
      PatientProfile p = patients.findByUserId(userId).orElseThrow();
      List<AppointmentDtos.View> next = appointments
        .findTop5ByPatientIdAndStartAtAfterAndStatusNotOrderByStartAtAsc(
          p.getId(),
          Instant.now(),
          AppointmentStatus.CANCELLED
        )
        .stream()
        .map(appointmentService::toView)
        .toList();
      return new PatientDashboard(
        appointments.countByPatientId(p.getId()),
        records.countByPatientId(p.getId()),
        prescriptions.countByPatientId(p.getId()),
        invoices.countByPatientId(p.getId()),
        documents.countByPatientId(p.getId()),
        notifications.unread(userId),
        next
      );
    }
    DoctorProfile d = doctors.findByUserId(userId).orElseThrow();
    List<AppointmentDtos.View> next = appointments
      .findTop5ByDoctorIdAndStartAtAfterAndStatusNotOrderByStartAtAsc(
        d.getId(),
        Instant.now(),
        AppointmentStatus.CANCELLED
      )
      .stream()
      .map(appointmentService::toView)
      .toList();
    return new DoctorDashboard(
      appointments.countByDoctorIdAndStartAtAfter(d.getId(), Instant.now()),
      prescriptions.countByDoctorId(d.getId()),
      notifications.unread(userId),
      next
    );
  }

  public record PatientDashboard(
    long totalAppointments,
    long medicalRecords,
    long prescriptions,
    long invoices,
    long clinicalDocuments,
    long unreadNotifications,
    List<AppointmentDtos.View> upcoming
  ) {}

  public record DoctorDashboard(
    long upcomingAppointments,
    long prescriptionsIssued,
    long unreadNotifications,
    List<AppointmentDtos.View> upcoming
  ) {}
}
