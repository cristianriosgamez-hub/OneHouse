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

## Entrega 5 — Modelo de comandos KNX

- Añadido `KnxCommand`, modelo normalizado de acciones KNX independientes del transporte.
- Añadido `KnxDptResolver`, que normaliza el DPT original y propone uno compatible cuando InsideControl no lo guardó.
- Añadido `KnxCommandBuilder`, que genera automáticamente los comandos disponibles para cada dispositivo.
- Luces preparadas para lectura, encendido, apagado y alternancia.
- Persianas preparadas para subir, bajar, parar y posición porcentual.
- Climatización preparada para escritura de valor y escenas para ejecución numerada.
- Sensores y objetos de solo lectura conservan el comando de lectura cuando existe dirección válida.
- `ImportedKnxDevice` incorpora DPT resuelto y lista de comandos.
- La ficha desplegable muestra DPT original, DPT resuelto y comandos disponibles.
- El buscador también localiza objetos por DPT resuelto o nombre de comando.

### Alcance

Esta entrega prepara todos los objetos importados para su conexión con `KnxConnectionManager`. Los controles aún no envían telegramas desde la pantalla de importación; el envío real se incorporará en la Entrega 6.
