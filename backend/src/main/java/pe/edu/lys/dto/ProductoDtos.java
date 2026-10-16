package pe.edu.lys.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class ProductoDtos {
  private ProductoDtos() {}

  public record ProductoRequest(
      @NotBlank @Size(max = 100) String nombre,
      @Size(max = 500) String descripcion,
      @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precio,
      @NotNull Long categoriaId,
      @NotNull Boolean activo) {}

  public record ProductoResponse(
      Long id,
      String nombre,
      String descripcion,
      BigDecimal precio,
      Long categoriaId,
      String categoria,
      boolean activo) {}
}
