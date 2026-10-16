package pe.edu.lys.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.lys.dto.ProductoDtos.*;
import pe.edu.lys.service.ProductoService;

@RestController
@RequestMapping("/api")
public class ProductoController {
  private final ProductoService service;

  public ProductoController(ProductoService service) {
    this.service = service;
  }

  @GetMapping("/productos")
  public List<ProductoResponse> listar() {
    return service.listar(false);
  }

  @GetMapping("/admin/productos")
  public List<ProductoResponse> gestion() {
    return service.listar(true);
  }

  @GetMapping("/productos/{id}")
  public ProductoResponse consultar(@PathVariable Long id) {
    return service.consultar(id);
  }

  @PostMapping("/productos")
  @ResponseStatus(HttpStatus.CREATED)
  public ProductoResponse crear(@Valid @RequestBody ProductoRequest datos) {
    return service.crear(datos);
  }

  @PutMapping("/productos/{id}")
  public ProductoResponse actualizar(
      @PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
    return service.actualizar(id, datos);
  }

  @DeleteMapping("/productos/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void desactivar(@PathVariable Long id) {
    service.desactivar(id);
  }
}
