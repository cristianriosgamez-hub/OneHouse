# OneHouse v1.4.0 — KNX

## Configuración KNX

- Añadida una pantalla **Más** preparada para incorporar nuevas herramientas en futuras versiones.
- La opción **Configuración** abre una pantalla independiente de configuración KNX.
- Añadidos los campos de conexión local y secundaria: dirección IP y puerto.
- Añadida validación de direcciones IP y puertos.
- La configuración se guarda localmente mediante `SharedPreferences`.
- Añadidos el estado de configuración, la versión de la aplicación y la información del dispositivo.
- Añadida la opción de reconexión automática.
- Añadido el botón **Probar conexión**, que en esta versión valida la configuración. La comunicación KNX real se implementará en una versión posterior.
- Los campos de usuario y contraseña se han retirado temporalmente.

## Navegación

- El botón inferior **Más** ya no abre directamente Configuración.
- Desde **Más** se puede acceder a Configuración.
- Se han reservado accesos futuros para Acerca de, Diagnóstico, Manual, Copias de seguridad y Herramientas.
