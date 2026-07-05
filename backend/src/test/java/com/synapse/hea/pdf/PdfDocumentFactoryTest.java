package com.synapse.hea.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfDocumentFactoryTest {
  @Test
  void createsReadablePdfEnvelope() {
    byte[] bytes = PdfDocumentFactory.textDocument("Test document", List.of("Line one", "Line two"));
    String prefix = new String(bytes, 0, 8, StandardCharsets.ISO_8859_1);
    assertThat(prefix).isEqualTo("%PDF-1.4");
    assertThat(bytes.length).isGreaterThan(400);
  }
}
