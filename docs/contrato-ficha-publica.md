# Contrato público de ficha de lote

## Consulta

`GET /api/public/v1/lotes/ficha` devuelve JSON usando `FichaLoteResponse` o `ApiErrorResponse`. No requiere autenticación.

Enviar exactamente una alternativa: `identificador`, o `latitud` y `longitud` juntas. Las reglas se validan en backend antes de acceder al repositorio.

| Parámetro | Tipo | Regla existente |
| --- | --- | --- |
| identificador | string | No vacío ni sólo espacios; máximo 100 caracteres antes de recortar espacios extremos. No se exige un patrón catastral ni el prefijo LOT. |
| latitud | decimal | WGS84, entre -90 y 90 inclusive. Requiere longitud. |
| longitud | decimal | WGS84, entre -180 y 180 inclusive. Requiere latitud. |

Omitir los parámetros de la alternativa no elegida. Sin criterio, identificador vacío, coordenadas incompletas/no numéricas/fuera de rango o criterios combinados producen 400. Los campos numéricos vacíos se convierten a ausencia durante el binding; enviar valores numéricos en la alternativa por coordenadas.

```http
GET /api/public/v1/lotes/ficha?identificador=LOT-DEMO-001
GET /api/public/v1/lotes/ficha?latitud=-32.8895&longitud=-68.8458
```

La resolución por coordenadas exige **coincidencia exacta** con el punto almacenado. No busca por proximidad ni por contención en polígonos. Una ubicación válida sin coincidencia devuelve 404. Los ejemplos corresponden al inicializador demostrativo existente.

## HTTP 200 — FichaLoteResponse

Todos los campos raíz actuales son los siguientes; no hay `procedencia` en la raíz. La procedencia pertenece a cada una de las cinco secciones de datos, incluida la orientación de consulta cuando existe.

| Campo | Tipo | Significado |
| --- | --- | --- |
| criterioConsulta | CriterioConsultaResponse | Criterio efectivamente utilizado, con identificador normalizado cuando corresponde. |
| lote | DatosLoteResponse | Identificación, dirección y ubicación pública del lote. |
| zonificacion | ZonificacionResponse | Descripción y estado de verificación. |
| infraestructuraElectrica | ElectricidadResponse | Distancia a la red en metros y acceso. |
| coberturaAgua | AguaResponse | Indicación de cobertura. |
| advertencia | string | Texto del backend sobre el carácter demostrativo y no oficial de la ficha. |
| dondeConsultar | DondeConsultarResponse o null | Contacto e instrucciones del departamento asociado. Null si el lote no tiene departamento. |

### Estructuras internas completas

| DTO | Campos JSON y tipos |
| --- | --- |
| CriterioConsultaResponse | `tipo`: string (`IDENTIFICADOR` o `COORDENADAS`); `identificador`: string o null; `latitud`: number o null; `longitud`: number o null. Por identificador, ambas coordenadas son null; por coordenadas, el identificador es null. |
| DatosLoteResponse | `identificador`: string; `direccionAproximada`: string o null; `departamento`: string o null; `coordenadas`: CoordenadasResponse; `procedencia`: ProcedenciaResponse. |
| CoordenadasResponse | `latitud`: number; `longitud`: number. Ubicación del lote en WGS84. |
| ZonificacionResponse | `descripcion`: string o null; `verificada`: boolean; `procedencia`: ProcedenciaResponse. |
| ElectricidadResponse | `distanciaRedElectricaMts`: number o null; `tieneAcceso`: boolean; `procedencia`: ProcedenciaResponse. |
| AguaResponse | `tieneCobertura`: boolean; `procedencia`: ProcedenciaResponse. |
| DondeConsultarResponse | `departamento`: string; `contacto`: string o null; `direccionOficina`: string o null; `comoConsultar`: string o null; `procedencia`: ProcedenciaResponse. |
| ProcedenciaResponse | `tipo`: string; `fuente`: string o null; `oficial`: boolean. Las cuatro secciones originales conservan `SIMULADO`, `Dataset demostrativo B-01` y `false`. En `dondeConsultar` se usa `SIMULADO`, la fuente almacenada del departamento (null si falta) y `false`. |

Los valores null no significan cero ni una cadena vacía. Los números JSON no garantizan una cantidad fija de decimales. `verificada` y `oficial` son campos diferentes; el cliente no debe deducir procedencia a partir de la descripción o de la verificación.

Ejemplo completo por identificador, construido con los valores del inicializador y el mapeo del servicio:

```json
{
  "criterioConsulta": {
    "tipo": "IDENTIFICADOR",
    "identificador": "LOT-DEMO-001",
    "latitud": null,
    "longitud": null
  },
  "lote": {
    "identificador": "LOT-DEMO-001",
    "direccionAproximada": "Calle Demostración 123, Mendoza",
    "departamento": "Luján de Cuyo",
    "coordenadas": { "latitud": -32.8895, "longitud": -68.8458 },
    "procedencia": { "tipo": "SIMULADO", "fuente": "Dataset demostrativo B-01", "oficial": false }
  },
  "zonificacion": {
    "descripcion": "Residencial R2 (dato simulado)",
    "verificada": false,
    "procedencia": { "tipo": "SIMULADO", "fuente": "Dataset demostrativo B-01", "oficial": false }
  },
  "infraestructuraElectrica": {
    "distanciaRedElectricaMts": 320.0,
    "tieneAcceso": true,
    "procedencia": { "tipo": "SIMULADO", "fuente": "Dataset demostrativo B-01", "oficial": false }
  },
  "coberturaAgua": {
    "tieneCobertura": true,
    "procedencia": { "tipo": "SIMULADO", "fuente": "Dataset demostrativo B-01", "oficial": false }
  },
  "advertencia": "La información de esta ficha es demostrativa y no constituye información catastral, municipal ni legal oficial.",
  "dondeConsultar": {
    "departamento": "Luján de Cuyo",
    "contacto": "Contacto demostrativo no oficial",
    "direccionOficina": "Oficina demostrativa",
    "comoConsultar": "Consultar la zonificación ante el organismo municipal competente",
    "procedencia": { "tipo": "SIMULADO", "fuente": "Dataset demostrativo B-01", "oficial": false }
  }
}
```

Por coordenadas, la misma ficha tendrá este `criterioConsulta`:

```json
{ "tipo": "COORDENADAS", "identificador": null, "latitud": -32.8895, "longitud": -68.8458 }
```

### Datos parciales y dónde consultar

Un lote existente devuelve 200 aunque falten dirección, departamento, descripción de zonificación o distancia eléctrica: esos valores se conservan como null. Si falta el departamento, `dondeConsultar` es null. Si existe pero le faltan datos de contacto, el objeto conserva su nombre y los campos faltantes son null; no se inventan teléfonos, oficinas, instrucciones ni fuentes.

El frontend puede presentar “Sin información disponible” para un dato null y mostrar la orientación sólo cuando haya datos útiles. El nombre público `contacto` evita afirmar que el contacto demostrativo sea oficial, aunque internamente provenga de `contactoOficial`. `procedencia.oficial` sigue siendo false. Los booleanos existentes de agua y electricidad mantienen su semántica y no agregan un estado desconocido en esta issue.

La ampliación agrega `dondeConsultar` sin renombrar ni eliminar campos anteriores. Los consumidores con esquemas cerrados deberán aceptar este nuevo campo.

Sólo se exponen DTOs: no se serializan entidades JPA, IDs internos, geometrías ORM, usuarios, titular ni situación dominial simulada.

## Errores — ApiErrorResponse

| Campo | Tipo | Significado |
| --- | --- | --- |
| timestamp | string | Instante UTC de generación en formato ISO-8601. |
| status | integer | Estado HTTP, también enviado como estado de la respuesta. |
| codigo | string | Código para distinguir el caso sin interpretar mensajes. |
| mensaje | string | Descripción para el consumidor. |
| path | string | Ruta solicitada, sin query string. |
| errores | array de DetalleError | Cada detalle contiene `campo` y `mensaje` (strings). Para la regla conjunta se usa `campo: "criterio"`; para errores de campos se usa su nombre. Vacío para 404 y 500. |

| HTTP | codigo | Interpretación para el cliente |
| --- | --- | --- |
| 400 | CONSULTA_INVALIDA | Mostrar consulta inválida y detalles disponibles. |
| 404 | LOTE_NO_ENCONTRADO | Mostrar estado sin resultados. |
| 500 | ERROR_INTERNO | Mostrar fallo técnico y permitir reintento. No interpretarlo como lote inexistente. |

Ejemplo 400 sin parámetros (timestamp ilustrativo; generado en cada respuesta):

```json
{
  "timestamp": "2026-09-20T12:00:00Z",
  "status": 400,
  "codigo": "CONSULTA_INVALIDA",
  "mensaje": "Los parámetros de consulta no son válidos",
  "path": "/api/public/v1/lotes/ficha",
  "errores": [
    { "campo": "criterio", "mensaje": "Debe informar un identificador o ambas coordenadas, pero no ambos criterios" }
  ]
}
```

Ejemplo 404:

```json
{
  "timestamp": "2026-09-20T12:00:00Z",
  "status": 404,
  "codigo": "LOTE_NO_ENCONTRADO",
  "mensaje": "No existe un lote demostrativo asociado al criterio indicado",
  "path": "/api/public/v1/lotes/ficha",
  "errores": []
}
```

Ejemplo 500 por falla inesperada del servicio o persistencia:

```json
{
  "timestamp": "2026-09-20T12:00:00Z",
  "status": 500,
  "codigo": "ERROR_INTERNO",
  "mensaje": "No se pudo completar la consulta",
  "path": "/api/public/v1/lotes/ficha",
  "errores": []
}
```

El manejador existente conserva 400/404 y registra las excepciones técnicas en el servidor; no envía detalles internos de conexión. Los errores de red o respuestas de infraestructura ajena a la aplicación pueden carecer de este cuerpo y deben tratarse como fallos técnicos.

## Integración futura

Al seleccionar ubicación, el cliente captura latitud/longitud y envía ambos parámetros. Muestra loading mientras espera; con 200 presenta ficha, advertencia, procedencia y orientación disponible; con 400 presenta consulta inválida; con 404, sin resultados; con 500 o fallo de red, error técnico. Puede consultar por identificador como alternativa.

El backend valida, resuelve el lote, compone la ficha, aplica las reglas y determina la procedencia. El frontend no necesita entidades, repositorios ni reglas de persistencia y no debe inferir si los datos son oficiales. La publicación del cliente en otro origen requerirá definir la configuración de despliegue correspondiente; esta issue no agrega un cliente ni configuración CORS.

## Ejemplos ejecutables

[examples/ficha-publica.http](../examples/ficha-publica.http) contiene nueve consultas GET para un editor compatible con archivos HTTP. Con la aplicación y el dataset demo en ejecución también se pueden verificar automáticamente desde PowerShell 7:

```powershell
./examples/probar-ficha.ps1 -BaseUrl http://localhost:8080
```

El script comprueba los estados 200/400/404, los códigos de error y la presencia de orientación con procedencia simulada. Los 404 presuponen el dataset demostrativo original. Los escenarios de datos parciales y fallo técnico se reproducen con tests, sin agregar un endpoint que modifique datos o fuerce errores.
