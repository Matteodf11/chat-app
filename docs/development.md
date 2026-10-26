# Desarrollo de Chimera

[Volver a la presentación del proyecto](../README.md)

Para estudiar la arquitectura y seguir las funcionalidades de principio a fin,
consulta la [guía completa de funcionamiento](guia-completa-chimera.md).

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

El MSI se genera en `desktopApp/build/compose/binaries/main/msi/Chimera-1.1.5.msi`.
El script de empaquetado comprueba las versiones internas del APK y MSI y crea
`build/release/Chimera-1.1.5-demo.zip`, con guía y sumas SHA-256. Estas sumas permiten
comprobar integridad; no sustituyen una firma digital. El empaquetado se ejecuta
en Windows y necesita el Android SDK utilizado para compilar.
El cliente se ejecuta también como JVM en macOS/Linux, pero la captura de webcam,
la ubicación de escritorio y la distribución de esta versión se validan para
Windows. No se publica una aplicación para iPhone.

## Pruebas

```powershell
.\gradlew.bat :shared:jvmTest
node --test scripts/location-map.test.cjs
```

Las 120 pruebas Kotlin del recorrido local de 1.1.5 cubren estado, autenticación,
reintentos, borradores, caché, menciones, entrada de ubicación, puente local
del mapa, ciclo de vida de cámara y reproducción de audio, entre otros casos. Los tests de
hardware usan dobles: no certifican webcam, micrófono o localización reales.

Las seis pruebas JavaScript adicionales verifican la selección de ubicación en
el mapa. Requieren Node.js 24 solo en desarrollo y CI; no hay dependencias npm.
En PC, elegir ubicación abre el navegador y devuelve el punto a Chimera para
revisarlo y enviarlo, sin introducir coordenadas como texto en el panel.

El workflow `Client checks` construye APK/MSI y guarda los informes y paquetes
como artefactos de GitHub Actions durante 14 días. El resultado de cada commit
se consulta en Actions; no equivale a publicar una release permanente.

## Decisiones y límites

El chat usa HTTP para operaciones e historial y STOMP/WebSocket para eventos.
La caché es acotada para mejorar navegación sin crecer indefinidamente. El UUID
estable de cada envío permite reintentar sin crear otro mensaje en el backend.
Las interfaces de repositorio y las implementaciones `expect/actual` permiten
probar lógica compartida y adaptar hardware a cada plataforma.
