package pe.edu.lys.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.lys.dto.AuthDtos.CuentaResponse;
import pe.edu.lys.dto.UsuarioDtos.*;
import pe.edu.lys.service.UsuarioService;

@RestController
@RequestMapping("/api")
public class UsuarioController {
  private final UsuarioService service;

  public UsuarioController(UsuarioService service) {
    this.service = service;
  }



  @GetMapping("/roles")
  public List<RolResponse> roles() {
    return service.roles();
  }
}
