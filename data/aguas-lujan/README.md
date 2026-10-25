# Radio de servicio de Aguas Luján — Luján de Cuyo

## Fuente

Geoportal municipal de Luján de Cuyo (GeoServer/GeoNode público):
`geoportal.lujandecuyo.gob.ar`, capa técnica `geonode:radio_servicio_lujan`
(publicada como "Radio de Servicio de Aguas Luján").

Descargada el 24/10/2026 vía WFS (`GetFeature`, `outputFormat=application/json`,
`srsName=EPSG:4326`). La fecha de actualización del dato no figura en la capa.

## Contenido

`radio_servicio_lujan.geojson` — **3 polígonos** (`MultiPolygon`, EPSG:4326)
con los campos `zona` (SUR, NORTE y OESTE) y `superficie`. Son el área general
donde presta servicio el operador municipal Aguas Luján, **no** la traza de las
cañerías calle por calle.

## Cómo se usa

El importador (`scripts/importar_territorio.py`) la carga como capa `aguas_lujan`.
La consulta de un punto informa si cae dentro o fuera de este radio, igual que con
AySAM (`data/aysam-agua/`). Estar dentro no confirma conexión de agua, y estar fuera
no significa carecer de ella: puede haber otros prestadores.
