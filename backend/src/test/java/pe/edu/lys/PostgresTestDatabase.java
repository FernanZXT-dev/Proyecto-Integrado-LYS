package pe.edu.lys;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Cada ejecución de pruebas usa su propio esquema, nunca las tablas de la aplicación. */
final class PostgresTestDatabase implements AutoCloseable {
  private final String url = env("TEST_DB_URL", "jdbc:postgresql://localhost:5432/lys_test");
  private final String username = env("TEST_DB_USERNAME", "postgres");
  private final String password = env("TEST_DB_PASSWORD", "");
  private final String schema = "lys_test_" + UUID.randomUUID().toString().replace("-", "");
  private boolean created;

  void configure(DynamicPropertyRegistry properties) {
    if (!url.startsWith("jdbc:postgresql:")) {
      throw new IllegalArgumentException("TEST_DB_URL debe apuntar a PostgreSQL");
    }
    // No aceptar parámetros que cambien el esquema de búsqueda fuera del esquema aislado.
    if (url.contains("?")) {
      throw new IllegalArgumentException("TEST_DB_URL debe ser una URL sin parámetros JDBC");
    }
    try (Connection connection = connection(); var statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
      created = true;
    } catch (SQLException e) {
      throw new IllegalStateException(
          "No se pudo preparar PostgreSQL para pruebas. Crear lys_test y configurar "
              + "TEST_DB_URL, TEST_DB_USERNAME y TEST_DB_PASSWORD. "
              + "Las pruebas requieren permiso CREATE en esa base.", e);
    }
    properties.add("spring.datasource.url", () -> url);
    properties.add("spring.datasource.username", () -> username);
    properties.add("spring.datasource.password", () -> password);
    properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    properties.add("spring.datasource.hikari.schema", () -> schema);
    // Las suites mantienen contextos distintos; evitar agotar servidores de pruebas pequeños.
    properties.add("spring.datasource.hikari.maximum-pool-size", () -> "4");
    properties.add("spring.datasource.hikari.minimum-idle", () -> "1");
    properties.add("spring.flyway.default-schema", () -> schema);
    properties.add("spring.flyway.schemas", () -> schema);
    properties.add("spring.flyway.clean-disabled", () -> "true");
  }

  String schema() {
    return schema;
  }

  @Override
  public void close() throws SQLException {
    if (!created) return;
    try (Connection connection = connection(); var statement = connection.createStatement()) {
      // schema solo contiene el prefijo fijo y un UUID generado aquí.
      statement.execute("DROP SCHEMA " + schema + " CASCADE");
      created = false;
    }
  }

  private Connection connection() throws SQLException {
    return DriverManager.getConnection(url, username, password);
  }

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }
}
