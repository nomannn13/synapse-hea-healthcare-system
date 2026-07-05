package com.synapse.hea.urgent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.synapse.hea.audit.AuditService;
import com.synapse.hea.notification.NotificationService;
import com.synapse.hea.patient.PatientProfileRepository;
import com.synapse.hea.user.UserRepository;
import org.junit.jupiter.api.Test;

class UrgentCaseServiceTest {

  private final UrgentCaseService service = new UrgentCaseService(
    mock(UrgentCaseRepository.class),
    mock(PatientProfileRepository.class),
    mock(UserRepository.class),
    mock(NotificationService.class),
    mock(AuditService.class)
  );

  @Test
  void lowOxygenIsCritical() {
    var input = new UrgentCaseService.CreateRequest(
      null,
      90,
      89,
      120,
      37.0,
      4,
      "Breathing difficulty"
    );
    assertThat(service.priority(input)).isEqualTo(UrgentCase.Priority.CRITICAL);
  }

  @Test
  void severeSymptomsAreHighPriority() {
    var input = new UrgentCaseService.CreateRequest(
      null,
      90,
      98,
      120,
      37.0,
      9,
      "Severe pain"
    );
    assertThat(service.priority(input)).isEqualTo(UrgentCase.Priority.HIGH);
  }

  @Test
  void mildSymptomsRemainLowPriority() {
    var input = new UrgentCaseService.CreateRequest(
      null,
      75,
      99,
      120,
      36.8,
      2,
      "Mild discomfort"
    );
    assertThat(service.priority(input)).isEqualTo(UrgentCase.Priority.LOW);
  }
}
