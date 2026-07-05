package com.synapse.hea.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AppointmentDtos {

  private AppointmentDtos() {}

  public record CreateRequest(
    @NotNull UUID doctorId,
    @NotNull @Future Instant startAt,
    @NotBlank @Size(max = 500) String reason
  ) {}

  public record RescheduleRequest(@NotNull @Future Instant startAt) {}

  public record CancelRequest(@NotBlank @Size(max = 500) String reason) {}

  public record StatusRequest(
    @NotNull AppointmentStatus status,
    @Size(max = 1000) String notes
  ) {}

  public record View(
    UUID id,
    UUID patientId,
    String patientName,
    UUID doctorId,
    String doctorName,
    String department,
    Instant startAt,
    Instant endAt,
    AppointmentStatus status,
    String reason,
    String notes,
    String cancellationReason
  ) {}
}
