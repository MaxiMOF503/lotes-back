# Estadísticas de uso

El rol ADMIN consulta contadores diarios anónimos de visitas y consultas. No hay datos de personas ni de lo que se buscó.

## Qué cuenta

- **Visita:** una carga de la aplicación (`POST /api/public/v1/visitas`). Recargar suma otra; cambiar de panel no suma.
- **Consulta:** una respuesta del servidor a una consulta real sobre el catastro, clasificada por cómo se llegó a la parcela:

| Criterio | Qué consulta lo genera |
| --- | --- |
| `COORDENADAS` | Clic en el mapa: `GET /api/public/v1/territorio/consulta`. También se genera al abrir una parcela elegida desde un buscador, porque la ficha se arma con esa misma consulta. |
| `NOMENCLATURA` | Búsqueda por nomenclatura: `GET /api/public/v1/territorio/parcelas`. |
| `DIRECCION` | Búsqueda de las parcelas cercanas a una dirección elegida: `GET /api/public/v1/territorio/parcelas/cercanas`. Las sugerencias mientras se escribe no cuentan. |

- **Resultado:** `EXITO`, `INVALIDA` (400), `SIN_RESULTADOS` (404) o `ERROR_TECNICO` (500). Que una búsqueda responda 200 sin parcelas se cuenta como éxito del servidor, no como sin resultados.
- Mover el mapa o activar capas no cuenta.
- Los registros de la etapa anterior (criterio `IDENTIFICADOR`, de los lotes demostrativos) se conservan en la tabla y suman al total de consultas, pero no a las columnas actuales.

## API

`GET /api/admin/estadisticas?desde=2026-09-01&hasta=2026-09-07` (solo ADMIN). Máximo 366 días; los días sin actividad aparecen con cero.

```json
{
  "desde": "2026-09-01", "hasta": "2026-09-07", "zona": "America/Argentina/Buenos_Aires",
  "visitas": 12, "consultas": 30,
  "dias": [{
    "fecha": "2026-09-01", "visitas": 3, "consultas": 9,
    "porNomenclatura": 2, "porCoordenadas": 5, "porDireccion": 2, "sinCriterioValido": 0,
    "exitosas": 8, "invalidas": 1, "sinResultados": 0, "erroresTecnicos": 0
  }]
}
```

## Privacidad y límites

Se guardan solamente fecha, categoría y cantidad agregada: no se almacenan IP, correo, texto buscado, nomenclatura ni coordenadas. La zona horaria es `America/Argentina/Buenos_Aires` (configurable con `estadisticas.zona`). El incremento es atómico en MySQL; si falla el registro, la consulta responde igual y puede quedar subcontada (se emite una advertencia en el log). La medición es orientativa, no una auditoría de personas.

## Verificación

`ConsultaEstadisticaFilterTest` comprueba qué rutas cuentan y con qué criterio y resultado; `SeguridadRolesControllerTest`, que solo ADMIN accede. Ver [integración MySQL](integracion-mysql.md).
