package pe.edu.lys.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.edu.lys.dto.PedidoDtos.*;
import pe.edu.lys.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
  private final PedidoService service;

  public PedidoController(PedidoService service) { this.service = service; }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public PedidoResponse crear(@Valid @RequestBody PedidoRequest datos, @AuthenticationPrincipal Jwt jwt) {
    return service.crear(datos, jwt);
  }

  @GetMapping
  public List<PedidoResponse> listar(@AuthenticationPrincipal Jwt jwt) { return service.listar(jwt); }

  @GetMapping("/{id}")
  public PedidoResponse consultar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    return service.consultar(id, jwt);
  }

  @PutMapping("/{id}")
  public PedidoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PedidoRequest datos,
      @AuthenticationPrincipal Jwt jwt) { return service.actualizar(id, datos, jwt); }


}
