# Instalación local para el profesor y el equipo

La aplicación usa Angular en http://localhost:4200, Spring Boot en el puerto 8080 y una base MySQL propia de cada PC. GitHub comparte código y archivos de origen; no comparte tu base, tus usuarios personales ni tus contraseñas.

## 1. Requisitos en Windows

Instalar Git, JDK 21 (no solamente un JRE), MySQL Server y cliente MySQL 8.0.18 o posterior, Python 3.10 o posterior y Node.js 22 con npm. MySQL debe estar iniciado, normalmente en el puerto 3306. Configurar JAVA_HOME apuntando a la carpeta del JDK y abrir una nueva PowerShell después de instalar.

No hace falta Maven ni Angular CLI global: el proyecto incluye Maven Wrapper y utiliza su Angular local. La primera instalación necesita Internet para descargar dependencias. El mapa base también necesita conexión.

## 2. Descargar ambos proyectos

Mientras estos cambios estén en revisión, usar la rama publicada en ambos repositorios:

```powershell
New-Item -ItemType Directory -Force C:\LoteSeguro | Out-Null
cd C:\LoteSeguro
git clone --branch area-interna-parcelas-reales https://github.com/MaxiMOF503/lotes-back.git
git clone --branch area-interna-parcelas-reales https://github.com/MaxiMOF503/lotes-front.git
```

Si ya existen esas carpetas, no clonarlas encima ni borrar archivos. Revisar `git status` y conservar los cambios propios antes de cambiar de rama. Con una copia limpia, ejecutar en cada carpeta:

```powershell
git fetch origin
git switch area-interna-parcelas-reales
git pull --ff-only origin area-interna-parcelas-reales
```

Git creará la rama local siguiendo la remota si todavía no existe. Una vez que los dos Pull Requests estén integrados, también se podrá usar `git switch main` y `git pull --ff-only origin main` en ambos proyectos.

## 3. Preparar la base e iniciar el backend

En la primera terminal:

```powershell
cd C:\LoteSeguro\lotes-back
.\iniciar-local.cmd
```

El asistente pide el usuario de MySQL (por defecto root), la contraseña de MySQL de esa PC y el correo y la contraseña del administrador. Las contraseñas no se muestran mientras se escriben.

Crea la base `verificador_lotes` si falta, importa las capas territoriales y ejecuta Spring Boot. El primer arranque puede tardar varios minutos. Esperar el mensaje `Started LotesApplication` y mantener esta terminal abierta.

La contraseña del administrador debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8. No hay cuentas ni contraseñas publicadas. Los accesos son:

| Acceso | Correo | Contraseña | Permisos |
|---|---|---|---|
| Público | No necesita cuenta | — | Consulta pública y mapa |
| Administrador | El que escribas en el asistente | La que elijas | Fichas complementarias de parcelas y estadísticas |
| Usuario | Se registra desde el frontend | La que elija cada persona | Consultas guardadas, historial y comparación |

Si el correo del administrador ya existía, el asistente conserva su contraseña actual; no se cambia desde acá. El registro desde el frontend crea usuarios normales; no concede el rol administrador. Si venías usando las cuentas `demo.admin` o `demo.usuario` de versiones anteriores, la migración V3 las elimina al arrancar: ver [migraciones](migraciones-flyway.md).

La carga de capas es repetible: compara el contenido y evita reimportar archivos que no cambiaron. No hay que ejecutar manualmente el dump de electricidad: contiene tablas ajenas a esa capa. El importador extrae únicamente sus líneas eléctricas.

Opciones del asistente:

```powershell
.\iniciar-local.cmd --preparar-solo
.\iniciar-local.cmd --sin-admin
```

La primera prepara la base sin arrancar ni crear cuentas. La segunda arranca sin pedir administrador: usa las cuentas que ya existan o la configuración ADMIN_EMAIL / ADMIN_PASSWORD del entorno.

## 4. Iniciar el frontend

En otra terminal:

```powershell
cd C:\LoteSeguro\lotes-front
.\iniciar-front.cmd
```

El comando instala las versiones fijadas en package-lock.json y abre el servidor en el puerto 4200. Entrar desde el navegador a http://localhost:4200. Las solicitudes /api llegan al backend mediante proxy.conf.json, sin configurar una conexión MySQL en el navegador.

Para siguientes arranques, si las dependencias no cambiaron:

```powershell
cd C:\LoteSeguro\lotes-front
npm.cmd start -- --host 127.0.0.1 --port 4200
```

Mantener las dos terminales abiertas. Para detener cada servidor, presionar Ctrl+C en su terminal.

## 5. Qué mostrar en clase

- Abrir la consulta pública sin iniciar sesión.
- Activar Catastro y acercar el mapa a nivel de calles; sus parcelas se cargan a partir del zoom 15. Electricidad se muestra desde el zoom 12.
- Hacer clic en una parcela: se consulta su geometría y nomenclatura, atributos publicados, radio AYSAM y distancia a la línea eléctrica publicada más cercana.
- Como referencia reproducible, consultar latitud -33.0688 y longitud -68.8929 con la API pública: `http://localhost:8080/api/public/v1/territorio/consulta?latitud=-33.0688&longitud=-68.8929`. La parcela de la captura importada tiene nomenclatura 060406000300002300007.
- Ingresar al área interna con la cuenta de administrador para revisar fichas complementarias, permisos y estadísticas.

El buscador por nomenclatura consulta el catastro importado. El buscador por dirección usa calles reales de Georef y muestra las parcelas cercanas al punto aproximado para que elijas la tuya: la fuente catastral no trae domicilios fiables.

Las capas importadas contienen 65.756 registros catastrales: 65.621 geometrías válidas, 110 inválidas y 25 sin geometría. También contienen 13.494 tramos eléctricos, 2 polígonos de radio AYSAM y 3 de radio Aguas Luján. Los registros sin geometría válida se conservan para revisión, pero no se dibujan ni se usan para identificar parcelas.

Son capturas de las fuentes indicadas, no una conexión en vivo a los organismos. El radio AYSAM no confirma conexión de agua; la cercanía a una línea eléctrica no acredita factibilidad de suministro. Tampoco se acredita titularidad o situación legal. Ver [datos territoriales y limitaciones](datos-territoriales-reales.md).

## 6. Problemas frecuentes

- Puerto 8080 o 4200 ocupado: detener la instancia anterior desde su terminal o Spring Tools. El asistente no cierra procesos ajenos.
- MySQL rechaza la conexión: revisar que el servicio esté iniciado y usar las credenciales de esa PC. El usuario necesita permiso para crear la base y tablas; no copiar la contraseña del autor del proyecto.
- Java incorrecto: revisar `java --version` y JAVA_HOME; debe apuntar a un JDK 21 o posterior.
- Cliente MySQL instalado fuera de la ruta habitual: indicar su ruta antes de ejecutar el asistente:
  `$env:MYSQL_EXE = 'C:\ruta\MySQL\bin\mysql.exe'`.
- Una base o puerto local diferentes: configurar DB_URL, DB_USERNAME y DB_PASSWORD como variables de entorno antes de iniciar. El asistente acepta únicamente servidores locales; no publica credenciales ni las guarda en archivos.
- PowerShell bloquea npm.ps1: los comandos del proyecto usan npm.cmd, por lo que no hace falta modificar la política global.
- Un 401 en /api/me sin iniciar sesión indica que no hay sesión. Un 401 al enviar el login exige revisar correo y contraseña. El administrador es el que configuraste con ADMIN_EMAIL o en el asistente.
- Dependencias pendientes de descarga: verificar la conexión a Internet y volver a ejecutar el comando.
- Para inspeccionar tablas en DBeaver: conectar a localhost:3306 con las mismas credenciales y seleccionar la base verificador_lotes. El filtro del navegador puede ocultar otras bases sin eliminarlas.

## Conexión y estructura de datos

Spring Boot usa DB_URL, DB_USERNAME y DB_PASSWORD para abrir la conexión con MySQL. Flyway crea o completa las tablas al arrancar y Hibernate solo las valida ([migraciones](migraciones-flyway.md)). El importador usa la misma definición de territorio_elementos, territorio_sin_geometria y territorio_fuentes, con índice espacial y procedencia. El backend consulta esos datos al seleccionar un punto o cargar una capa del mapa.

Angular consume las APIs; nunca accede directamente a MySQL. Las cuentas y los datos de cada instalación viven solamente en su base local.
