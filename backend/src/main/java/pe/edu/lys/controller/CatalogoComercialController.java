package pe.edu.lys.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.lys.dto.ComboDtos.*;
import pe.edu.lys.service.CatalogoComercialService;

@RestController
@RequestMapping("/api")
public class CatalogoComercialController {
  private final CatalogoComercialService service;

  public CatalogoComercialController(CatalogoComercialService service) { this.service = service; }


  @GetMapping("/combos")
  public List<ComboResponse> combos() { return service.listarCombos(false); }

  @GetMapping("/combos/{id}")
  public ComboResponse combo(@PathVariable Long id) { return service.consultarCombo(id); }

  @GetMapping("/admin/combos")
  public List<ComboResponse> gestionCombos() { return service.listarCombos(true); }

  @PostMapping("/combos") @ResponseStatus(HttpStatus.CREATED)
  public ComboResponse crearCombo(@Valid @RequestBody ComboRequest datos) {
    return service.crearCombo(datos);
  }

  @PutMapping("/combos/{id}")
  public ComboResponse editarCombo(@PathVariable Long id, @Valid @RequestBody ComboRequest datos) {
    return service.actualizarCombo(id, datos);
  }

  @DeleteMapping("/combos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void desactivarCombo(@PathVariable Long id) { service.desactivarCombo(id); }






}
