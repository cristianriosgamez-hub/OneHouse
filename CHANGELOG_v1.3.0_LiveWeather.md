# OneHouse v1.3.0 — Live Weather

## Meteorología exterior real

- Sustitución de los datos meteorológicos de demostración por información real de Open-Meteo.
- Condiciones actuales para L'Hospitalet de Llobregat.
- Temperatura, sensación térmica, humedad, precipitación, viento, visibilidad y presión en tiempo real.
- Calidad del aire mediante Open-Meteo Air Quality.
- Índice UV diario, amanecer, atardecer y duración de la luz solar.
- Pronóstico real de cinco días.
- Traducción de códigos WMO a descripciones e iconos meteorológicos.

## Integración entre escenas

- La cabecera de Climatización muestra ahora la temperatura y condición exterior reales.
- La tarjeta de clima exterior de Terraza usa los mismos datos meteorológicos reales.
- Caché compartida de 15 minutos para evitar llamadas repetidas al abrir distintas escenas.
- Conservación del último dato válido si una actualización de red falla.

## Plataforma

- Permiso de acceso a Internet añadido al manifiesto.
- Sin claves API ni dependencias externas adicionales.
- `versionCode`: 22
- `versionName`: 1.3.0
