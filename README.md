# Chimera Chat

Mensajería para Android y Windows, con conversaciones privadas y servidores,
menciones y mensajes sin leer, fotos, adjuntos, notas de voz y preferencias de
apariencia. El cliente comparte lógica y componentes mediante Kotlin y Compose
Multiplatform; el backend utiliza Java, Spring Boot, PostgreSQL y WebSocket.

La versión 1.1.1 incluye captura con webcam en Windows y ubicación con selección
manual y revisión antes de enviar. Los paquetes son de demostración: el APK es
debug y el MSI no tiene firma digital. La captura básica de webcam ha sido
confirmada por el usuario; la ubicación automática y la actualización del MSI
siguen pendientes de comprobación manual.

La versión 1.1.2 descarga las notas de voz al pulsar reproducir, permite reintentar
si falla la carga y libera el audio al salir de la conversación. No se descarga
el audio de todo el historial para mostrar sus controles. La duración se conoce
al preparar la reproducción.

- [Instalación y distribución de Windows](docs/desktop-distribution.md)
- [Captura de cámara y comprobaciones manuales](docs/desktop-camera.md)
- [Ubicación en Windows y Android](docs/location-sharing.md)
- [Fiabilidad de mensajes](docs/chat-reliability.md)
- [Servidores, lectura y menciones](docs/server-interface.md)
- [Plan de cierre para el portfolio](docs/portfolio-roadmap.md)

## Probar la demo

La [guía de demostración](docs/demo-guide.md) incluye instalación, recorrido de
cinco minutos y límites conocidos. El [estado de cierre](docs/release-checklist.md)
distingue las pruebas automatizadas de las verificaciones manuales pendientes.
Se necesitan dos cuentas para probar conversación y sincronización entre
dispositivos. Los paquetes predeterminados conectan al backend desplegado en
Render; el repositorio del backend es
[chat-backend](https://github.com/Matteodf11/chat-backend).

Funciones principales:

- Conversaciones privadas y canales de servidores, con roles e invitaciones.
- Respuestas, edición, eliminación, búsqueda, galería y envío de adjuntos.
- Menciones por clic o teclado; contadores de mensajes y menciones simultáneos.
- Cola de envíos con reintentos identificados, recuperación de conexión y
  borradores por conversación.
- Caché acotada de historial e imágenes y preferencias de apariencia.
- Integración de plataforma: voz, archivos, webcam, ubicación y notificaciones
  locales en Android. Windows mantiene los indicadores de mensajes sin leer y
  menciones dentro de la app, sin avisos externos.

No se ofrece cifrado de extremo a extremo ni push garantizado con la app
terminada. La recuperación de contraseña sin sesión todavía no está implementada.
La explicación completa de estos límites está en la guía de demo.

## Arquitectura

El código compartido vive en `shared/src/commonMain/kotlin/com/matteo/chatapp`:

- `di`: punto de composición de dependencias. `AppContainer` crea y comparte el
  cliente HTTP, las APIs, los WebSockets y los ViewModels.
- `model`: contratos serializables intercambiados con el backend.
- `network`: acceso HTTP, WebSocket, renovación de sesión y traducción de errores.
- `platform`: abstracciones `expect/actual` para archivos, portapapeles, navegación
  y grabación de voz.
- `repository`: contratos de acceso a datos e implementaciones de autenticación,
  chat, servidores, amigos y perfiles, además del acceso a sesión y preferencias.
- `viewmodel`: estado y lógica de presentación por dominio.
- `ui/components`: componentes visuales reutilizables y sin navegación.
- `ui/screens`: pantallas y coordinación de eventos de usuario.
- `ui/theme`: colores, tipografía y preferencias de apariencia.

La aplicación utiliza flujo unidireccional:

```text
UI -> acción del ViewModel -> Repository -> API/WebSocket -> UiState -> UI
```

Los dominios exponen estado observable con `StateFlow` (`ChatConversationUiState`,
`FriendUiState` y `ServerUiState`, entre otros). Los eventos puntuales se modelan
con `SharedFlow`; los datos que deben conservarse se mantienen en estado.

`AppContainer` es el único lugar donde se construyen las dependencias.
Los ViewModels reciben repositorios y fuentes de eventos mediante su constructor;
ninguno conoce clientes HTTP, WebSocket ni el almacenamiento de sesión.
Autenticación, chat, servidores, amigos y perfiles están detrás de contratos
sustituibles por implementaciones falsas en pruebas.

## Añadir una funcionalidad

1. Define o amplía el contrato correspondiente en `model`.
2. Implementa la operación remota en `network`.
3. Expón el caso de uso desde el repositorio del dominio cuando exista.
4. Actualiza el `UiState` y las acciones del ViewModel.
5. Representa el estado en un componente o pantalla de `ui`.
6. Añade pruebas de reductores y ViewModels con dependencias falsas en `commonTest`.

Evita comunicar pantallas directamente con APIs, introducir estado duplicado en
varios `StateFlow` o construir dependencias fuera de `AppContainer`.

## Configuración del backend

La URL del backend está definida en
`shared/src/commonMain/kotlin/com/matteo/chatapp/network/HttpClient.kt`.
Para trabajar contra un backend local hay que cambiar esa URL y recompilar.
Windows puede usar `http://localhost:8080`; en Android, `localhost` es el propio
dispositivo. El acceso HTTP local en Android necesita configuración de red
de depuración adicional y un host accesible desde el emulador o teléfono.
La demo publicada utiliza HTTPS. No se incluye una selección de servidor en la UI.

## Compilar y ejecutar

Requisitos: JDK 21, Android SDK con plataforma 36 y `ANDROID_HOME` o un
`local.properties` propio. Se utiliza el Gradle Wrapper incluido. No copiar
`local.properties` de otro equipo.

En Windows:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :desktopApp:run
.\gradlew.bat :desktopApp:packageMsi
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\package-demo.ps1
```

En macOS o Linux:

```bash
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
```

El APK debug se genera en:

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

El MSI se genera en `desktopApp/build/compose/binaries/main/msi/Chimera-1.1.4.msi`.
El script de empaquetado comprueba las versiones internas del APK y MSI y crea
`build/release/Chimera-1.1.4-demo.zip`, con guía y sumas SHA-256. Estas sumas permiten
comprobar integridad; no sustituyen una firma digital. El empaquetado se ejecuta
en Windows y necesita el Android SDK utilizado para compilar.
El cliente se ejecuta también como JVM en macOS/Linux, pero la captura de webcam,
la ubicación de escritorio y la distribución de esta versión se validan para
Windows. No se publica una aplicación para iPhone.

## Pruebas

```powershell
.\gradlew.bat :shared:jvmTest
```

Las 118 pruebas del recorrido local de 1.1.4 cubren estado, autenticación,
reintentos, borradores, caché, menciones, entrada de ubicación, cierre del proceso
de localización, ciclo de vida de cámara y reproducción de audio, entre otros casos. Los tests de
hardware usan dobles: no certifican webcam, micrófono o localización reales.

El workflow `Client checks` construye APK/MSI y guarda los informes y paquetes
como artefactos de GitHub Actions durante 14 días. El resultado de cada commit
se consulta en Actions; no equivale a publicar una release permanente.

## Decisiones y límites

El chat usa HTTP para operaciones e historial y STOMP/WebSocket para eventos.
La caché es acotada para mejorar navegación sin crecer indefinidamente. El UUID
estable de cada envío permite reintentar sin crear otro mensaje en el backend.
Las interfaces de repositorio y las implementaciones `expect/actual` permiten
probar lógica compartida y adaptar hardware a cada plataforma.

Antes de una presentación, seguir el recorrido de `docs/release-checklist.md`.
Capturas y vídeo deben grabarse con la app real y datos de demostración: todavía
no se incluyen materiales visuales verificados en este README.
