# Migraciones de base de datos con Flyway

Desde esta versión **Flyway es el único responsable del esquema** de MySQL. Hibernate ya no crea ni modifica tablas: solo comprueba al arrancar que las entidades coinciden con las tablas (`JPA_DDL_AUTO=validate`, el valor predeterminado). Si algo no coincide, la aplicación no inicia y el error indica qué tabla o columna difiere; nunca toca datos.

Las migraciones están en `src/main/resources/db/migration`:

| Versión | Archivo | Qué crea |
| --- | --- | --- |
| 1 | `V1__esquema_aplicacion.sql` | Las nueve tablas de la aplicación: departamentos, roles, usuarios, lineas_electricas, lotes, estadisticas_diarias, consultas_guardadas, consultas_historial y fichas_parcelas. |
| 2 | `V2__capas_territoriales.sql` | territorio_elementos, territorio_fuentes y territorio_sin_geometria, más la columna `duplicado_de` si falta. |
| 3 | `V3__retirar_datos_demo.sql` | No crea nada: retira los datos y las tablas de demostración (ver abajo). |

Ambas son idempotentes: usan `CREATE TABLE IF NOT EXISTS` y agregan `duplicado_de` solo si no existe. Repetirlas no recrea tablas, no falla y no modifica ninguna fila. Las definiciones de V1 son las mismas que Hibernate venía generando (mismos nombres de índices y claves), de modo que una base ya instalada y una base nueva quedan idénticas.

## Instalación nueva (base vacía)

1. Crear la base vacía en MySQL 8, por ejemplo `CREATE DATABASE verificador_lotes CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`. `.\iniciar-local.cmd` la crea solo.
2. Arrancar el backend. Flyway crea V1 y V2 y deja registrado el historial en la tabla `flyway_schema_history`.
3. Importar las capas oficiales con `py -3 scripts/importar_territorio.py` (o usar `.\iniciar-local.cmd`, que hace todo en orden).

El importador ejecuta la misma `V2__capas_territoriales.sql` antes de cargar datos, así que también funciona si todavía no se arrancó el backend. Cuando después arranca, Flyway completa V1 y registra V2 sin tocar lo importado.

## Instalación existente (base con datos)

Es el caso de cualquier computadora que ya venía usando la aplicación con `ddl-auto=update`.

1. **Hacer una copia de seguridad antes de actualizar**, por ejemplo: `mysqldump --user=<usuario> --password --routines --databases verificador_lotes > copia.sql`, en una carpeta fuera del repositorio.
2. Actualizar el código y arrancar el backend como siempre.
3. Flyway detecta que la base ya tiene tablas pero no tiene historial, la **marca como versión 0** (baseline) y aplica V1 y V2. Como son idempotentes, no recrean nada: solo se agrega `duplicado_de` si la base se había importado antes de ocultar parcelas repetidas.
   En el log aparecen avisos `WARN ... Table '...' already exists`: son normales, indican que la tabla ya estaba y se dejó como está.
4. Para verificar: `SELECT version, description, type, success FROM flyway_schema_history;` debe mostrar `0 BASELINE`, `1 SQL` y `2 SQL` con `success = 1`.

Usuarios, fichas, consultas guardadas, historial y capas importadas se conservan. Las migraciones no modifican ni borran filas, así que un fallo no puede perder datos; la copia de seguridad permite volver atrás de todos modos.

Arrancar de nuevo, o arrancar con otra computadora que usa la misma base, no vuelve a migrar: Flyway compara su historial y no encuentra versiones pendientes.

## Cómo agregar un cambio de esquema

1. Crear `V3__descripcion_corta.sql` (el número siguiente, dos guiones bajos, sin espacios) en `db/migration`.
2. Cambiar la entidad Java en el mismo Pull Request. Si no coinciden, `validate` impide que arranque.
3. **No editar nunca** una migración ya mergeada (Flyway calcula un checksum y se negaría a arrancar). Un error se corrige con una migración nueva.
4. Procurar migraciones idempotentes y transaccionales cuando MySQL lo permita (los `CREATE`/`ALTER` confirman solos, así que conviene que sean repetibles).
5. Si la tabla es territorial (la carga el importador), también va en su migración: el importador la lee de ahí.

## Limpieza de datos demostrativos (V3)

V3 retira lo que dejaron las versiones con lotes de ejemplo. Se aplica sola en el próximo arranque, así que **antes de actualizar una instalación con datos**:

1. Hacer la copia de seguridad (arriba).
2. Opcional: abrir `scripts/diagnostico-datos-demo.sql` en MySQL Workbench; solo lee y muestra qué se va a borrar y qué se conserva.
3. **Configurar un administrador real** (`ADMIN_EMAIL` y `ADMIN_PASSWORD`, mínimo 12 caracteres, en el entorno o en la configuración de ejecución). V3 elimina las cuentas `demo.admin@loteseguro.invalid`, `demo.usuario@loteseguro.invalid` y `usuario.demo@loteseguro.invalid`; si no hay otro administrador, nadie podría editar fichas. El administrador se crea al arrancar si su correo no existe (si existe, se conserva su contraseña).

Qué hace V3:

- Borra las cuentas demo con sus consultas guardadas e historial.
- Borra los lotes demo (`LOT-DEMO-*`, `es_dato_simulado = 1` o fuente demostrativa), la línea eléctrica `RED DEMOSTRATIVA` y el departamento demo si ningún lote lo usa.
- Elimina las tablas `lotes`, `departamentos` y `lineas_electricas` **solo si quedaron vacías**. Si alguna conserva filas que no son demo (por ejemplo un lote cargado por un administrador), la tabla queda sin usar para revisarla a mano.
- No toca: capas territoriales, cuentas reales, fichas (aunque las haya firmado una cuenta demo), guardadas e historial de cuentas reales ni estadísticas.
- Puede repetirse sin error: cada paso comprueba antes que la tabla exista.

## Seguridad y límites

- `spring.flyway.clean-disabled=true`: la aplicación no puede borrar el contenido de una base. Solo la prueba de integración lo habilita, sobre `lotes_flyway_test`.
- `JPA_DDL_AUTO=update`, `create` o `create-drop` ya no se usan en instalaciones reales.
- No agregar `spring.jpa.defer-datasource-initialization=true`: hace que Hibernate valide antes de que Flyway cree las tablas.
- Una base que ya tiene tablas se toma como instalación existente: no apuntar `DB_URL` a la base de otro sistema, porque se le crearían las tablas que faltan.
- Una base cuyas tablas difieren de las entidades (por ejemplo, creada a mano o con una versión muy anterior) no se repara sola: `validate` informa la diferencia y hay que resolverla con una migración revisada.

## Pruebas

`MigracionesFlywayMySqlIT` (perfil `mysql-it`) arranca la aplicación completa, con Hibernate en `validate`, sobre una base propia `lotes_flyway_test` que vacía antes de cada caso. Comprueba:

- base nueva: quedan las nueve tablas vigentes, el historial queda en V1, V2 y V3 y Hibernate valida;
- repetir el arranque no agrega migraciones ni cambia datos;
- base con tablas de la instalación anterior y datos reales: se marca el baseline, se agrega `duplicado_de` y no se pierde ninguna fila;
- importador antes que el backend: el backend completa el esquema;
- V3 con datos demo: se borran las cuentas, lotes, línea y departamento demo, se conservan cuentas reales, fichas, guardadas y capas, se retiran las tablas vacías y repetir el script no falla;
- V3 con un lote que no es demo: no se pierde y su tabla se conserva.

Para ejecutarla localmente hay que crear `lotes_flyway_test` vacía (ver [integración MySQL](integracion-mysql.md)).
