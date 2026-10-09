# Fiabilidad y uso diario del chat

## Cambios

- Cada envío de texto, archivo o nota de voz lleva un UUID estable. Un reintento reutiliza ese UUID.
- El backend bloquea la fila del remitente durante la comprobación y el guardado, y la migración V16 añade un índice único por remitente e identificador. Dos reintentos concurrentes devuelven el mismo mensaje.
- Los envíos muestran «Enviando…», el mensaje confirmado muestra «Enviado» y los fallos permiten reintentar o descartar. La confirmación por websocket también retira el envío pendiente.
- Los envíos pendientes se guardan antes de limpiar el editor: un manifiesto por envío y un archivo binario para adjuntos y notas de voz. Sobreviven al cierre del proceso, se separan por cuenta y se recuperan con opción de reintentar o descartar. No se reenvían automáticamente.
- Al reconectar o volver a primer plano se recuperan mensajes mediante HTTP. Si se perdió más de una página, se recorren las páginas hasta alcanzar el historial conocido. Los eventos recibidos durante esa consulta se aplican después, incluidas ediciones y eliminaciones.
- Los cortes de carga conservan el historial existente y ofrecen un reintento. La conexión usa espera progresiva, señales STOMP cada diez segundos y detección de silencio cuando el servidor negocia esas señales.
- Los borradores de texto se guardan localmente por cuenta y conversación, también después de reiniciar la app. El envío aceptado en la cola limpia su borrador.
- La lista de mensajes directos muestra la vista previa, fecha/hora y contador. El servidor ordena por última actividad antes de paginar y el cliente conserva el resumen entre reinicios. Abrir, marcar como leído o borrar el último mensaje no devuelve el chat al orden de creación. Los fijados aparecen primero.
- El menú de cada conversación permite fijar, silenciar y archivar. Son preferencias personales guardadas en el servidor y sincronizadas entre dispositivos. Silenciar evita avisos del sistema, conserva los contadores. Los archivados se consultan desde «Archivados» y permanecen allí aunque llegue un mensaje.
- El perfil del contacto ofrece una galería con multimedia, documentos y enlaces. Recorre todo el historial en páginas acotadas, verifica pertenencia a la conversación y omite mensajes eliminados. La paginación usa fecha e identificador para no saltarse mensajes con la misma fecha. Las imágenes se decodifican como miniaturas.
- «Enviado» confirma el guardado; «Leído» indica que el destinatario ha abierto y cargado ese mensaje en un chat visible. La confirmación avanza hasta el identificador cargado, se sincroniza por websocket y se conserva al recargar el historial.
- Registro, búsqueda de amigos y guardado del perfil tienen validación o indicadores de carga y protección contra envíos duplicados.

## Despliegue y alcance

Desplegar primero el backend con V16 y V17 y después actualizar los clientes. V17 rellena la última actividad a partir del historial e incorpora preferencias por participante. No requiere Firebase ni cambios manuales en Supabase; Flyway aplica la migración al arrancar el backend. Estas funciones requieren ese backend actualizado.

Los pendientes viven en el directorio privado de la app en Android (excluido de copias de seguridad) y en `~/.chimera/chat-state` en escritorio, separados por un hash de la cuenta. Cerrar sesión detiene nuevos envíos; al volver a la misma cuenta se recuperan para reintento manual. Una selección de archivo todavía no enviada pertenece al editor y no forma parte de la cola persistente. Los avisos locales siguen dependiendo de que el proceso y su conexión estén activos; no se ha añadido push con la app cerrada.

## Verificación

Pruebas del cliente: recuperación de texto/archivo/voz desde disco, aislamiento entre cuentas, sustitución atómica de resúmenes, fallos de almacenamiento que conservan el editor, reintentos con UUID original, orden tras recarga desactualizada, fijados, mensajes leídos tras recarga y silencio con contador activo. También se mantienen las pruebas anteriores de reconexión y confirmación websocket.

Pruebas del backend: reintentos concurrentes, orden antes de paginar, estabilidad tras lectura y borrado, preferencias aisladas, acceso denegado a terceros, lecturas parciales que no retroceden y galería de enlaces cifrados con fechas idénticas. PostgreSQL verifica el esquema nuevo, la actualización del historial y el relleno de actividad sin pérdida de mensajes.
