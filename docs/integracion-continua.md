# Integración continua del backend

El workflow `.github/workflows/backend-ci.yml` se ejecuta en cada Pull Request y en cada push a `main`. No usa bases, cuentas ni secretos personales del equipo.

## Verificaciones rápidas

El primer trabajo usa Java 21 y Python 3.12 para ejecutar:

```powershell
.\mvnw.cmd verify
py -3 -m unittest discover -s scripts -p 'test_*.py' -v
```

Esto compila, ejecuta los tests habituales, empaqueta el JAR y verifica los scripts de importación y arranque local.

## Integración MySQL

El segundo trabajo crea un servicio descartable MySQL 8 con tres bases:

- `verificador_lotes`: recibe las tres capas reales incluidas en `data/` y ejecuta las pruebas territoriales, de Mi espacio e historial.
- `lotes_ficha_test`: se usa exclusivamente por `PublicLoteMySqlIT`, que crea y elimina su esquema durante la prueba (con Flyway desactivado).
- `lotes_flyway_test`: la usa `MigracionesFlywayMySqlIT`, que la vacía y prueba las migraciones de [Flyway](migraciones-flyway.md) sobre una base nueva y sobre una instalación existente.

Las credenciales declaradas en el workflow existen solo durante esa ejecución. No corresponden a ninguna instalación del equipo.

Para reproducir esta verificación en Windows se necesita una base MySQL local descartable, las capas importadas y las bases vacías `lotes_ficha_test` y `lotes_flyway_test` (`CREATE DATABASE lotes_flyway_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`):

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/verificador_lotes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<contraseña local>'
$env:MYSQL_IT_URL = 'jdbc:mysql://127.0.0.1:3306/lotes_ficha_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:MYSQL_IT_USERNAME = 'root'
$env:MYSQL_IT_PASSWORD = '<contraseña local>'
$env:TERRITORIO_IT = 'true'
$env:JPA_DDL_AUTO = 'validate'
$env:DEMO_LOTES_ENABLED = 'false'
$env:DEMO_SQL_INIT_MODE = 'never'
$env:DEMO_ACCOUNTS_ENABLED = 'false'

py -3 scripts/importar_territorio.py
.\mvnw.cmd -Pmysql-it verify
```

Las contraseñas del ejemplo deben definirse solamente en la terminal y nunca guardarse en archivos versionados. Los datos demo permanecen deshabilitados para la base territorial. `PublicLoteMySqlIT` activa su único lote histórico mediante propiedades internas del test y solamente dentro de `lotes_ficha_test`; ese caso se retirará junto con el contrato antiguo al completar la issue #35.

## Diagnóstico de fallos

Cuando falla un trabajo, el workflow conserva durante siete días los archivos de:

- `target/surefire-reports` para tests habituales;
- `target/failsafe-reports` para integración MySQL.

Primero debe revisarse el paso fallido y luego descargarse el artefacto correspondiente desde la ejecución de GitHub Actions. Los reportes pueden contener nombres de tests y trazas, pero el workflow no imprime contraseñas ni conecta con bases externas.
