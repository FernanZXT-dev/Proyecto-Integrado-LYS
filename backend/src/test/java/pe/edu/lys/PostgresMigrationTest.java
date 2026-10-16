package pe.edu.lys;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class PostgresMigrationTest {
  @Test
  void actualizaUnaBaseVersionUnoSinPerderSusProductos() throws Exception {
    try (PostgresTestDatabase database = new PostgresTestDatabase()) {
      Map<String, Object> settings = new HashMap<>();
      database.configure((name, supplier) -> settings.put(name, supplier.get()));
      String url = settings.get("spring.datasource.url").toString();
      String user = settings.get("spring.datasource.username").toString();
      String password = settings.get("spring.datasource.password").toString();
      Flyway.configure().dataSource(url, user, password).schemas(database.schema())
          .defaultSchema(database.schema()).target("1").load().migrate();
      try (var connection = DriverManager.getConnection(url, user, password)) {
        connection.setSchema(database.schema());
        try (var statement = connection.createStatement()) {
          statement.executeUpdate("insert into categorias (id, nombre) values (777, 'Categoria anterior')");
          statement.executeUpdate("insert into productos (id, nombre, precio, categoria_id) values (777, 'Producto anterior', 35.50, 777)");
        }
      }
      Flyway flyway = Flyway.configure().dataSource(url, user, password).schemas(database.schema())
          .defaultSchema(database.schema()).load();
      assertEquals(1, flyway.migrate().migrationsExecuted);
      assertEquals(0, flyway.migrate().migrationsExecuted);
      try (var connection = DriverManager.getConnection(url, user, password)) {
        connection.setSchema(database.schema());
        try (var statement = connection.createStatement(); var result = statement.executeQuery(
            "select nombre, precio from productos where id = 777")) {
          assertTrue(result.next());
          assertEquals("Producto anterior", result.getString("nombre"));
          assertEquals("35.50", result.getBigDecimal("precio").toPlainString());
        }
      }
      assertEquals("2", flyway.info().current().getVersion().toString());
    }
  }
}
