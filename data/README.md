# Datos crudos (en progreso)

Esta carpeta reúne material de referencia recopilado de fuentes públicas
para Luján de Cuyo, pensado para alimentar futuras entidades/consultas del
backend. **No está integrado a la aplicación todavía**: `DatosDemostrativosInitializer`
sigue cargando únicamente el dataset demostrativo mínimo (B-01). Nada de lo
que hay acá se ejecuta automáticamente al levantar la app.

## Estado por tema

| Carpeta | Estado | Contenido |
|---|---|---|
| `lineas-electricas/` | Completo | Dump MySQL con ~13.500 tramos de líneas eléctricas de EDEMSA para Luján de Cuyo |
| `zonas-pluviales/` | Pendiente | — |
| `zonificacion/` | Pendiente | — |
| `aysam-agua/` | Pendiente | — |

## Por qué está separado así

Cada tema tiene su propia fuente, fecha de descarga y formato — igual
criterio que ya usa la ficha pública al distinguir la procedencia de cada
sección de datos. Cuando se integre alguno de estos datasets a una entidad
real, conviene documentar en el README de esa carpeta cómo se hizo la
importación, para que quede trazable.
