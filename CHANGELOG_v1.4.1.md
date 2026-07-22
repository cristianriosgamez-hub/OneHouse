# OneHouse v1.4.1 — Prueba de conexión KNX/IP

## Añadido

- Prueba real y no intrusiva del interfaz KNX/IP desde **Configuración → Probar conexión KNX/IP**.
- Envío de una petición `KNXnet/IP Description Request` por UDP a la IP y puerto configurados.
- Espera de respuesta durante 3 segundos y validación de la cabecera `KNXnet/IP Description Response`.
- Mensajes diferenciados para conexión correcta, tiempo de espera, respuesta no válida y error de red.

## Seguridad

- La prueba no abre un túnel KNX.
- No envía telegramas al bus.
- No acciona luces, persianas, climatización ni escenas.
