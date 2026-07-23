# OneHouse v1.4.2 — Entrega 2

## Selección automática de conexión KNX/IP

- La pantalla de configuración detecta la red activa del teléfono.
- WiFi y Ethernet seleccionan automáticamente la dirección KNX/IP local.
- Los datos móviles y otras redes seleccionan automáticamente la dirección remota.
- Cuando no existe conexión de red, la prueba queda bloqueada con un mensaje claro.
- Se muestran la red actual, la ruta elegida, el destino activo y la hora de la última prueba.

## Prueba KNXnet/IP real

- La prueba de conexión ya no se limita a solicitar la descripción del dispositivo.
- Se abre un canal KNXnet/IP Tunnelling mediante Connect Request.
- El canal se cierra inmediatamente después de validar la respuesta.
- Se distinguen los errores de red, tiempo agotado, respuesta inválida y rechazo del túnel.
- La prueba no envía telegramas ni órdenes al bus KNX.

## Archivos modificados

- `app/src/main/java/com/onehouse/app/knx/KnxConnectionTester.kt`
- `app/src/main/java/com/onehouse/app/feature/settings/SettingsViewModel.kt`
- `app/src/main/java/com/onehouse/app/feature/settings/SettingsScreen.kt`
- `CHANGELOG_v1.4.2.md`
