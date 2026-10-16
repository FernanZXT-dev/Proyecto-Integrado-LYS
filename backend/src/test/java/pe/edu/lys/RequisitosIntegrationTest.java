package pe.edu.lys;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.edu.lys.entity.*;
import pe.edu.lys.repository.*;
import pe.edu.lys.service.TokenService;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "app.jwt.secret=pruebas-requisitos-clave-de-al-menos-32-bytes",
    "app.admin.correo=", "app.admin.clave=", "app.pedidos.costo-delivery=6.00"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequisitosIntegrationTest {
  private static final PostgresTestDatabase DATABASE = new PostgresTestDatabase();
  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry properties) { DATABASE.configure(properties); }
  @AfterAll
  static void cerrar() throws Exception { DATABASE.close(); }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired TokenService tokens;
  @Autowired PasswordEncoder encoder;
  @Autowired RolRepository roles;
  @Autowired UsuarioRepository usuarios;
  @Autowired ClienteRepository clientes;
  @Autowired CategoriaRepository categorias;
  @Autowired ProductoRepository productos;
  @Autowired ComboRepository combos;
  @Autowired PromocionRepository promociones;
  @Autowired PedidoRepository pedidos;
  private String admin, cliente, otroCliente, cocina, mesera, ventas;
  private long clienteId, productoId, bebidaId;

  @BeforeEach
  void preparar() throws Exception {
    pedidos.deleteAll();
    promociones.deleteAll();
    combos.deleteAll();
    productos.deleteAll();
    categorias.deleteAll();
    clientes.deleteAll();
    usuarios.deleteAll();
    admin = personal("ADMINISTRADOR");
    cocina = personal("COCINA");
    mesera = personal("MESERA");
    ventas = personal("VENTAS");
    Cliente ana = cliente("ana@example.test");
    clienteId = ana.getId();
    cliente = tokens.generar("cliente:" + clienteId, "CLIENTE");
    otroCliente = tokens.generar("cliente:" + cliente("otro@example.test").getId(), "CLIENTE");
    long categoria = enviar(post("/api/categorias"), admin, Map.of("nombre", "Carta", "activo", true), 201).get("id").asLong();
    productoId = enviar(post("/api/productos"), admin, producto(categoria, "Pollo", 60, true), 201).get("id").asLong();
    bebidaId = enviar(post("/api/productos"), admin, producto(categoria, "Bebida", 10, true), 201).get("id").asLong();
  }

  private String personal(String rol) {
    Usuario usuario = new Usuario();
    usuario.setNombre(rol);
    usuario.setCorreo(rol.toLowerCase(Locale.ROOT) + "@example.test");
    usuario.setClave(encoder.encode("ClavePrueba123"));
    usuario.setRol(roles.findByNombre(rol).orElseThrow());
    usuarios.save(usuario);
    return tokens.generar("usuario:" + usuario.getId(), rol);
  }

  private Cliente cliente(String correo) {
    Cliente c = new Cliente();
    c.setNombre("Cliente"); c.setCorreo(correo); c.setClave(encoder.encode("ClavePrueba123"));
    return clientes.save(c);
  }

  private Map<String, Object> producto(long categoria, String nombre, int precio, boolean activo) {
    return Map.of("nombre", nombre, "precio", precio, "categoriaId", categoria, "activo", activo,
        "opcionesPersonalizacion", List.of("Sin ensalada", "Salsa aparte"));
  }

  private JsonNode enviar(MockHttpServletRequestBuilder request, String token, Object body, int estado) throws Exception {
    if (token != null) request.header("Authorization", "Bearer " + token);
    if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
    String content = mvc.perform(request).andExpect(status().is(estado)).andReturn().getResponse().getContentAsString();
    return content.isBlank() ? json.getNodeFactory().nullNode() : json.readTree(content);
  }

  private Map<String, Object> comboBody(boolean activo) {
    return Map.of("nombre", "Combo familiar", "descripcion", "Pollo y bebida", "precio", 65,
        "activo", activo, "items", List.of(Map.of("productoId", productoId, "cantidad", 1),
            Map.of("productoId", bebidaId, "cantidad", 1)));
  }

  private long crearCombo() throws Exception {
    return enviar(post("/api/combos"), admin, comboBody(true), 201).get("id").asLong();
  }

  private Map<String, Object> promoBody(Long producto, Long combo, int descuento, Instant inicio, Instant fin) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("nombre", "Promoción " + descuento); body.put("porcentajeDescuento", descuento);
    body.put("inicio", inicio.toString()); body.put("fin", fin.toString()); body.put("activo", true);
    if (producto != null) body.put("productoId", producto);
    if (combo != null) body.put("comboId", combo);
    return body;
  }

  private long promocion(Long producto, Long combo, int descuento) throws Exception {
    return enviar(post("/api/promociones"), admin, promoBody(producto, combo, descuento,
        Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600)), 201).get("id").asLong();
  }

  private Map<String, Object> pedido(Modalidad modalidad) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("modalidad", modalidad.name()); body.put("nombreContacto", "Ana");
    body.put("telefono", "999888777"); body.put("mesa", "03");
    body.put("direccion", "Av. Principal 123"); body.put("referencia", "Puerta azul");
    body.put("items", List.of(Map.of("productoId", productoId, "cantidad", 1,
        "personalizaciones", List.of("Sin ensalada"), "indicaciones", "Salsa en envase aparte")));
    return body;
  }

  private long crearPedido() throws Exception {
    return enviar(post("/api/pedidos"), cliente, pedido(Modalidad.RECOJO), 201).get("id").asLong();
  }

  @Test
  void rf01AdministradorReactivaProductoYClienteNoGestiona() throws Exception {
    enviar(delete("/api/productos/" + productoId), admin, null, 204);
    enviar(get("/api/productos/" + productoId), null, null, 404);
    long categoria = enviar(get("/api/admin/productos"), admin, null, 200).get(0).get("categoriaId").asLong();
    enviar(put("/api/productos/" + productoId), cliente, producto(categoria, "Pollo", 60, true), 403);
    JsonNode actualizado = enviar(put("/api/productos/" + productoId), admin,
        producto(categoria, "Pollo editado", 62, true), 200);
    assertEquals(62, actualizado.get("precio").asInt());
    assertTrue(enviar(get("/api/productos/" + productoId), null, null, 200).get("activo").asBoolean());
  }

  @Test
  void rf02CombosSeCreanEditanDesactivanYReactivan() throws Exception {
    long id = crearCombo();
    Map<String, Object> editado = new LinkedHashMap<>(comboBody(true));
    editado.put("nombre", "Combo editado"); editado.put("precio", 70);
    editado.put("items", List.of(Map.of("productoId", productoId, "cantidad", 2)));
    JsonNode combo = enviar(put("/api/combos/" + id), admin, editado, 200);
    assertEquals(1, combo.get("items").size());
    assertEquals(2, combo.get("items").get(0).get("cantidad").asInt());
    enviar(delete("/api/combos/" + id), admin, null, 204);
    enviar(get("/api/combos/" + id), null, null, 404);
    assertEquals(1, enviar(get("/api/admin/combos"), admin, null, 200).size());
    enviar(put("/api/combos/" + id), admin, comboBody(true), 200);
    assertTrue(enviar(get("/api/combos/" + id), null, null, 200).get("disponible").asBoolean());
    enviar(post("/api/combos"), cliente, comboBody(true), 403);
    enviar(delete("/api/combos/" + id), mesera, null, 403);
  }

  @Test
  void rf02PromocionesSeEditanReactivanYValidanFechasYDestino() throws Exception {
    long id = promocion(productoId, null, 10);
    Map<String, Object> body = promoBody(null, crearCombo(), 20,
        Instant.now().minusSeconds(100), Instant.now().plusSeconds(1000));
    JsonNode editada = enviar(put("/api/promociones/" + id), admin, body, 200);
    assertTrue(editada.get("productoId").isNull());
    assertEquals(52, editada.get("precioPromocional").asInt());
    enviar(delete("/api/promociones/" + id), admin, null, 204);
    enviar(get("/api/promociones/" + id), null, null, 404);
    enviar(put("/api/promociones/" + id), admin, body, 200);
    enviar(get("/api/promociones/" + id), null, null, 200);
    body.put("productoId", productoId);
    enviar(post("/api/promociones"), admin, body, 400);
    body.remove("productoId"); body.put("fin", body.get("inicio"));
    enviar(post("/api/promociones"), admin, body, 400);
    enviar(post("/api/promociones"), cliente, body, 403);
    assertEquals(1, enviar(get("/api/admin/promociones"), admin, null, 200).size());
  }

  @Test
  void rf03CatalogoSoloPublicaDisponiblesYPromocionesVigentes() throws Exception {
    long combo = crearCombo();
    promocion(null, combo, 10);
    promocion(productoId, null, 20);
    enviar(post("/api/promociones"), admin, promoBody(bebidaId, null, 30,
        Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200)), 201);
    enviar(post("/api/promociones"), admin, promoBody(bebidaId, null, 40,
        Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600)), 201);
    JsonNode catalogo = enviar(get("/api/catalogo"), null, null, 200);
    assertEquals(2, catalogo.get("productos").size());
    assertEquals(1, catalogo.get("combos").size());
    assertEquals(2, catalogo.get("promociones").size());
    enviar(delete("/api/productos/" + productoId), admin, null, 204);
    catalogo = enviar(get("/api/catalogo"), null, null, 200);
    assertEquals(1, catalogo.get("productos").size());
    assertEquals(0, catalogo.get("combos").size());
    assertEquals(0, catalogo.get("promociones").size());
    Map<String, Object> pedido = pedido(Modalidad.RECOJO);
    pedido.put("items", List.of(Map.of("comboId", combo, "cantidad", 1)));
    enviar(post("/api/pedidos"), cliente, pedido, 409);
  }

  @ParameterizedTest
  @EnumSource(Modalidad.class)
  void rf04RegistraLasTresModalidadesYCalculaImportesEnServidor(Modalidad modalidad) throws Exception {
    Map<String, Object> body = pedido(modalidad);
    body.put("total", 1); body.put("clienteId", -1); body.put("estado", "EN_PREPARACION");
    JsonNode result = enviar(post("/api/pedidos"), cliente, body, 201);
    assertEquals(modalidad.name(), result.get("modalidad").asText());
    assertEquals("RECIBIDO", result.get("estado").asText());
    assertEquals(clienteId, result.get("clienteId").asLong());
    assertEquals(60, result.get("subtotal").asInt());
    assertEquals(modalidad == Modalidad.DELIVERY ? 66 : 60, result.get("total").asInt());
    assertEquals(modalidad == Modalidad.LOCAL, !result.get("mesa").isNull());
    assertEquals(modalidad == Modalidad.DELIVERY, !result.get("direccion").isNull());
  }

  @Test
  void rf04PersonalRegistraPedidosLocalesSinCuentaDeCliente() throws Exception {
    Map<String, Object> body = pedido(Modalidad.LOCAL);
    body.remove("telefono");
    JsonNode result = enviar(post("/api/pedidos"), mesera, body, 201);
    assertTrue(result.get("clienteId").isNull());
    enviar(get("/api/pedidos/" + result.get("id").asLong()), cliente, null, 404);
    assertEquals(1, enviar(get("/api/pedidos"), ventas, null, 200).size());
  }

  @Test
  void rf05PersisteOpcionesEIndicacionesPorLineaYPorComponenteDeCombo() throws Exception {
    long combo = crearCombo();
    Map<String, Object> body = pedido(Modalidad.RECOJO);
    body.put("items", List.of(
        Map.of("productoId", productoId, "cantidad", 1, "personalizaciones", List.of("Sin ensalada"), "indicaciones", "Sin sal"),
        Map.of("productoId", productoId, "cantidad", 1, "personalizaciones", List.of("Salsa aparte")),
        Map.of("comboId", combo, "cantidad", 1, "personalizaciones", List.of(productoId + ":Sin ensalada"))));
    long id = enviar(post("/api/pedidos"), cliente, body, 201).get("id").asLong();
    JsonNode guardado = enviar(get("/api/pedidos/" + id), cliente, null, 200);
    assertEquals(3, guardado.get("items").size());
    assertEquals("Sin sal", guardado.get("items").get(0).get("indicaciones").asText());
    assertEquals("Salsa aparte", guardado.get("items").get(1).get("personalizaciones").get(0).asText());
    assertEquals(productoId + ":Sin ensalada", guardado.get("items").get(2).get("personalizaciones").get(0).asText());
    assertTrue(guardado.get("items").get(2).get("contenidoCombo").asText().contains("Pollo"));
  }

  @Test
  void rf05RechazaOpcionesInventadasYDuplicadas() throws Exception {
    Map<String, Object> body = pedido(Modalidad.RECOJO);
    for (List<String> opciones : List.of(List.of("Extra gratis inventado"), List.of("Sin ensalada", "sin ensalada"))) {
      body.put("items", List.of(Map.of("productoId", productoId, "cantidad", 1, "personalizaciones", opciones)));
      enviar(post("/api/pedidos"), cliente, body, 400);
    }
    assertEquals(0, pedidos.count());
  }

  @Test
  void preciosYDescuentosSeConservanAunqueCambieElCatalogo() throws Exception {
    promocion(productoId, null, 10);
    long mejor = promocion(productoId, null, 20);
    long id = crearPedido();
    JsonNode pedido = enviar(get("/api/pedidos/" + id), cliente, null, 200);
    assertEquals(48, pedido.get("total").asInt());
    assertEquals(mejor, pedido.get("items").get(0).get("promocionId").asLong());
    long categoria = enviar(get("/api/productos/" + productoId), null, null, 200).get("categoriaId").asLong();
    enviar(put("/api/productos/" + productoId), admin, producto(categoria, "Nuevo nombre", 100, true), 200);
    enviar(delete("/api/promociones/" + mejor), admin, null, 204);
    pedido = enviar(get("/api/pedidos/" + id), cliente, null, 200);
    assertEquals(48, pedido.get("total").asInt());
    assertEquals("Pollo", pedido.get("items").get(0).get("nombre").asText());
  }

  @Test
  void aplicaPromocionDeComboSinAcumularLosDescuentosDeSusProductos() throws Exception {
    long combo = crearCombo();
    promocion(productoId, null, 50);
    promocion(null, combo, 10);
    Map<String, Object> body = pedido(Modalidad.RECOJO);
    body.put("items", List.of(Map.of("comboId", combo, "cantidad", 2)));
    JsonNode pedido = enviar(post("/api/pedidos"), cliente, body, 201);
    assertEquals("58.50", pedido.get("items").get(0).get("precioUnitario").decimalValue().setScale(2).toPlainString());
    assertEquals(117, pedido.get("total").asInt());
  }

  @Test
  void categoriaInactivaOcultaCombosPromocionesYRechazaPedidos() throws Exception {
    crearCombo();
    promocion(productoId, null, 10);
    long categoria = enviar(get("/api/productos/" + productoId), null, null, 200).get("categoriaId").asLong();
    enviar(delete("/api/categorias/" + categoria), admin, null, 204);
    JsonNode catalogo = enviar(get("/api/catalogo"), null, null, 200);
    assertEquals(0, catalogo.get("productos").size());
    assertEquals(0, catalogo.get("combos").size());
    assertEquals(0, catalogo.get("promociones").size());
    enviar(post("/api/pedidos"), cliente, pedido(Modalidad.RECOJO), 409);
  }

  @Test
  void rf06ModificaYCancelaAntesDePrepararSinBorrarElPedido() throws Exception {
    long id = crearPedido();
    Map<String, Object> body = pedido(Modalidad.DELIVERY);
    body.put("items", List.of(Map.of("productoId", bebidaId, "cantidad", 2)));
    JsonNode editado = enviar(put("/api/pedidos/" + id), cliente, body, 200);
    assertEquals(26, editado.get("total").asInt());
    assertEquals(1, editado.get("items").size());
    assertEquals(2, editado.get("items").get(0).get("cantidad").asInt());
    assertEquals("CANCELADO", enviar(post("/api/pedidos/" + id + "/cancelar"), cliente, null, 200).get("estado").asText());
    enviar(put("/api/pedidos/" + id), cliente, body, 409);
    enviar(post("/api/pedidos/" + id + "/iniciar-preparacion"), cocina, null, 409);
    assertEquals("CANCELADO", enviar(get("/api/pedidos/" + id), cliente, null, 200).get("estado").asText());
  }

  @Test
  void rf06ImpideModificarOCancelarUnaVezIniciadaLaPreparacion() throws Exception {
    long id = crearPedido();
    enviar(post("/api/pedidos/" + id + "/iniciar-preparacion"), cliente, null, 403);
    enviar(post("/api/pedidos/" + id + "/iniciar-preparacion"), mesera, null, 403);
    assertEquals("EN_PREPARACION", enviar(post("/api/pedidos/" + id + "/iniciar-preparacion"), cocina, null, 200).get("estado").asText());
    enviar(put("/api/pedidos/" + id), cliente, pedido(Modalidad.LOCAL), 409);
    enviar(post("/api/pedidos/" + id + "/cancelar"), admin, null, 409);
    assertEquals("EN_PREPARACION", enviar(get("/api/pedidos/" + id), cliente, null, 200).get("estado").asText());
  }

  @Test
  void clienteSoloConsultaModificaOCancelaSusPropiosPedidos() throws Exception {
    long id = crearPedido();
    assertEquals(0, enviar(get("/api/pedidos"), otroCliente, null, 200).size());
    enviar(get("/api/pedidos/" + id), otroCliente, null, 404);
    enviar(put("/api/pedidos/" + id), otroCliente, pedido(Modalidad.LOCAL), 404);
    enviar(post("/api/pedidos/" + id + "/cancelar"), otroCliente, null, 404);
    enviar(get("/api/pedidos/" + id), null, null, 401);
    enviar(get("/api/pedidos"), cocina, null, 403);
    assertEquals(1, enviar(get("/api/pedidos"), cliente, null, 200).size());
    enviar(get("/api/pedidos/" + id), admin, null, 200);
  }

  @Test
  void validaCantidadesModalidadYDestinosSinGuardarPedidosInvalidos() throws Exception {
    Map<String, Object> body = pedido(Modalidad.LOCAL);
    body.remove("mesa"); enviar(post("/api/pedidos"), cliente, body, 400);
    body = pedido(Modalidad.DELIVERY); body.put("direccion", "   ");
    enviar(post("/api/pedidos"), cliente, body, 400);
    body = pedido(Modalidad.RECOJO); body.remove("telefono");
    enviar(post("/api/pedidos"), cliente, body, 400);
    body = pedido(Modalidad.RECOJO);
    for (Object items : List.of(List.of(), List.of(Map.of("productoId", productoId, "cantidad", 0)),
        List.of(Map.of("productoId", productoId, "comboId", 1, "cantidad", 1)),
        List.of(Map.of("cantidad", 1)), List.of(Map.of("productoId", productoId, "cantidad", 51)))) {
      body.put("items", items); enviar(post("/api/pedidos"), cliente, body, 400);
    }
    body.put("items", List.of(Map.of("productoId", 999999, "cantidad", 1)));
    enviar(post("/api/pedidos"), cliente, body, 404);
    assertEquals(0, pedidos.count());
  }

  @Test
  void validaComposicionDeCombosYLimitesDeDescuento() throws Exception {
    Map<String, Object> body = new LinkedHashMap<>(comboBody(true));
    body.put("items", List.of(Map.of("productoId", productoId, "cantidad", 1),
        Map.of("productoId", productoId, "cantidad", 1)));
    enviar(post("/api/combos"), admin, body, 400);
    body.put("items", List.of()); enviar(post("/api/combos"), admin, body, 400);
    body.put("items", List.of(Map.of("productoId", 999999, "cantidad", 1)));
    enviar(post("/api/combos"), admin, body, 404);
    enviar(post("/api/promociones"), admin, promoBody(productoId, null, 101,
        Instant.now().minusSeconds(60), Instant.now().plusSeconds(60)), 400);
    assertEquals(0, combos.count());
    assertEquals(0, promociones.count());
  }

  @Test
  void cuentaDesactivadaNoPuedeOperarPedidosConSuTokenAnterior() throws Exception {
    long id = crearPedido();
    Cliente cuenta = clientes.findById(clienteId).orElseThrow();
    cuenta.setActivo(false); clientes.save(cuenta);
    enviar(get("/api/pedidos/" + id), cliente, null, 401);
    enviar(post("/api/pedidos"), cliente, pedido(Modalidad.RECOJO), 401);
  }

  @Test
  void cancelacionYPreparacionConcurrentesNoPuedenConfirmarseAmbas() throws Exception {
    long id = crearPedido();
    CountDownLatch inicio = new CountDownLatch(1);
    ExecutorService workers = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> cancelar = workers.submit(() -> {
        inicio.await();
        return mvc.perform(post("/api/pedidos/" + id + "/cancelar")
            .header("Authorization", "Bearer " + cliente)).andReturn().getResponse().getStatus();
      });
      Future<Integer> preparar = workers.submit(() -> {
        inicio.await();
        return mvc.perform(post("/api/pedidos/" + id + "/iniciar-preparacion")
            .header("Authorization", "Bearer " + cocina)).andReturn().getResponse().getStatus();
      });
      inicio.countDown();
      List<Integer> estados = new ArrayList<>(List.of(cancelar.get(15, TimeUnit.SECONDS), preparar.get(15, TimeUnit.SECONDS)));
      Collections.sort(estados);
      assertEquals(List.of(200, 409), estados);
    } finally { workers.shutdownNow(); }
  }
}
