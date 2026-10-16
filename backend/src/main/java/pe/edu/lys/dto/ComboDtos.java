package pe.edu.lys.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class ComboDtos {
  private ComboDtos() {}

  public record ComboItemRequest(@NotNull @Positive Long productoId, @NotNull @Min(1) @Max(50) Integer cantidad) {}

  public record ComboRequest(
      @NotBlank @Size(max = 100) String nombre,
      @Size(max = 500) String descripcion,
      @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precio,
      @NotNull Boolean activo,
      @NotEmpty @Size(max = 20) List<@NotNull @Valid ComboItemRequest> items) {}

  public record ComboItemResponse(Long productoId, String nombre, int cantidad, List<String> opcionesPersonalizacion) {}
  public record ComboResponse(Long id, String nombre, String descripcion, BigDecimal precio,
      boolean activo, boolean disponible, List<ComboItemResponse> items) {}
}
