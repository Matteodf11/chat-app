# Probar Chimera 1.1.5

Chimera es un proyecto de portfolio de mensajería para Android y Windows. La demo
utiliza el backend desplegado en Render y almacenamiento de Supabase. Requiere
conexión a Internet; el ordenador no necesita instalar Java para usar el MSI.

## Instalación

1. Descomprime el paquete de demostración.
2. En Windows instala `Chimera-1.1.5.msi` y abre Chimera desde Inicio.
3. En Android instala `Chimera-1.1.5-debug.apk`, permitiendo la instalación desde
   la aplicación desde la que lo abras si Android lo solicita.
4. Registra una cuenta de demostración y accede. Para probar mensajería entre
   dispositivos usa dos cuentas distintas, una en cada dispositivo.

El APK está firmado con una clave de depuración; el MSI no tiene firma digital.
Los avisos del sistema por ese motivo no equivalen a pruebas de seguridad ni
deben ocultarse en la presentación. Si una instalación Android anterior fue
firmada con otra clave, no podrá actualizarse encima. Desinstalarla elimina sus
datos locales, incluidos borradores y envíos pendientes: no hacerlo si se quieren
conservar sin revisar antes una alternativa con la misma firma.

Para actualizar Windows se conserva el identificador de actualización del MSI
y se incrementa la versión. La actualización desde un instalador anterior debe
probarse en el equipo de demostración.

## Recorrido de cinco minutos

| Tiempo | Qué mostrar | Qué demuestra |
| --- | --- | --- |
| 0:00–0:40 | Inicio de sesión y cambio de color de tema | Autenticación y preferencias compartidas en la UI |
| 0:40–1:40 | Buscar la segunda cuenta, enviar/aceptar amistad y abrir el chat | Flujo de usuarios y conversación en tiempo real |
| 1:40–2:40 | Mensaje, respuesta, dos fotos y visor | Adjuntos, revisión y navegación multimedia |
| 2:40–3:40 | Crear servidor, invitar/aceptar y mencionar a la otra cuenta | Roles, pertenencia, canales y menciones |
| 3:40–4:20 | Destinatario fuera del canal; después abrirlo y leer | Contadores simultáneos de mensajes y menciones |
| 4:20–5:00 | Volver a un chat y mostrar borrador o breve corte de conexión | Estado local, caché y recuperación |

La webcam y la ubicación son demostraciones adicionales, si el equipo dispone
del hardware y los permisos. Preparar antes una foto de ejemplo y coordenadas
de un lugar público para disponer de alternativa. La captura y la localización
no envían nada hasta confirmarlo.

## Preparación de la sesión

- Crear las dos cuentas manualmente, con contraseñas propias; no distribuir
  credenciales personales ni una contraseña compartida en el repositorio.
- Usar nombres, avatares y conversaciones ficticios. Registrar las cuentas antes
  de la entrevista y comprobar que pueden entrar; la recuperación por correo
  todavía no está implementada.
- Abrir la app y probar un mensaje antes de empezar. El servicio gratuito puede
  tardar en responder tras estar inactivo; no presentar ese arranque como una
  medición del rendimiento del cliente.
- Tener a mano APK/MSI y un vídeo de respaldo grabado con la demo real. Este
  repositorio no incluye aún capturas o un vídeo verificados.

## Límites que se deben explicar

- Windows muestra los mensajes sin leer y menciones dentro de Chimera, sin
  avisos externos. Las notificaciones locales de Android dependen de que el
  proceso y la conexión sigan activos. No existe push garantizado con la app terminada.
- Se usa cifrado de mensajes en el servidor; no se ofrece cifrado de extremo a
  extremo. El backend puede descifrar contenido para devolverlo y buscarlo.
- «Enviado» confirma el guardado, no la lectura. «Leído» depende del estado de
  lectura del destinatario.
- La ubicación puede ser aproximada; se comparte un punto fijo. Los enlaces
  cortos de mapas y las direcciones de texto no se resuelven automáticamente.
- Los textos de borrador sobreviven al reinicio. Los adjuntos aún preparados en
  el editor sobreviven a la navegación de la sesión, no al fin del proceso.

## Si algo falla

| Problema | Comprobación |
| --- | --- |
| No conecta o el primer acceso tarda | Internet y estado del backend; esperar su arranque y reintentar |
| No aparece un contador | Probar con otro usuario enviando mensajes nuevos, con el destinatario fuera del canal |
| No hay webcam o está ocupada | Permisos de Windows, cerrar otras apps o adjuntar una foto existente |
| No obtiene ubicación | Permisos/proveedor del sistema; pegar coordenadas y revisar el mapa |
| No llega un aviso con la app cerrada | Es una limitación de esta demo; abrir la app para recuperar mensajes |

Las comprobaciones técnicas y manuales están en `release-checklist.md`.
