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

  public PedidoResponse crear(PedidoRequest datos, Jwt jwt) {
    Actor actor = actor(jwt);
    Pedido pedido = new Pedido();
    pedido.setCliente(actor.cliente());
    pedido.setRegistradoPor(actor.usuario());
    pedido.setCreadoEn(Instant.now());
    completar(pedido, datos);
    return respuesta(pedidos.saveAndFlush(pedido));
  }

  @Transactional(readOnly = true)
  public List<PedidoResponse> listar(Jwt jwt) {
    Actor actor = actor(jwt);
    return (actor.cliente() != null
        ? pedidos.findByClienteIdOrderByCreadoEnDescIdDesc(actor.cliente().getId())
        : pedidos.findAllByOrderByCreadoEnDescIdDesc()).stream().map(this::respuesta).toList();
  }

  @Transactional(readOnly = true)
  public PedidoResponse consultar(Long id, Jwt jwt) {
    Pedido pedido = pedidos.findById(id).orElseThrow(this::noEncontrado);
    autorizarPropietario(pedido, actor(jwt));
    return respuesta(pedido);
  }

  public PedidoResponse actualizar(Long id, PedidoRequest datos, Jwt jwt) {
    Pedido pedido = pedidos.bloquear(id).orElseThrow(this::noEncontrado);
    autorizarPropietario(pedido, actor(jwt));
    comprobarEditable(pedido);
    completar(pedido, datos);
    return respuesta(pedidos.saveAndFlush(pedido));
  }

  public PedidoResponse cancelar(Long id, Jwt jwt) {
    Pedido pedido = pedidos.bloquear(id).orElseThrow(this::noEncontrado);
    autorizarPropietario(pedido, actor(jwt));
    comprobarEditable(pedido);
    pedido.setEstado(EstadoPedido.CANCELADO);
    pedido.setActualizadoEn(Instant.now());
    return respuesta(pedido);
  }

  /** Marca el límite de RF06. El flujo completo de cocina y despacho queda para RF08/RF09. */
  public PedidoResponse iniciarPreparacion(Long id, Jwt jwt) {
    Actor actor = actor(jwt);
    if (actor.usuario() == null || !Set.of("ADMINISTRADOR", "COCINA")
        .contains(actor.usuario().getRol().getNombre()))
      throw new ApiException(HttpStatus.FORBIDDEN, "Solo cocina o administración puede iniciar la preparación");
    Pedido pedido = pedidos.bloquear(id).orElseThrow(this::noEncontrado);
    comprobarEditable(pedido);
    pedido.setEstado(EstadoPedido.EN_PREPARACION);
    pedido.setActualizadoEn(Instant.now());
    return respuesta(pedido);
  }

  private void comprobarEditable(Pedido pedido) {
    if (pedido.getEstado() != EstadoPedido.RECIBIDO)
      throw new ApiException(HttpStatus.CONFLICT,
          "Solo se puede modificar o cancelar un pedido recibido que aún no inicia su preparación");
  }

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
    for (String seleccion : datos.personalizaciones() == null ? List.<String>of() : datos.personalizaciones()) {
      String canonica = opciones.stream().filter(o -> o.equalsIgnoreCase(seleccion.trim())).findFirst()
          .orElseThrow(() -> invalido("Personalización no permitida para el producto o combo: " + seleccion));
      if (!item.getPersonalizaciones().add(canonica)) throw invalido("Personalizaciones duplicadas");
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
    item.setIndicaciones(limpiar(datos.indicaciones()));
    return item;
  }

  private void autorizarPropietario(Pedido pedido, Actor actor) {
    if (actor.cliente() != null && (pedido.getCliente() == null
        || !pedido.getCliente().getId().equals(actor.cliente().getId()))) throw noEncontrado();
  }

  private Actor actor(Jwt jwt) {
    if (jwt == null) throw sesionInvalida();
    String rol = jwt.getClaimAsString("rol");
    String prefijo = "CLIENTE".equals(rol) ? "cliente:" : "usuario:";
    if (jwt.getSubject() == null || !jwt.getSubject().startsWith(prefijo)) throw sesionInvalida();
    long id;
    try { id = Long.parseLong(jwt.getSubject().substring(prefijo.length())); }
    catch (NumberFormatException e) { throw sesionInvalida(); }
    if ("CLIENTE".equals(rol)) {
      Cliente cliente = clientes.findById(id).filter(Cliente::isActivo).orElseThrow(this::sesionInvalida);
      return new Actor(cliente, null);
    }
    Usuario usuario = usuarios.findById(id).filter(Usuario::isActivo)
        .filter(u -> u.getRol().getNombre().equals(rol)).orElseThrow(this::sesionInvalida);
    return new Actor(null, usuario);
  }

  private PedidoResponse respuesta(Pedido pedido) {
    return new PedidoResponse(pedido.getId(), pedido.getCliente() == null ? null : pedido.getCliente().getId(),
        pedido.getModalidad(), pedido.getEstado(), pedido.getNombreContacto(), pedido.getTelefono(),
        pedido.getMesa(), pedido.getDireccion(), pedido.getReferencia(), pedido.getSubtotal(),
        pedido.getCostoDelivery(), pedido.getTotal(), pedido.getCreadoEn(), pedido.getActualizadoEn(),
        pedido.getItems().stream().map(i -> new PedidoItemResponse(i.getId(),
            i.getProducto() == null ? null : i.getProducto().getId(),
            i.getCombo() == null ? null : i.getCombo().getId(),
            i.getPromocion() == null ? null : i.getPromocion().getId(), i.getNombre(), i.getContenidoCombo(),
            i.getCantidad(), i.getPrecioBase(), i.getPorcentajeDescuento(), i.getPrecioUnitario(), i.getTotal(),
            i.getPersonalizaciones().stream().sorted().toList(), i.getIndicaciones())).toList());
  }

  private String limpiar(String texto) { return texto == null || texto.isBlank() ? null : texto.trim(); }
  private ApiException noEncontrado() { return new ApiException(HttpStatus.NOT_FOUND, "Pedido no encontrado"); }
  private ApiException sesionInvalida() { return new ApiException(HttpStatus.UNAUTHORIZED, "La cuenta o sesión no está activa"); }
  private ApiException invalido(String mensaje) { return new ApiException(HttpStatus.BAD_REQUEST, mensaje); }
  private ApiException conflicto(String mensaje) { return new ApiException(HttpStatus.CONFLICT, mensaje); }
}
