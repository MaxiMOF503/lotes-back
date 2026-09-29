# Catastro — parcelas — Luján de Cuyo

## Fuente

Geoportal municipal de Luján de Cuyo (GeoServer/GeoNode público):
`geoportal.lujandecuyo.gob.ar`, capa técnica `geonode:Parcelas_visor0`.

Descargado vía WFS (`GetFeature`, `outputFormat=application/json`,
`srsName=EPSG:4326`).

## Por qué está comprimido

`parcelas_lujan_de_cuyo.geojson.gz` son **65.756 parcelas** (`MultiPolygon`,
EPSG:4326) — el GeoJSON sin comprimir pesa ~78 MB, demasiado para
versionar en git sin comprimir. Comprimido con `gzip -9` queda en ~9 MB.

Para descomprimirlo:

```bash
gunzip -k parcelas_lujan_de_cuyo.geojson.gz   # -k conserva el .gz original
```

## Contenido

Cada parcela trae:

- `nomenclatu`: nomenclatura catastral.
- `codigo`, `zonificaci`: código y zona de zonificación (cruzable con
  `data/zonificacion/`).
- `tipo_lote`: `cuadra`, `esquina` o `indistinto` (posición de la parcela
  en la manzana, no tiene que ver con dominio público/privado).
- `suptotal`, `sup_real_m`, `sup_regl_m`: superficies (real vs. la que
  permite la reglamentación).
- `frenmetros`, `frente_min`: frente del lote y frente mínimo permitido.
- `fos_max`: Factor de Ocupación del Suelo máximo permitido.

**El campo `uso` viene vacío en el 100% de las filas** — no sirve para
saber si una parcela es fiscal o privada, ni su uso real. Para eso hay que
usar `data/loteos-definitivos/` (campo `caracteris`), que sí distingue
dominio público/privado, aunque solo para los 731 loteos que cubre esa
capa (no las 65.756 parcelas de acá).

## Cómo se usaría

Es el dato más fino de "forma del lote": complementa a `zonificacion/`
(que da la norma) y a `loteos-definitivos/` (que da la situación dominial)
con la geometría exacta de cada parcela individual. Sirve, por ejemplo,
para mostrar el polígono real del lote en el mapa en vez de solo un punto.

## Pendiente para integrarlo de verdad

- Por el volumen (65k filas), habría que cargarlo directo a MySQL con un
  script de importación (no un `ApplicationRunner` en el arranque de la
  app) — mismo criterio que se usó para líneas eléctricas.
- Evaluar si conviene generalizar/simplificar la geometría (menos vértices
  por polígono) si el objetivo es solo pintar el lote en el mapa y no
  hacer cálculos catastrales exactos.
