package pe.edu.lys.dto;

import jakarta.validation.constraints.*;

public final class UsuarioDtos {
  private UsuarioDtos() {}

  public record CrearUsuarioRequest(
      @NotBlank @Size(max = 100) String nombre,
      @NotBlank @Email @Size(max = 150) String correo,
      @NotBlank @Size(min = 8, max = 64) String clave,
      @NotNull Long rolId) {}

  public record RolResponse(Long id, String nombre) {}
}
