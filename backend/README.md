# Integrador — Pollería LYS

Backend en **Java 17, Spring Boot y PostgreSQL**. Este avance cubre **RF01 a RF06: 6 de 14 requisitos (42,9 % por cantidad)**: productos, combos, promociones, catálogo, pedidos por modalidad, personalización y modificación/cancelación antes de preparar. Incluye registro, login, JWT y permisos. PostgreSQL es la única base de datos del proyecto, también en las pruebas. pgAdmin se utiliza para administrarla.

Ver **[guía del avance y contratos de API](docs/AVANCE-RF01-RF06.md)** y **[demostración para Postman](docs/RF01-RF06.postman_collection.json)**. El frontend todavía debe conectarse a estas rutas; este porcentaje corresponde a funciones del backend.

## Ejecución

Requisitos: JDK 17 o compatible, Maven 3.6.3 o superior y un servidor PostgreSQL (validado con PostgreSQL 17). pgAdmin permite crear la base y consultar las tablas; la aplicación se conecta directamente al servidor PostgreSQL, no a pgAdmin. También se puede abrir `pom.xml` en NetBeans.

### 1. Crear la base desde pgAdmin

1. Conectar con el servidor local usando su usuario y contraseña de PostgreSQL.
2. Abrir **Tools > Query Tool** sobre la base de mantenimiento `postgres`.
3. Con **Auto-commit activado**, abrir y ejecutar `database/crear-lys.sql`. Esto crea la base vacía `lys`. Si ya estás usando la versión anterior con Flyway V1, omitir este paso: conservar la base y sus datos. Al ejecutar la nueva versión se aplicará automáticamente V2.
4. Actualizar **Databases** en el árbol de pgAdmin.

Las tablas se crean al iniciar el backend. En pgAdmin estarán en **Databases > lys > Schemas > public > Tables**: las tablas anteriores más `combos`, `combo_items`, `promociones`, `producto_personalizaciones`, `pedidos`, `pedido_items` y `pedido_item_personalizaciones`.

No ejecutar manualmente `V1__estructura_inicial.sql`: Flyway lo ejecuta y registra su versión automáticamente. Las siguientes modificaciones de tablas deben agregarse como nuevas migraciones `V3__...sql`, `V4__...sql`, etc., sin cambiar una migración ya aplicada. Hibernate valida la estructura al iniciar y no la cambia mediante `ddl-auto=update`.

### 2. Configurar la conexión y ejecutar

Desde PowerShell, situado en la carpeta que contiene `pom.xml`:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/lys"
$env:DB_USERNAME="postgres"
$dbClave = Read-Host "Contraseña de PostgreSQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $dbClave).Password

# Generar una sola vez y conservar el secreto en la configuración privada del IDE
# o en las variables del entorno para las siguientes ejecuciones.
$jwtBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)

$env:ADMIN_CORREO="admin@lys.pe"
$adminClave = Read-Host "Contraseña del primer administrador (8 a 64 caracteres)" -AsSecureString
$env:ADMIN_CLAVE = [System.Net.NetworkCredential]::new("", $adminClave).Password
.\mvnw.cmd spring-boot:run
```

Si usas otro usuario, puerto o nombre de base, ajustar `DB_USERNAME` y `DB_URL`. La contraseña solicitada es la del usuario de PostgreSQL, que puede ser distinta de la contraseña maestra de pgAdmin. No escribir contraseñas en archivos que se publiquen en GitHub.

En NetBeans, configurar las variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y, para crear el primer administrador, `ADMIN_CORREO` y `ADMIN_CLAVE` en el entorno de ejecución del proyecto. En otros sistemas se utilizan las mismas variables con la sintaxis de su terminal.

Servidor: `http://localhost:8080`. El perfil predeterminado es `postgres`; no hace falta definir `SPRING_PROFILES_ACTIVE`. Si el IDE conserva un perfil `local` de una configuración anterior, quitarlo o cambiarlo a `postgres`.

La cuenta inicial solo se crea si el correo no existe; cambiar las variables después no cambia su contraseña. Sin las variables de administrador, se crean los roles pero ninguna cuenta del personal. `JWT_SECRET` debe contener al menos 32 bytes y no tiene una clave predeterminada.

`.env.example` documenta las variables disponibles. El archivo `.env` **no se carga automáticamente**: configurar sus valores en la terminal o en el IDE.

### Datos de versiones anteriores

El cambio de configuración no copia datos de una base anterior. Si tienes datos en archivos de la versión anterior o una base PostgreSQL que ya contenga tablas, conservar una copia y preparar su importación antes de usarla con estas migraciones. No borrar las tablas ni activar `baseline-on-migrate` sin revisar su estructura. Para empezar sin datos anteriores, utilizar una base nueva y vacía.

### Problemas de conexión

- **Connection refused**: comprobar que el servicio PostgreSQL esté iniciado y coincidan el host y el puerto.
- **Password authentication failed**: revisar `DB_USERNAME` y `DB_PASSWORD`; no usar la contraseña maestra de pgAdmin en su lugar.
- **Database lys does not exist**: ejecutar el script de creación sobre `postgres` y actualizar el árbol de pgAdmin.
- **Could not resolve placeholder DB_PASSWORD / JWT_SECRET**: configurar esa variable en el entorno del proceso que inicia el backend.
- **Found non-empty schema without schema history**: la base ya tiene tablas anteriores; no se adopta automáticamente. Usar una base vacía o preparar una migración de esos datos.

## Organización y explicación

| Carpeta | Responsabilidad |
|---|---|
| `entity` | Entidades del catálogo, combos, promociones, pedidos y sus detalles. |
| `repository` | Consultas y persistencia con Spring Data JPA. |
| `service` | Reglas de negocio, registro, comprobación de credenciales y emisión del JWT. |
| `controller` | Rutas HTTP y validación de las solicitudes. |
| `dto` | Datos de entrada y salida; nunca se devuelve una contraseña ni su hash. |
| `config` | Spring Security, BCrypt, JWT, rutas y carga inicial de roles/administrador. |
| `exception` | Respuestas para validación, conflictos y recursos inexistentes. |

El controlador recibe los datos, el servicio ejecuta la lógica y el repositorio accede a la BD. Las transacciones se delimitan en el servicio. Lombok genera getters y setters de las entidades; los DTO usan records de Java.

Los clientes se registran públicamente y siempre reciben el rol `CLIENTE` en su token. Las cuentas del personal están en otra tabla y solo un administrador puede crearlas. Los roles iniciales son `ADMINISTRADOR`, `COCINA`, `VENTAS`, `REPARTO` y `MESERA`. El administrador gestiona el catálogo; cliente, mesera, ventas y administrador registran pedidos. Cocina y administrador pueden iniciar su preparación para aplicar el bloqueo de RF06. El flujo completo de cocina, caja, despacho y reparto queda pendiente.

## Rutas

| Método | Ruta | Acceso / finalidad |
|---|---|---|
| POST | `/api/auth/registro` | Público: registrar cliente. |
| POST | `/api/auth/login/cliente` | Público: login del cliente. |
| POST | `/api/auth/login/personal` | Público: login del personal. |
| GET | `/api/roles` | Administrador: consultar roles y sus IDs. |
| GET, POST | `/api/usuarios` | Administrador: listar y crear personal. |
| GET | `/api/categorias`, `/api/categorias/{id}` | Público: categorías activas. |
| POST | `/api/categorias` | Administrador: crear categoría. |
| PUT, DELETE | `/api/categorias/{id}` | Administrador: actualizar o desactivar. |
| GET | `/api/productos`, `/api/productos/{id}` | Público: productos activos en categorías activas. |
| POST | `/api/productos` | Administrador: crear producto. |
| PUT, DELETE | `/api/productos/{id}` | Administrador: actualizar o desactivar. |
| GET | `/api/admin/categorias`, `/api/admin/productos` | Administrador: listar incluyendo inactivos. |
| GET | `/api/catalogo` | Público: productos, combos y promociones disponibles. |
| GET | `/api/combos`, `/api/combos/{id}` | Público: combos disponibles. |
| GET | `/api/promociones`, `/api/promociones/{id}` | Público: promociones vigentes y disponibles. |
| POST | `/api/combos`, `/api/promociones` | Administrador: crear. |
| PUT, DELETE | `/api/combos/{id}`, `/api/promociones/{id}` | Administrador: editar o desactivar; PUT también reactiva. |
| GET | `/api/admin/combos`, `/api/admin/promociones` | Administrador: incluir inactivos, no vigentes o no disponibles. |
| GET, POST | `/api/pedidos` | Cliente: listar propios/crear; administrador, ventas y mesera: listar/crear. |
| GET, PUT | `/api/pedidos/{id}` | Consultar o modificar; cliente solo accede a sus pedidos. |
| POST | `/api/pedidos/{id}/cancelar` | Cancelar conservando el registro, únicamente antes de preparar. |
| POST | `/api/pedidos/{id}/iniciar-preparacion` | Cocina/administrador: iniciar y bloquear modificaciones y cancelación. |

Las operaciones privadas requieren `Authorization: Bearer <token>`. El JWT vence en una hora; no se implementan renovación ni revocación anticipada en esta entrega. Un login nuevo comprueba que la cuenta esté activa. Las contraseñas se almacenan con BCrypt.

`DELETE` desactiva el registro, conserva sus datos y responde 204. Para reactivarlo se usa `PUT` con `activo: true`. Desactivar una categoría oculta sus productos del catálogo público. Los nombres de categoría no se pueden duplicar sin distinguir mayúsculas en las operaciones del servicio.

## Ejemplos de solicitudes

Registro de cliente (`POST /api/auth/registro`):

```json
{"nombre":"Ana","correo":"ana@example.com","clave":"ClaveCliente123","telefono":"999888777"}
```

Login (`POST /api/auth/login/cliente` o `/api/auth/login/personal`):

```json
{"correo":"ana@example.com","clave":"ClaveCliente123"}
```

Categoría (`POST /api/categorias` o `PUT /api/categorias/{id}`):

```json
{"nombre":"Pollos","activo":true}
```

Producto (`POST /api/productos` o `PUT /api/productos/{id}`):

```json
{"nombre":"Pollo entero","descripcion":"Con papas y ensalada","precio":65.00,"categoriaId":1,"activo":true}
```

Personal (`POST /api/usuarios`; obtener `rolId` consultando `/api/roles`):

```json
{"nombre":"Luis","correo":"cocina@example.com","clave":"ClavePersonal123","rolId":2}
```

Los IDs de los ejemplos son ilustrativos; usar los devueltos por el servidor. Todas las solicitudes con cuerpo llevan `Content-Type: application/json`. Errores: 400 para datos inválidos, 401 para credenciales/token inválidos o faltantes, 403 para permisos insuficientes, 404 para recursos inexistentes y 409 para conflictos.

## Verificación y exposición

Las pruebas requieren una segunda base PostgreSQL. En pgAdmin, ejecutar `database/crear-lys-test.sql` sobre `postgres` con Auto-commit activado. A continuación, en PowerShell:

```powershell
$env:TEST_DB_URL="jdbc:postgresql://localhost:5432/lys_test"
$env:TEST_DB_USERNAME="postgres"
$testClave = Read-Host "Contraseña de PostgreSQL para pruebas" -AsSecureString
$env:TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new("", $testClave).Password
.\mvnw.cmd clean verify
```

Las pruebas ejecutan peticiones HTTP con MockMvc contra **PostgreSQL real**. Cada ejecución crea un esquema `lys_test_<uuid>`, aplica las mismas migraciones que la aplicación y elimina únicamente ese esquema al terminar. El usuario de pruebas necesita permiso `CREATE` en `lys_test`. No se utiliza `DB_URL` ni se vacían las tablas `public` de la aplicación. Si el proceso se interrumpe abruptamente, puede quedar un esquema temporal en `lys_test` para eliminar manualmente después de comprobar que ya no hay pruebas activas.

Se verifican PostgreSQL/Flyway, actualización V1 → V2 conservando productos, registro, BCrypt, login, permisos, catálogo, combos/promociones, las tres modalidades, importes y descuentos del servidor, personalización, aislamiento de pedidos por cliente y bloqueo de cambios al preparar. Una prueba concurrente verifica que cancelar e iniciar preparación no puedan confirmarse ambas. No requieren Postman ni Docker. El paquete ejecutable queda en `target/integrador-0.0.1-SNAPSHOT.jar`.

Para exponer el código, seguir: `AuthController → AuthService → ClienteRepository`, y luego `SecurityConfig`. Para el catálogo: `ProductoController → ProductoService → ProductoRepository`. Las pruebas muestran llamadas completas al backend y los resultados esperados.

Pendientes: RF07 a RF14 (ventas/pagos, comandas de cocina completas, estados posteriores, reparto, consulta en tiempo real, notificaciones, reportes y dashboard). La consulta HTTP de pedidos y el inicio de preparación son bases para esos módulos y no se cuentan como requisitos completos adicionales.

## Maven Wrapper

Desde `backend/`, usar `.\mvnw.cmd` en PowerShell o `./mvnw` en Linux/macOS. El Wrapper descarga Maven en su primera ejecucion; no requiere instalar Maven por separado.
