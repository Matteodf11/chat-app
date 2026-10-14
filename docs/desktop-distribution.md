# Distribución para Windows

Generar el instalador desde la raíz del proyecto:

```powershell
.\gradlew.bat :desktopApp:packageMsi --console=plain
```

El archivo resultante es `desktopApp/build/compose/binaries/main/msi/Chimera-1.1.5.msi`.
Se puede compartir ese MSI directamente. Incluye el entorno Java necesario;
el destinatario no necesita instalar Java. Se instala para el usuario actual,
permite elegir carpeta y añade acceso al menú Inicio y al escritorio.

Gradle descarga WiX en la primera generación. `upgradeUuid` identifica la misma
aplicación entre versiones: conservarlo y aumentar `packageVersion` al distribuir
una versión nueva. El instalador actual no tiene firma digital.

La ventana recuerda tamaño y si estaba maximizada, y ajusta el tamaño restaurado
al área disponible del monitor principal. Mantiene un mínimo de 900 × 600,
limitado por el tamaño de la pantalla. La navegación existente se adapta cuando
la ventana es estrecha. Las preferencias de tamaño son locales al equipo.

Atajos con el foco dentro de la interfaz:

| Atajo | Acción |
| --- | --- |
| Ctrl + F | Buscar en la conversación abierta |
| Ctrl + K | Abrir búsqueda general |
| Ctrl + , | Abrir ajustes |
| Esc en el editor | Cancelar la respuesta seleccionada |
| Enter / Shift + Enter | Enviar / insertar salto de línea |
| ↑ / ↓ con sugerencias de mención | Elegir persona |
| Tab con sugerencias de mención | Completar la persona resaltada |

La versión 1.1.0 incorpora captura con webcam en Windows. Véanse la implementación
y las comprobaciones pendientes en [Cámara de escritorio](desktop-camera.md).
El cambio de versión conserva `upgradeUuid` para permitir actualizar el MSI
anterior. La versión 1.1.1 añade ubicación de Windows y selección manual mejorada;
Android usa también 1.1.1 y `versionCode = 3`. Véase [Compartir ubicación](location-sharing.md).

Validación manual en PC: instalar el MSI, abrir desde ambos accesos, iniciar
sesión, cargar fotos y enviar un mensaje. Redimensionar, maximizar, cerrar y
abrir de nuevo; probar atajos desde el editor y los campos de búsqueda.
Probar después una actualización con versión superior y el mismo `upgradeUuid`.

La versión 1.1.2 carga las notas de voz al pulsar reproducir y libera el audio
al abandonar el chat. Android usa también 1.1.2 y versionCode = 4.

La versión 1.1.3 corrige la apertura del selector de foto de perfil, portada e
icono de servidor en Windows: el diálogo nativo y la lectura se ejecutan fuera
del contexto de Compose, el selector se libera al cerrarlo y la lectura está
limitada a 2 MB. Android usa también 1.1.3 y versionCode = 5.

La versión 1.1.4 elimina los avisos externos en Windows. Conserva los indicadores
internos y la bandeja para abrir y salir de Chimera. Android conserva sus avisos
locales y usa también 1.1.4, con versionCode = 6.

La versión 1.1.5 sustituye la consulta antigua de Windows por un mapa en el
navegador: ubicación con permiso o selección de un punto con el ratón, seguida
de revisión y envío en Chimera. Incluye jdk.httpserver en el runtime del MSI.
Corrige también textos de registro y perfiles. Android usa 1.1.5 y versionCode = 7.
