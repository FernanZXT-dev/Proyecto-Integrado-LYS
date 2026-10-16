package pe.edu.lys.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.lys.dto.AuthDtos.*;
import pe.edu.lys.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/registro")
  @ResponseStatus(HttpStatus.CREATED)
  public CuentaResponse registro(@Valid @RequestBody RegistroClienteRequest datos) {
    return service.registrar(datos);
  }

  @PostMapping("/login/cliente")
  public LoginResponse cliente(@Valid @RequestBody LoginRequest datos) {
    return service.loginCliente(datos);
  }

  @PostMapping("/login/personal")
  public LoginResponse personal(@Valid @RequestBody LoginRequest datos) {
    return service.loginPersonal(datos);
  }
}
