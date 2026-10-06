# Backend de consulta pública de lotes

Spring Boot 4.1.1, Java 21, Maven Wrapper, Spring MVC, Bean Validation y JPA/MySQL.

El [mantenedor y las estadísticas](docs/mantenedor-estadisticas.md) agregan administración de lotes, acceso con rol ADMIN y contadores diarios anónimos. La guía explica cómo crear el administrador y conectar el frontend.

El contrato de `GET /api/public/v1/lotes/ficha` está en [docs/contrato-ficha-publica.md](docs/contrato-ficha-publica.md). Incluye parámetros, todos los DTOs públicos, ejemplos y tratamiento de errores para el futuro frontend.

## Ejecución

Requiere Java 21 y MySQL 8+ con la base `verificador_lotes` creada. Configurar las variables `DB_URL` (por ejemplo `jdbc:mysql://localhost:3306/verificador_lotes?useSSL=false&serverTimezone=UTC`), `DB_USERNAME` y `DB_PASSWORD`. `JPA_DDL_AUTO` tiene valor predeterminado `update`.

```powershell
.\mvnw.cmd spring-boot:run
```

El inicializador carga el lote `LOT-DEMO-001` y [la carga SQL de Luján de Cuyo](src/main/resources/data.sql) agrega diez lotes ficticios (`LOT-DEMO-LUJ-002` a `LOT-DEMO-LUJ-011`) al iniciar el backend. Los puntos indican localidades aproximadas: no son parcelas ni domicilios reales. La carga es reejecutable: omite identificadores existentes sin reemplazar sus datos, incluso si fueron editados o desactivados. Cada integrante necesita su propia base MySQL; después de actualizar el repositorio, debe iniciar el backend conectado a esa base para recibir los ejemplos. También puede ejecutar `src/main/resources/data.sql` manualmente en DBeaver sobre `verificador_lotes`. La ficha es simulada, sin valor oficial. Puerto predeterminado: 8080.

Para presentar el proyecto con un administrador y un usuario de consulta, usar la [guía de demostración en clase](docs/demo-clase.md). El modo demo pide las contraseñas al iniciarse y no publica credenciales en el repositorio.

Swagger UI: `/swagger-ui/index.html`. OpenAPI JSON: `/v3/api-docs`.

## Verificación

```powershell
.\mvnw.cmd test
.\mvnw.cmd "-Dtest=PublicLoteControllerTest,PublicLoteContractTest" test
.\mvnw.cmd verify
```

En Linux/macOS usar `./mvnw`. `verify` ejecuta tests y empaqueta el JAR. No hay lint ni Checkstyle configurado en el POM. Los tests habituales no requieren MySQL. El perfil opcional `mysql-it` prueba la aplicación completa y la consulta espacial contra MySQL 8 real; consultar [las instrucciones de integración](docs/integracion-mysql.md).

La [propuesta de PR](docs/propuesta-pr.md) incluye las respuestas de integración. No requiere un frontend para compilar o probarse.

La ficha incluye `dondeConsultar`, con contacto, oficina, instrucciones y procedencia del departamento; los datos faltantes se conservan como null.

Para probar el contrato contra la aplicación iniciada: [solicitudes HTTP](examples/ficha-publica.http) o `./examples/probar-ficha.ps1` (PowerShell 7). Para verificar también persistencia real, configurar las variables de la guía y ejecutar `./mvnw.cmd -Pmysql-it verify`.
