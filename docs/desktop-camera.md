# Cámara de escritorio

En Windows, Adjuntar → Cámara abre una ventana de captura dentro de Chimera.
Si hay varias webcams se puede seleccionar una. La imagen conserva la orientación
de la cámara: ni la vista previa ni la captura se reflejan automáticamente.
Después de capturar se utiliza el panel compartido de revisión, con comentario,
repetición, volteo manual y envío. No se envía ninguna foto al capturarla.

La captura utiliza Webcam Capture 0.3.12 y el controlador nativo 1.3.1 de
[eduramiba](https://github.com/eduramiba/webcam-capture-driver-native). Se excluye
BridJ, el controlador antiguo, y se empaquetan las dependencias con la aplicación.
La integración se habilita en Windows; Android mantiene su implementación nativa.

El descubrimiento tiene un límite de 15 segundos. La sesión serializa las
operaciones nativas en un dispatcher de IO, limita la vista previa a 10 fps y
elige una resolución disponible hasta 1280 × 720. Los fotogramas se copian para
que el envío corresponda a una imagen estable. Cada apertura tiene un propietario
para que la limpieza de una sesión anterior no cierre la cámara recién elegida.
Se libera el dispositivo al capturar, cancelar, cambiar de cámara o desmontar
la ventana. La ausencia de webcam y los fallos de acceso tienen mensajes y opción
de reintentar.

Las pruebas automatizadas usan cámaras falsas: cubren liberación tras fallo,
cambio de dispositivo y limpieza tardía, cierre definitivo, independencia de los
fotogramas y orientación del JPEG/volteo. No abren ni fotografían la webcam real.

## Validación manual pendiente

- Con el MSI, abrir la cámara, capturar, revisar y enviar a Android.
- Cancelar y volver a abrir; comprobar que se apaga el indicador de cámara.
- Repetir la foto y usar Voltear; comprobar el resultado recibido.
- Seleccionar una segunda cámara, si existe.
- Desconectar la webcam o bloquear su acceso en Windows; comprobar error/reintento.
- Abrir con otra aplicación usando la webcam; comprobar que Chimera sigue respondiendo.
- Probar una actualización desde el MSI anterior y el arranque sin Java instalado.
