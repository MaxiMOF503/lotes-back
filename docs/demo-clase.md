# Demostración en clase

La base de cada integrante es independiente. Para cargar los diez lotes ficticios de Luján de Cuyo, iniciar expresamente con el script de esta guía o configurar DEMO_LOTES_ENABLED=true y DEMO_SQL_INIT_MODE=always. La ejecución normal prioriza las capas reales y no inserta ejemplos. No son datos catastrales reales.

## Iniciar con cuentas de clase

Desde PowerShell, en la carpeta del backend, ejecutar:

```powershell
.\examples\iniciar-demo.ps1
```

El script solicita la contraseña de MySQL (si no está configurada) y dos contraseñas nuevas de al menos 12 caracteres. No las escribe en archivos ni en GitHub. Si PowerShell impide ejecutar scripts, configurar las variables de la sección siguiente en Spring Tools.

| Correo para iniciar sesión | Rol | Acceso |
| --- | --- | --- |
| `demo.admin@loteseguro.invalid` | ADMIN | Mantenedor y estadísticas |
| `demo.usuario@loteseguro.invalid` | USUARIO | Consulta interna de solo lectura |

Las contraseñas son las que se eligieron al iniciar el backend. Compartirlas con el profesor por un canal privado. Si se vuelve a iniciar en modo demo con otras contraseñas, se actualizan **solo estas dos cuentas de clase**. Los demás usuarios y lotes no se modifican.

El frontend se inicia aparte desde `lotes-front` con `npm.cmd start` y se abre en `http://localhost:4200/`. Si el puerto 8080 está ocupado, detener primero la otra instancia del backend.

## Desde Spring Tools

En **Run Configurations → Spring Boot App → lotes - LotesApplication → Environment**, configurar:

- `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` para la propia base MySQL.
- `DEMO_ACCOUNTS_ENABLED=true`.
- `DEMO_LOTES_ENABLED=true` y `DEMO_SQL_INIT_MODE=always` para incluir lotes ficticios.
- `DEMO_ADMIN_PASSWORD` y `DEMO_USER_PASSWORD` con valores de al menos 12 caracteres elegidos para esa PC.

Ejecutar nuevamente el backend. Las cuentas se crean en esa base, no en la base de otra persona. Para no crear ni actualizar cuentas de clase, dejar `DEMO_ACCOUNTS_ENABLED` sin configurar o en `false`. En un servidor público debe permanecer desactivado.

**No usar** `usuario.demo@loteseguro.invalid`: es un registro antiguo no operativo que puede aparecer en la tabla de usuarios. Las dos cuentas válidas de esta guía terminan en `demo.admin` y `demo.usuario`.
