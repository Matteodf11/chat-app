# Chimera Chat

Estado de la retoma y cambios conservados:
[auditoría del 9 de octubre de 2026](docs/restart-audit-2026-10-09.md).

Cliente de chat multiplataforma construido con Kotlin Multiplatform y Compose
Multiplatform. Comparte la lógica y la interfaz entre Android y Desktop (JVM).

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

Cada dominio expone un único `StateFlow` coherente (`ChatConversationUiState`,
`FriendUiState` y `ServerUiState`). Los eventos que sólo deben consumirse una vez
se modelan con `SharedFlow`, no como estado persistente.

`AppContainer` es el único lugar donde se construyen las dependencias. Los
Los ViewModels reciben repositorios y fuentes de eventos mediante su constructor;
ninguno conoce clientes HTTP, WebSocket ni el almacenamiento de sesión.
Autenticación, chat, servidores, amigos y perfiles están detrás de contratos
sustituibles por implementaciones falsas en pruebas. Esto evita recursos
duplicados y separa completamente la presentación de la infraestructura.

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

## Compilar y ejecutar

En Windows:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :desktopApp:run
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

## Pruebas

```powershell
.\gradlew.bat :shared:jvmTest
```

Las pruebas compartidas cubren actualmente traducción de errores, ViewModels de
autenticación, chat y amigos con repositorios falsos, y reductores de estado de
chat, servidores, amigos, presencia y perfiles.
