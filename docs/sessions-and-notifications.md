# Sesiones y notificaciones

## Sesiones

El token de acceso dura 15 minutos y se renueva automáticamente. El backend
mantiene las sesiones recordadas durante 90 días desde su último uso; cada
renovación prolonga ese plazo. REMEMBERED_SESSION_DAYS permite configurarlo.
Las sesiones sin «Recordarme» conservan su plazo fijo de un día.
El registro crea una sesión recordada. Cerrar sesión y cambiar la contraseña
siguen revocando las credenciales correspondientes.

El cliente conserva la sesión ante errores de conexión, HTTP 429 y fallos 5xx.
Al arrancar ofrece reintentar sin perder las credenciales; durante el uso
reintenta la renovación a los 30 segundos. Solo un rechazo 401/403 del endpoint
de renovación invalida la sesión.

## Notificaciones locales

Los mensajes directos recibidos por WebSocket producen avisos del sistema
cuando la aplicación no está en primer plano. No se notifican mensajes propios,
ediciones, eliminaciones ni entregas duplicadas recientes. Pulsar el aviso abre
la conversación cuando la sesión y la lista de conversaciones están disponibles.

En escritorio se utiliza la bandeja del sistema. La X oculta la ventana si la
bandeja está disponible; «Abrir Chimera» la recupera y «Salir» termina el proceso.
Si no hay bandeja disponible, la X termina la aplicación. Se necesita mantener
el proceso activo y tener conexión para recibir estos avisos.

En Android se crea el canal «Mensajes directos» y se solicita POST_NOTIFICATIONS
una vez al iniciar sesión en Android 13 o posterior. Los permisos y sonidos
pueden cambiarse desde los ajustes del sistema. Los avisos actuales necesitan
que el proceso y su conexión WebSocket sigan activos: no son notificaciones push.

## Pendiente: Android con el proceso cerrado

Para este caso hace falta integrar Firebase Cloud Messaging con un proyecto
del propietario, registrar los dispositivos autenticados y enviar los avisos
desde el backend tras guardar el mensaje. No se ha configurado Firebase ni se
han incorporado credenciales o conexiones persistentes artificiales.

Los cambios del backend deben desplegarse para aplicar la nueva duración de
las sesiones y la descripción de perfil. Las credenciales ya caducadas requieren
iniciar sesión de nuevo.
