# Errores de la API y diagnóstico

## Formato único

Todas las respuestas de error (de cualquier controlador, de seguridad o de la plataforma) tienen esta forma:

```json
{
  "timestamp": "2026-10-24T18:30:00Z",
  "status": 503,
  "codigo": "DIRECCIONES_NO_DISPONIBLES",
  "mensaje": "El servicio oficial de direcciones no respondió. Probá en unos minutos o buscá por nomenclatura.",
  "path": "/api/public/v1/territorio/direcciones",
  "errores": [],
  "correlacionId": "7c1d6f0e-5b0e-4c1e-9f55-2b8f6d6a9a10"
}
```

- `codigo` es **estable**: el frontend decide por el código, no por el texto ni solo por el estado HTTP.
- `mensaje` está pensado para mostrarse a la persona; no incluye trazas, rutas internas, direcciones de servidores, credenciales ni el texto buscado.
- `errores` trae el detalle por campo en las validaciones (`{"campo": "...", "mensaje": "..."}`); en el resto va vacío.
- `correlacionId` identifica la solicitud: también viaja en el encabezado `X-Correlation-Id` y aparece en todas las líneas del log del servidor. Si algo falla de forma inesperada, ese identificador es lo que hay que informar.

Un cliente puede mandar su propio `X-Correlation-Id` (8 a 64 caracteres: letras, números, `.`, `_` y `-`); si no, el servidor genera uno.

## Códigos

**Recuperable** indica si la persona puede resolverlo sin soporte (reintentar, corregir o volver a ingresar).

| Estado | Código | Cuándo ocurre | Recuperable |
| --- | --- | --- | --- |
| 400 | `CONSULTA_INVALIDA` | Parámetros o campos que no pasan la validación (ver `errores`). | Sí: corregir |
| 400 | `DATOS_INVALIDOS` | Cuerpo ilegible, parámetro mal formado o ausente. | Sí: corregir |
| 400 | `CONSULTA_TERRITORIAL_INVALIDA` | Coordenadas, ventana, zoom o límite fuera de rango. | Sí: corregir |
| 400 | `DIRECCION_INVALIDA` | Texto de dirección de menos de 3 o más de 150 caracteres. | Sí: corregir |
| 400 | `PASSWORD_INVALIDA` | La contraseña no cumple los requisitos (12 caracteres, 72 bytes como máximo). | Sí: corregir |
| 400 | `RANGO_INVALIDO` | Fechas de estadísticas desordenadas o de más de 366 días. | Sí: corregir |
| 400 | `PAGINA_INVALIDA` | Página negativa. | Sí: corregir |
| 400 | `COMPARACION_INVALIDA` | Hay que elegir entre dos y tres guardadas distintas. | Sí: corregir |
| 400 | `PUNTO_FUERA_PARCELA` | El punto no pertenece a la parcela elegida. | Sí: elegir otro punto |
| 400 | `RESPUESTA_REQUERIDA` | Un reporte final necesita una respuesta de al menos 10 caracteres. | Sí: completar |
| 401 | `AUTENTICACION_REQUERIDA` | No hay sesión o venció. | Sí: volver a ingresar |
| 401 | `CREDENCIALES_INVALIDAS` | Correo o contraseña incorrectos al iniciar sesión. | Sí: corregir |
| 403 | `ACCESO_DENEGADO` | La cuenta no tiene permiso para esa operación. | No |
| 403 | `CSRF_INVALIDO` | Falta o venció el token de seguridad (suele ser una sesión caducada). | Sí: recargar la página |
| 404 | `PARCELA_NO_ENCONTRADA` | La parcela no tiene geometría válida en la importación actual. | No |
| 404 | `GUARDADA_NO_ENCONTRADA` | La consulta no está entre las guardadas de la cuenta. | No |
| 404 | `HISTORIAL_NO_ENCONTRADO` | La consulta no está en el historial de la cuenta. | No |
| 404 | `REPORTE_NO_ENCONTRADO` | El reporte solicitado no existe. | No |
| 404 | `RECURSO_NO_ENCONTRADO` | La ruta no existe. | No |
| 405 | `METODO_NO_PERMITIDO` | Método HTTP no disponible para ese recurso. | No |
| 406 | `FORMATO_NO_ACEPTABLE` | No se puede responder en el formato pedido. | No |
| 409 | `FICHA_MODIFICADA` | Otra persona modificó la ficha mientras se editaba. | Sí: recargar y reintentar |
| 409 | `EMAIL_EN_USO` | Ya existe una cuenta con ese correo. | Sí: usar otro correo |
| 409 | `CONFLICTO_DATOS` | Los datos entran en conflicto con otro registro. | Según el caso |
| 409 | `PARCELA_CAMBIO` | El punto guardado ya no coincide con la parcela. | Sí: volver a elegir en el mapa |
| 409 | `PARCELA_NO_INCLUIDA` | La selección tiene demasiadas coincidencias. | Sí: elegir un punto interior |
| 415 | `FORMATO_NO_SOPORTADO` | El formato de los datos enviados no es compatible. | No |
| 500 | `ERROR_INTERNO` | Falla inesperada. El detalle está en el log con el `correlacionId`. | Reintentar; si se repite, informar el identificador |
| 503 | `DIRECCIONES_NO_DISPONIBLES` | Georef no respondió y no hay direcciones relevadas por el equipo. | Sí: reintentar o buscar por nomenclatura |
| 503 | `CAPA_NO_IMPORTADA` | Falta importar una capa territorial en esa base. | No: lo resuelve quien administra (importador) |
| 503 | `TERRITORIO_NO_IMPORTADO` | Las capas territoriales todavía no fueron importadas. | No: lo resuelve quien administra |
| 503 | `BASE_DATOS_NO_DISPONIBLE` | La base de datos no responde. | Sí: reintentar en unos minutos |

Los 503 de dependencia o de base indican que el servicio sigue en pie pero no puede completar esa operación: el frontend puede ofrecer reintentar sin recargar toda la aplicación. Reintentar es seguro en las consultas (GET); en escrituras conviene confirmar antes que no se aplicó.

## Estado del servicio

`GET /api/public/v1/estado` (público, no requiere sesión) responde el estado operativo sin credenciales, rutas privadas ni datos personales:

```json
{
  "estado": "DEGRADADO",
  "fecha": "2026-10-24T18:30:00Z",
  "baseDatos": "OK",
  "esquema": "3",
  "georef": "NO_DISPONIBLE",
  "capas": [
    {"capa": "catastro", "requerida": true, "importada": true, "registros": 65756, "importadoEn": "2026-10-20 10:15:00"},
    {"capa": "aysam", "requerida": true, "importada": true, "registros": 2, "importadoEn": "2026-10-20 10:15:02"},
    {"capa": "aguas_lujan", "requerida": false, "importada": true, "registros": 3, "importadoEn": "2026-10-24 16:38:13"},
    {"capa": "electricidad", "requerida": true, "importada": false, "registros": null, "importadoEn": null}
  ],
  "capasFaltantes": ["electricidad"]
}
```

| `estado` | HTTP | Significa |
| --- | --- | --- |
| `OK` | 200 | Base disponible, capas requeridas importadas y Georef responde. |
| `DEGRADADO` | 200 | Falta alguna capa requerida o Georef no responde. El mapa y la búsqueda por nomenclatura pueden seguir funcionando. |
| `NO_DISPONIBLE` | 503 | La base de datos no responde. |

- `esquema` es la última versión de migración aplicada (ver [migraciones](migraciones-flyway.md)).
- Georef es un servicio externo: se verifica como máximo una vez por minuto, con tiempos de espera cortos.
- Las capas requeridas para consultar son `catastro`, `aysam` y `electricidad`. `aguas_lujan` es esperada pero no requerida: sin ella las consultas siguen funcionando (ese radio figura sin dato). Cualquier capa esperada que falte se nombra en `capasFaltantes` y deja el estado en `DEGRADADO`; se carga con `scripts/importar_territorio.py` o `iniciar-local.cmd --preparar-solo`. Al arrancar, el log también avisa.

## Registro (log)

Cada línea del log lleva el `correlacionId` entre corchetes. Los errores inesperados registran método, ruta y la excepción completa, **sin** parámetros de consulta, cabeceras, cookies ni contraseñas. Los mensajes de error de la base de datos o de Georef, que pueden incluir direcciones de servidores o lo que la persona buscó, no se devuelven al cliente.
