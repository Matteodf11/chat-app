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
