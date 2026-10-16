package pe.edu.lys.dto;

import jakarta.validation.constraints.*;

public final class CategoriaDtos {
  private CategoriaDtos() {}

  public record CategoriaRequest(
      @NotBlank @Size(max = 100) String nombre, @NotNull Boolean activo) {}

  public record CategoriaResponse(Long id, String nombre, boolean activo) {}
}
