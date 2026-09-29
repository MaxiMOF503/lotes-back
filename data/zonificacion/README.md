# Zonificación / uso de suelo — Luján de Cuyo

## Fuente

Geoportal municipal de Luján de Cuyo (GeoServer/GeoNode público):
`geoportal.lujandecuyo.gob.ar`, capa técnica `geonode:Zonas`, publicada a
partir de la Ordenanza 14106-2021 (Código de Ordenamiento Territorial).

Descargado vía WFS (`GetFeature`, `outputFormat=application/json`,
`srsName=EPSG:4326` para que las coordenadas vengan en lat/lon).

## Contenido

`zonificacion_lujan_de_cuyo.geojson` — 143 polígonos (`MultiPolygon`,
EPSG:4326). Cada polígono es una zona homogénea dentro de un distrito, con:

- `distrito_`: distrito del departamento (ej. `CIUDAD`, `LAS COMPUERTAS`).
- `zona_` / `codigo`: nombre y código de zonificación (ej. `RESIDENCIAL 4` /
  `ZR4`, `RESERVA VITIVINICOLA` / `ZRVIT`, `COMERCIAL` / `ZC`).
- `lote_min`, `lado_min`: superficie y frente mínimo permitido para
  subdividir (m² y metros).
- `fos`: Factor de Ocupación del Suelo (% del lote que se puede construir).
- `retiro_fro` / `retiro_lat` / `retiro_pos`: retiros obligatorios (m).
- `alt_max`: altura máxima permitida (m).
- `ordenanza`: norma que la establece.
- `sup_m2`: superficie total de la zona.

## Cómo se usaría

Es la fuente natural para completar `LoteEntity.zonificacion` con un dato
real en vez de la constante demostrativa: dado un punto (lat/lon), un
`ST_Contains` contra estos polígonos da la zona real, su `fos`, retiros y
altura máxima — información que hoy la ficha pública no muestra.

## Pendiente para integrarlo de verdad

- Decidir si se carga completo (143 filas, es chico) vía un
  `ApplicationRunner` o script de importación.
- Sumar los campos de `fos`/retiros/altura a la ficha pública si se
  considera relevante para el usuario final (hoy `ZonificacionResponse`
  solo expone el nombre de la zona).
