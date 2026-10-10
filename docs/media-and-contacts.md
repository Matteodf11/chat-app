# Imágenes, adjuntos y contactos

El botón de adjuntar ofrece **Galería**, **Archivo** y **Ubicación**. Android usa
el selector de fotos del sistema; en escritorio se abre un selector de imágenes.
Las fotos se envían con sus bytes originales, sin compresión adicional. Los
formatos admitidos para imágenes son JPEG, PNG, WebP y GIF; el límite de los
adjuntos sigue siendo 25 MB. Un formato o tamaño incompatible muestra un error.

Las imágenes aparecen en el chat. Pulsarlas abre el visor con zoom y desplazamiento;
también hay controles +/− para escritorio. La previsualización limita la resolución
decodificada para controlar el consumo de memoria. Guardar descarga el original,
con una URL renovada mediante el backend y comprobación de pertenencia al chat.
El destino se elige con el diálogo nativo de guardar. Audio y documentos también
pueden guardarse directamente. Cancelar el selector no envía ni guarda nada.

Las fotos del chat conservan su proporción completa, sin recortes, nombre de archivo
ni botones bajo la imagen. Guardar y compartir se ofrecen dentro del visor, con
iconos. Android abre el panel del sistema para compartir el archivo original; en
PC «Copiar imagen» permite pegarla en otra aplicación. No se comparte la URL privada
de descarga. Los archivos preparados para compartir se limpian después de 24 horas
cuando se vuelve a usar esta función.

La ubicación se envía como texto compatible con clientes anteriores y un enlace
al mapa. Se valida latitud/longitud y se pide confirmación antes de enviarla.
Android solicita permiso solamente al pulsar «Usar mi ubicación» y obtiene una
posición puntual, con tiempo de espera y cancelación al cerrar la pantalla.
En escritorio se introducen coordenadas manualmente. No requiere Firebase ni
seguimiento en segundo plano. Se puede comprobar el punto en el mapa antes de
enviarlo.

Desde el perfil se puede eliminar a un amigo o bloquearlo, previa confirmación.
La pestaña **Bloqueados** permite deshacer un bloqueo. El servidor impide mensajes,
adjuntos, ediciones y solicitudes en ambas direcciones mientras cualquiera de los
dos mantenga un bloqueo. Se conserva el historial; eliminar una amistad permite
seguir hablando, y desbloquear no restaura la amistad. Los canales de servidores
compartidos no se restringen. Los cambios de contactos se sincronizan por eventos
privados y al reconectar. Migraciones del backend: V18 y V19 (automáticas por Flyway).

Los avatares mantienen el original: el límite de 2 MB rechaza archivos grandes,
no los reduce. Se aplica filtrado de alta calidad al escalarlos. Una imagen de
origen con pocos píxeles puede seguir viéndose borrosa en pantallas grandes.

Comprobaciones manuales recomendadas antes de distribuir:

- Android físico: seleccionar una foto, cancelar, enviar, ampliar y guardar.
- Permiso de ubicación concedido, aproximado, denegado y ubicación desactivada.
- Dos cuentas: bloquear con el otro chat abierto, intentar enviar y desbloquear.
- PC: ampliar, arrastrar y guardar una imagen; cancelar el diálogo de destino.


La opción **Cámara** está disponible en Android: abre la cámara del sistema y usa
una foto a resolución completa, con vista previa, comentario y botones para
repetir, quitar o enviar. Cancelar conserva el borrador y un adjunto anterior.
No solicita permiso de cámara porque la captura la realiza la aplicación del sistema.
Los archivos temporales se eliminan al regresar; capturas abandonadas se limpian
al iniciar después de 24 horas.

**Galería** permite seleccionar hasta diez imágenes en Android y PC (25 MB por
foto y 50 MB en total). La revisión permite escoger miniaturas y quitar imágenes;
el comentario y la respuesta se incluyen una sola vez, en la primera foto.
La selección completa se guarda de forma atómica en la cola local antes de
cerrar la revisión y se sube secuencialmente para mantener el orden. Los fallos
de subida siguen teniendo reintento manual individual y conservan su identificador.
Las vistas previas y el visor respetan la orientación EXIF de las fotos JPEG.

Pruebas adicionales en móvil físico: cámara horizontal/vertical, cancelar,
repetir y cancelar la repetición, galería múltiple, quitar la última imagen,
volver del selector tras cambiar de conversación y envío con conexión interrumpida.

La revisión de una captura incluye **Voltear** para ajustar el efecto espejo de
los selfies antes de enviar. Cambia la orientación EXIF del JPEG original sin
recomprimir sus píxeles y respeta la rotación en fotos verticales. Se puede pulsar
otra vez para deshacerlo; repetir la captura carga una foto nueva sin el ajuste.
La vista previa, el chat, el visor y el archivo descargado usan esa orientación.
La cámara externa no informa de forma fiable qué lente se ha utilizado, por lo
que el ajuste se confirma en la revisión. Verificar con una camiseta con texto
en cámara frontal, tanto vertical como horizontal, y comprobarlo con otra cuenta.


El visor permite deslizar entre fotos y usar las flechas anterior/siguiente.
Con zoom, arrastrar mueve la imagen; al volver al 100 % se puede pasar a otra.
La acción **Anteriores** carga más fotos por páginas, tanto en mensajes directos
como en canales, sin abrir otro panel. Mantiene la foto seleccionada al añadir
historial y carga imágenes reducidas para limitar memoria. Guardar, compartir
y reenviar siempre actúan sobre la foto visible.

**Reenviar** aparece en el menú de un mensaje y en el visor de imágenes. Permite
buscar entre otros chats existentes (incluidos los archivados) y canales de texto,
seleccionar un destino y confirmar. Los chats bloqueados se excluyen del selector;
el servidor vuelve a comprobar permisos y bloqueos en el envío.
Se obtiene una copia actual del mensaje y se renueva su URL privada antes de
descargar un adjunto. Se envía el original con su comentario, como un mensaje
nuevo de la cuenta actual, sin atribuirle el autor original ni arrastrar la
respuesta a un mensaje del chat de origen. No añade una etiqueta «Reenviado».
Reutiliza la cola persistente y los identificadores idempotentes. Preparar el
reenvío requiere conexión; una vez guardado, los fallos de envío se reintentan
desde el chat de destino. Cancelar el selector no envía nada.

Comprobar en móvil y PC: pasar diez fotos, ampliar/arrastrar, volver al 100 %,
cargar historial sin saltar de foto, guardar la segunda foto; reenviar texto,
imagen, documento y nota de voz a un chat y canal, cancelar, bloquear el destino,
probar sin conexión y confirmar que el borrador del chat de origen se conserva.


La barra de escritura reúne adjuntos, texto y micrófono/envío en una sola fila.
El micrófono cambia al botón de enviar cuando hay texto o un adjunto; el texto
puede crecer hasta cuatro líneas. Se mantienen Enter para enviar, Shift+Enter
para saltos de línea y las vistas de respuesta/adjuntos encima de la barra.

La búsqueda muestra un campo principal, el acceso a filtros plegados y tarjetas
con autor, fecha, texto y archivo. Los filtros por autor/fechas siguen validándose,
se pueden limpiar y los resultados antiguos no se abren durante una búsqueda nueva.
La lista permite desplazarse incluso con los filtros desplegados.

Mantener pulsada una tarjeta en Amigos o Mensajes abre sus opciones: abrir chat,
ver perfil y, si ya existe una conversación, fijar/desfijar, silenciar/reactivar
notificaciones y archivar/desarchivar. Los tres puntos ofrecen el mismo menú.
Una pulsación breve abre el chat. Las preferencias se comparten entre ambas listas.
Comprobar en móvil y escritorio: mensajes multilínea y teclado, búsqueda con filtros
y fechas erróneas, pulsación larga sin abrir el chat y persistencia de preferencias.


Las opciones de conversaciones, mensajes y adjuntos se muestran en un panel modal
que entra desde abajo, con iconos, fondo atenuado y cierre por gesto/atrás/toque
fuera. Su ancho se limita en PC y se puede desplazar en pantallas bajas.
Los paneles de una conversación o un mensaje muestran la foto de la persona
junto al nombre; si no tiene foto, se muestra su inicial.
Al abrir los adjuntos se retira el teclado. El borde de escritura utiliza el
color personal, con una transición breve al ganar o perder el foco.

Las fotos conservan su proporción al salir y volver a entrar en pantalla.
Una caché de miniaturas de hasta 24 MB evita decodificarlas de nuevo en cada
pasada; la clave es el mensaje, independiente de la renovación de su URL privada.
El seguimiento de mensajes nuevos mide la distancia al final en píxeles, no en
número de tarjetas, para respetar la lectura de una foto alta. Cargar historial
o actualizar un envío pendiente no fuerza el desplazamiento; enviar un mensaje
propio sí lleva al final. El teclado ya no fuerza un salto por cada fotograma
de su animación.

Comprobar en móvil: abrir adjuntos con teclado visible, cerrar por gesto y por
atrás, mantener pulsada una conversación, y usar los paneles en horizontal.
En un chat con fotos verticales y horizontales, subir/bajar varias veces, recibir
mensajes leyendo historial y abrir el teclado sin perder la posición. La primera
carga de una foto aún necesita obtener sus dimensiones antes de mostrarla.

El editor del perfil propio usa un diálogo de hasta 680 dp con pestañas Perfil
y Seguridad. La cabecera de perfil muestra portada, avatar, nombre, correo y
estado; las imágenes elegidas se previsualizan antes de guardar y cada selección
se puede deshacer. Los archivos de más de 2 MB se rechazan antes del envío.
Cada pestaña conserva su posición de lectura y los campos al cambiar entre ellas.
Las acciones de guardar/cambiar contraseña quedan fuera del contenido desplazable;
durante una operación se bloquean la edición, el guardado y el cierre.
Comprobar con teclado abierto y pantalla horizontal, nombres/correos largos,
cambio y cancelación de imágenes, estados y errores de guardado/contraseña.
