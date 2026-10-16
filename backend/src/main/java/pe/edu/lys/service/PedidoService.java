package pe.edu.lys.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.lys.dto.PedidoDtos.*;
import pe.edu.lys.entity.*;
import pe.edu.lys.exception.ApiException;
import pe.edu.lys.repository.*;

@Service
@Transactional
public class PedidoService {
  private final PedidoRepository pedidos;
  private final ProductoRepository productos;
  private final ComboRepository combos;
  private final ClienteRepository clientes;
  private final UsuarioRepository usuarios;
  private final CatalogoComercialService catalogo;
  private final BigDecimal costoDelivery;

  public PedidoService(PedidoRepository pedidos, ProductoRepository productos, ComboRepository combos,
      ClienteRepository clientes, UsuarioRepository usuarios, CatalogoComercialService catalogo,
      @Value("${app.pedidos.costo-delivery}") BigDecimal costoDelivery) {
    this.pedidos = pedidos;
    this.productos = productos;
    this.combos = combos;
    this.clientes = clientes;
    this.usuarios = usuarios;
    this.catalogo = catalogo;
    if (costoDelivery.signum() < 0 || costoDelivery.compareTo(BigDecimal.valueOf(1000)) > 0
        || costoDelivery.stripTrailingZeros().scale() > 2)
      throw new IllegalArgumentException("DELIVERY_COST debe estar entre 0 y 1000, con hasta 2 decimales");
    this.costoDelivery = costoDelivery.setScale(2, RoundingMode.UNNECESSARY);
  }

  private record Actor(Cliente cliente, Usuario usuario) {}








  private void completar(Pedido pedido, PedidoRequest datos) {
    String mesa = limpiar(datos.mesa());
    String direccion = limpiar(datos.direccion());
    String telefono = limpiar(datos.telefono());
    if (datos.modalidad() == Modalidad.LOCAL && mesa == null)
      throw invalido("La atención local requiere una mesa");
    if (datos.modalidad() == Modalidad.DELIVERY && direccion == null)
      throw invalido("Delivery requiere una dirección");
    if (datos.modalidad() != Modalidad.LOCAL && telefono == null)
      throw invalido("Recojo y delivery requieren un teléfono de contacto");
    Instant ahora = Instant.now();
    List<Promocion> ofertas = catalogo.vigentes(ahora);
    // Preparar todas las líneas antes de sustituir el detalle existente.
    List<PedidoItem> nuevos = datos.items().stream().map(i -> prepararItem(i, ofertas)).toList();
    pedido.setModalidad(datos.modalidad());
    pedido.setNombreContacto(datos.nombreContacto().trim());
    pedido.setTelefono(telefono);
    pedido.setMesa(datos.modalidad() == Modalidad.LOCAL ? mesa : null);
    pedido.setDireccion(datos.modalidad() == Modalidad.DELIVERY ? direccion : null);
    pedido.setReferencia(datos.modalidad() == Modalidad.DELIVERY ? limpiar(datos.referencia()) : null);
    pedido.getItems().clear();
    nuevos.forEach(i -> { i.setPedido(pedido); pedido.getItems().add(i); });
    pedido.setSubtotal(nuevos.stream().map(PedidoItem::getTotal).reduce(new BigDecimal("0.00"), BigDecimal::add));
    pedido.setCostoDelivery(datos.modalidad() == Modalidad.DELIVERY ? costoDelivery : new BigDecimal("0.00"));
    pedido.setTotal(pedido.getSubtotal().add(pedido.getCostoDelivery()));
    pedido.setActualizadoEn(ahora);
  }

  private PedidoItem prepararItem(PedidoItemRequest datos, List<Promocion> ofertas) {
    if ((datos.productoId() == null) == (datos.comboId() == null))
      throw invalido("Cada línea debe indicar productoId o comboId, exclusivamente");
    PedidoItem item = new PedidoItem();
    Set<String> opciones = new LinkedHashSet<>();
    if (datos.productoId() != null) {
      Producto producto = productos.findById(datos.productoId())
          .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
      if (!Disponibilidad.producto(producto)) throw conflicto("El producto no está disponible");
      item.setProducto(producto);
      item.setNombre(producto.getNombre());
      item.setPrecioBase(producto.getPrecio());
      opciones.addAll(producto.getOpcionesPersonalizacion());
    } else {
      Combo combo = combos.findById(datos.comboId())
          .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Combo no encontrado"));
      if (!Disponibilidad.combo(combo)) throw conflicto("El combo no está disponible");
      item.setCombo(combo);
      item.setNombre(combo.getNombre());
      item.setPrecioBase(combo.getPrecio());
      item.setContenidoCombo(combo.getItems().stream()
          .map(i -> i.getCantidad() + " x " + i.getProducto().getNombre()).collect(Collectors.joining("; ")));
      for (ComboItem componente : combo.getItems()) {
        Producto producto = componente.getProducto();
        producto.getOpcionesPersonalizacion().forEach(o -> opciones.add(producto.getId() + ":" + o));
      }
    }
    Promocion oferta = ofertas.stream().filter(p -> item.getProducto() != null
        ? p.getProducto() != null && p.getProducto().getId().equals(item.getProducto().getId())
        : p.getCombo() != null && p.getCombo().getId().equals(item.getCombo().getId()))
        .max(Comparator.comparing(Promocion::getPorcentajeDescuento).thenComparing(Promocion::getId))
        .orElse(null);
    item.setPromocion(oferta);
    item.setPorcentajeDescuento(oferta == null ? new BigDecimal("0.00") : oferta.getPorcentajeDescuento());
    item.setPrecioUnitario(CatalogoComercialService.precioConDescuento(item.getPrecioBase(), item.getPorcentajeDescuento()));
    item.setCantidad(datos.cantidad());
    item.setTotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(datos.cantidad())));
    return item;
  }




  private String limpiar(String texto) { return texto == null || texto.isBlank() ? null : texto.trim(); }
  private ApiException invalido(String mensaje) { return new ApiException(HttpStatus.BAD_REQUEST, mensaje); }
  private ApiException conflicto(String mensaje) { return new ApiException(HttpStatus.CONFLICT, mensaje); }
}
