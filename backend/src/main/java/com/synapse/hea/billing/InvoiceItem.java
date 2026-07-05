package com.synapse.hea.billing;

import com.synapse.hea.common.model.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(nullable = false, length = 180)
  private String description;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal lineTotal;

  protected InvoiceItem() {}

  InvoiceItem(Invoice invoice, String description, BigDecimal quantity, BigDecimal unitPrice) {
    this.invoice = invoice;
    this.description = description.trim();
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.lineTotal = quantity.multiply(unitPrice).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  public String getDescription() { return description; }
  public BigDecimal getQuantity() { return quantity; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public BigDecimal getLineTotal() { return lineTotal; }
}
