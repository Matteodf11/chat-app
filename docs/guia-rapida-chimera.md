# Chimera: las piezas principales

**Lectura aproximada: 5–10 minutos.** Esta guía explica qué hace cada componente y cómo encaja con los demás. Para profundizar, tienes la [guía completa](guia-completa-chimera.md).

## 1. La aplicación entera en cuatro piezas

| Pieza | Para qué sirve |
| --- | --- |
| **Frontend: Android y Windows** | Es lo que usa la persona: pantallas, editor, fotos, navegación y acciones. Está hecho con Kotlin y Compose Multiplatform. |
| **Backend: Java y Spring Boot** | Recibe solicitudes, comprueba identidad y permisos, aplica reglas y coordina el guardado. Se ejecuta en Render. |
| **PostgreSQL en Supabase** | Guarda datos estructurados: usuarios, mensajes, comunidades, miembros y lecturas. |
| **Supabase Storage** | Guarda archivos: fotos, documentos, avatares y notas de voz. La base de datos conserva sus referencias. |

GitHub guarda el código y ejecuta comprobaciones automatizadas; no es donde se procesan los mensajes de los usuarios.

## 2. Frontend: qué hace cada componente

### Pantallas y componentes Compose

Las **pantallas** organizan una vista completa, como acceso, amigos o conversación. Los **componentes** son piezas reutilizables: avatar, mensaje, botón o visor de imágenes.

Compose dibuja a partir del estado: si cambia la lista de mensajes, actualiza su representación. Las pantallas recogen acciones y las entregan a la lógica correspondiente.

### ViewModels: coordinan lo que ocurre en la interfaz

Un ViewModel mantiene el estado de una funcionalidad y responde a acciones. Por ejemplo: «cargar mensajes», «enviar», «hay un error» o «ha llegado un mensaje nuevo». **No dibuja la pantalla ni accede directamente a PostgreSQL.**

En Chimera tenemos estos cuatro:

| ViewModel | Qué gestiona |
| --- | --- |
| **`AuthViewModel`** | Registro e inicio de sesión: validación del formulario, operación en curso, resultado y errores. |
| **`ChatViewModel`** | Historial, conversación activa, envío de mensajes y adjuntos, cola local, reintentos y cambios recibidos en tiempo real. |
| **`FriendViewModel`** | Amigos, solicitudes, búsqueda de usuarios, apertura de chats privados, bloqueos y perfiles. También coordina acciones de cuenta como cambiar contraseña y cerrar sesión. |
| **`ServerViewModel`** | Servidores, canales, miembros, invitaciones, roles y actividad pendiente, incluidos mensajes sin leer y menciones. |

No tenemos un ViewModel distinto por cada botón. Agrupamos acciones relacionadas. Por eso los perfiles están actualmente dentro de `FriendViewModel`, aunque también exista un repositorio específico de perfiles.

### Estado y flujos

El **estado** contiene lo que necesita la pantalla: datos, selección, carga o error. `StateFlow` permite observar su último valor; al cambiar, la interfaz reacciona. `SharedFlow` comunica eventos puntuales según su configuración.

Estos mecanismos viven en memoria. Para conservar algo al terminar el proceso hay que guardarlo en preferencias, archivos o una base de datos.

### Repositorios del cliente

`AuthRepository`, `ChatRepository`, `FriendRepository`, `ServerRepository` y `ProfileRepository` ofrecen operaciones de datos al resto del cliente.

El ViewModel pide «envía este mensaje» al repositorio, sin construir la petición HTTP. Las implementaciones `Network…Repository` realizan esas operaciones mediante las APIs. Tener interfaces permite sustituirlas por versiones falsas en pruebas.

### APIs y cliente HTTP

`AuthApi`, `MessageApi`, `FriendApi`, `ServerApi` y `UserApi` conocen las rutas del backend y cómo enviar o recibir datos. **Ktor Client** ejecuta las peticiones; la serialización convierte objetos Kotlin a JSON y viceversa.

Ejemplo: `MessageApi` sabe que enviar texto privado utiliza `POST /api/messages/dm/{dmId}`. El ViewModel no necesita conocer esa URL.

### Otras piezas del frontend

| Componente | Función |
| --- | --- |
| **Modelos** | Representan datos en Kotlin, como un mensaje o un usuario. |
| **`AppContainer`** | Construye y conecta APIs, repositorios, ViewModels y fuentes de eventos: inyección de dependencias manual. |
| **`SessionManager`** | Mantiene identidad y credenciales de la sesión actual. |
| **`SessionRefresher`** | Renueva credenciales y coordina peticiones simultáneas para evitar renovaciones en conflicto. |
| **`WebSocketClient` y fuentes de eventos** | Reciben cambios en tiempo real: mensajes, lecturas, perfiles y actividad. |
| **`FileChatLocalStore`** | Guarda envíos pendientes y sus archivos para poder recuperarlos y reintentarlos. |
| **Cachés** | Reutilizan avatares, imágenes e historial reciente para reducir esperas. |
| **Preferencias** | Conservan ajustes como tema y sesión recordada. |
| **Integraciones de plataforma** | Resuelven cámara, micrófono, ubicación y archivos de forma distinta en Android y Windows. |

`commonMain` contiene lo compartido; `androidMain` y `jvmMain`, las adaptaciones. Los contratos `expect/actual` permiten pedir una operación común y ejecutarla con la implementación de cada plataforma.

Las **corrutinas** coordinan operaciones que pueden esperar y cancelarse. El trabajo bloqueante necesita un contexto apropiado para no congelar la interfaz; escribir `suspend` no lo mueve automáticamente a otro hilo.

## 3. Backend: qué hace cada componente

### Controllers: la entrada de las solicitudes

Un controller recibe una petición HTTP, recoge sus datos, llama al servicio adecuado y devuelve una respuesta.

Por ejemplo, **`MessageController`** recibe un envío y delega sus reglas en `MessageService`. También tenemos controllers de autenticación, usuarios, amistad, conversaciones y servidores.

### DTO: los datos que entran y salen

**DTO significa Data Transfer Object**, objeto de transferencia de datos. Define la forma de una solicitud o respuesta.

- **`MessageRequest`**: texto, destino, mensaje al que respondes e identificador para reintentos.
- **`MessageResponse`**: mensaje confirmado, autor, fecha, datos del adjunto y lectura, entre otros campos.
- **`LoginRequest`**: datos necesarios para iniciar sesión.
- **`UserProfileResponse`**: campos de perfil que se pueden devolver al cliente.

Un DTO transporta datos y puede declarar validaciones de formato o longitud. No es quien decide todos los permisos ni quien guarda por su cuenta en la base.

### Services: las reglas de la aplicación

Un service decide cómo se realiza una operación y coordina sus pasos.

| Service | Responsabilidad |
| --- | --- |
| **`AuthService`** | Registro, acceso, renovación, revocación de sesiones y cambio de contraseña. |
| **`MessageService`** | Validar, guardar, editar, borrar y buscar mensajes; gestionar reintentos y adjuntos. |
| **`FriendshipService`** | Solicitudes y relaciones de amistad. |
| **`UserService`** | Datos y cambios de perfil. |
| **`ServerService`** | Comunidades, canales, miembros, invitaciones y roles. |
| **`ChannelActivityService`** | Marcas de lectura y contadores de actividad de canales. |
| **`ChatAuthorizationService` y `UserSafetyService`** | Comprobaciones de acceso y reglas de bloqueos. |
| **`PresenceService`** | Conexiones activas y presencia de usuarios. |
| **Servicios de Storage** | Subidas, referencias y acceso a archivos de Supabase. |
| **`MessageCryptoService`** | Cifrar y descifrar el texto de los mensajes en el servidor. |

Ejemplo: aunque un cliente modificado muestre «expulsar administrador», el servicio debe rechazarlo si quien lo solicita no tiene permiso.

### Repositories: acceso a la base de datos

`UserRepository`, `MessageRepository` o `ServerRepository` consultan y modifican registros mediante **Spring Data JPA**. Por ejemplo: buscar un usuario por correo o cargar mensajes de una conversación.

La diferencia con el cliente es importante: **el repositorio del frontend suele acceder al backend; el del backend accede a la base de datos**. Comparten la idea de abstraer el acceso a datos, pero trabajan con destinos distintos.

### Entities: lo que se persiste

Las entidades representan registros y relaciones: `User`, `Message`, `DirectMessage`, `Server`, `Channel`, `ServerMember` o `AuthSession`.

**Entidad y DTO no son lo mismo.** `User` contiene un hash de contraseña que no debe salir en un perfil. `Message` conserva contenido cifrado; `MessageResponse` entrega el contenido preparado para un usuario autorizado. Separarlos evita exponer directamente el almacenamiento interno.

### Seguridad, configuración y errores

| Pieza | Función |
| --- | --- |
| **`SecurityConfig` y `JwtFilter`** | Configurar rutas protegidas y comprobar la credencial de las peticiones. |
| **`JwtService`** | Crear y verificar JWT de acceso. |
| **BCrypt** | Guardar hashes de contraseñas para verificarlas sin almacenar su texto original. |
| **Configuración WebSocket** | Preparar conexiones, suscripciones y distribución de eventos con STOMP. |
| **`RequestRateLimiter`** | Limitar la frecuencia de operaciones para reducir abuso y sobrecarga. |
| **`GlobalExceptionHandler`** | Convertir errores en respuestas comprensibles y consistentes. |
| **Flyway** | Aplicar cambios versionados del esquema de PostgreSQL. |

Una **transacción** agrupa cambios de base de datos que deben confirmarse juntos. No incluye automáticamente una subida a Storage o la recepción de un evento en otro dispositivo.

## 4. Cómo se conectan: enviar «Hola»

```text
Pantalla Compose
    ↓ acción del usuario
ChatViewModel
    ↓ guarda el envío pendiente y coordina la operación
ChatRepository → MessageApi → petición HTTP
    ↓
Seguridad del backend → MessageController → MessageRequest
    ↓
MessageService: comprueba permisos y aplica reglas
    ↓
MessageRepository → PostgreSQL
    ↓
MessageResponse → cliente → estado → pantalla actualizada
```

Además, el backend publica un evento **WebSocket** para actualizar al receptor. El recorrido principal envía por HTTP y recibe novedades por WebSocket.

El identificador estable del envío permite reintentar sin crear otro mensaje si el servidor ya lo guardó. La caché acelera lo ya conocido; la cola protege lo todavía pendiente. Son responsabilidades diferentes.

Para estudiar una función, sigue este mismo recorrido: **pantalla → ViewModel → repositorio → API → controller → service → repositorio de base de datos**. Después mira cómo vuelve el resultado a la pantalla.
