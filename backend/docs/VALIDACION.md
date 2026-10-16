# Validación del avance RF01–RF06

Validado el 15 de octubre de 2026 con PostgreSQL 17.6 y Java 24, compilando para Java 17.

## Resultado

- `mvn clean verify`: **31 pruebas, 0 fallos, 0 errores, 0 omitidas**.
- `BackendIntegrationTest`: 10 pruebas de autenticación, permisos, catálogo y PostgreSQL.
- `RequisitosIntegrationTest`: 20 casos de productos, combos/promociones, disponibilidad, modalidades, personalización, precios, privacidad y restricciones por estado.
- `PostgresMigrationTest`: 1 prueba de actualización V1 → V2 conservando un producto anterior y comprobando que repetir las migraciones no vuelve a ejecutarlas.
- Colección de demostración: **34 solicitudes HTTP y 39 comprobaciones correctas** contra el backend ejecutable.
- Comprobación adicional HTTP: login, catálogo, creación de combos/promociones, pedido personalizado con descuento y delivery, modificación, cancelación y conservación del pedido tras reiniciar el backend.

Las pruebas usan una instancia temporal de PostgreSQL separada del servidor del usuario. Cada suite crea su propio esquema de pruebas. La instancia temporal se detuvo al terminar.

La prueba concurrente de RF06 confirma que iniciar preparación y cancelar el mismo pedido no pueden tener éxito ambas: una operación devuelve 200 y la otra 409.

La migración V1 se conserva exactamente igual a la entrega anterior. V2 agrega las tablas y no elimina ni reemplaza productos, categorías o usuarios existentes.

## Alcance

Se cuentan RF01–RF06 como funciones implementadas y verificadas del backend: 6/14 = 42,9 %, suponiendo igual peso. No se cuenta todavía la integración del frontend ni RF07–RF14. La colección incluye datos de demostración y debe ejecutarse únicamente contra una base en la que se desee crear esos registros.

## Comprobacion durante la organizacion para GitHub

El 16 de octubre de 2026 se compilo desde cero cada paso de codigo y se ejecutaron las pruebas disponibles en ese punto sobre PostgreSQL temporal. El ultimo paso de codigo completo obtuvo 31 pruebas correctas, sin fallos, errores ni omisiones. Los tres commits posteriores de documentacion conservan esas mismas fuentes y pruebas.

El historial del backend se organiza en 37 commits feat dentro de siete ramas feature nacidas desde dev. Se conserva el frontend incorporado previamente en dev; su integracion con la API sigue pendiente. La publicacion remota se comprueba por separado.
