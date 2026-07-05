package com.synapse.hea.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import com.synapse.hea.patient.PatientProfile;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoiceTest {
  @Test
  void calculatesTotalsAndTransitions() {
    Invoice invoice = new Invoice("HEA-TEST", mock(PatientProfile.class), null, "EUR", LocalDate.now(), null);
    invoice.addItem("Consultation", new BigDecimal("1"), new BigDecimal("100.00"));
    invoice.addItem("Test", new BigDecimal("2"), new BigDecimal("25.00"));
    invoice.calculate(new BigDecimal("20"), new BigDecimal("10.00"));

    assertThat(invoice.getSubtotal()).isEqualByComparingTo("150.00");
    assertThat(invoice.getTaxAmount()).isEqualByComparingTo("28.00");
    assertThat(invoice.getTotalAmount()).isEqualByComparingTo("168.00");

    invoice.issue();
    invoice.markPaid();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
  }
}
