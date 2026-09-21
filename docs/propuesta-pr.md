# Propuesta de Pull Request

## Título

Completa la ficha pública con orientación de consulta y pruebas MySQL

## Objetivo

Dejar un contrato verificable para que el futuro frontend reemplace sus mocks y pueda mostrar dónde continuar una consulta. La ficha agrega `dondeConsultar` a partir del departamento asociado, preserva datos opcionales como null y mantiene explícita la procedencia demostrativa. Las consultas espaciales ahora se verifican contra MySQL real.

## Contrato y errores

`GET /api/public/v1/lotes/ficha` acepta `identificador` o el par `latitud`/`longitud`, de manera excluyente. La búsqueda por coordenadas es exacta.

- 200: `FichaLoteResponse`, ahora con `dondeConsultar`.
- 400: `ApiErrorResponse`, código `CONSULTA_INVALIDA`.
- 404: `ApiErrorResponse`, código `LOTE_NO_ENCONTRADO`.
- 500: `ApiErrorResponse`, código `ERROR_INTERNO`, sin detalles técnicos internos en el cuerpo.

Se reutilizan los componentes existentes. La ampliación agrega un DTO público `DondeConsultarResponse` y un campo raíz, sin renombrar ni eliminar campos anteriores. Si falta el departamento, `dondeConsultar` es null; si faltan contacto, oficina o instrucciones, se conservan como null. Los consumidores con esquemas cerrados deberán admitir el nuevo campo.

El [contrato completo](contrato-ficha-publica.md) incluye los tipos, nulabilidad y ejemplos; las anotaciones OpenAPI exponen los DTOs y los cuatro estados HTTP.

## Procedencia

Las cuatro secciones originales conservan `ProcedenciaResponse` con `SIMULADO`, `Dataset demostrativo B-01`, `false`. `dondeConsultar` conserva `SIMULADO` y `false`, y toma `fuente` del departamento (null si no está disponible). No se etiqueta un contacto demostrativo como oficial ni se inventa una fuente.

## Validación

`mvnw.cmd -Pmysql-it verify` completado con **39 tests habituales y 8 de integración**, sin fallos ni omitidos. Build y JAR ejecutable correctos.

Las pruebas cubren consultas por ambas alternativas, validaciones, normalización del identificador, ausencia de resultados, fallas técnicas, procedencia y datos parciales. La integración usa aplicación, inicializador y repositorios reales sobre MySQL 8.0.46: verifica SRID y orden de coordenadas, coincidencia exacta, rechazo de puntos cercanos/invertidos, orientación y campos null persistidos.

Se agregan el perfil Maven `mysql-it` y [su guía](integracion-mysql.md). Requiere una base descartable `lotes_ficha_test`: crea y elimina sus tablas. No utiliza las variables `DB_*` de la aplicación. El ciclo habitual `verify` sigue funcionando sin MySQL. No hay lint ni Checkstyle configurado.

Los nueve [ejemplos HTTP](../examples/ficha-publica.http) también fueron ejecutados con [el script PowerShell](../examples/probar-ficha.ps1) contra el JAR y MySQL temporal. La documentación se contrastó con la respuesta real y el OpenAPI publicado.

## Preguntas de integración

### ¿Qué campos forman parte del contrato y por qué?

`criterioConsulta` identifica la alternativa y sus parámetros. `lote` contiene identificación, dirección, departamento y coordenadas. `zonificacion` contiene descripción y verificación. `infraestructuraElectrica` contiene distancia a la red y acceso; `coberturaAgua`, cobertura. `advertencia` comunica el carácter demostrativo. `dondeConsultar` agrega departamento, contacto, oficina e instrucciones para continuar la consulta. Cada sección de datos conserva su procedencia con `tipo`, `fuente` y `oficial`.

### ¿Cómo se representa un error de consulta en la interfaz?

El futuro cliente distinguirá 400 (consulta inválida), 404 (sin resultados) y 500/fallos de red (error técnico). `ApiErrorResponse` conserva `timestamp`, `status`, `codigo`, `mensaje`, `path` y `errores`, cuyos detalles contienen `campo` y `mensaje`. Un dato parcial null en una ficha 200 se puede mostrar como “Sin información disponible”; no se transforma en 404.

### ¿Cómo evitaste duplicar reglas entre frontend y backend?

El backend valida, resuelve el lote, compone ficha y orientación, conserva procedencia y genera errores de dominio. El frontend capturará la selección, enviará parámetros y mostrará loading, ficha, orientación disponible, error o sin resultados. No reconstruirá reglas ni accederá a entidades o repositorios.

## Alcance y revisión

Se amplía esta issue con orientación del departamento, pruebas MySQL, datos parciales y ejemplos ejecutables. No se agregan frontend, autenticación, entidades, endpoints, motor geográfico ni dependencias de ejecución.

Este PR parte de `work/issue-1-api-publica-Fontana`, la rama del PR #3 todavía pendiente de integración. El diff contiene únicamente los cambios de B-02. Una vez integrado el PR #3, cambiar la base de este PR a `main` antes de integrarlo.

Refs #2

Este PR cubre el contrato backend de la issue #2. La implementación del cliente y sus estados visuales queda pendiente en el repositorio frontend; no se cierra automáticamente la issue.
