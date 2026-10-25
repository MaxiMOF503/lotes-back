# Reportes de datos incorrectos o faltantes

Los reportes siempre pertenecen a una cuenta autenticada y a una parcela vigente del catastro importado.
No modifican las capas oficiales ni la ficha complementaria: crean una tarea de revisión separada y trazable.

## Usuario o administrador autenticado

### Crear

`POST /api/mi-espacio/reportes`

```json
{
  "parcelaId": "Parcelas_visor0.123",
  "seccion": "AGUA",
  "tipo": "FALTANTE",
  "descripcion": "No figura el prestador informado en la documentación municipal."
}
```

La descripción admite entre 10 y 1000 caracteres. La parcela debe existir en la importación actual. El nuevo
reporte queda en `PENDIENTE` y responde `201 Created`.

### Consultar los propios

`GET /api/mi-espacio/reportes?pagina=0`

Devuelve páginas de 20 elementos, del más nuevo al más antiguo. Una cuenta nunca recibe reportes ajenos.

## Administrador

### Bandeja

`GET /api/admin/reportes?estado=PENDIENTE&pagina=0`

`estado` es opcional: `PENDIENTE`, `EN_REVISION`, `RESUELTO` o `DESCARTADO`.

### Revisar o responder

`PATCH /api/admin/reportes/{id}`

```json
{
  "estado": "RESUELTO",
  "respuesta": "Se verificó la fuente y se corrigió la ficha complementaria."
}
```

Para `RESUELTO` y `DESCARTADO`, la respuesta es obligatoria y debe tener al menos 10 caracteres. La respuesta,
la persona administradora y la fecha quedan visibles en la consulta del usuario.

Todas las escrituras requieren el token CSRF de `/api/csrf`. Los errores respetan `ApiErrorResponse`.
