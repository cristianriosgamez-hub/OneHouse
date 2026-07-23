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
