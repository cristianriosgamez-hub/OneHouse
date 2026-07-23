# OneHouse v1.4.2 — Entrega 3

## Persistencia completa de la configuración KNX/IP

- Se mantienen la IP y el puerto local, la IP y el puerto remoto y el modo de selección automática.
- Se guarda también el resultado de la última prueba, su mensaje y la fecha y hora en la que se realizó.
- Al volver a abrir la aplicación, la pantalla recupera el último estado conocido sin marcar una prueba interrumpida como conectada.

## Gestión de cambios de red

- El ViewModel observa continuamente la red activa.
- Un cambio entre WiFi, Ethernet, datos móviles o ausencia de red cancela de forma segura una prueba en curso.
- Al cambiar la ruta activa, el estado queda pendiente de comprobar para evitar mostrar como válida una conexión realizada por otra ruta.

## Motor de prueba preparado para reutilización

- Se introduce `KnxEndpoint`, un modelo común de host y puerto reutilizable por el futuro motor de telegramas KNX.
- `KnxConnectionTester` permite configurar el tiempo de espera y mantiene compatibilidad con la llamada anterior por host y puerto.
- La prueba sigue abriendo y cerrando un túnel KNXnet/IP real sin enviar telegramas al bus.

## Pantalla de configuración

- El estado KNX incluye el mensaje detallado de la última prueba.
- La fecha de la última prueba permanece disponible después de cerrar y volver a abrir la aplicación.
- La suscripción al ViewModel se libera correctamente al abandonar la pantalla.

## Archivos modificados

- `app/src/main/java/com/onehouse/app/data/knx/KnxSettings.kt`
- `app/src/main/java/com/onehouse/app/data/knx/SettingsDataStore.kt`
- `app/src/main/java/com/onehouse/app/knx/KnxConnectionTester.kt`
- `app/src/main/java/com/onehouse/app/feature/settings/SettingsViewModel.kt`
- `app/src/main/java/com/onehouse/app/feature/settings/SettingsScreen.kt`
- `CHANGELOG_v1.4.2.md`
