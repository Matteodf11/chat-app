# Retoma del proyecto: 9 de octubre de 2026

## Cambios encontrados

Git mostraba 16 rutas modificadas/eliminadas y 28 archivos nuevos en el cliente.
La mayor parte correspondía al trabajo previo de esta conversación:

- Separación de MainScreen en áreas y diálogos.
- Estados coherentes de chat, amigos y servidores, con reductores comprobables.
- Repositorios de autenticación, perfiles, amigos, servidores y chat.
- Inyección de dependencias manual en AppContainer.
- Fuentes de eventos WebSocket especializadas.
- Manejo de errores de autenticación y pruebas.

Los otros dos cambios eran el tema Nebula y sus componentes. No compilaban.
Se han conservado completos en docs/experiments/nebula como archivos .kt.txt.
El tema funcional se ha recuperado de HEAD, conservando la refactorización.

## Compatibilidad con backend

Las listas REST existentes siguen devolviendo arrays. La paginación nueva del
backend requiere paged=true. Los APK ya instalados conservan el contrato anterior.
El lector de errores soporta tanto el formato antiguo como el nuevo objeto con
message y details, y da prioridad al mensaje concreto o a los errores de campos.

## Verificación

Comando: .\gradlew.bat :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --no-daemon

24 pruebas del cliente. Desktop y Android compilados. APK debug regenerado.
No se ha probado una sesión interactiva contra producción.

## Recuperación

Antes de editar se copiaron todos los cambios y un diff binario a:

C:\Users\Matteo\Documents\Proyecto Chat\.recovery\2026-10-09-before-cleanup\chat-app

La copia es local y no forma parte de los commits.
