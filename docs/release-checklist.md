# Candidato de demostración 1.1.1

La versión tiene funciones implementadas y paquetes locales. No se declara
cerrada hasta completar las comprobaciones manuales pendientes.

| Comprobación | Estado y evidencia |
| --- | --- |
| Pruebas del cliente | 104 pruebas pasan en el último recorrido local |
| Pruebas del backend | 57 pruebas pasan en el recorrido previo al commit e9e4bcd |
| APK debug y MSI | Compilación local correcta de 1.1.1 |
| Captura básica con webcam en PC | Confirmada por el usuario |
| Menciones por clic/Tab y contadores combinados | Implementados; clic y contadores confirmados durante las iteraciones |
| Ubicación automática en Windows | Implementada y cubierta con proveedor falso; prueba real pendiente |
| Cancelar, cambiar webcam y desconexión de hardware | Casos de sesión probados con cámaras falsas; prueba real pendiente |
| Instalar en un equipo sin Java | Pendiente |
| Actualizar el MSI anterior a 1.1.1 | Pendiente |
| Enviar/recibir texto, fotos y voz Android ↔ Windows | Pendiente de recorrido final documentado con dos cuentas |
| Cortar/restaurar red y reintentar sin duplicados | Pruebas automáticas; recorrido final con dispositivos pendiente |
| Recuperar borrador y cola pendiente tras reinicio | Pruebas automáticas; recorrido final con dispositivos pendiente |
| Notificaciones, apertura del chat y cierre a bandeja | Pendiente de recorrido final con instalador |
| GitHub Actions del cliente | Workflow preparado; ejecución remota pendiente de subir los cambios |
| Capturas y vídeo reales para README/demo | Pendientes; no sustituir por maquetas |
| Recuperación por correo | Aplazada por decisión del usuario hasta cerrar escritorio |

## Recorrido manual final

Registrar fecha, sistema operativo, versión y resultado de cada caso. «Pendiente»
no significa fallo, pero tampoco cuenta como verificado.

1. Instalar/actualizar, entrar en las dos cuentas y cambiar de conversaciones.
2. Enviar texto, respuesta, foto vertical, varias fotos, documento y nota de voz
   en ambas direcciones; comprobar visor, descarga y reproducción.
3. Leer historial mientras llegan mensajes, después volver al final. Repetir con
   una foto alta cerca del último mensaje y revisar que no aparezcan saltos.
4. Crear servidor, aceptar invitación, cambiar de canal y mencionar a otro miembro
   por clic y Tab. Probar un miembro sin permisos contra operaciones de admin.
5. Comprobar `N · @M`, leer desde el otro dispositivo y verificar la actualización.
6. Cortar red brevemente: escribir, reintentar y recuperar la conexión; comprobar
   que un mismo envío no se duplica y que el editor conserva lo no aceptado.
7. Probar webcam y ubicación: éxito, cancelación, reintento y permiso denegado.
8. Cerrar y abrir, revisar borradores, cerrar sesión y volver a entrar. No usar
   una cuenta distinta para reintentar envíos de otra cuenta.

## Criterio de cierre

Se podrán presentar los paquetes como versión de demostración cuando el recorrido
principal entre dispositivos y la instalación estén verificados, y cualquier
limitación restante aparezca en la guía. Recuperación por correo y la decisión
de publicar una release pública se mantienen como pasos separados.
