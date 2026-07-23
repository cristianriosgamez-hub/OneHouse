# OneHouse v1.5.0 — Entrega 1 Rev.2

## Acceso biométrico

- Activado el inicio de sesión mediante la biometría del dispositivo desde la pantalla de acceso.
- La aplicación comprueba si el terminal dispone de sensor biométrico y si existe una huella registrada.
- Tras una autenticación correcta se abre OneHouse sin solicitar nuevamente usuario y contraseña.
- Se conserva el acceso tradicional como alternativa.
- Se muestran mensajes específicos cuando no hay hardware, no existen huellas registradas o el sensor no está disponible.

## Temperatura exterior en Inicio

- La tarjeta **Terraza de estancia** muestra ahora la temperatura exterior obtenida del servicio meteorológico.
- El dato se carga al abrir la pantalla principal y se actualiza automáticamente cada 15 minutos mientras la pantalla permanece activa.
- También se muestran el estado meteorológico y su símbolo correspondiente.
- Cuando todavía no existe un dato válido, la tarjeta muestra un estado de carga o indisponibilidad en lugar de una temperatura fija.

## Versión

- `versionName`: `1.5.0`
- `versionCode`: `24`


## Revisión 2 — Corrección biométrica

- Sustituida la API de plataforma `android.hardware.biometrics` por la biblioteca compatible `androidx.biometric`.
- Añadida la dependencia `androidx.biometric:biometric:1.1.0` al catálogo de versiones y al módulo `app`.
- `MainActivity` hereda ahora de `FragmentActivity`, requisito del diálogo biométrico de AndroidX.
- Corregida la comprobación de disponibilidad mediante `BiometricManager.from(context)`.
- Corregidas las constantes de error y la construcción de `BiometricPrompt`.
- Eliminado el uso manual de `CancellationSignal`; AndroidX gestiona el ciclo de vida del diálogo.

## Entrega 2 — Primer control KNX real

- Añadida la representación validada de direcciones de grupo KNX de tres niveles (`KnxGroupAddress`).
- Añadidos telegramas `GroupValueRead` y `GroupValueWrite` booleano para DPT 1.xxx.
- Implementado el encapsulado cEMI dentro de `KNXnet/IP Tunnelling Request`.
- Implementada la validación de `KNXnet/IP Tunnelling ACK`, incluyendo canal, secuencia y estado.
- Añadido `LightDevice` para actuadores DPT 1.001 (Switch).
- La pantalla Inicio incorpora un primer control de prueba para la dirección `1/0/1`.
- El control selecciona automáticamente el endpoint local o remoto guardado según la red activa.
- El estado visual solo cambia cuando el servidor KNX/IP confirma la recepción del telegrama.

> Importante: `1/0/1` es una dirección inicial de prueba. Debe coincidir con la dirección de grupo de escritura configurada en ETS antes de accionar una carga real.
