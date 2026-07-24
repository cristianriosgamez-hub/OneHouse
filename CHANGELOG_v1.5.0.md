# OneHouse v1.5.0

## Entrega 3 — Importador InsideControl

- Añadido descifrado compatible con proyectos `.knx` creados por InsideControl Builder 2.0.10.
- Añadido parser del formato propietario de habitaciones y dispositivos.
- Conversión automática de direcciones internas `9:1` al formato KNX `1/1/1`.
- Clasificación inicial de iluminación, persianas, clima, sensores, alarmas, escenas y mediciones.
- Nueva pantalla **Más → Importar InsideControl**.
- Selector de archivos mediante el almacenamiento del dispositivo.
- Resumen de habitaciones, objetos y direcciones únicas.
- Filtros por categoría y detalle de lectura, escritura, DPT, unidad y tipo original.
- Persistencia local del último proyecto importado.

### Alcance

Esta entrega permite visualizar y conservar la configuración importada. La activación de los objetos importados mediante telegramas KNX se incorporará en una entrega posterior.

## v1.5.0 – Entrega 3 Rev.2

- Corregidas las funciones `import(...)` de `InsideControlImporter` para usar cuerpo de bloque.
- Eliminados los `return` incompatibles con funciones declaradas mediante cuerpo de expresión.
- Sustituidas referencias ambiguas a funciones de `String` por lambdas explícitas en el parser.
- Añadidas validaciones para archivos vacíos y contenido Base64 sin datos.
- Mejorados los mensajes de error al abrir, descifrar o interpretar proyectos InsideControl.

## Entrega 4 — Catálogo de dispositivos importados

- Añadido `ImportedKnxDevice`, modelo normalizado para utilizar los objetos de InsideControl en OneHouse.
- Añadido `KnxDeviceFactory`, que valida las direcciones y determina el control adecuado para cada objeto.
- Clasificación de controles como interruptor, persiana, climatización, escena, sensor, alarma, medición o solo lectura.
- Añadido buscador por nombre, habitación, categoría, DPT o dirección KNX.
- Agrupación de todos los objetos importados por habitación.
- Añadidos contadores de objetos, direcciones y elementos controlables.
- Los filtros muestran el número de objetos de cada categoría.
- La ficha desplegable muestra habitación, control OneHouse previsto, lectura, escritura, DPT, unidad, favorito y tipo original.

### Alcance

Esta entrega convierte la configuración importada en un catálogo normalizado y preparado para generar controles KNX. El envío de telegramas desde estos controles se incorporará en la siguiente entrega.

## Entrega 5 Rev.2

- Corregido el cierre al abrir el selector de archivos en determinados dispositivos Xiaomi/HyperOS.
- El permiso persistente del proveedor de documentos ahora es opcional y está protegido frente a excepciones.
- La selección cancelada vuelve de forma segura al estado inicial.
- La lectura, descifrado y parseo se ejecutan fuera del hilo principal.
- Añadidos estados de selección, importación, éxito y error.
- Añadido indicador de progreso y nombre del archivo durante la importación.
- El botón de selección queda deshabilitado mientras existe una operación en curso.

## Entrega 5 Rev.3

- Corregido el error `Can only use lower 16 bits for requestCode` al abrir el selector de archivos `.knx`.
- Añadida una versión moderna y explícita de AndroidX Fragment (`fragment-ktx 1.8.5`) para hacer compatible `FragmentActivity` con Activity Result API.
- Se mantiene `ActivityResultContracts.OpenDocument`, sin códigos de petición manuales ni APIs obsoletas.
- Conservada la compatibilidad con la autenticación biométrica basada en `FragmentActivity`.

## Entrega 6 — Control de dispositivos importados

- Añadida pantalla de control KNX accesible desde cada objeto importado.
- Habilitado el envío real de `GroupValueWrite` para luces e interruptores DPT 1.x.
- Habilitada la solicitud `GroupValueRead` para objetos con dirección de lectura.
- Añadido `KnxCommandExecutor`, que selecciona automáticamente la ruta KNX/IP local o remota configurada.
- Añadidos estados visibles de conexión, envío, confirmación y error.
- Los DPT todavía no codificados se bloquean de forma segura para evitar telegramas incorrectos.
- Eliminado de la pantalla principal el cuadro temporal **Primer control KNX** y su dirección de prueba fija.

## Entrega 7

- El proyecto InsideControl importado permanece disponible al reiniciar la aplicación.
- Nueva navegación por habitaciones plegables para listas grandes.
- Iconos visuales según el tipo de dispositivo KNX.
- Ficha de dispositivo ampliada con estado conocido y origen del estado.
- Caché persistente de estados KNX enviados localmente.
- Infraestructura preparada para integrar respuestas `GroupValueResponse` del bus mediante `KnxDeviceStateRepository.updateFromBus()`.
- Se mantiene la lectura `GroupValueRead` desde la pantalla de control.
