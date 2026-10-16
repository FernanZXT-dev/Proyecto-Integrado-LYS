package pe.edu.lys.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.lys.dto.ComboDtos.*;
import pe.edu.lys.dto.ProductoDtos.ProductoResponse;
import pe.edu.lys.entity.*;
import pe.edu.lys.exception.ApiException;
import pe.edu.lys.repository.*;

@Service
@Transactional
public class CatalogoComercialService {
  private final ComboRepository combos;
  private final ProductoRepository productos;
  private final ProductoService productoService;

  public CatalogoComercialService(ComboRepository combos,
      ProductoRepository productos, ProductoService productoService) {
    this.combos = combos;
    this.productos = productos;
    this.productoService = productoService;
  }



  @Transactional(readOnly = true)
  public List<ComboResponse> listarCombos(boolean gestion) {
    return combos.findAll().stream().filter(c -> gestion || Disponibilidad.combo(c))
        .sorted(Comparator.comparing(Combo::getNombre).thenComparing(Combo::getId))
        .map(this::respuestaCombo).toList();
  }

  @Transactional(readOnly = true)
  public ComboResponse consultarCombo(Long id) {
    Combo combo = obtenerCombo(id);
    if (!Disponibilidad.combo(combo)) throw noEncontrado("Combo");
    return respuestaCombo(combo);
  }

  public ComboResponse crearCombo(ComboRequest datos) {
    return guardarCombo(new Combo(), datos);
  }

  public ComboResponse actualizarCombo(Long id, ComboRequest datos) {
    return guardarCombo(obtenerCombo(id), datos);
  }

  public void desactivarCombo(Long id) {
    obtenerCombo(id).setActivo(false);
  }

  private ComboResponse guardarCombo(Combo combo, ComboRequest datos) {
    Map<Long, Producto> seleccionados = new LinkedHashMap<>();
    for (ComboItemRequest item : datos.items()) {
      if (seleccionados.containsKey(item.productoId()))
        throw invalido("Un producto no puede repetirse en el combo; usar cantidad");
      seleccionados.put(item.productoId(), productos.findById(item.productoId())
          .orElseThrow(() -> noEncontrado("Producto")));
    }
    combo.setNombre(datos.nombre().trim());
    combo.setDescripcion(datos.descripcion());
    combo.setPrecio(datos.precio());
    combo.setActivo(datos.activo());
    // Conservar filas existentes evita conflictos de unicidad al editar la composición.
    combo.getItems().removeIf(i -> !seleccionados.containsKey(i.getProducto().getId()));
    for (ComboItemRequest datosItem : datos.items()) {
      ComboItem item = combo.getItems().stream()
          .filter(i -> i.getProducto().getId().equals(datosItem.productoId())).findFirst().orElse(null);
      if (item == null) {
        item = new ComboItem();
        item.setCombo(combo);
        item.setProducto(seleccionados.get(datosItem.productoId()));
        combo.getItems().add(item);
      }
      item.setCantidad(datosItem.cantidad());
    }
    return respuestaCombo(combos.saveAndFlush(combo));
  }








  private Combo obtenerCombo(Long id) {
    return combos.findById(id).orElseThrow(() -> noEncontrado("Combo"));
  }


  private ComboResponse respuestaCombo(Combo combo) {
    return new ComboResponse(combo.getId(), combo.getNombre(), combo.getDescripcion(),
        combo.getPrecio(), combo.isActivo(), Disponibilidad.combo(combo),
        combo.getItems().stream().map(i -> new ComboItemResponse(
            i.getProducto().getId(), i.getProducto().getNombre(), i.getCantidad(),
            i.getProducto().getOpcionesPersonalizacion().stream().sorted().toList())).toList());
  }



  private ApiException noEncontrado(String recurso) {
    return new ApiException(HttpStatus.NOT_FOUND, recurso + " no encontrado");
  }

  private ApiException invalido(String mensaje) {
    return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
  }
}
