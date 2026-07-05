package com.synapse.hea.pdf;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Small dependency-free PDF writer for deterministic invoices and prescriptions.
 * It intentionally supports plain text only; clinical documents remain stored as
 * their original files.
 */
public final class PdfDocumentFactory {
  private static final int LINES_PER_PAGE = 44;

  private PdfDocumentFactory() {}

  public static byte[] textDocument(String title, List<String> sourceLines) {
    List<String> lines = new ArrayList<>();
    lines.add(title);
    lines.add(" ");
    sourceLines.forEach(line -> wrap(lines, line == null ? "" : line, 86));
    List<List<String>> pages = new ArrayList<>();
    for (int i = 0; i < lines.size(); i += LINES_PER_PAGE) {
      pages.add(lines.subList(i, Math.min(i + LINES_PER_PAGE, lines.size())));
    }
    if (pages.isEmpty()) pages.add(List.of(title));
    return build(pages);
  }

  private static void wrap(List<String> output, String raw, int width) {
    String value = sanitize(raw);
    if (value.isBlank()) {
      output.add(" ");
      return;
    }
    String[] words = value.split("\\s+");
    StringBuilder current = new StringBuilder();
    for (String word : words) {
      if (current.length() > 0 && current.length() + word.length() + 1 > width) {
        output.add(current.toString());
        current.setLength(0);
      }
      if (current.length() > 0) current.append(' ');
      current.append(word);
    }
    if (!current.isEmpty()) output.add(current.toString());
  }

  private static byte[] build(List<List<String>> pages) {
    int objectCount = 3 + pages.size() * 2;
    List<byte[]> objects = new ArrayList<>(objectCount + 1);
    objects.add(new byte[0]);
    objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));

    StringBuilder kids = new StringBuilder("[");
    for (int i = 0; i < pages.size(); i++) kids.append(4 + i * 2).append(" 0 R ");
    kids.append(']');
    objects.add(bytes("<< /Type /Pages /Kids " + kids + " /Count " + pages.size() + " >>"));
    objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));

    for (int i = 0; i < pages.size(); i++) {
      int contentObject = 5 + i * 2;
      objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
        "/Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObject + " 0 R >>"));
      String content = pageContent(pages.get(i));
      byte[] contentBytes = bytes(content);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      write(stream, "<< /Length " + contentBytes.length + " >>\nstream\n");
      stream.writeBytes(contentBytes);
      write(stream, "\nendstream");
      objects.add(stream.toByteArray());
    }

    ByteArrayOutputStream pdf = new ByteArrayOutputStream();
    write(pdf, "%PDF-1.4\n%SynapseHEA\n");
    long[] offsets = new long[objectCount + 1];
    for (int i = 1; i <= objectCount; i++) {
      offsets[i] = pdf.size();
      write(pdf, i + " 0 obj\n");
      pdf.writeBytes(objects.get(i));
      write(pdf, "\nendobj\n");
    }
    long xref = pdf.size();
    write(pdf, "xref\n0 " + (objectCount + 1) + "\n");
    write(pdf, "0000000000 65535 f \n");
    for (int i = 1; i <= objectCount; i++) {
      write(pdf, String.format("%010d 00000 n \n", offsets[i]));
    }
    write(pdf, "trailer\n<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF");
    return pdf.toByteArray();
  }

  private static String pageContent(List<String> lines) {
    StringBuilder b = new StringBuilder("BT\n/F1 11 Tf\n50 795 Td\n");
    for (int i = 0; i < lines.size(); i++) {
      if (i == 0) b.append("/F1 16 Tf\n");
      else if (i == 1) b.append("/F1 11 Tf\n");
      b.append('(').append(escape(lines.get(i))).append(") Tj\n0 -16 Td\n");
    }
    return b.append("ET").toString();
  }

  private static String escape(String value) {
    return sanitize(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
  }

  private static String sanitize(String value) {
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD);
    return normalized.replaceAll("[^\\x20-\\x7E]", "?");
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.ISO_8859_1);
  }

  private static void write(ByteArrayOutputStream out, String value) {
    out.writeBytes(bytes(value));
  }
}
