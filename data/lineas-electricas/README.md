# Líneas eléctricas — Luján de Cuyo

## Fuente

EDEMSA — Portal de Datos Abiertos de Energía (`datos.energia.gob.ar`),
recorte geográfico de Luján de Cuyo.

## Contenido del dump

`lineas_electricas_lujan_de_cuyo.sql` es un `mysqldump` completo de una
base local usada para preparar y verificar la importación. Incluye:

- `lineas_electricas`: ~13.500 tramos reales (`MULTILINESTRING`, SRID 4326),
  con tensión (66kV/132kV) y `fuente_datos` completada.
- `departamentos`: un registro de Luján de Cuyo (mismo formato que usa
  `DepartamentoEntity`).
- `lotes`, `roles`, `usuarios`: son las mismas tablas y el mismo dato
  demostrativo que ya genera `DatosDemostrativosInitializer` — están en
  el dump porque forman parte del esquema de la base de prueba, **no son
  datos nuevos**.

## Cómo se generó

Datos descargados del portal nacional de datos abiertos de energía,
filtrados a la zona de Luján de Cuyo, e importados a una base MySQL local
para verificar geometría (SRID 4326) y consistencia antes de compartirlos.

## Pendiente para integrarlo de verdad

- Confirmar si conviene cargarlo completo o resumirlo (13.500 filas es
  mucho más que el dataset demostrativo actual).
- Definir si se carga vía un `ApplicationRunner` nuevo (como el de B-01)
  o vía un script de importación aparte, dado el volumen.
- Decidir si `LineaElectricaEntity` necesita algún campo adicional
  (ej. tensión) que hoy no tiene mapeado explícitamente en el modelo,
  más allá de `tipo`.
