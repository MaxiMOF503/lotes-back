# Backend de consulta pública de lotes

Spring Boot 4.1.1, Java 21, Maven Wrapper, Spring MVC, Bean Validation y JPA/MySQL.

El contrato de `GET /api/public/v1/lotes/ficha` está en [docs/contrato-ficha-publica.md](docs/contrato-ficha-publica.md). Incluye parámetros, todos los DTOs públicos, ejemplos y tratamiento de errores para el futuro frontend.

## Ejecución

Requiere Java 21 y MySQL 8+ con la base `verificador_lotes` creada. Configurar las variables `DB_URL` (por ejemplo `jdbc:mysql://localhost:3306/verificador_lotes?useSSL=false&serverTimezone=UTC`), `DB_USERNAME` y `DB_PASSWORD`. `JPA_DDL_AUTO` tiene valor predeterminado `update`.

```powershell
.\mvnw.cmd spring-boot:run
```

El inicializador existente carga el lote `LOT-DEMO-001` en las coordenadas `-32.8895, -68.8458` y los datos demostrativos asociados si no existen. La ficha es simulada, sin valor oficial. Puerto predeterminado: 8080.

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
