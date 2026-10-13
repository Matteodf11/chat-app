# Interfaz de servidores

El panel compartido de Android y escritorio organiza la información en General,
Canales y Miembros. Cada pestaña conserva su desplazamiento y los formularios
mantienen el texto al cambiar entre pestañas. La cabecera muestra el icono,
nombre y rol personal; los textos usan UTF-8 y «Dueño» incluye la ñ.

- General: nombre, descripción multilínea e icono con vista previa. El guardado
  solo se habilita con cambios y un nombre válido de 2 a 100 caracteres. Las
  selecciones de icono se validan y pueden deshacerse antes de guardar.
- Canales: búsqueda, apertura de un canal y creación para dueños/administradores.
- Miembros: búsqueda, presencia, rol y gestión mediante un panel inferior con
  avatar. Los permisos visibles conservan las reglas existentes: solo el dueño
  cambia roles; los administradores pueden expulsar miembros ordinarios.
- Salir, eliminar y expulsar tienen confirmaciones explícitas. La acción de
  eliminar queda separada de la edición habitual.
- Las invitaciones muestran el icono del servidor y botones con etiquetas para
  unirse o rechazar. Hay estados vacíos, de carga y reintento.

Las operaciones de escritura se bloquean frente a pulsaciones duplicadas. Guardar
no cierra el panel: una respuesta correcta muestra la confirmación y actualiza los
datos. Los formularios de canal e invitación se limpian únicamente mediante el
callback de éxito; los errores conservan lo escrito. Los cierres y controles de
escritura se bloquean durante la operación. Se muestran por separado los errores
de carga de la lista de servidores y de canales, con reintento.

La navegación incorpora búsqueda de servidores en móvil, búsqueda de canales en
ambas plataformas y una bienvenida en escritorio con descripción, rol y acceso
al primer canal. Reabrir el mismo servidor conserva su lista de canales y miembros.

No requiere migraciones ni cambios del backend.

## Verificación

Las pruebas del ViewModel cubren guardado fallido y reintento, pulsación duplicada
de invitación, callbacks exclusivos de éxito, creación de canales y recuperación
de errores al cargar servidores/canales. Se mantienen las pruebas anteriores de
roles, orden de miembros y sustitución de servidores sin duplicados.

Comprobaciones manuales pendientes en dispositivos: tamaños estrechos, orientación
horizontal y teclado abierto; nombres largos y descripciones multilínea; cambiar
entre pestañas con formularios preparados; elegir/cancelar/deshacer iconos; probar
con dueño, administrador y miembro; cancelar las confirmaciones de salir, eliminar
y expulsar; desconectar la red durante un guardado o invitación y reintentar.
# Mensajes sin leer y menciones

- Los servidores y canales muestran contadores con el color de tema y texto de alto contraste. `12 · @2` significa 12 mensajes sin leer en total, de los cuales 2 te mencionan. La barra estrecha de servidores usa `12 @2`. Los dos números se limitan visualmente a `99+`. La pestaña móvil Servidores muestra también un aviso.
- Al escribir `@` aparecen hasta cinco miembros con avatar. Se elige con clic o con las flechas ↑↓ y se completa con Tab; Escape cierra las sugerencias y Shift+Tab conserva la navegación de foco. La inserción respeta el cursor y el resto del borrador. Las sugerencias permanecen durante el clic aunque el editor pierda foco y lo recuperan tras insertar. Se pueden escribir menciones manualmente; el backend valida miembros reales y conserva sus identificadores. No hay menciones masivas.
- Los mensajes que te mencionan se resaltan y muestran «Te ha mencionado». Las menciones propias no generan avisos.
- El estado de lectura se guarda en el backend y se sincroniza entre dispositivos. Se avanza hasta el último mensaje confirmado con la app en primer plano y el final del chat visible; navegar por la lista de canales no los marca como leídos.
- Los eventos WebSocket invalidan los contadores y se agrupan brevemente antes de consultar una única respuesta de actividad. Hay reconciliación cada 30 segundos y al volver a primer plano, sin banners de sincronización.
- Editar o eliminar mensajes recalcula las menciones. Los reintentos no duplican avisos. Solo se cuentan mensajes de otros usuarios posteriores a la incorporación al servidor y al último mensaje leído.
- Requiere desplegar el backend con V20 y V21 antes de probar el cliente. Las migraciones se aplican con Flyway y protegen las tablas nuevas con RLS. Para miembros existentes se parte del momento del despliegue: no se etiqueta todo el historial antiguo como nuevo.
- Pendiente de validación manual con dos cuentas: leer en Android y comprobar los contadores en PC; mencionar desde un canal distinto; comprobar que al leer mensajes antiguos no se borran los avisos de los nuevos; comprobar el teclado y la inserción a mitad de un borrador.
