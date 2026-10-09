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
