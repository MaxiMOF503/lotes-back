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
| `zonificacion/` | Completo | 143 zonas del Código de Ordenamiento Territorial (Ordenanza 14106-2021) |
| `zonas-pluviales/` | Completo (geometría) | 84 polígonos de riesgo aluvional — geometría válida, atributos de texto no confiables |
| `aysam-agua/` | Completo (aproximado) | Solo 2 polígonos de radio de servicio general, no traza de red calle por calle |
| `loteos-definitivos/` | Completo | 731 loteos/barrios con situación dominial (público/privado) y de regularización |
| `catastro-parcelas/` | Completo | 65.756 parcelas individuales (comprimido en `.gz` por tamaño) |
| `asentamientos-renabap/` | Completo | 33 barrios populares (fuente nacional RENABAP, no municipal) |
| `areas-protegidas/` | Pendiente | — sin datos geoespaciales descargables encontrados todavía |

## Por qué está separado así

Cada tema tiene su propia fuente, fecha de descarga y formato — igual
criterio que ya usa la ficha pública al distinguir la procedencia de cada
sección de datos. Cuando se integre alguno de estos datasets a una entidad
real, conviene documentar en el README de esa carpeta cómo se hizo la
importación, para que quede trazable.

## Sobre "tierra fiscal"

No hay (todavía) un dataset municipal dedicado exclusivamente a tierra
fiscal. La aproximación más cercana encontrada hasta ahora es el campo
`caracteris` (`PUBLICO` / `PUBLICO CON BARRERA` / `PRIVADO`) de
`loteos-definitivos/`, que distingue dominio público de privado — pero
solo cubre los 731 loteos de esa capa, no la totalidad del territorio.
Ver el README de esa carpeta para el detalle.
