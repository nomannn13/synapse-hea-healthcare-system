package com.synapse.hea.search;

import com.synapse.hea.department.*;
import com.synapse.hea.doctor.*;
import com.synapse.hea.patient.*;
import com.synapse.hea.user.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalSearchService {
  private final UserRepository users;
  private final DoctorProfileRepository doctors;
  private final PatientProfileRepository patients;
  private final DepartmentRepository departments;

  public GlobalSearchService(UserRepository users, DoctorProfileRepository doctors,
      PatientProfileRepository patients, DepartmentRepository departments) {
    this.users = users;
    this.doctors = doctors;
    this.patients = patients;
    this.departments = departments;
  }

  @Transactional(readOnly = true)
  public Result search(UUID actorId, String raw) {
    String q = raw == null ? "" : raw.trim();
    if (q.length() < 2) return new Result(List.of(), List.of(), List.of());
    User actor = users.findById(actorId).orElseThrow();
    var doctorResults = doctors.search(q, null, PageRequest.of(0, 8)).stream()
      .map(d -> new DoctorResult(d.getId(), d.getUser().getDisplayName(), d.getSpecialization(), d.getDepartment().getName()))
      .toList();
    var departmentResults = departments.findByActiveTrueOrderByNameAsc().stream()
      .filter(d -> d.getName().toLowerCase().contains(q.toLowerCase()) || d.getCode().toLowerCase().contains(q.toLowerCase()))
      .limit(8).map(d -> new DepartmentResult(d.getId(), d.getCode(), d.getName())).toList();
    List<PatientResult> patientResults = List.of();
    if (actor.getRole() == Role.DOCTOR || actor.getRole() == Role.ADMIN) {
      patientResults = patients.search(q, PageRequest.of(0, 8)).stream()
        .map(p -> new PatientResult(p.getId(), p.getUser().getDisplayName(), p.getUser().getEmail(), p.getBloodGroup()))
        .toList();
    }
    return new Result(doctorResults, departmentResults, patientResults);
  }

  public record DoctorResult(UUID id, String name, String specialization, String department) {}
  public record DepartmentResult(UUID id, String code, String name) {}
  public record PatientResult(UUID id, String name, String email, String bloodGroup) {}
  public record Result(List<DoctorResult> doctors, List<DepartmentResult> departments, List<PatientResult> patients) {}
}
