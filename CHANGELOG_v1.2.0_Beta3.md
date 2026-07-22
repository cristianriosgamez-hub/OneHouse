# OneHouse v1.2.0 Beta 3 — Energy Analytics

## Novedades

- Nuevo panel de evolución eléctrica de los últimos doce meses.
- Comparativa automática frente al mes anterior.
- Gráfica con área, tendencia y referencias temporales.
- Consolidación de ENDESA y climatización en kWh para evitar mezclar unidades.
- Extracción de las gráficas a `EnergyCharts.kt` para facilitar su reutilización.
- Se mantiene la gráfica detallada por contador y periodo.

## Archivos modificados

- `ConsumptionScreen.kt`
- `EnergyDashboardComponents.kt`
- `EnergyViewModel.kt`
- `EnergyCharts.kt` (nuevo)
