# Clasificación de coincidencias catastrales (#49)

## Flujo y alcance

Antes, `copias_exactas` comparaba sólo nomenclatura y geometría y llamaba exactas
a todas las copias. Se conserva ese grupo: misma geometría GeoJSON estructural y
`(nomenclatu or "")`, sin introducir igualdad topológica ni cambiar el tratamiento
anterior de nomenclaturas vacías. La primera aparición es el representante y las
otras filas conservan sus IDs y `duplicado_de`.

Ahora `clasificar_coincidencias` compara cada copia con el representante:

- `DUPLICADO_IDENTICO`: todos los atributos relevantes coinciden.
- `CONFLICTO_ATRIBUTOS`: difieren; guarda los nombres de campos ordenados.

Un grupo es conflictivo cuando alguna copia difiere, aunque otras sean idénticas.
No se mezclan propiedades, no se elige un valor verdadero ni se eliminan filas.
Las filas sin geometría no se agrupan.

## Única exclusión técnica

Se ignora **sólo `fid`**, identificador de fila del exportador, distinto del ID de
origen conservado en `origen_id`. `id`, `id_ev`, `id_bp`, `alerta_sup`, `aluvional`,
`ochava`, retiros y cualquier campo futuro siguen siendo relevantes: no se
presupone que otro identificador sea puramente técnico.

Se compara JSON ordenando claves de objetos, también anidados. Ausencia difiere
de null, null de cadena vacía, número de cadena, booleano de número, 1 de 1.0;
espacios y mayúsculas se conservan. El orden de arrays es significativo. Los
campos diferentes son propiedades superiores; se conservan los valores completos
para inspeccionar diferencias anidadas.

## Persistencia

V5 añade `tipo_duplicado varchar(32)`, `campos_conflicto json` (nullable) e índice
`(capa,duplicado_de)` a `territorio_elementos`. No contamina `propiedades`.
Representantes: ambos metadatos null; copias idénticas: lista `[]`; conflictivas:
lista de campos diferentes. V1–V4 no se modifican.

V5 no adivina clasificaciones de filas antiguas: después del esquema se debe
ejecutar el importador. El preparador reutiliza V2 y V5 idempotentes antes de
importar; Flyway registra V5 al arrancar. Catastro recalcula los metadatos aunque
se omita la carga por checksum. El marcado del grupo es transaccional.

No se reconcilian versiones: no se limpian filas ausentes de los grupos del
archivo actual ni se deciden altas, bajas o vigencia (#50). La carga conserva el
comportamiento previo; el marcado actualiza sólo IDs de los grupos actuales.

```powershell
# Sólo lectura del archivo, sin MySQL:
py -3 scripts/importar_territorio.py --analizar-coincidencias
# Con DB_* autorizadas: esquema y clasificación, incluso con checksum igual:
py -3 scripts/importar_territorio.py --capa catastro
```

## Inspección administrativa

`EstadoService` informa disponibilidad y `/fuentes` recuentos de importación;
ninguno permite inspeccionar variantes. Se añaden consultas bajo `/api/admin/**`,
ya restringido a ADMIN por `SecurityConfig`:

- `GET /api/admin/territorio/calidad`: grupos coincidentes, idénticos, conflictivos,
  copias de cada tipo, registros involucrados, pendientes y `clasificacionCompleta`.
- `GET /api/admin/territorio/calidad/coincidencias?soloConflictos=true&pagina=0`:
  localiza los IDs de los grupos conflictivos, de a 20; `soloConflictos=false`
  incluye los idénticos y pendientes. Permite elegir un grupo para inspeccionarlo.
- `GET /api/admin/territorio/calidad/parcelas/{id}`: representante o copia;
  devuelve representante, registros con propiedades originales, clasificación
  individual y campos diferentes. Orden: representante, luego copias por ID.

La clasificación ausente es `PENDIENTE_CLASIFICACION`, nunca igualdad presumida.
Un ID desconocido da 404 y un enlace sin representante directo da 409. Estas son
consultas de calidad de los grupos directos escritos por el importador, no un
reemplazo del resolutor canónico de #48. No se añade API pública ni se modifica
frontend. Anónimos reciben 401 y USUARIO 403.

Mapa, búsqueda y cercanas conservan `duplicado_de IS NULL`: no reaparecen
polígonos repetidos. La lectura exacta por ID mantiene accesibles las copias. Los
metadatos complementan la identidad lógica de #48 sin cambiarla.

## Dataset versionado

Archivo `parcelas_lujan_de_cuyo.geojson.gz`, SHA256:
`9680901ded252fad3ccdd2df489210183a49e0118ba36339bb9d09f077b51949`.

| Métrica | Cantidad |
|---|---:|
| Grupos coincidentes | 94 |
| Grupos íntegramente idénticos | 11 |
| Grupos con conflicto | 83 |
| Copias idénticas al representante | 251 |
| Copias con conflicto | 354 |
| Registros involucrados, incluidos representantes | 699 |
| Registros en grupos conflictivos, incluidas copias idénticas | 611 |

Frecuencia por **grupo**, contando un campo una sola vez por grupo:
`alerta_sup: 52`, `id_ev: 21`, `aluvional: 10`, `id_bp: 7`, `id: 2`, `ochava: 1`,
`ret_front2: 1`, `ret_lat1: 1`, `ret_lat2: 1`, `ret_post: 1`.
No se ajustó el criterio para alcanzar 83. No se excluyó `id` sin justificación.

## Pruebas

```powershell
.\mvnw.cmd test
py -3 -m unittest discover -s scripts -p "test_*.py" -v
.\mvnw.cmd verify
git diff --check
```

Integración: MySQL 8.0.18+ local, Java 21 y Python 3.10+ (`py -3` en Windows,
`python3` en Linux). Crear una base nueva y exclusiva, no una base de otro issue:

```sql
CREATE DATABASE lotes_issue49_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

```powershell
Set-Location C:\LoteSeguro\back-issue49
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/lotes_issue49_test?serverTimezone=UTC'
$credencial=Get-Credential -Message 'Usuario MySQL de la base aislada de #49'
$env:DB_USERNAME=$credencial.UserName
$env:DB_PASSWORD=$credencial.GetNetworkCredential().Password
$env:ISSUE49_MYSQL_IT='true'
.\mvnw.cmd -Pmysql-it "-Dit.test=ClasificacionDuplicadosMySqlIT" verify
```

El guard verifica nombre y host antes de Flyway/Hibernate. Las cinco pruebas
crean fixtures sintéticos con rollback, llaman al clasificador Python real y
ejecutan sus UPDATE en MySQL. Cubren persistencia, campos y valores originales,
lecturas, recuentos, deduplicación y repetición idempotente. No requieren importar
todo el dataset. Sin ejecutarlas no se considera validada la integración.

Después de integrar #48, el mismo test comprueba que una copia idéntica y otra
con conflicto resuelvan al mismo canónico y mantengan `idsRelacionados`. Incluye
Guardadas históricas y nuevas, informes, rechazo de comparación equivalente,
fichas de ambas copias, reportes con ID original y snapshots sin reescritura.
Usa fuentes y un usuario sintéticos transaccionales en la base exclusiva #49;
no necesita ejecutar los tests del caso oficial contra otra base.

No ejecutar toda la suite MySQL sin revisar su documentación: otras clases
utilizan `lotes_flyway_test` de forma destructiva, fuera de esta validación.
