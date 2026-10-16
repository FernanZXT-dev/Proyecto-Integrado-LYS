package pe.edu.lys.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.lys.dto.CategoriaDtos.*;
import pe.edu.lys.service.CategoriaService;

@RestController
@RequestMapping("/api")
public class CategoriaController {
  private final CategoriaService service;

  public CategoriaController(CategoriaService service) {
    this.service = service;
  }

  @GetMapping("/categorias")
  public List<CategoriaResponse> listar() {
    return service.listar(false);
  }

  @GetMapping("/admin/categorias")
  public List<CategoriaResponse> gestion() {
    return service.listar(true);
  }

  @GetMapping("/categorias/{id}")
  public CategoriaResponse consultar(@PathVariable Long id) {
    return service.consultar(id);
  }

  @PostMapping("/categorias")
  @ResponseStatus(HttpStatus.CREATED)
  public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest datos) {
    return service.crear(datos);
  }


}
