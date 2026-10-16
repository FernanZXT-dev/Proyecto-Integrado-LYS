package pe.edu.lys;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.edu.lys.entity.*;
import pe.edu.lys.repository.*;

@SpringBootTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=validate",
      "app.jwt.secret=pruebas-clave-aislada-de-al-menos-32-bytes",
      "app.admin.correo=",
      "app.admin.clave="
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendIntegrationTest {
  private static final PostgresTestDatabase DATABASE = new PostgresTestDatabase();

  @DynamicPropertySource
  static void configurarPostgres(DynamicPropertyRegistry properties) {
    DATABASE.configure(properties);
  }

  @AfterAll
  static void eliminarEsquemaTemporal() throws Exception {
    DATABASE.close();
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired ClienteRepository clientes;
  @Autowired UsuarioRepository usuarios;
  @Autowired RolRepository roles;
  @Autowired ProductoRepository productos;
  @Autowired CategoriaRepository categorias;
  @Autowired PasswordEncoder encoder;

  @Test
  void usaPostgresConMigracionesEnUnEsquemaAislado() {
    assertTrue(jdbc.queryForObject("select version()", String.class).startsWith("PostgreSQL"));
    assertEquals(DATABASE.schema(), jdbc.queryForObject("select current_schema()", String.class));
    assertEquals(1, jdbc.queryForObject(
        "select count(*) from flyway_schema_history where success = true and version = '1'",
        Integer.class));
  }

  @BeforeEach
  void preparar() {
    productos.deleteAll();
    categorias.deleteAll();
    clientes.deleteAll();
    usuarios.deleteAll();
    Usuario admin = new Usuario();
    admin.setNombre("Admin");
    admin.setCorreo("admin@lys.pe");
    admin.setClave(encoder.encode("ClaveSegura123"));
    admin.setRol(roles.findByNombre("ADMINISTRADOR").orElseThrow());
    usuarios.save(admin);
  }

  String body(Object value) throws Exception {
    return json.writeValueAsString(value);
  }

  JsonNode contenido(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString());
  }

  String adminToken() throws Exception {
    return contenido(
            mvc.perform(
                    post("/api/auth/login/personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("correo", "admin@lys.pe", "clave", "ClaveSegura123"))))
                .andExpect(status().isOk())
                .andReturn())
        .get("token")
        .asText();
  }

  void registrar() throws Exception {
    mvc.perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "nombre",
                            "Ana",
                            "correo",
                            "ana@lys.pe",
                            "clave",
                            "ClaveCliente123",
                            "rol",
                            "ADMINISTRADOR"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("rol").value("CLIENTE"))
        .andExpect(jsonPath("clave").doesNotExist());
  }

  long categoria(String token) throws Exception {
    return contenido(
            mvc.perform(
                    post("/api/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("nombre", "Pollos", "activo", true))))
                .andExpect(status().isCreated())
                .andReturn())
        .get("id")
        .asLong();
  }

  @Test
  void registroCifraClaveYEvitaDuplicados() throws Exception {
    registrar();
    var c = clientes.findByCorreo("ana@lys.pe").orElseThrow();
    assertNotEquals("ClaveCliente123", c.getClave());
    assertTrue(encoder.matches("ClaveCliente123", c.getClave()));
    mvc.perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "nombre", "Otra", "correo", "ANA@lys.pe", "clave", "ClaveCliente123"))))
        .andExpect(status().isConflict());
  }

  @Test
  void clienteNoPuedeGestionarCatalogoNiUsuarios() throws Exception {
    registrar();
    String token =
        contenido(
                mvc.perform(
                        post("/api/auth/login/cliente")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                body(Map.of("correo", "ana@lys.pe", "clave", "ClaveCliente123"))))
                    .andExpect(status().isOk())
                    .andReturn())
            .get("token")
            .asText();
    mvc.perform(
            post("/api/categorias")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre", "Pollos", "activo", true))))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/productos").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void credencialesInvalidasYCuentaInactivaNoIngresan() throws Exception {
    mvc.perform(
            post("/api/auth/login/personal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("correo", "admin@lys.pe", "clave", "incorrecta"))))
        .andExpect(status().isUnauthorized());
    var u = usuarios.findByCorreo("admin@lys.pe").orElseThrow();
    u.setActivo(false);
    usuarios.save(u);
    mvc.perform(
            post("/api/auth/login/personal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("correo", "admin@lys.pe", "clave", "ClaveSegura123"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void catalogoPublicoYOperacionesProtegidas() throws Exception {
    mvc.perform(get("/api/productos")).andExpect(status().isOk());
    mvc.perform(post("/api/productos")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/usuarios").header("Authorization", "Bearer falso.token.falso"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void administradorCreaPersonalConRolYNoExponeClave() throws Exception {
    String token = adminToken();
    long rol = roles.findByNombre("COCINA").orElseThrow().getId();
    mvc.perform(
            post("/api/usuarios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "nombre",
                            "Cocinero",
                            "correo",
                            "cocina@lys.pe",
                            "clave",
                            "ClaveCocina123",
                            "rolId",
                            rol))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("rol").value("COCINA"))
        .andExpect(jsonPath("clave").doesNotExist());
    mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].clave").doesNotExist());
    String cocinaToken =
        contenido(
                mvc.perform(
                        post("/api/auth/login/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                body(Map.of("correo", "cocina@lys.pe", "clave", "ClaveCocina123"))))
                    .andExpect(status().isOk())
                    .andReturn())
            .get("token")
            .asText();
    mvc.perform(get("/api/roles").header("Authorization", "Bearer " + cocinaToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void productoSeActualizaYDesactivaSinEliminarse() throws Exception {
    String token = adminToken();
    long categoriaId = categoria(token);
    long id =
        contenido(
                mvc.perform(
                        post("/api/productos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                body(
                                    Map.of(
                                        "nombre",
                                        "Pollo entero",
                                        "precio",
                                        60,
                                        "categoriaId",
                                        categoriaId,
                                        "activo",
                                        true))))
                    .andExpect(status().isCreated())
                    .andReturn())
            .get("id")
            .asLong();
    mvc.perform(
            put("/api/productos/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "nombre",
                            "Pollo entero",
                            "precio",
                            65,
                            "categoriaId",
                            categoriaId,
                            "activo",
                            true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("precio").value(65));
    mvc.perform(get("/api/productos/" + id)).andExpect(status().isOk());
    mvc.perform(delete("/api/productos/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/productos/" + id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/productos")).andExpect(jsonPath("$.length()").value(0));
    assertTrue(productos.existsById(id));
    mvc.perform(get("/api/admin/productos").header("Authorization", "Bearer " + token))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void categoriaInactivaOcultaProductosYRechazaNuevos() throws Exception {
    String token = adminToken();
    long id = categoria(token);
    var request = body(Map.of("nombre", "Pollo", "precio", 30, "categoriaId", id, "activo", true));
    mvc.perform(
            post("/api/productos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isCreated());
    mvc.perform(delete("/api/categorias/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/productos")).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/categorias/" + id)).andExpect(status().isNotFound());
    mvc.perform(
            post("/api/productos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/categorias/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre", "Pollos", "activo", true))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/productos")).andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void contrasenaMultibyteExcesivaSeRechaza() throws Exception {
    mvc.perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(Map.of("nombre", "Ana", "correo", "ana@lys.pe", "clave", "界".repeat(30)))))
        .andExpect(status().isBadRequest());
    assertFalse(clientes.existsByCorreo("ana@lys.pe"));
  }

  @Test
  void validacionesYNoEncontrados() throws Exception {
    String token = adminToken();
    mvc.perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre", "", "correo", "invalido", "clave", "123"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("campos.correo").exists());
    mvc.perform(
            post("/api/productos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "nombre", "Pollo", "precio", -5, "categoriaId", 999, "activo", true))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("campos.precio").exists());
    mvc.perform(get("/api/productos/999")).andExpect(status().isNotFound());
    mvc.perform(get("/api/productos/abc")).andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/categorias")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest());
    categoria(token);
    mvc.perform(
            post("/api/categorias")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre", "POLLOS", "activo", true))))
        .andExpect(status().isConflict());
  }
}
