# Asentamientos informales — RENABAP — Luján de Cuyo

## Fuente

Registro Nacional de Barrios Populares (RENABAP), dataset nacional
descargado desde
`infra.datos.gob.ar/catalog/otros/dataset/3/distribution/3.2/download/barrios-populares.zip`
(no existe una capa municipal separada para esto — se usó la fuente
nacional y se filtró por `departamento = "LUJAN DE CUYO"`).

## Contenido

`asentamientos_renabap_lujan_de_cuyo.geojson` — 33 polígonos (`Polygon`,
EPSG:4326/WGS84), cada uno un barrio popular relevado por RENABAP:

- `idrenabap`: ID nacional del barrio (cruzable con el campo `id renabap`
  de `data/loteos-definitivos/`, que solo lo tiene completado en 29 de los
  731 registros — el solapamiento entre ambos datasets es parcial).
- `nombreBarr`: nombre del barrio.
- `provincia`, `departamento`, `localidad`.

Por definición, todo lo que aparece en RENABAP es una situación de
ocupación informal/irregular, no necesariamente sobre tierra fiscal — hay
que cruzarlo con `caracteris` de `loteos-definitivos` (vía `idrenabap`
donde exista) para saber si el suelo de abajo es público o privado.

## Cómo se usaría

Como capa de advertencia en el mapa: si un punto cae dentro de uno de
estos 33 polígonos, mostrar que está en un barrio popular relevado por
RENABAP, con la salvedad de que esto describe una situación social/legal
del barrio, no del lote individual.

## Pendiente para integrarlo de verdad

- Es un dataset chico (33 filas): se podría cargar completo sin problema.
- Cruzar con `id renabap` de `loteos-definitivos` para enriquecer ambos
  (RENABAP da nombre/ID oficial del barrio; loteos-definitivos da el
  detalle de regularización municipal/provincial/nacional).
