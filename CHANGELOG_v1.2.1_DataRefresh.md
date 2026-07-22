# OneHouse v1.2.1 — Data Refresh

## Datos de consumo

- Sustituido el histórico de consumo por los valores corregidos del libro `ConsumoLecturas_14-15-16-17-18-19-20-21-22-23-24-25-26`.
- Actualizadas las lecturas de ACS, climatización, ENDESA y AGBAR desde febrero de 2014 hasta junio de 2026.
- Incorporados los costes disponibles de ENDESA y AGBAR.
- Eliminados registros de arrastre sin fecha de lectura y proyecciones que no pertenecían al histórico confirmado.

## Actualización de la base de datos

- Añadido un sembrado versionado del histórico energético.
- Al abrir la pantalla de consumos, los registros importados desde Excel se sustituyen una sola vez por el nuevo conjunto de datos.
- Las lecturas creadas manualmente por el usuario se conservan.
- La sustitución de datos importados se realiza dentro de una transacción Room.

## Versión

- `versionCode`: 21
- `versionName`: 1.2.1
