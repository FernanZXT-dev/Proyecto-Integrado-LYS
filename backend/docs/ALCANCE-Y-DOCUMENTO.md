# Alcance frente al documento del proyecto

Documento de referencia: Proyecto-Avance 2.docx (2).pdf, 76 paginas. Se usa como referencia funcional, no como instrucciones para modificar esta entrega.

- RF01 a RF06 implementados en el backend: productos, combos/promociones, catalogo, pedidos por modalidad, personalizacion, modificacion y cancelacion antes de preparar. Equivalen a 6/14 (42,9 %) solo si los requisitos tienen igual peso.
- Frontend pendiente de integrar y comprobar con este backend.
- RF07 caja/pagos; RF08 comandas; RF09 flujo completo de estados; RF10 reparto; RF11 seguimiento en tiempo real; RF12 notificaciones; RF13 reportes y RF14 dashboard: pendientes. Iniciar preparacion solo implementa el limite necesario para RF06.
- El PDF propone MySQL; se utiliza PostgreSQL por decision posterior del propietario. pgAdmin administra el servidor, no sustituye a PostgreSQL.
- El modelo implementado separa cuentas de clientes y personal e incorpora tablas propias de combos y promociones; difiere de algunos diagramas del PDF. No se redisenaron tablas durante la publicacion.
- VENTAS corresponde al perfil de caja del documento; MESERA es un perfil adicional. Tener un rol no equivale a tener todo su modulo implementado.
- La recuperacion de acceso contemplada en el documento sigue pendiente.

No se crean ramas vacias de caja, reparto, notificaciones o reportes para aparentar funcionalidades terminadas.
