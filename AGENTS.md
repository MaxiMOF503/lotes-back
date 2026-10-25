# Instrucciones para agentes de IA (Codex, Claude, etc.)

Backend de LoteSeguro: Spring Boot 4, Java 21, MySQL 8 con Flyway. Todo en español (código, mensajes, commits, PR).

## Después de hacer `git pull`

El código llega con `git pull`, pero **los datos y el esquema de tu base local no se actualizan solos**. Hacé esto
en orden, cada vez que el pull trae cambios en `data/`, `scripts/importar_territorio.py`, `src/main/resources/db/migration/`
o `pom.xml`:

1. **Capas territoriales** (catastro, agua de AySAM y de Aguas Luján, electricidad): importarlas es idempotente
   y no duplica nada. Con las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` de la persona, ejecutar:
   - Windows: `.\iniciar-local.cmd --preparar-solo` (pide las credenciales de MySQL y no arranca nada), o
   - `py -3 scripts/importar_territorio.py` (lee las mismas variables del entorno).
   Si no tenés esas credenciales, **pedíselas a la persona**; no las inventes ni las escribas en ningún archivo.
2. **Esquema**: Flyway aplica solo las migraciones nuevas al arrancar el backend (`./mvnw spring-boot:run`). No hay que
   ejecutar SQL a mano. Si Hibernate dice `Schema validation`, no cambies `JPA_DDL_AUTO`: avisá a la persona.
3. **Comprobar**: con el backend arriba, `GET http://localhost:8080/api/public/v1/estado` tiene que devolver
   `"capasFaltantes": []`. Si lista capas, repetí el paso 1. El log también avisa al arrancar:
   `Faltan capas territoriales por importar`.
4. Antes de dar la tarea por terminada: `./mvnw verify` y `py -3 -m unittest discover -s scripts -p "test_*.py"`.

## Reglas del proyecto

- **Nunca** subir a Git contraseñas, volcados de la base ni archivos con datos personales. En particular, la capa
  `baldios` del geoportal trae nombre, CUIT y domicilio de los titulares: no se importa ni se versiona completa
  (ver `data/README.md`). El importador rechaza campos fuera de su lista permitida.
- Los cambios de esquema van **solo** como migraciones nuevas en `src/main/resources/db/migration/` (`V4__...`,
  `V5__...`). Nunca editar una migración ya mergeada: Flyway la rechaza.
- Ramas `feature/...`; un PR por tarea con `Closes #N`. Los datos oficiales viven en `data/` y se cargan con el importador.
- Errores de la API: formato único y códigos estables en `docs/errores-api.md`. Instalación y detalles: `docs/`.
