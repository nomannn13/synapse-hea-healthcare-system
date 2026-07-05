package com.synapse.hea.prescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import com.synapse.hea.doctor.DoctorProfile;
import com.synapse.hea.patient.PatientProfile;
import org.junit.jupiter.api.Test;

class PrescriptionTest {
  @Test
  void issuesStructuredPrescription() {
    Prescription prescription = new Prescription("RX-TEST", mock(PatientProfile.class),
      mock(DoctorProfile.class), null, "Hydration advice");
    prescription.addItem("Medicine A", "10 mg", "Once daily", 7, "After food");
    prescription.issue();

    assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    assertThat(prescription.getItems()).hasSize(1);
    assertThat(prescription.getIssuedAt()).isNotNull();
  }
}
