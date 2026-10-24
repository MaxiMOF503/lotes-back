# LoteSeguro · backend

[![Backend CI](https://github.com/MaxiMOF503/lotes-back/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/MaxiMOF503/lotes-back/actions/workflows/backend-ci.yml)

Spring Boot 4.1.1, Java 21, Maven Wrapper, Spring MVC, Bean Validation y JPA/MySQL.

## Instalar y presentar en otra PC

La [guía de instalación local](docs/instalacion-local.md) explica requisitos, descarga de ambos proyectos, arranque y cuentas para el profesor y el equipo.

En Windows, con JDK 21, MySQL 8.0.18+, cliente MySQL y Python 3.10+ instalados:

```powershell
.\iniciar-local.cmd
```

El asistente crea la base local, importa las tres capas reales incluidas en data/ y arranca el backend en localhost:8080. Pide las credenciales de MySQL de esa PC y el correo y la contraseña del administrador (mínimo 12 caracteres). No guarda contraseñas en archivos. Cada instalación tiene su propia base.

El [mapa territorial real](docs/datos-territoriales-reales.md) consulta parcelas catastrales, radio AYSAM y cercanía de tramos eléctricos. Expone procedencia y limitaciones: no confirma titularidad ni conexión de servicios.

La aplicación no incluye lotes, cuentas ni datos de ejemplo: trabaja solo con las capas oficiales importadas. Si venís de una versión anterior, la migración V3 retira los datos de demostración (ver [migraciones](docs/migraciones-flyway.md)).

## Ejecución manual

Configurar DB_URL (por ejemplo jdbc:mysql://localhost:3306/verificador_lotes?useSSL=false&serverTimezone=UTC), DB_USERNAME y DB_PASSWORD. La base debe existir y puede estar vacía: Flyway crea o completa las tablas al arrancar (ver [migraciones](docs/migraciones-flyway.md)) y Hibernate solo las valida (JPA_DDL_AUTO=validate). Antes de actualizar una instalación con datos, hacer una copia de seguridad. Preparar las capas con `py -3 scripts/importar_territorio.py`, usando esa conexión.

```powershell
.\mvnw.cmd spring-boot:run
```

Para crear el primer administrador, definir ADMIN_EMAIL y ADMIN_PASSWORD (mínimo 12 caracteres) en el entorno antes de arrancar. Si la cuenta no existe se crea; si existe, se conserva su contraseña. Los usuarios comunes se registran desde el frontend.

## Contratos y funcionalidades

La [nueva área interna](docs/area-interna-real.md) vincula las fichas complementarias con el catastro real y agrega consultas guardadas privadas por cuenta.

- [Estadísticas](docs/estadisticas.md): contadores diarios anónimos de visitas y consultas por mapa, nomenclatura y dirección.
- [Datos territoriales reales](docs/datos-territoriales-reales.md): importación, calidad, APIs espaciales y limitaciones.
- [Migraciones de base de datos](docs/migraciones-flyway.md): Flyway, instalación nueva, instalación existente y cómo agregar cambios de esquema.
- Swagger UI: /swagger-ui/index.html. OpenAPI JSON: /v3/api-docs.

Las consultas (mapa, nomenclatura y dirección) trabajan sobre las parcelas importadas del catastro. La dirección se resuelve con Georef y devuelve las parcelas cercanas para que la persona elija la suya.

## Verificación

```powershell
.\mvnw.cmd test
py -3 -m unittest discover -s scripts -p 'test_*.py' -v
.\mvnw.cmd verify
```

El workflow [Backend CI](.github/workflows/backend-ci.yml) reproduce estas verificaciones en cada Pull Request y push a `main`. Además prepara bases temporales MySQL 8, importa las capas territoriales incluidas y ejecuta todas las pruebas `*IT`. No utiliza la base ni las contraseñas de ningún integrante. Los detalles para reproducir los controles están en [Integración continua](docs/integracion-continua.md).

Los tests habituales no necesitan MySQL. Para probar el cruce espacial contra MySQL real, importar las capas y configurar DB_*:

```powershell
$env:TERRITORIO_IT='true'
.\mvnw.cmd -Pmysql-it "-Dit.test=TerritorioMySqlIT" verify
```

En Linux/macOS usar ./mvnw. verify ejecuta tests y empaqueta el JAR. El [perfil de integración MySQL](docs/integracion-mysql.md) contiene instrucciones adicionales.
