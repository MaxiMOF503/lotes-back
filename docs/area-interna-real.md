# Área interna vinculada a parcelas reales

Primera etapa: fichas complementarias sobre catastro y consultas guardadas por cuenta. No se eliminan los registros anteriores ni se modifican las tablas importadas.

## Usuario

La sección Mi espacio abre las consultas guardadas de la cuenta activa. Desde una parcela identificada en el mapa, Guardar consulta persiste la referencia y el punto seleccionado en MySQL. Repetir la acción conserva un único registro por usuario y parcela. Consultar en mapa vuelve a consultar ese punto con las capas actuales; Quitar solo elimina la referencia personal, no la parcela.

Cada lista y eliminación se limita al usuario de la sesión. No se acepta un ID de usuario enviado por el navegador. La lista tiene paginación de 20 registros. Guardar exige una parcela válida y que el punto esté dentro o sobre el límite de su geometría.

## Administrador

La sección Parcelas reales permite buscar por nomenclatura (3 a 30 dígitos, prefijo) o abrir una parcela desde el mapa mediante Gestionar parcela. Se muestra su contorno real y atributos publicados en modo lectura.

La ficha complementaria permite guardar dirección relevada, observaciones, fuente y referencia documental. Fuente y referencia son obligatorias. El aporte aparece en la consulta pública identificado como información complementaria del equipo, separado del catastro. No se infiere conexión de agua, factibilidad eléctrica o titularidad.

La ficha registra autor y fecha de su última modificación y usa una versión para rechazar sobrescrituras de cambios de otra persona. El correo del autor y la versión son visibles únicamente en administración. No constituye un historial completo de todas las revisiones.

Los cambios sin guardar tienen confirmación antes de cambiar de parcela, pestaña o salir al mapa. Los errores conservan lo escrito.

## Registros anteriores

Se conserva el mantenedor original en Registros anteriores, con sus permisos, formularios, eliminación, datos y procedencias. Los ejemplos no pasan a ser parcelas reales por cambiar de pantalla. Estadísticas mantiene sus permisos y contadores existentes.

## Base y arranque

Con JPA_DDL_AUTO=update (valor predeterminado), Hibernate crea consultas_guardadas y fichas_parcelas al iniciar. Las tablas territorio_elementos, territorio_fuentes y territorio_sin_geometria siguen siendo fuentes de lectura. Los guardados tienen clave única usuario_id/parcela_id y relación con usuarios; las fichas usan parcela_id y control de versión.

Mantener ambos proyectos actualizados localmente, iniciar el backend y luego el frontend según instalacion-local.md. No hace falta reimportar capas que no cambiaron ni reiniciar las cuentas de clase.

## APIs

| Método y ruta | Acceso | Uso |
|---|---|---|
| GET /api/public/v1/territorio/parcelas?nomenclatura=060406&pagina=0 | Público | Buscar geometrías válidas; 20 resultados y hayMas |
| GET /api/public/v1/territorio/parcelas/{id} | Público | Parcela por ID de origen |
| GET /api/public/v1/territorio/parcelas/{id}/ficha | Público | Aporte complementario; respuesta vacía si no existe |
| GET /api/mi-espacio/guardadas?pagina=0 | USUARIO o ADMIN | Solo guardadas de la cuenta activa |
| PUT /api/mi-espacio/guardadas/{parcelaId} | USUARIO o ADMIN + CSRF | Guardar o actualizar punto y etiqueta personal |
| DELETE /api/mi-espacio/guardadas/{id} | USUARIO o ADMIN + CSRF | Quitar exclusivamente una guardada propia |
| GET /api/admin/parcelas/fichas?pagina=0 | ADMIN | Fichas complementarias paginadas |
| GET /api/admin/parcelas/{id}/ficha | ADMIN | Datos editables, versión y autor; ficha vacía si aún no existe |
| PUT /api/admin/parcelas/{id}/ficha | ADMIN + CSRF | Guardar aporte con fuente, referencia y versión |

El campo version es null al crear y debe ser el valor recibido al editar. Un conflicto devuelve 409 y requiere recargar la ficha. Las geometrías ausentes o inválidas devuelven 404; no se permite guardar aportes sobre parcelas inexistentes.

## Verificación

Pruebas unitarias y de permisos: mvnw.cmd test. Integración real, con DB_* configuradas y capas importadas: TERRITORIO_IT=true y mvnw.cmd -Pmysql-it "-Dit.test=TerritorioMySqlIT,EspacioTerritorialMySqlIT" verify. Los registros de las pruebas de integración se revierten con transacciones. Compilación frontend: npm.cmd run build.

Se comprobó en navegador el ingreso de ambos roles, guardar y reabrir una parcela real, buscar por nomenclatura, confirmar cambios sin guardar, guardar un aporte con su fuente y consultar su publicación.

## Segunda etapa: historial, comparación e informes

Con sesión iniciada, las consultas nuevas del mapa pasan por POST /api/mi-espacio/consultas. El servidor vuelve a consultar las fuentes reales y guarda una copia completa: geometrías y atributos, servicios, fuentes, fecha de importación y aportes complementarios publicados en ese momento. No acepta un snapshot enviado por el navegador. Las consultas anónimas siguen usando la API pública y no se guardan. No hay reconstrucción retroactiva de consultas anteriores.

La tabla consultas_historial se crea mediante Hibernate ddl-auto=update, igual que las otras tablas de la aplicación. Conserva usuario_id, punto, fecha, resumen y snapshot JSON; un índice por usuario/fecha facilita la paginación. Cada cuenta tiene su propio historial, incluidos los administradores. No hay acceso administrativo al historial de otras personas.

Los informes de historial leen exclusivamente esa copia; no cambian al editar una ficha o reimportar una capa. El documento distingue fecha de consulta y fecha de generación. Las guardadas y la comparación, en cambio, consultan datos actuales y validan que el punto siga perteneciendo a la parcela elegida. Una incompatibilidad devuelve 409 y pide seleccionar de nuevo. Comparar no crea historial adicional ni asigna puntuaciones de aptitud.

| Método y ruta | Uso |
|---|---|
| POST /api/mi-espacio/consultas | Consulta real + copia histórica; body latitud/longitud; sesión + CSRF |
| GET /api/mi-espacio/historial?pagina=0 | Historial propio, 20 por página |
| GET /api/mi-espacio/historial/{id}/informe | Copia histórica propia |
| DELETE /api/mi-espacio/historial/{id} | Quitar copia propia; sesión + CSRF; no borra guardadas |
| GET /api/mi-espacio/guardadas/{id}/informe | Informe actual de una guardada propia |
| GET /api/mi-espacio/comparacion?ids=1,2 | Dos o tres guardadas distintas y propias |

La propiedad se obtiene del Principal del servidor. Un ID ajeno devuelve 404; la comparación valida la propiedad de toda la selección antes de consultar el territorio. Quitar un registro histórico elimina esa copia y requiere confirmación visual.

El frontend agrega Historial y Comparar parcelas a Mi espacio y Gestión territorial. La comparación conserva la selección entre páginas y adapta sus columnas a fichas verticales en celular. Los informes tienen vista previa, navegación por teclado y exportación autónoma HTML/JSON. Imprimir / guardar PDF usa el diálogo de impresión del navegador (elegir Guardar como PDF); no instala librerías adicionales. Si el navegador bloquea la ventana, se puede descargar HTML, abrirlo y usar Ctrl+P. Los textos incorporados al HTML se escapan; no se ejecutan referencias o atributos como código. El JSON conserva geometrías para inspeccionar los datos.

Pruebas de esta etapa: mvnw.cmd test; con MySQL y DB_* configuradas, TERRITORIO_IT=true y mvnw.cmd -Pmysql-it "-Dit.test=TerritorioMySqlIT,EspacioTerritorialMySqlIT,ActividadTerritorialMySqlIT" verify. Todas las altas personales de integración se revierten. En frontend: node --test scripts/test-informe.cjs y npm.cmd run build.
