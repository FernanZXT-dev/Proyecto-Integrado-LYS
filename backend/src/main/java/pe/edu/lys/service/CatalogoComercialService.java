package pe.edu.lys.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.lys.dto.ComboDtos.*;
import pe.edu.lys.dto.PromocionDtos.*;
import pe.edu.lys.dto.ProductoDtos.ProductoResponse;
import pe.edu.lys.entity.*;
import pe.edu.lys.exception.ApiException;
import pe.edu.lys.repository.*;

@Service
@Transactional
public class CatalogoComercialService {
  private final ComboRepository combos;
  private final PromocionRepository promociones;
  private final ProductoRepository productos;
  private final ProductoService productoService;

  public CatalogoComercialService(ComboRepository combos, PromocionRepository promociones,
      ProductoRepository productos, ProductoService productoService) {
    this.combos = combos;
    this.promociones = promociones;
    this.productos = productos;
    this.productoService = productoService;
  }

  public record CatalogoResponse(List<ProductoResponse> productos, List<ComboResponse> combos,
      List<PromocionResponse> promociones) {}

  @Transactional(readOnly = true)
  public CatalogoResponse catalogo() {
    return new CatalogoResponse(productoService.listar(false), listarCombos(false), listarPromociones(false));
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

  @Transactional(readOnly = true)
  public List<PromocionResponse> listarPromociones(boolean gestion) {
    Instant ahora = Instant.now();
    return promociones.findAll().stream()
        .filter(p -> gestion || Disponibilidad.promocion(p, ahora))
        .sorted(Comparator.comparing(Promocion::getNombre).thenComparing(Promocion::getId))
        .map(p -> respuestaPromocion(p, ahora)).toList();
  }

  @Transactional(readOnly = true)
  public PromocionResponse consultarPromocion(Long id) {
    Promocion promocion = obtenerPromocion(id);
    Instant ahora = Instant.now();
    if (!Disponibilidad.promocion(promocion, ahora)) throw noEncontrado("Promoción");
    return respuestaPromocion(promocion, ahora);
  }

  public PromocionResponse crearPromocion(PromocionRequest datos) {
    return guardarPromocion(new Promocion(), datos);
  }

  public PromocionResponse actualizarPromocion(Long id, PromocionRequest datos) {
    return guardarPromocion(obtenerPromocion(id), datos);
  }

  public void desactivarPromocion(Long id) {
    obtenerPromocion(id).setActivo(false);
  }

  private PromocionResponse guardarPromocion(Promocion promocion, PromocionRequest datos) {
    if ((datos.productoId() == null) == (datos.comboId() == null))
      throw invalido("La promoción debe corresponder a un producto o a un combo, exclusivamente");
    if (!datos.fin().isAfter(datos.inicio())) throw invalido("El fin debe ser posterior al inicio");
    promocion.setNombre(datos.nombre().trim());
    promocion.setDescripcion(datos.descripcion());
    promocion.setPorcentajeDescuento(datos.porcentajeDescuento());
    promocion.setInicio(datos.inicio());
    promocion.setFin(datos.fin());
    promocion.setActivo(datos.activo());
    promocion.setProducto(datos.productoId() == null ? null : productos.findById(datos.productoId())
        .orElseThrow(() -> noEncontrado("Producto")));
    promocion.setCombo(datos.comboId() == null ? null : obtenerCombo(datos.comboId()));
    return respuestaPromocion(promociones.save(promocion), Instant.now());
  }

  @Transactional(readOnly = true)
  public List<Promocion> vigentes(Instant ahora) {
    return promociones.findByActivoTrueAndInicioLessThanEqualAndFinGreaterThan(ahora, ahora)
        .stream().filter(p -> Disponibilidad.promocion(p, ahora)).toList();
  }

  private Combo obtenerCombo(Long id) {
    return combos.findById(id).orElseThrow(() -> noEncontrado("Combo"));
  }

  private Promocion obtenerPromocion(Long id) {
    return promociones.findById(id).orElseThrow(() -> noEncontrado("Promoción"));
  }

  private ComboResponse respuestaCombo(Combo combo) {
    return new ComboResponse(combo.getId(), combo.getNombre(), combo.getDescripcion(),
        combo.getPrecio(), combo.isActivo(), Disponibilidad.combo(combo),
        combo.getItems().stream().map(i -> new ComboItemResponse(
            i.getProducto().getId(), i.getProducto().getNombre(), i.getCantidad(),
            i.getProducto().getOpcionesPersonalizacion().stream().sorted().toList())).toList());
  }

  private PromocionResponse respuestaPromocion(Promocion promocion, Instant ahora) {
    BigDecimal base = promocion.getProducto() != null
        ? promocion.getProducto().getPrecio() : promocion.getCombo().getPrecio();
    return new PromocionResponse(promocion.getId(), promocion.getNombre(), promocion.getDescripcion(),
        promocion.getPorcentajeDescuento(), promocion.getInicio(), promocion.getFin(),
        promocion.isActivo(), Disponibilidad.promocion(promocion, ahora),
        promocion.getProducto() == null ? null : promocion.getProducto().getId(),
        promocion.getCombo() == null ? null : promocion.getCombo().getId(),
        precioConDescuento(base, promocion.getPorcentajeDescuento()));
  }

  static BigDecimal precioConDescuento(BigDecimal precio, BigDecimal porcentaje) {
    return precio.multiply(BigDecimal.valueOf(100).subtract(porcentaje))
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
  }

  private ApiException noEncontrado(String recurso) {
    return new ApiException(HttpStatus.NOT_FOUND, recurso + " no encontrado");
  }

  private ApiException invalido(String mensaje) {
    return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
  }
}
