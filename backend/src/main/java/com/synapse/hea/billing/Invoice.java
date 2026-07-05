package com.synapse.hea.billing;

import com.synapse.hea.appointment.Appointment;
import com.synapse.hea.common.model.BaseEntity;
import com.synapse.hea.patient.PatientProfile;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices", indexes = {
  @Index(name = "idx_invoice_patient_status", columnList = "patient_id,status"),
  @Index(name = "idx_invoice_number", columnList = "invoice_number", unique = true)
})
public class Invoice extends BaseEntity {
  @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
  private String invoiceNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private PatientProfile patient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InvoiceStatus status = InvoiceStatus.DRAFT;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal taxAmount = BigDecimal.ZERO;

  @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(nullable = false, length = 3)
  private String currency = "EUR";

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "issued_at")
  private Instant issuedAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(length = 1000)
  private String notes;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt asc")
  private List<InvoiceItem> items = new ArrayList<>();

  protected Invoice() {}

  public Invoice(String invoiceNumber, PatientProfile patient, Appointment appointment,
                 String currency, LocalDate dueDate, String notes) {
    this.invoiceNumber = invoiceNumber;
    this.patient = patient;
    this.appointment = appointment;
    this.currency = currency == null || currency.isBlank() ? "EUR" : currency.toUpperCase();
    this.dueDate = dueDate;
    this.notes = notes;
  }

  public void addItem(String description, BigDecimal quantity, BigDecimal unitPrice) {
    items.add(new InvoiceItem(this, description, quantity, unitPrice));
  }

  public void calculate(BigDecimal taxPercent, BigDecimal discountAmount) {
    subtotal = items.stream().map(InvoiceItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
      .setScale(2, RoundingMode.HALF_UP);
    this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount.setScale(2, RoundingMode.HALF_UP);
    BigDecimal taxable = subtotal.subtract(this.discountAmount).max(BigDecimal.ZERO);
    taxAmount = taxable.multiply(taxPercent == null ? BigDecimal.ZERO : taxPercent)
      .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    totalAmount = taxable.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
  }

  public void issue() {
    if (status != InvoiceStatus.DRAFT) throw new IllegalStateException("Only draft invoices can be issued");
    status = InvoiceStatus.ISSUED;
    issuedAt = Instant.now();
  }

  public void markPaid() {
    if (status != InvoiceStatus.ISSUED && status != InvoiceStatus.OVERDUE)
      throw new IllegalStateException("Only issued invoices can be paid");
    status = InvoiceStatus.PAID;
    paidAt = Instant.now();
  }

  public void cancel() {
    if (status == InvoiceStatus.PAID) throw new IllegalStateException("Paid invoices cannot be cancelled");
    status = InvoiceStatus.CANCELLED;
  }

  public String getInvoiceNumber() { return invoiceNumber; }
  public PatientProfile getPatient() { return patient; }
  public Appointment getAppointment() { return appointment; }
  public InvoiceStatus getStatus() { return status; }
  public BigDecimal getSubtotal() { return subtotal; }
  public BigDecimal getTaxAmount() { return taxAmount; }
  public BigDecimal getDiscountAmount() { return discountAmount; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public String getCurrency() { return currency; }
  public LocalDate getDueDate() { return dueDate; }
  public Instant getIssuedAt() { return issuedAt; }
  public Instant getPaidAt() { return paidAt; }
  public String getNotes() { return notes; }
  public List<InvoiceItem> getItems() { return List.copyOf(items); }
}
