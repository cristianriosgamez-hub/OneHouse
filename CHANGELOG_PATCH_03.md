# OneHouse v1.1.9 Premium UI — Patch 03

## Objetivo
Refinar el sistema visual compartido sin modificar la lógica de negocio ni la navegación.

## Cambios

- Animación táctil en `OneHouseCard` y `OneHouseSecondaryCard`.
- Escala y elevación animadas al pulsar, con respuesta suave tipo spring.
- Sombra de acento azul muy sutil en las tarjetas principales.
- Escala tipográfica ampliada para titulares y pantallas futuras.
- Mejor definición de `outline`, `outlineVariant`, `surfaceTint` y `scrim` en Material 3.

## Archivos modificados

- `app/src/main/java/com/onehouse/app/design/OneHouseComponents.kt`
- `app/src/main/java/com/onehouse/app/ui/theme/Theme.kt`
- `app/src/main/java/com/onehouse/app/ui/theme/Type.kt`

## Validación recomendada

1. Abrir Home, Login y cualquier pantalla que use tarjetas compartidas.
2. Pulsar varias tarjetas y comprobar que la escala vuelve a 1.0 sin saltos.
3. Revisar contraste y bordes en modo oscuro.
4. Ejecutar `Build > Make Project` antes de hacer commit.
