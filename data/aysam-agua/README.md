# Cobertura de agua (AySAM) — Luján de Cuyo

## Fuente

Geoportal municipal de Luján de Cuyo (GeoServer/GeoNode público):
`geoportal.lujandecuyo.gob.ar`, capa técnica `geonode:radio_servicio_aysam`.
AySAM es la empresa provincial de agua y saneamiento de Mendoza.

Descargado vía WFS (`GetFeature`, `outputFormat=application/json`,
`srsName=EPSG:4326`).

## Contenido

`radio_servicio_aysam_lujan_de_cuyo.geojson` — apenas **2 polígonos**
(`MultiPolygon`, EPSG:4326), ambos con `operador: AYSAM`. Son el radio de
servicio general de AySAM en el departamento, no la traza de la red de agua
potable calle por calle.

**Importante para lo que se pidió del mapa:** esto NO alcanza para decir
"esta cuadra tiene agua, esta otra no" — solo dice si un punto cae dentro
del área general donde AySAM presta servicio en Luján de Cuyo. Es un dato
mucho más grueso que, por ejemplo, la capa de líneas eléctricas (que sí
tiene la traza real tramo por tramo).

## Cómo se usaría

Cruzar la coordenada del lote contra estos 2 polígonos (`ST_Contains`) para
completar `LoteEntity.tieneCoberturaAgua` con "está dentro del radio de
servicio de AySAM" en vez de la constante demostrativa — con la advertencia
de que es una aproximación por zona, no una confirmación de conexión real.

## Pendiente para integrarlo de verdad

- Buscar si AySAM publica la traza real de la red (más fina que el radio de
  servicio) para un dato más preciso.
- Si no aparece, documentar en `AguaResponse`/`ProcedenciaDatos` que el
  dato es "dentro del radio de servicio", no "con conexión confirmada".
