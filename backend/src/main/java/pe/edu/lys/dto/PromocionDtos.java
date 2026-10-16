package pe.edu.lys.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class PromocionDtos {
  private PromocionDtos() {}

  public record PromocionRequest(
      @NotBlank @Size(max = 100) String nombre,
      @Size(max = 500) String descripcion,
      @NotNull @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal porcentajeDescuento,
      @NotNull Instant inicio,
      @NotNull Instant fin,
      @NotNull Boolean activo,
      @Positive Long productoId,
      @Positive Long comboId) {}

  public record PromocionResponse(Long id, String nombre, String descripcion,
      BigDecimal porcentajeDescuento, Instant inicio, Instant fin, boolean activo,
      boolean disponible, Long productoId, Long comboId, BigDecimal precioPromocional) {}
}
