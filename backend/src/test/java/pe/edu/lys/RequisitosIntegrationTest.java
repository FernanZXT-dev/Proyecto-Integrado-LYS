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
  private String admin, cliente, otroCliente, cocina, mesera, ventas;
  private long clienteId, productoId, bebidaId;

  @BeforeEach
  void preparar() throws Exception {
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
















}
