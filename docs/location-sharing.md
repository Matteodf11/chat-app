# Compartir ubicación

Desde 1.1.5, PC abre un mapa interactivo en el navegador desde Adjuntar → Ubicación.
Se ha sustituido el proveedor antiguo de Windows y se ha retirado del panel de
PC el campo para pegar coordenadas o texto.

- «Usar mi ubicación» abre el mapa y solicita permiso mediante el navegador.
- «Elegir un punto en el mapa» permite navegar, ampliar y pulsar un lugar, o
  arrastrar el marcador. La vista inicial no cuenta como selección.
- «Usar este punto» devuelve la selección a Chimera. Después hay que pulsar
  «Enviar ubicación» en la app. No se envían mensajes desde el navegador.

Si el navegador no obtiene la posición o se deniega el permiso, el mapa sigue
permitiendo elegir un lugar. La precisión depende del navegador, el dispositivo
y sus permisos; este cambio no garantiza que todos los equipos puedan localizarse.
No hay seguimiento en tiempo real ni se utiliza una estimación por IP propia.

El mapa utiliza Leaflet 1.9.4 y teselas de OpenStreetMap mediante HTTPS. Necesita
Internet, pero no requiere cuenta ni clave de mapas. Se mantiene la atribución
visible y la caché normal del navegador, sin descargas masivas. La página informa
si no puede cargar el mapa. Los proveedores externos reciben las solicitudes de
sus recursos; las coordenadas seleccionadas vuelven al cliente por una conexión
local y no se envían al backend hasta confirmar el mensaje en Chimera.

La página se sirve solo en 127.0.0.1, con puerto aleatorio, ruta temporal y token
por selección. La devolución valida origen, token, tamaño y coordenadas finitas.
No acepta solicitudes de otros sitios ni permite reutilizar una selección.
Cancelar, cerrar el diálogo de Chimera o agotar cinco minutos libera el servidor.
Cerrar solo la pestaña no devuelve un punto: se puede cancelar desde Chimera.
El runtime del MSI incluye jdk.httpserver; no requiere instalar Node.js o Java.

Android conserva su proveedor nativo y su entrada alternativa de coordenadas o
enlaces de Google Maps con un punto explícito. Ambos clientes envían el mismo
formato y muestran una acción «Ver en el mapa» en el chat.

## Verificación

Seis pruebas Kotlin usan solicitudes HTTP locales reales para validar devolución,
cancelación, aislamiento, errores y cierre del listener. Seis pruebas JavaScript
cubren selección manual, confirmación, permiso denegado, cancelación, error de
carga y una respuesta de ubicación tardía que no debe reemplazar la selección.
No solicitan la ubicación real de ningún dispositivo.

```powershell
.\gradlew.bat :shared:jvmTest
node --test scripts/location-map.test.cjs
```

Las pruebas JavaScript necesitan Node.js 24 en desarrollo y también corren en CI;
no añaden dependencias npm. La comprobación visual con el navegador de herramientas
no pudo acceder al servidor local del equipo. Queda comprobar en el navegador
real del usuario: carga del mapa, permiso concedido/denegado, arrastre del marcador,
regreso a Chimera, envío Android ↔ Windows y cancelación desde la app.

Referencias:

- [Leaflet: guía de inicio](https://leafletjs.com/examples/quick-start/)
- [OpenStreetMap: política de teselas](https://operations.osmfoundation.org/policies/tiles/)
