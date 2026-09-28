# Zonas pluviales / riesgo aluvional — Luján de Cuyo

## Fuente

Geoportal municipal de Luján de Cuyo (GeoServer/GeoNode público):
`geoportal.lujandecuyo.gob.ar`, capa técnica `geonode:aluvional`.

Descargado vía WFS (`GetFeature`, `outputFormat=application/json`,
`srsName=EPSG:4326`).

## Contenido

`riesgo_aluvional_lujan_de_cuyo.geojson` — 84 polígonos (`MultiPolygon`,
EPSG:4326) que delimitan zonas de riesgo aluvional/hídrico del
departamento.

**Aviso de calidad del dato:** los atributos (`name`, `descriptio`) vienen
vacíos o con metadata genérica de plantilla ("Lake, < 0.5 sq. mi.") — son
residuos de la capa base con la que se armó el shapefile original en el
geoportal, no describen el riesgo real de cada polígono. **La geometría es
válida y utilizable, pero no hay que confiar en los atributos de texto.**
Si se necesita saber el nivel/tipo de riesgo de cada zona, hay que
contrastar esto con el Departamento General de Irrigación (DGI), que es
quien produce el dato oficialmente (`datosabiertos.mendoza.gov.ar`,
organismo DGI) — no se llegó a bajar esa versión todavía.

## Cómo se usaría

Cruzar la coordenada del lote contra estos polígonos (`ST_Contains`) para
marcar si está dentro de una zona de riesgo aluvional, como un dato más de
la ficha pública (con su propia `ProcedenciaDatos`, ya que la fuente es
distinta a la del resto).

## Pendiente para integrarlo de verdad

- Conseguir la versión de DGI con atributos reales de riesgo (bajo/medio/alto).
- Hasta entonces, esta capa solo sirve para "está en zona de riesgo: sí/no",
  no para clasificar el nivel de riesgo.
