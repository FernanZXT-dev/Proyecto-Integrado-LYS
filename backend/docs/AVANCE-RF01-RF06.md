# Avance de backend: RF01 a RF06

Se cubren seis de los catorce requisitos: **42,9 % por cantidad de requisitos**. Este cálculo supone el mismo peso para cada requisito; no representa una ponderación académica ni el porcentaje de integración del sistema completo.

| Requisito | Implementación del backend |
|---|---|
| RF01 | Administrador crea, edita, activa y desactiva productos. |
| RF02 | Administrador gestiona combos, su composición y promociones con fechas y descuentos. |
| RF03 | Catálogo público de productos activos, combos disponibles y promociones vigentes. |
| RF04 | Pedidos LOCAL, RECOJO y DELIVERY con datos de contacto y validación de cada modalidad. |
| RF05 | Opciones por producto, selección de personalizaciones e indicaciones por línea de pedido. |
| RF06 | Modificar o cancelar solo en RECIBIDO; iniciar preparación bloquea ambas acciones. |

RF07 a RF14 quedan pendientes. No se presenta la consulta HTTP de pedidos como seguimiento en tiempo real, ni el inicio de preparación como un módulo completo de cocina. El frontend no se modifica en esta entrega.

## Actualizar la versión PostgreSQL que ya tienes

1. Detener el backend anterior.
2. Abrir el nuevo proyecto desde `pom.xml`. Conservar los valores privados de conexión y `JWT_SECRET` de tu entorno/IDE. Si escribiste las credenciales en tu archivo de propiedades, conservarlas localmente al reemplazar el código; el proyecto distribuido usa variables de entorno.
3. Usar la misma base `lys`. **No borrarla ni volver a crearla.**
4. Ejecutar `IntegradorApplication`. Flyway aplicará `V2__combos_promociones_y_pedidos.sql` después de V1. V1 se conserva sin cambios.
5. En pgAdmin, actualizar Tables y comprobar:

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Deben figurar las versiones 1 y 2 con `success = true`. La actualización agrega tablas; conserva los productos, categorías y usuarios anteriores. No ejecutar los archivos de migración manualmente en Query Tool.

Si se ejecutó una versión antigua sin `flyway_schema_history`, primero revisar esa base; esta guía de actualización supone la entrega anterior con Flyway V1.

## Acceso y demostración

Configurar `ADMIN_CORREO` y `ADMIN_CLAVE` en el entorno antes de iniciar para crear el primer administrador. La contraseña necesita de 8 a 64 caracteres y no debe superar 72 bytes UTF-8. Si esa cuenta ya existe, las variables no cambian su clave. Sin configurar estas variables no se crea una cuenta de administrador.

Importar `RF01-RF06.postman_collection.json` en Postman y configurar las variables de la colección `adminCorreo`, `adminClave` y `clienteClave`. Ejecutar en orden. La colección guarda automáticamente tokens e IDs en sus variables para las siguientes solicitudes. Crea registros de demostración en la base a la que apunte `baseUrl`.

No publicar una exportación que contenga contraseñas o tokens guardados en Postman.

## Convenciones de API

- Base: `http://localhost:8080/api`. Solicitudes con cuerpo: `Content-Type: application/json`.
- Autorización: `Authorization: Bearer <token>` para las rutas privadas.
- `POST` de creación devuelve 201; consultas y actualizaciones, 200; desactivar catálogo, 204.
- 400: solicitud inválida; 401: sesión ausente/inválida o cuenta inactiva en pedidos; 403: rol no autorizado; 404: recurso no encontrado o pedido ajeno; 409: estado no editable o artículo no disponible.
- Los IDs son los devueltos por la API, nunca valores que deban adivinarse.

## Productos y personalización — RF01 y RF05

`POST /productos` y `PUT /productos/{id}` (ADMINISTRADOR):

```json
{
  "nombre": "Pollo entero",
  "descripcion": "Pollo con papas y ensalada",
  "precio": 60.00,
  "categoriaId": 1,
  "activo": true,
  "opcionesPersonalizacion": ["Sin ensalada", "Sin papas", "Salsa aparte"]
}
```

Las opciones son opcionales y no tienen recargo. Se validan por producto y se rechazan opciones repetidas o inventadas al pedir. `indicaciones` permite además texto libre de hasta 300 caracteres por línea. Si se edita un producto omitiendo `opcionesPersonalizacion`, su lista queda vacía. Esto no cambia los pedidos registrados anteriormente.

`DELETE /productos/{id}` desactiva; `PUT` con `activo: true` reactiva si su categoría está activa. `GET /admin/productos` incluye inactivos.

## Combos y promociones — RF02

`POST /combos` y `PUT /combos/{id}` (ADMINISTRADOR):

```json
{
  "nombre": "Combo familiar",
  "descripcion": "Pollo con bebida",
  "precio": 65.00,
  "activo": true,
  "items": [
    {"productoId": 1, "cantidad": 1},
    {"productoId": 2, "cantidad": 1}
  ]
}
```

Un combo necesita al menos un componente. Cada producto aparece una sola vez, con cantidad de 1 a 50. Se pueden editar su precio y composición. Un combo solo está disponible si está activo y todos sus productos y categorías están activos.

`POST /promociones` y `PUT /promociones/{id}` (ADMINISTRADOR):

```json
{
  "nombre": "Descuento de ejemplo",
  "descripcion": "20 por ciento en pollo entero",
  "productoId": 1,
  "porcentajeDescuento": 20.00,
  "inicio": "2026-10-15T00:00:00-05:00",
  "fin": "2026-12-31T23:59:59-05:00",
  "activo": true
}
```

Las fechas son ilustrativas: usar una vigencia adecuada al día de la demostración. Para un combo, usar `comboId` en lugar de `productoId`. Debe enviarse **uno de los dos**, no ambos. El porcentaje debe ser mayor que 0 y menor o igual que 100; fin debe ser posterior a inicio. Inicio es inclusivo y fin exclusivo.

`DELETE /combos/{id}` y `DELETE /promociones/{id}` desactivan sin eliminar. Se reactivan con `PUT`. Las listas `/admin/combos` y `/admin/promociones` incluyen registros que no se muestran públicamente.

## Catálogo público — RF03

- `GET /catalogo`: un objeto con listas `productos`, `combos` y `promociones`.
- `GET /productos`, `GET /combos`, `GET /promociones` y sus consultas por ID.
- Las promociones se muestran solo durante su vigencia, si están activas y su producto o combo está disponible.
- Las promociones incluyen `precioPromocional`; cada componente de un combo incluye sus opciones de personalización.

## Pedidos — RF04 y RF05

`POST /pedidos` (CLIENTE, ADMINISTRADOR, VENTAS o MESERA):

```json
{
  "modalidad": "DELIVERY",
  "nombreContacto": "Ana",
  "telefono": "999888777",
  "direccion": "Av. Principal 123",
  "referencia": "Puerta azul",
  "items": [
    {
      "productoId": 1,
      "cantidad": 2,
      "personalizaciones": ["Sin ensalada"],
      "indicaciones": "Salsa en un envase aparte"
    },
    {
      "comboId": 1,
      "cantidad": 1,
      "personalizaciones": ["1:Sin papas"],
      "indicaciones": "Separar en dos bolsas"
    }
  ]
}
```

La personalización de un combo usa `productoId:opción`, para indicar a qué componente corresponde. Solo se aceptan productos que pertenezcan al combo y opciones configuradas en ellos. Una selección aplica a todas las unidades de esa línea; para unidades distintas, enviar líneas separadas.

| Modalidad | Datos adicionales obligatorios |
|---|---|
| LOCAL | `mesa`; teléfono opcional. |
| RECOJO | `telefono`; sin dirección ni mesa. |
| DELIVERY | `telefono` y `direccion`; referencia opcional. |

El teléfono, cuando se proporciona, debe tener de 7 a 15 dígitos, sin espacios. La respuesta descarta datos de mesa/dirección que no correspondan a la modalidad.

Los pedidos de cliente se vinculan al usuario del token. El cliente no elige otro propietario ni el estado, precios o total. El personal puede registrar pedidos sin cuenta de cliente y se conserva qué usuario los registró. La lista `/pedidos` y la consulta `/pedidos/{id}` muestran al cliente únicamente sus pedidos; los roles de administración, ventas y mesera pueden atender todos.

### Precios y descuentos

El servidor calcula precios, subtotal y total a partir del catálogo. Si hay varias promociones vigentes para una misma línea, aplica únicamente el mayor porcentaje. En empate selecciona la de mayor ID. Las promociones del producto no se acumulan con las del combo: el combo usa su propio precio y promoción.

El precio unitario con descuento se redondea a dos decimales antes de multiplicar por cantidad. Delivery agrega 6.00 por defecto (`DELIVERY_COST` configurable); local y recojo no agregan ese cargo. La respuesta incluye `precioBase`, `porcentajeDescuento`, `precioUnitario`, subtotal, costo de delivery y total.

Los nombres, composición del combo, precios y personalizaciones se copian al detalle del pedido. Editar posteriormente el catálogo no altera su historial. Una modificación de un pedido recibido recalcula sus precios con el catálogo y las promociones actuales.

## Modificación y cancelación — RF06

- `PUT /pedidos/{id}`: reemplazar los datos y líneas con el mismo formato de creación. Mantiene propietario, ID y fecha de creación.
- `POST /pedidos/{id}/cancelar`: cambia a CANCELADO y conserva datos e importes; no elimina el pedido.
- `POST /pedidos/{id}/iniciar-preparacion`: solo COCINA o ADMINISTRADOR; cambia RECIBIDO a EN_PREPARACION.
- Una vez EN_PREPARACION o CANCELADO, modificar, cancelar o volver a iniciar devuelve 409.
- Las operaciones bloquean la fila del pedido durante la transacción. Si cancelar e iniciar preparación llegan al mismo tiempo, solo una puede completarse.

## Conectar posteriormente el frontend

El frontend actual todavía usa datos locales. Hay que sustituir sus servicios por llamadas a estas rutas y adaptar nombres de campos y roles. Equivalencias de modalidad:

| Frontend actual | Backend |
|---|---|
| `salon` | `LOCAL` |
| `pickup` | `RECOJO` |
| `delivery` | `DELIVERY` |

El rol de cajero se representa como `VENTAS`; `MESERA` se agregó para el flujo existente del frontend. Los clientes reciben `CLIENTE`. No mezclar los tokens de personal con los de cliente.
