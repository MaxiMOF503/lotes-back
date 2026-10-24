# Pruebas de integración con MySQL

El perfil `mysql-it` ejecuta las clases `*IT` con MySQL 8 real (Failsafe). Los tests habituales (`mvnw test` / `verify` sin el perfil) no necesitan MySQL.

## Requisitos

- Java 21 y MySQL 8.x.
- Capas territoriales importadas en la base de la aplicación (`py -3 scripts/importar_territorio.py` con `DB_*`), para `TerritorioMySqlIT`, `EspacioTerritorialMySqlIT` y `ActividadTerritorialMySqlIT`. Se activan con `TERRITORIO_IT=true` y solo leen; las pruebas con datos personales se revierten por transacción.
- Una **base descartable y exclusiva** llamada `lotes_flyway_test`, creada vacía antes de ejecutar, para `MigracionesFlywayMySqlIT`.

## Ejecutar

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/verificador_lotes?serverTimezone=UTC'
$env:DB_USERNAME = 'usuario_de_pruebas'
$env:DB_PASSWORD = '<contraseña local>'
$env:MYSQL_IT_URL = 'jdbc:mysql://127.0.0.1:3306/lotes_flyway_test?serverTimezone=UTC'
$env:MYSQL_IT_USERNAME = 'usuario_de_pruebas'
$env:MYSQL_IT_PASSWORD = '<contraseña local>'
$env:TERRITORIO_IT = 'true'
.\mvnw.cmd -Pmysql-it verify
```

Las contraseñas se definen solo en la terminal. Si falta `MYSQL_IT_URL` o no apunta a `lotes_flyway_test`, la prueba de migraciones falla en lugar de omitirse. **`MigracionesFlywayMySqlIT` vacía por completo `lotes_flyway_test` antes de cada caso**: nunca apuntarla a una base con datos.

En Linux/macOS usar `./mvnw`. Los informes quedan en `target/failsafe-reports` y `target/surefire-reports`.

## Cobertura

- `TerritorioMySqlIT`: consulta espacial real (SRID 4326, ejes longitud/latitud), parcelas repetidas ocultas, parcelas cercanas a un punto de Georef.
- `EspacioTerritorialMySqlIT` y `ActividadTerritorialMySqlIT`: consultas guardadas, fichas con control de versión, historial y privacidad entre cuentas.
- `MigracionesFlywayMySqlIT`: base nueva, repetir el arranque, base con tablas y datos de la instalación anterior, importador primero y, para la migración V3, que retira solo los datos demostrativos y conserva cuentas reales, fichas, guardadas y capas (ver [migraciones](migraciones-flyway.md)).
