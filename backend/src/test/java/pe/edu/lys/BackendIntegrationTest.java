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
  @Autowired PasswordEncoder encoder;


  @BeforeEach
  void preparar() {
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
  void contrasenaMultibyteExcesivaSeRechaza() throws Exception {
    mvc.perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(Map.of("nombre", "Ana", "correo", "ana@lys.pe", "clave", "界".repeat(30)))))
        .andExpect(status().isBadRequest());
    assertFalse(clientes.existsByCorreo("ana@lys.pe"));
  }

}
