# Proyecto Integrado LYS

Sistema para la polleria Lenas y Sabores. El backend esta organizado en [`backend/`](backend/README.md), con Java 17, Spring Boot y PostgreSQL.

## Ejecutar

1. Entrar en `backend/`.
2. Crear `lys` en PostgreSQL con `database/crear-lys.sql` si todavia no existe.
3. Configurar las variables privadas indicadas en `.env.example` en la terminal o IDE.
4. Ejecutar `.\mvnw.cmd spring-boot:run` (PowerShell) o `./mvnw spring-boot:run` (Linux/macOS).

Consultar la [guia de configuracion](backend/README.md) para crear el administrador, generar JWT_SECRET y ejecutar las pruebas sobre `lys_test`.

## Ramas

`dev` integra los avances. Las ramas `feature/*` nacen desde `dev`, incorporan commits pequenos por funcionalidad y se integran en orden de dependencias. `main` se conserva para una revision posterior.

La importacion inicial organiza el codigo existente en commits incrementales; no reconstruye fechas ni una historia de desarrollo anterior. La carpeta original de Descargas no se modifica.

## Alcance actual

RF01 a RF06 implementados en el backend; frontend pendiente de integrar. Consultar [alcance y diferencias con el documento](backend/docs/ALCANCE-Y-DOCUMENTO.md). Caja, cocina completa, reparto, notificaciones y reportes quedan pendientes.

El frontend existente se conserva en [Front-Polleria-LYS-main/](Front-Polleria-LYS-main/README.md), incorporado previamente en dev. Su conexion y validacion con este backend siguen pendientes.
