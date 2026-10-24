# Instalación MySQL vacía del backend actual

[`crear-base-vacia.sql`](crear-base-vacia.sql) crea **12 tablas y cero registros**. Fuente revisada: `origin/main` **0f8e476814f61b80053755a95cb01909f9c19bb9**, consultada el 19/10/2026. Incluye nueve tablas JPA y las tres tablas externas definidas por `scripts/importar_territorio.py`.

No carga roles, usuarios, lotes, estadísticas, catastro, AySAM ni electricidad. No crea usuarios MySQL, contiene credenciales ni modifica la conexión del backend. No es una migración ni un reparador de bases existentes.

## Requisitos y nombre de la base

- MySQL 8.0.16+ con InnoDB, JSON y soporte espacial SRID 4326; probado en MySQL Community 8.0.46. Para el flujo posterior del importador, su código requiere cliente `mysql` 8.0.18+ y Python 3.10+. No validado en MariaDB ni fuera de MySQL 8.
- Cuenta autorizada para crear una base, tablas, índices y claves foráneas.
- El nombre predeterminado es `verificador_lotes`, para una instalación nueva en otra computadora. **No ejecutar sobre la base de desarrollo existente.** Para pruebas u otro nombre, reemplazarlo por el mismo nombre nuevo en las dos sentencias `CREATE DATABASE` y `USE`.

Si la base existe, `CREATE DATABASE` falla con **ERROR 1007**: detenerse y elegir otro nombre, sin borrar la base. No hay `IF NOT EXISTS`, `DROP`, borrado de registros ni desactivación de claves foráneas.

Ejecutar el archivo completo, sin seleccionar fragmentos ni saltarse errores. `@ls_base_creada` registra inmediatamente si esta ejecución creó la base. Cada bloque `PREPARE` ejecuta su DDL solamente en ese caso; si el cliente continúa tras el error, emite un mensaje y omite las tablas. No se crean procedimientos permanentes. No ejecutar con `--force`.

MySQL confirma DDL automáticamente: un error posterior a crear la base puede dejar una instalación parcial. Revisar el error y usar una nueva base para reintentar; no reutilizar este script para reparar la instalación parcial.

## MySQL Workbench

1. Conectar a la instancia de destino con una cuenta autorizada.
2. Abrir este archivo SQL con **File > Open SQL Script**.
3. Elegir un nombre de base que no exista, como se explica arriba, y activar la detención ante errores.
4. Ejecutar el archivo **completo** y revisar **Action Output**. Si falla, detenerse.
5. Refrescar **Schemas** y ejecutar los conteos de la sección de verificación.

No arrancar el backend ni ejecutar el importador antes de comprobar que las tablas están vacías.

## Windows PowerShell

Desde el repositorio, después de elegir un nombre nuevo en el archivo:

```powershell
Set-Location C:\LoteSeguro\back
$mysqlExe = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$esquemaSql = Get-Content -LiteralPath .\scripts\mysql\crear-base-vacia.sql -Raw -Encoding UTF8
& $mysqlExe --host=127.0.0.1 --port=3306 --user=usuario_instalador --password --default-character-set=utf8mb4 --execute=$esquemaSql
if ($LASTEXITCODE -ne 0) { throw 'Instalación detenida: revisar el error de MySQL. No continuar manualmente.' }
```

Ajustar ruta del cliente, host, puerto y usuario. `--password` pide la contraseña interactivamente: no escribirla en el comando ni guardarla en Git. No se cambia ninguna variable `DB_*` del proyecto.

## Tablas, relaciones y tipos

| Tabla | Definición relevante |
| --- | --- |
| `departamentos` | PK `id` BIGINT AUTO_INCREMENT; `nombre` VARCHAR(100) único |
| `roles` | PK `id` BIGINT AUTO_INCREMENT; `nombre` ENUM('ADMIN','USUARIO') único |
| `usuarios` | PK `id` BIGINT AUTO_INCREMENT; `email` VARCHAR(150) único; FK obligatoria `rol_id` → `roles.id` |
| `lineas_electricas` | PK `id` BIGINT AUTO_INCREMENT; `trazado` MULTILINESTRING NOT NULL SRID 4326 |
| `lotes` | PK `id` BIGINT AUTO_INCREMENT; `identificador` único; FK opcional `departamento_id` → `departamentos.id`; `ubicacion` POINT NOT NULL SRID 4326; procedencia embebida |
| `estadisticas_diarias` | PK `clave` VARCHAR(100); índice `idx_estadistica_fecha` |
| `consultas_guardadas` | PK `id` BIGINT AUTO_INCREMENT; FK obligatoria `usuario_id` → `usuarios.id`; UNIQUE (`usuario_id`,`parcela_id`); `creada_en` DATETIME(6) |
| `consultas_historial` | PK `id` BIGINT AUTO_INCREMENT; FK obligatoria `usuario_id` → `usuarios.id`; índice `idx_historial_usuario_fecha` (`usuario_id`,`consultada_en`); `snapshot` LONGTEXT NOT NULL; `consultada_en` DATETIME(6) |
| `fichas_parcelas` | PK `parcela_id` VARCHAR(100); `actualizado_en` DATETIME(6) NOT NULL; `version` BIGINT NOT NULL para bloqueo optimista |
| `territorio_elementos` | PK compuesta (`capa`,`origen_id`); `geometria` GEOMETRY NOT NULL SRID 4326; SPATIAL INDEX `territorio_geometria`; `propiedades` JSON NOT NULL; `valida` BOOLEAN NOT NULL DEFAULT TRUE |
| `territorio_fuentes` | PK `capa` VARCHAR(24); `sha256` CHAR(64), fuente, URL y conteos obligatorios; `importado_en` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |
| `territorio_sin_geometria` | PK compuesta (`capa`,`origen_id`); `propiedades` JSON NOT NULL; sin columna geométrica |

Se mantienen nombres de índices y restricciones de Hibernate. Las cuatro FK usan **NO ACTION** para DELETE y UPDATE (equivalente a RESTRICT en InnoDB), sin cascadas. La UNIQUE y el índice por usuario ya sirven de soporte a las nuevas FK; no se inventan índices adicionales.

Todas las tablas usan **InnoDB, utf8mb4 y utf8mb4_0900_ai_ci**. JPA no fija la collation; el importador fija utf8mb4, sin collation explícita. El instalador la hace reproducible, compatible con el dump territorial y el MySQL 8 probado.

Los `Instant` de las entidades nuevas son DATETIME(6), sin default SQL. La inicialización de fechas en Java no equivale a DEFAULT CURRENT_TIMESTAMP. `@Version Long` se genera como BIGINT NOT NULL, sin default. `eliminado` conserva TINYINT(1) DEFAULT 0 y los otros booleanos originales, BIT(1) NOT NULL sin default SQL. `valida` territorial es TINYINT(1) DEFAULT 1 en MySQL.

La convención del proyecto es longitud = X, latitud = Y. Se conserva SRID 4326. Para WKT/WKB geográficos, el código utiliza `axis-order=long-lat`. Las coordenadas de guardadas/historial son DOUBLE, no geometrías. No se agregan FK desde los `parcela_id` a `territorio_elementos`: las entidades usan identificadores de texto, y la tabla territorial tiene clave compuesta por capa e identificador. Tampoco se inventa una FK para `actualizado_por`.

No se encontraron otras entidades, DDL de tablas adicionales vigente, vistas, triggers, eventos, procedimientos ni migraciones Flyway/Liquibase propias. El dump `data/lineas-electricas/lineas_electricas_lujan_de_cuyo.sql` es histórico y contiene registros: no se usa como instalador ni se ejecuta completo.

## Diferencias respecto del instalador anterior

El archivo anterior cubría seis tablas de `e0479ba`: departamentos, roles, usuarios, líneas eléctricas, lotes y estadísticas. Esos seis mapeos no cambiaron en `origin/main`; se conservaron sus tipos, longitudes, columnas, nulabilidad, defaults, índices, FK y SRID.

Faltaban las seis tablas nuevas listadas arriba. Se agregaron sus 38 columnas, tres PK simples, dos PK compuestas adicionales y la PK territorial por capa; dos FK hacia usuarios, la UNIQUE compuesta de guardadas, el índice de historial y el índice espacial territorial. Se respetan JSON, LONGTEXT, GEOMETRY, DATETIME(6), VARCHAR y defaults del código actual.

La configuración de demostración también cambió respecto de la revisión anterior; ver abajo. Este archivo representa `origin/main`, aunque la rama de trabajo que lo contiene todavía parte del commit antiguo: no actualiza por sí mismo el código Java de esa rama.

## Diferencia con el importador territorial

Este instalador **solo crea la estructura**, incluidas las tres tablas territoriales. No ejecuta Python ni extrae datos de GeoJSON o del dump eléctrico.

`scripts/importar_territorio.py`, disponible en `origin/main`, crea sus tablas con `IF NOT EXISTS` e importa después los datos reales de catastro, AySAM y EDEMSA. También registra fuentes, checksums, cantidades y fechas. **Esa carga se realiza posteriormente**, en una tarea separada y sobre la conexión elegida conscientemente. No ejecutar el importador para validar que una base está vacía.

Las tablas JPA y territoriales de líneas eléctricas tienen fines distintos: `lineas_electricas` pertenece al modelo JPA; el importador carga la capa eléctrica en `territorio_elementos`, no en `lineas_electricas`. Sin importación, fuentes y capas estarán vacías, aunque la estructura sea compatible.

## Atención: el backend puede insertar registros al arrancar

Según la configuración de **origin/main 0f8e476**:

- `DatosDemostrativosInitializer` crea siempre ADMIN/USUARIO si faltan. Con `DEMO_LOTES_ENABLED=true`, además puede crear el usuario demostrativo no operativo, departamento, `LOT-DEMO-001` y línea eléctrica demostrativa. Por defecto ese bloque demo está desactivado.
- `src/main/resources/data.sql` agrega un departamento y diez lotes ficticios faltantes cuando `DEMO_SQL_INIT_MODE=always`; el valor predeterminado actual es `never`.
- `AdminInitializer` crea cuentas/roles si se proporcionan `ADMIN_EMAIL`/`ADMIN_PASSWORD` o `USUARIO_EMAIL`/`USUARIO_PASSWORD`.
- `CuentasDemostrativasInitializer`, habilitado con `DEMO_ACCOUNTS_ENABLED=true`, puede crear cuentas de clase y actualizar sus hashes.
- `JPA_DDL_AUTO` sigue teniendo `update` como valor predeterminado y podría actualizar estructura.

**La rama local antigua conserva otros defaults**: su `data.sql` estaba activado con `always` y su inicializador cargaba demostraciones sin la nueva condición. La advertencia se refiere al código que realmente se arranque, no solo al script SQL.

Aquí no se cambia ningún inicializador ni configuración. Una base vacía no tiene usuarios para login, departamentos, lotes ni capas territoriales; ciertas funciones necesitan sus datos mínimos o importación posterior. Validar Hibernate por separado evita ejecutar esos inicializadores y mantiene las doce tablas vacías.

## Verificar cero filas

Seleccionar en la misma conexión la base recién creada (reemplazar el nombre):

```sql
USE `nombre_de_base_nueva`;
SHOW TABLES;
SELECT 'departamentos' AS tabla, COUNT(*) AS filas FROM departamentos
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'usuarios', COUNT(*) FROM usuarios
UNION ALL SELECT 'lineas_electricas', COUNT(*) FROM lineas_electricas
UNION ALL SELECT 'lotes', COUNT(*) FROM lotes
UNION ALL SELECT 'estadisticas_diarias', COUNT(*) FROM estadisticas_diarias
UNION ALL SELECT 'consultas_guardadas', COUNT(*) FROM consultas_guardadas
UNION ALL SELECT 'consultas_historial', COUNT(*) FROM consultas_historial
UNION ALL SELECT 'fichas_parcelas', COUNT(*) FROM fichas_parcelas
UNION ALL SELECT 'territorio_elementos', COUNT(*) FROM territorio_elementos
UNION ALL SELECT 'territorio_fuentes', COUNT(*) FROM territorio_fuentes
UNION ALL SELECT 'territorio_sin_geometria', COUNT(*) FROM territorio_sin_geometria;
```

Deben aparecer 12 tablas y **12 conteos de 0**, no estimaciones de `TABLE_ROWS`. Revisar definiciones con `SHOW CREATE TABLE nombre_tabla`; en `information_schema.STATISTICS` el índice `territorio_geometria` debe ser SPATIAL y en `information_schema.ST_GEOMETRY_COLUMNS` las tres columnas espaciales deben tener `SRS_ID=4326`.

## Validación ejecutada el 19/10/2026

Se exportó una copia de los archivos de `origin/main` mediante `git archive` dentro de `target/`, sin checkout, merge ni rebase. Se compiló esa copia con Maven y las versiones reales del POM: Spring Boot 4.1.1, Hibernate ORM/Spatial 7.4.5 y Java 21.

Instancia temporal MySQL Community **8.0.46**, ligada únicamente a `127.0.0.1:33328`, directorio de datos nuevo bajo `target/`. No se utilizó `verificador_lotes` ni se conectó al servicio de desarrollo.

- Se generaron nueve tablas esperadas mediante Hibernate, con estrategia `PhysicalNamingStrategySnakeCaseImpl`, sin arrancar Spring Boot ni inicializadores.
- Se extrajeron **solo los literales SCHEMA** del importador de `origin/main` y se ejecutaron en otra base temporal. No se ejecutó el importador ni se cargó ningún dataset.
- El instalador completo creó la base nueva `lotes_vacio_actual_20261019` con 12 tablas. Las doce quedaron con cero filas.
- Los doce `SHOW CREATE TABLE` coinciden exactamente con su fuente esperada: nueve con Hibernate y tres con el DDL del importador. Esto incluye tipos, nulabilidad/defaults, PK, UNIQUE, FK, longitudes, índices, SRID y collation.
- Se comprobó manualmente mediante metadatos: cuatro FK con NO ACTION/NO ACTION, las UNIQUE, el índice espacial territorial, JSON y tres columnas espaciales SRID 4326. No se generaron vistas, triggers ni rutinas.
- Se inició Hibernate directamente con las **nueve entidades actuales** y `JPA_DDL_AUTO=validate` en la base temporal: **exit code 0**. No se inició la aplicación completa, para evitar cargar roles u otros registros. Hibernate no valida las tablas territoriales; se verificaron por separado como se describe arriba.
- Segunda ejecución completa: **ERROR 1007, exit code 1**, sin cambios. También se probó un cliente que continúa tras errores: los doce bloques omitieron sus DDL. Después de ambas pruebas, las doce estructuras seguían idénticas y todos los conteos eran cero.

No se detectaron incompatibilidades estructurales pendientes frente a la revisión citada. Esto no certifica el esquema ni los datos de la base de desarrollo, que no fue consultada ni modificada. Tampoco es una prueba funcional de las API con datos reales.

La instancia temporal se detuvo al terminar. Los respaldos de los dos archivos originales, las fuentes aisladas, los metadatos y los diffs de revisión quedan en `target/` (ignorado por Git). No se borró ninguna base existente ni se hicieron commits, push o cambios de rama.
