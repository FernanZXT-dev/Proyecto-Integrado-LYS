package pe.edu.lys.dto;

import jakarta.validation.constraints.*;

public final class AuthDtos {
  private AuthDtos() {}

  public record RegistroClienteRequest(
      @NotBlank @Size(max = 100) String nombre,
      @NotBlank @Email @Size(max = 150) String correo,
      @NotBlank @Size(min = 8, max = 64) String clave,
      @Size(max = 20) String telefono) {}

  public record LoginRequest(@NotBlank @Email String correo, @NotBlank String clave) {}

  public record LoginResponse(String token, String tipo, long expiraEnSegundos, String rol) {}

  public record CuentaResponse(Long id, String nombre, String correo, String rol, boolean activo) {}
}
