# LoteSeguro · backend

[![Backend CI](https://github.com/MaxiMOF503/lotes-back/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/MaxiMOF503/lotes-back/actions/workflows/backend-ci.yml)

Spring Boot 4.1.1, Java 21, Maven Wrapper, Spring MVC, Bean Validation y JPA/MySQL.

## Instalar y presentar en otra PC

La [guía de instalación local](docs/instalacion-local.md) explica requisitos, descarga de ambos proyectos, arranque y cuentas para el profesor y el equipo.

En Windows, con JDK 21, MySQL 8.0.18+, cliente MySQL y Python 3.10+ instalados:

```powershell
.\iniciar-local.cmd
```

El asistente crea la base local, importa las tres capas reales incluidas en data/ y arranca el backend en localhost:8080. Pide las credenciales de MySQL de esa PC y las contraseñas para demo.admin@loteseguro.invalid y demo.usuario@loteseguro.invalid. No guarda contraseñas en archivos. Cada instalación tiene su propia base.

El [mapa territorial real](docs/datos-territoriales-reales.md) consulta parcelas catastrales, radio AYSAM y cercanía de tramos eléctricos. Expone procedencia y limitaciones: no confirma titularidad ni conexión de servicios.

Los lotes ficticios son optativos; no se insertan en el arranque normal. Para utilizarlos, ejecutar `.\iniciar-local.cmd --con-ejemplos` o seguir la [guía de demostración](docs/demo-clase.md). No se borran los usuarios ni lotes existentes.

## Ejecución manual

Configurar DB_URL (por ejemplo jdbc:mysql://localhost:3306/verificador_lotes?useSSL=false&serverTimezone=UTC), DB_USERNAME y DB_PASSWORD. La base debe existir; JPA_DDL_AUTO tiene valor predeterminado update. Preparar las capas con `py -3 scripts/importar_territorio.py`, usando esa conexión.

```powershell
.\mvnw.cmd spring-boot:run
```

Para habilitar ejemplos manualmente: DEMO_LOTES_ENABLED=true y DEMO_SQL_INIT_MODE=always. Las cuentas de clase se habilitan con DEMO_ACCOUNTS_ENABLED=true y contraseñas propias en DEMO_ADMIN_PASSWORD / DEMO_USER_PASSWORD. La guía de instalación describe estas opciones.

## Contratos y funcionalidades

La [nueva área interna](docs/area-interna-real.md) vincula las fichas complementarias con el catastro real y agrega consultas guardadas privadas por cuenta. El mantenedor anterior se conserva.

- [Ficha pública de lote](docs/contrato-ficha-publica.md): parámetros, respuestas, errores y datos parciales; incluye dónde consultar y procedencia.
- [Mantenedor y estadísticas](docs/mantenedor-estadisticas.md): administración de lotes, permisos y contadores diarios anónimos.
- [Datos territoriales reales](docs/datos-territoriales-reales.md): importación, calidad, APIs espaciales y limitaciones.
- Swagger UI: /swagger-ui/index.html. OpenAPI JSON: /v3/api-docs.

La búsqueda por dirección o identificador sigue consultando los lotes registrados. El mapa consulta las parcelas importadas; no convierte automáticamente todo el catastro en lotes editables.

## Verificación

```powershell
.\mvnw.cmd test
py -3 -m unittest discover -s scripts -p 'test_*.py' -v
.\mvnw.cmd verify
```

El workflow [Backend CI](.github/workflows/backend-ci.yml) reproduce estas verificaciones en cada Pull Request y push a `main`. Además prepara dos bases temporales MySQL 8, importa las capas territoriales incluidas y ejecuta todas las pruebas `*IT`. No utiliza la base ni las contraseñas de ningún integrante. Los detalles para reproducir los controles están en [Integración continua](docs/integracion-continua.md).

Los tests habituales no necesitan MySQL. Para probar el cruce espacial contra MySQL real, importar las capas y configurar DB_*:

```powershell
$env:TERRITORIO_IT='true'
.\mvnw.cmd -Pmysql-it "-Dit.test=TerritorioMySqlIT" verify
```

En Linux/macOS usar ./mvnw. verify ejecuta tests y empaqueta el JAR. El [perfil de integración MySQL](docs/integracion-mysql.md) contiene instrucciones adicionales. Para probar la ficha: [solicitudes HTTP](examples/ficha-publica.http) o examples/probar-ficha.ps1 (PowerShell 7).
