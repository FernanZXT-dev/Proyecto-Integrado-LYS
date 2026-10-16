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


}
