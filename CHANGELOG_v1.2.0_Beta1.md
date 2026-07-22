# OneHouse v1.2.0 Beta 1 — Energy Foundation

Primera entrega de la actualización Energy Update.

## Cambios

- Nuevo modelo `EnergyOverview` para centralizar el resumen energético anual.
- Cálculo separado de electricidad (`kWh`) y agua (`m³`), evitando mezclar unidades incompatibles.
- Resumen de coste anual, contadores con datos, lecturas del año y última actualización.
- El `EnergyViewModel` publica el nuevo resumen junto con las tarjetas existentes.
- Nueva tarjeta principal en el Centro energético con electricidad, agua, coste y estado de actualización.
- Se mantiene intacta la gestión de lecturas, navegación, persistencia Room y comunicación KNX.

## Verificación

- `git diff --check`: correcto.
- No fue posible ejecutar `:app:compileDebugKotlin` porque el wrapper requiere descargar Gradle 9.5.0 y el entorno no dispone de acceso a Internet.
