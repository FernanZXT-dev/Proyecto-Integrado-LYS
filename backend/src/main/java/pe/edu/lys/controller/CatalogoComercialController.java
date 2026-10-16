package pe.edu.lys.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.lys.dto.ComboDtos.*;
import pe.edu.lys.dto.PromocionDtos.*;
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

  @GetMapping("/promociones")
  public List<PromocionResponse> promociones() { return service.listarPromociones(false); }

  @GetMapping("/promociones/{id}")
  public PromocionResponse promocion(@PathVariable Long id) { return service.consultarPromocion(id); }

  @GetMapping("/admin/promociones")
  public List<PromocionResponse> gestionPromociones() { return service.listarPromociones(true); }

  @PostMapping("/promociones") @ResponseStatus(HttpStatus.CREATED)
  public PromocionResponse crearPromocion(@Valid @RequestBody PromocionRequest datos) {
    return service.crearPromocion(datos);
  }

  @PutMapping("/promociones/{id}")
  public PromocionResponse editarPromocion(@PathVariable Long id, @Valid @RequestBody PromocionRequest datos) {
    return service.actualizarPromocion(id, datos);
  }

  @DeleteMapping("/promociones/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void desactivarPromocion(@PathVariable Long id) { service.desactivarPromocion(id); }
}
