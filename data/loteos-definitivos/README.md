# Loteos / barrios (formales, informales y populares) — Luján de Cuyo

## Fuente

Geoportal municipal de Luján de Cuyo (GeoServer/GeoNode público):
`geoportal.lujandecuyo.gob.ar`, capa técnica
`geonode:loteos_definitivos_11032024`.

Descargado vía WFS (`GetFeature`, `outputFormat=application/json`,
`srsName=EPSG:4326`).

## Ojo con el nombre de la capa: no es solo "loteos definitivos"

El nombre técnico de la capa (`loteos_definitivos`) es engañoso: al mirar
los atributos reales, esto es un **registro de barrios/loteos del
departamento clasificados por su situación dominial y de regularización**,
no solo subdivisiones formales aprobadas. De 731 polígonos:

| `tipo` | Cantidad |
|---|---|
| `FORMAL` | 463 |
| `INFORMAL EN REGULARIZACION` | 133 |
| `INFORMAL` | 93 |
| `POPULAR` | 41 |
| `EDITAR` (fila basura, ignorar) | 1 |

Y por `caracteris` (carácter del dominio del suelo):

| `caracteris` | Cantidad |
|---|---|
| `PRIVADO` | 434 |
| `PUBLICO` | 269 |
| `PUBLICO CON BARRERA` | 21 |
| (vacío) | 6 |
| `EDITAR` (fila basura) | 1 |

**Esto es justo lo que se buscaba para la pregunta de "qué es un terreno
fiscal / hay dataset de eso": `caracteris = PUBLICO` (o `PUBLICO CON
BARRERA`) marca que el suelo de ese loteo es del Estado — es decir, es la
aproximación más cercana a "tierra fiscal" que se encontró hasta ahora.
No es un catastro completo de TODA la tierra fiscal del departamento (solo
cubre los loteos/barrios que están en este registro), pero para los 731
polígonos que trae, sí distingue dominio público de privado.**

Otros campos relevantes por fila:
- `nom_barrio`, `distritos`, `codigo`: identificación del barrio/loteo.
- `parc_regul`: cantidad de parcelas regularizadas dentro del loteo.
- `regul_muni` / `regul_prov` / `regul_nac` / `regul_domi`: estado de
  regularización municipal, provincial, nacional y dominial (ej.
  `"Por Renabap"`, `"Plan nacional de hábitat"`, `"Relocalización"`).
- `id renabap`: 29 de los 731 loteos tienen un ID que los vincula al
  Registro Nacional de Barrios Populares (RENABAP) — hay overlap parcial
  con `data/asentamientos-renabap/`, pero no son el mismo dataset (este es
  municipal y más amplio; RENABAP es nacional y solo cubre asentamientos
  informales).
- `ordenanza`, `decreto`, `expte_lote`: normativa de aprobación, cuando
  existe (mayormente vacío para los informales/populares, como es
  esperable).

## Cómo se usaría

- Para "es tierra fiscal": filtrar por `caracteris IN ('PUBLICO','PUBLICO CON BARRERA')`.
- Para completar el contexto de un lote en la ficha pública: si el punto
  cae dentro de un polígono con `tipo != 'FORMAL'`, se podría mostrar una
  advertencia de que está en un barrio en proceso de regularización.

## Pendiente para integrarlo de verdad

- Confirmar con el resto del equipo si conviene modelar esto como una
  entidad nueva (`LoteoEntity` o similar) en vez de mezclarlo con
  `zonificacion`.
- Decidir si `caracteris` (público/privado) amerita un campo propio en
  `LoteEntity` para responder directamente "¿es tierra fiscal?".
