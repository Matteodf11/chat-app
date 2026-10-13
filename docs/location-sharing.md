# Compartir ubicación

La versión 1.1.1 permite usar la ubicación de Windows desde Adjuntar → Ubicación.
La búsqueda comienza únicamente al pulsar «Usar mi ubicación». Usa
GeoCoordinateWatcher de System.Device mediante Windows PowerShell, con un script
fijo empaquetado en el cliente. No necesita una clave de API adicional, no utiliza
servicios de geolocalización por IP y no cambia los permisos del sistema.

Windows debe tener un proveedor y permitir el acceso a la ubicación. Los equipos
sin proveedor disponible muestran una alternativa manual. El punto puede ser
aproximado, por lo que se muestra antes de enviarlo con una acción para abrirlo
en el navegador. Solo «Enviar ubicación» crea un mensaje; no hay seguimiento en
tiempo real ni envío automático al finalizar la búsqueda.

El panel permite pegar coordenadas como `38.345, -0.481` o, con decimales españoles,
`38,345; -0,481`. También acepta enlaces HTTPS de Google Maps que contengan un
par de coordenadas en `query` o `q`. Se genera un enlace canónico a partir del
punto validado; nunca se abre el enlace original introducido por el usuario.
No se resuelven enlaces cortos, direcciones de texto ni el centro de un mapa
contenido en una URL `/@...`, que puede ser distinto del lugar seleccionado.

La consulta de Windows tiene un límite de 20 segundos en el proveedor y de
30 segundos en el cliente. La cancelación o el cierre del diálogo terminan el
proceso auxiliar y liberan los recursos. No se registra su salida, que contiene
las coordenadas. El script no interpola ninguna entrada del usuario. Las
operaciones bloqueantes se ejecutan fuera del hilo de la interfaz y el proceso
auxiliar se inicia sin ventana de terminal.

Android conserva su proveedor nativo, con el mismo panel de revisión y elección
manual. El botón de ajustes abre la configuración del dispositivo; el usuario
decide si desea cambiar sus permisos.

## Verificación

Pruebas automáticas: formatos de coordenadas, validación de rangos y valores no
finitos, enlaces permitidos y rechazados, respuestas de Windows, cancelación,
espera agotada y cierre del proceso. La sintaxis del script y la disponibilidad
de System.Device se han comprobado sin solicitar la posición del equipo.

Pendiente de prueba manual: localizar con permisos activados, denegarlos y
reintentar desde ajustes, cancelar una búsqueda, cerrar el diálogo durante la
consulta, abrir el punto en el mapa y enviarlo entre Windows y Android. Probar
también un PC sin proveedor y los ejemplos manuales. La precisión real depende
del dispositivo y no está certificada por las pruebas con procesos falsos.

Referencias de Microsoft:
- [GeoCoordinateWatcher](https://learn.microsoft.com/en-us/dotnet/api/system.device.location.geocoordinatewatcher?view=netframework-4.8.1)
- [TryStart](https://learn.microsoft.com/en-us/dotnet/api/system.device.location.geocoordinatewatcher.trystart?view=netframework-4.8.1)
- [Ubicación y privacidad de Windows](https://support.microsoft.com/en-us/windows/privacy/windows-location-service-and-privacy)
