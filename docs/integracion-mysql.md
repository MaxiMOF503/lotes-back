# Integración de ficha pública con MySQL

El perfil `mysql-it` ejecuta `PublicLoteMySqlIT` con la aplicación completa, inicializador, JPA, consulta espacial real y MockMvc, sin mocks de repositorios. No agrega dependencias de ejecución ni requiere Docker.

## Requisitos

- Java 21 y MySQL 8.x.
- Una **base descartable y exclusiva** llamada `lotes_ficha_test`, creada antes de ejecutar.
- Un usuario con permisos sobre esa base para crear y eliminar tablas y operar con los datos.

El test configura `ddl-auto=create-drop`: reemplaza las tablas de la base indicada y las elimina al cerrar el contexto. Nunca usar una base con datos que deban conservarse. La configuración comprueba el nombre exacto `lotes_ficha_test` antes de iniciar JPA y no toma las credenciales `DB_*` de la aplicación.

## Ejecutar

Crear `lotes_ficha_test` en la instancia destinada a pruebas. Configurar las credenciales localmente, sin guardarlas en Git:

```powershell
$env:MYSQL_IT_URL = 'jdbc:mysql://127.0.0.1:3306/lotes_ficha_test?serverTimezone=UTC'
$env:MYSQL_IT_USERNAME = 'usuario_de_pruebas'
$env:MYSQL_IT_PASSWORD = '<contraseña local>'
./mvnw.cmd -Pmysql-it verify
```

Si falta la URL o el usuario, o MySQL no responde, el perfil falla: no omite silenciosamente la integración. `MYSQL_IT_PASSWORD` puede omitirse sólo si la instancia de pruebas acepta contraseña vacía. En Linux/macOS exportar las mismas variables y ejecutar `./mvnw -Pmysql-it verify`.

`./mvnw.cmd verify` conserva el ciclo normal de tests sin requerir MySQL; las clases terminadas en `IT` se ejecutan mediante Failsafe únicamente al activar `mysql-it`. Los informes quedan en `target/failsafe-reports` y `target/surefire-reports`.

## Cobertura

- Motor MySQL 8 real, punto persistido con SRID 4326 y latitud/longitud correctas.
- HTTP 200 por identificador y por coordenadas exactas.
- Orientación y fuente del departamento recuperadas desde la base.
- HTTP 404 para identificador inexistente, punto cercano, coordenadas invertidas y punto sin coincidencia.
- HTTP 200 con campos opcionales null persistidos en MySQL.

La prueba con datos parciales se revierte por transacción. Las otras consultas usan el lote cargado por el inicializador real, sin una transacción de test que oculte problemas de carga del departamento.

## Verificación en esta entrega

Ejecutado sobre una instancia temporal independiente de MySQL Community Server 8.0.46, limitada a `127.0.0.1:33317`, con directorio de datos propio. No se utilizó la base de la aplicación ni se modificó el servicio MySQL instalado.

La inicialización de esa instancia sigue el mecanismo documentado por [MySQL para directorios de datos nuevos](https://dev.mysql.com/doc/refman/8.0/en/data-directory-initialization.html). La instancia temporal se detiene al terminar la verificación.
