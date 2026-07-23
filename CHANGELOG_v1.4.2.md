# OneHouse v1.4.2 — Entrega 4 (cierre)

## Motor KNXnet/IP reutilizable

- Se añade `KnxConnectionManager`, responsable de abrir, mantener y cerrar un túnel KNXnet/IP.
- La conexión conserva el socket UDP y el identificador de canal mientras permanece activa.
- El cierre intenta enviar `Disconnect Request` antes de liberar el socket.
- Se centralizan en `KnxProtocol` los tiempos de espera, tamaños de paquete y construcción/validación de mensajes de conexión.

## Compatibilidad de la prueba de conexión

- `KnxConnectionTester` pasa a utilizar internamente `KnxConnectionManager`.
- La pantalla de configuración mantiene el mismo comportamiento y API.
- Una prueba correcta abre un túnel real y lo cierra inmediatamente después.

## API preparada para OneHouse v1.5.0

`KnxConnectionManager` deja definidos los puntos de entrada:

- `connect()`
- `disconnect()`
- `isConnected`
- `sendTelegram()`
- `readGroupValue()`

En v1.4.2 solo queda habilitado el ciclo de vida del túnel. El envío cEMI, la codificación DPT y las lecturas de direcciones de grupo se implementarán en v1.5.0; hasta entonces esos dos métodos devuelven un resultado explícito `NotAvailable` y nunca simulan una escritura en el bus.

## Versión de la aplicación

- `versionName`: `1.4.2`
- `versionCode`: `23`

## Archivos modificados

- `app/src/main/java/com/onehouse/app/knx/KnxConnectionManager.kt` (nuevo)
- `app/src/main/java/com/onehouse/app/knx/KnxConnectionTester.kt`
- `app/build.gradle.kts`
- `CHANGELOG_v1.4.2.md`
