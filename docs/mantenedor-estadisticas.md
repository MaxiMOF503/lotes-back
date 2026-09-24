# Mantenedor de lotes y estadísticas

## Qué incluye

- Inicio y cierre de sesión con contraseña BCrypt; mantenedor y estadísticas exclusivos del rol ADMIN.
- Alta, edición y listado paginado de lotes persistidos en MySQL. No se implementa eliminación.
- Validación de coordenadas, identificadores duplicados, distancia no negativa y departamento existente.
- Procedencia independiente para lote, zonificación, electricidad y agua: SIMULADO, NO_VERIFICADO u OFICIAL. Una fuente oficial exige referencia documental. La etiqueta expresa lo declarado por el administrador, no una verificación automática del documento.
- Ficha pública conectada a los datos guardados. Los campos opcionales vacíos se conservan sin información; los registros anteriores sin procedencia conservan el tratamiento demostrativo.
- Totales diarios de visitas y consultas, filtrados por fechas, criterio y resultado. Días sin actividad aparecen con cero.

## Preparación y ejecución

Requiere Java 21, MySQL 8 y las variables DB_URL, DB_USERNAME y DB_PASSWORD del README. En una base de desarrollo, el valor predeterminado JPA_DDL_AUTO=update agrega las columnas de procedencia a lotes y crea estadisticas_diarias. No usar create-drop con datos reales. Si se utiliza validate, preparar previamente una migración de esquema revisada para esas columnas y tabla; este cambio no incorpora un gestor de migraciones.

Para crear el primer administrador, configurar ADMIN_EMAIL y ADMIN_PASSWORD (mínimo 12 caracteres) en el entorno y ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

No hay contraseña predeterminada. El inicializador crea el usuario sólo si no existe su email; no cambia contraseñas ni roles existentes. Retirar esas variables después de la creación. Las credenciales no deben guardarse en el repositorio.

El frontend está en el proyecto lotes-front y utiliza el mismo origen para /api. En desarrollo, su proxy apunta a http://127.0.0.1:8080. En despliegue, servir frontend y /api detrás del mismo origen HTTPS y configurar SERVER_SERVLET_SESSION_COOKIE_SECURE=true. La sesión usa cookie HttpOnly; las escrituras exigen token CSRF. No se guardan contraseñas ni tokens en localStorage.

## Contratos agregados

| Método y ruta | Resultado |
| --- | --- |
| GET /api/csrf | headerName y token vinculados a la sesión |
| POST /api/login | Formulario username=email y password; 204 correcto, 401 incorrecto |
| POST /api/logout | Cierre de sesión; 204 |
| GET /api/admin/me | Email y rol del administrador autenticado |
| GET /api/admin/departamentos | Departamentos existentes: id y nombre |
| GET /api/admin/lotes?pagina=0 | items, pagina, totalPaginas y total; 20 por página |
| GET /api/admin/lotes/{id} | id y datos editables |
| POST /api/admin/lotes | Alta; 201, encabezado Location e id/datos |
| PUT /api/admin/lotes/{id} | Reemplazo de los campos editables; 200 e id/datos |
| POST /api/public/v1/visitas | Registro de carga de página; 204, requiere CSRF |
| GET /api/admin/estadisticas?desde=2026-09-01&hasta=2026-09-23 | Totales y detalle diario, rango inclusivo máximo de 366 días |

Solicitar un token CSRF antes de cada escritura y enviar el valor en el encabezado cuyo nombre devuelve headerName, conservando la cookie de sesión. Solicitar un token nuevo después de iniciar o cerrar sesión. Sin sesión se obtiene 401 al acceder a administración; un usuario sin rol ADMIN obtiene 403. Un token inválido también produce 403.

El cuerpo JSON de alta y edición contiene:

```json
{
  "identificador": "LOTE-EJEMPLO-002",
  "latitud": -32.88,
  "longitud": -68.84,
  "departamentoId": null,
  "direccionAproximada": null,
  "zonificacion": null,
  "zonificacionVerificada": false,
  "distanciaRedElectricaMts": null,
  "tieneAccesoElectricidad": false,
  "tieneCoberturaAgua": false,
  "procedenciaLote": {"tipo": "NO_VERIFICADO", "fuente": "Carga manual", "referencia": null},
  "procedenciaZonificacion": {"tipo": "NO_VERIFICADO", "fuente": "Carga manual", "referencia": null},
  "procedenciaElectricidad": {"tipo": "NO_VERIFICADO", "fuente": "Carga manual", "referencia": null},
  "procedenciaAgua": {"tipo": "NO_VERIFICADO", "fuente": "Carga manual", "referencia": null}
}
```

Los campos booleanos son obligatorios. Identificador admite hasta 100 caracteres; dirección, zonificación y fuente hasta 255; referencia hasta 1000. Las cuatro procedencias requieren tipo y fuente. Zonificación verificada exige descripción y procedencia OFICIAL. La edición conserva los campos internos ajenos a este formulario. Un identificador repetido devuelve 409; datos inválidos 400; id inexistente 404. Los errores mantienen el formato ApiErrorResponse.

## Cómo interpretar las estadísticas

Una visita es una carga de la aplicación; recargar suma otra. Cambiar entre paneles no suma una visita. Cada GET de ficha pública registra una consulta según su respuesta: éxito, solicitud inválida, sin resultados o error técnico. También distingue identificador, coordenadas y criterio inválido. Las consultas directas a la API cuentan; no se consideran personas únicas y no se filtran robots.

Se guardan solamente fecha, categoría y cantidad agregada; no se almacenan IP, email del visitante, identificador buscado ni coordenadas consultadas en la tabla estadística. La zona predeterminada es America/Argentina/Buenos_Aires (configurable mediante estadisticas.zona). Los agregados permanecen hasta que se establezca una política de conservación.

El incremento es atómico en MySQL. Si falla el registro estadístico, la consulta pública mantiene su respuesta y puede quedar subcontada; se emite una advertencia de servidor. La medición es orientativa, no un sistema de auditoría de personas.

## Verificación

Ejecutar `.\mvnw.cmd test` y el perfil MySQL documentado en [integracion-mysql.md](integracion-mysql.md). Esta entrega pasó 40 pruebas habituales y 14 pruebas con MySQL real, incluyendo login, permisos, CSRF, alta/edición, duplicados, validaciones y contadores.

Desde el frontend: ingresar a Administración, crear un lote con fuentes no verificadas, buscarlo en Consulta pública y comprobar su ficha. Volver a Estadísticas, consultar las fechas del día y comprobar los totales. Cerrar sesión y verificar que el panel solicite nuevamente acceso.
