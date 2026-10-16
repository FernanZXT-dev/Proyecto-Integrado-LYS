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
  void preparar() { clientes.deleteAll(); usuarios.deleteAll(); }

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
