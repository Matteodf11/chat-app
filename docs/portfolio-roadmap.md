# Cierre de Chimera y siguiente proyecto

Revisión del 13 de octubre de 2026. Objetivo: presentar un proyecto estable,
reproducible y explicable en entrevistas, y después ampliar conocimientos con
un segundo proyecto. Este documento recoge propuestas; no certifica pruebas
manuales de hardware ni funciones todavía no implementadas.

## Android y Windows: revisión del código

| Área | Estado observado | Trabajo de cierre |
| --- | --- | --- |
| Chats, servidores, perfiles, búsqueda, menciones y preferencias | Interfaz y lógica compartidas | Comprobar los recorridos con dos cuentas en Android y Windows |
| Fotos múltiples, revisión, visor y descarga | Implementaciones en ambas plataformas | Probar selección, cancelación y archivos grandes desde el instalador |
| Notas de voz | Android y JVM graban WAV; ambos tienen reproducción | Comprobar micrófono, reproducción cruzada y liberación de recursos |
| Cámara | Android captura; Windows tiene captura integrada con selector y vista previa | Validar webcam real, errores de acceso y liberación desde el MSI; ver desktop-camera.md |
| Ubicación | Android mantiene su proveedor; PC selecciona un punto en el mapa del navegador, con ubicación opcional mediante permiso | Validar navegador con y sin permisos, marcador, cancelación y envío cruzado; ver location-sharing.md |
| Compartir imágenes | Android usa compartir del sistema; JVM copia imagen y archivo al portapapeles | Verificar el comportamiento en Windows y documentar la adaptación |
| Notificaciones | Android tiene avisos locales; Windows conserva los indicadores internos y la bandeja, sin avisos externos | Probar indicadores y cierre a bandeja con el MSI; avisos del sistema en Android |

La equivalencia debe cubrir los casos de uso. Un PC sin webcam o sin servicio
de ubicación necesita una alternativa clara. No basta con habilitar un botón
que luego falla. Orden propuesto: prueba de recorridos existentes, webcam,
mejora de ubicación y revisión final de integración con Windows.

## Recuperación de contraseña

Actualmente existe cambio de contraseña con sesión iniciada. La autenticación
es propia del backend; utilizar PostgreSQL de Supabase no activa Supabase Auth.

Propuesta para Android y Windows: solicitar recuperación por correo, introducir
un código en la app y elegir una contraseña nueva. Así el usuario no depende de
tener instalada una aplicación que abra enlaces de recuperación.

El backend debe generar un secreto aleatorio, guardar solo su hash, limitar
intentos y solicitudes, aplicar caducidad y un único uso, responder de forma
uniforme exista o no la cuenta y revocar las sesiones tras el restablecimiento.
Se necesitan pruebas de caducidad, reutilización, concurrencia y autorización.

Falta elegir y configurar la entrega real de correo. Render gratuito bloquea
los puertos SMTP habituales; una API de correo por HTTPS permite mantener
Render y Supabase. No se requiere Firebase. No publicar códigos de recuperación
en logs ni mostrar una recuperación ficticia si el envío no está configurado.

Fuente: [límites de Render gratuito](https://render.com/docs/free).

## Preparación para entrevistas

Antes de ampliar funciones, cerrar una versión con:

- README de presentación con capturas reales, alcance, instalación y límites.
- Guion breve de demo y datos de prueba sin información privada.
- Instrucciones verificadas para levantar backend y cliente en otro equipo.
- Pruebas automáticas del cliente en CI; el backend ya tiene workflow de pruebas.
- Versiones identificables de APK y MSI y una comprobación de actualización.
- Explicación de autenticación, permisos, reintentos sin duplicados, reconexión,
  caché, lectura y menciones, incluyendo sus límites reales.

La versión se considerará cerrada cuando los recorridos principales funcionen
en ambas plataformas, las diferencias estén implementadas o documentadas, y
otra persona pueda ejecutar la demo siguiendo las instrucciones.

## Siguiente proyecto: recomendación provisional

Para ampliar el perfil que ya cubren Kotlin y Java/Spring, valorar C# con ASP.NET
Core, React con TypeScript y una base SQL. Propuesta de producto: gestión de
incidencias y mantenimiento de una pequeña empresa, con usuarios y roles,
asignación, estados, comentarios, adjuntos, filtros, historial de cambios y
estadísticas. Empezar con un alcance pequeño; después incorporar pruebas,
contenedores y CI. Las versiones concretas se elegirán al iniciar el proyecto.

La búsqueda es una muestra cualitativa, no un ranking de demanda ni una garantía
de vacantes junior. Evitar contar anuncios duplicados o cerrados como puestos
abiertos. Referencias consultadas:

- [Altia: React Developer en Alicante](https://es.linkedin.com/jobs/view/react-developer-at-altia-4438918342): React, TypeScript, Git y pruebas.
- [LynxView: ofertas en Alicante](https://lynxview.es/trabaja-con-nosotros/): Java, Python, Android y React, entre otras áreas; no todas son junior.
- [W Hunt: .NET en Sax](https://es.linkedin.com/jobs/view/desarrollador-web-net-at-w-hunt-espa%C3%B1a-4472889570): C#, ASP.NET, SQL y aplicaciones industriales. El anuncio solicita tres años y ya no acepta candidaturas; sirve como referencia de tecnologías y dominio, no como oportunidad junior activa.

Antes de decidir el segundo proyecto, ampliar la muestra con ofertas junior y
prácticas de Alicante, Elche, San Vicente y puestos remotos accesibles desde
España. Mantener Java/Spring como opción laboral ya representada por Chimera.
