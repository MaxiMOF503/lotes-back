# Datos territoriales reales de Luján de Cuyo

Se conectan los archivos existentes de `data/` a MySQL y al mapa actual. No se consulta un servicio remoto en cada clic: se trabaja sobre la copia importada, cuya fecha de importación no es la fecha de actualización del organismo.

## Qué está integrado

- **Catastro municipal:** 65.756 registros. 65.731 contienen geometría; 110 de esas geometrías son inválidas según MySQL. Las consultas precisas y las capas muestran las 65.621 geometrías válidas. Los 25 registros sin geometría se conservan en una tabla aparte.
- **Electricidad:** 13.494 tramos EDEMSA (el 13.495 del README original corresponde al siguiente AUTO_INCREMENT). Se muestra la traza y el tramo más cercano **hasta 1.000 metros** del punto consultado, con distancia geodésica WGS84. No confirma acometida, potencia disponible ni factibilidad.
- **Agua:** dos polígonos del radio general de AYSAM. Un punto dentro o en el límite pertenece al radio publicado. Estar fuera no significa carecer de agua: puede haber otros operadores. No se dispone de cañerías calle por calle.

**Parcelas repetidas en la fuente:** el archivo municipal publica 94 parcelas más de una vez con la misma nomenclatura y la misma geometría (605 copias en total; por ejemplo, `060103004400002700000` aparece 8 veces). El importador conserva la primera aparición y marca las demás en la columna `duplicado_de` con el `origen_id` original. El clic en el mapa, la capa de parcelas y la búsqueda por nomenclatura ocultan las copias. No se borran: `GET /parcelas/{id}` las sigue resolviendo, porque fichas, consultas guardadas e historial pueden referirse a cualquiera de ellas. Parcelas con la misma nomenclatura pero distinta geometría no se consideran copias.

Nomenclatura, zonificación, superficie, frente, FOS y ordenanza son los valores publicados de la parcela. No se deduce titularidad, dominio público/privado ni dirección a partir de campos vacíos.

## Importar en otra PC

Requisitos: MySQL 8.0.18+ (probado con 8.0.46), Python 3.10+ y el cliente `mysql`. No requiere módulos Python adicionales. Usar la misma conexión del backend:

- `DB_URL`: `jdbc:mysql://localhost:3306/verificador_lotes?useSSL=false&serverTimezone=UTC`.
- `DB_USERNAME` y `DB_PASSWORD`: credenciales propias; no guardar contraseñas en el repositorio.
- `MYSQL_EXE`: opcional, ruta al cliente si no está en PATH ni en la instalación habitual de Windows.

Desde la raíz del backend:

```powershell
py -3 scripts/importar_territorio.py
```

Se crean `territorio_elementos`, `territorio_sin_geometria` y `territorio_fuentes` con la migración `V2__capas_territoriales.sql` de Flyway, que el importador ejecuta antes de cargar (una sola definición; ver [migraciones](migraciones-flyway.md)). Hay índice espacial, claves de origen y propiedades JSON. Cada capa se carga en una transacción. Repetir la ejecución reconoce el SHA256 y no duplica datos. `--capa catastro`, `--capa electricidad` o `--capa aysam` permiten importar una capa; `--forzar` reimporta el mismo archivo.

En cada ejecución sobre el catastro se vuelven a marcar las copias exactas, aunque el archivo no haya cambiado (tarda unos segundos). **Si tu base ya estaba importada antes de este cambio**, alcanza con iniciar como siempre (`.\iniciar-local.cmd`) o correr `py -3 scripts/importar_territorio.py --capa catastro` una vez y reiniciar el backend: se agrega la columna `duplicado_de` (migración V2) y se marcan las copias sin reimportar. Mientras no se ejecute, el backend sigue funcionando como antes (sin ocultar copias).

**No ejecutar el dump eléctrico completo:** contiene DROP TABLE y otras tablas de la aplicación. El importador extrae exclusivamente las filas eléctricas, recupera la codificación OEM del dump incluido y establece explícitamente el orden longitud/latitud. Los lotes registrados, usuarios y roles permanecen en sus tablas originales.

Si falta la importación, la nueva API devuelve 503 con CAPA_NO_IMPORTADA o TERRITORIO_NO_IMPORTADO; el contrato anterior sigue disponible.

## Usar la página

Iniciar el backend con las variables DB_* habituales y luego:

```powershell
cd C:\LoteSeguro\lotes-front
npm.cmd start
```

Abrir http://localhost:4200/. Hacer clic en Luján para identificar una parcela y cruzar servicios; el contorno aparece aunque la capa de parcelas esté apagada. En **Capas del territorio** activar parcelas, electricidad o agua. Catastro requiere zoom 15; electricidad zoom 12. Las capas se refrescan al mover el mapa, cancelan respuestas anteriores y tienen un máximo de 700 elementos por ventana desde la interfaz. Si hay más, se avisa que hay que acercarse; no se afirma que el dibujo esté completo.

Los archivos catastrales no contienen direcciones confiables: la búsqueda por dirección usa Georef y las parcelas cercanas al punto aproximado. En las estadísticas, las consultas se cuentan por mapa (coordenadas), nomenclatura y dirección ([detalle](estadisticas.md)); mover o activar capas no cuenta como consulta.

No hay datos ficticios: la aplicación no inserta lotes, cuentas ni líneas de ejemplo (la migración V3 retira los de versiones anteriores).

## API adicional (pública)

- `GET /api/public/v1/territorio/fuentes`: fuentes, recuentos, calidad y fecha de importación.
- `GET /api/public/v1/territorio/capas/{catastro|electricidad|aysam}?oeste=-68.9&sur=-33.074&este=-68.888&norte=-33.065&zoom=16&limite=500`: FeatureCollection, `hayMas`, `acercar`, fuente y mensaje. Límite máximo 1.000.
- `GET /api/public/v1/territorio/consulta?latitud=-33.0688&longitud=-68.8929`: hasta cinco parcelas coincidentes (incluye límites), radio de agua y distancia eléctrica.
- `GET /api/public/v1/territorio/direcciones?texto=San Martín 1500`: direcciones normalizadas por Georef (calle y número, o `calle esquina calle`) con su ubicación aproximada, más las direcciones relevadas por el equipo en las fichas complementarias (origen `FICHA`, con su parcela).
- `GET /api/public/v1/territorio/parcelas/cercanas?latitud=-33.02626&longitud=-68.87491&radio=30`: parcelas a menos de `radio` metros (5 a 100, por defecto 30), de la más cercana a la más lejana, sin copias repetidas.

## Direcciones (Georef)

El catastro no trae direcciones. Para buscar por calle y número o por esquina se usa [Georef](https://www.argentina.gob.ar/georef), el servicio oficial de normalización de direcciones del Gobierno nacional: gratuito, sin clave, limitado a Mendoza y Luján de Cuyo.

- La ubicación es **aproximada**: Georef la calcula sobre el eje de la calle, no dentro de un lote. Por eso el punto se usa para buscar las parcelas cercanas y la persona elige la correcta en el mapa.
- No se guarda ni se registra en el log el texto buscado.
- Si Georef no responde (5 s como máximo), la API devuelve 503 `DIRECCIONES_NO_DISPONIBLES`; si hay direcciones relevadas en fichas, se devuelven igual con un aviso.
- La URL se puede cambiar con la variable `GEOREF_URL` (por defecto `https://apis.datos.gob.ar/georef/api`). Los tests usan respuestas reales grabadas y no dependen de internet.

Las geometrías usan SRID 4326; GeoJSON sale como [longitud, latitud]. Los parámetros se validan y las consultas SQL usan argumentos enlazados. No se agregan APIs de escritura para estas capas.

## Verificación

```powershell
.\mvnw.cmd test
py -3 -m unittest discover -s scripts -p 'test_*.py' -v
$env:TERRITORIO_IT='true'
.\mvnw.cmd -Pmysql-it "-Dit.test=TerritorioMySqlIT" verify
```

La última prueba requiere los archivos ya importados y DB_* configuradas; solo lee esas capas. El frontend se comprueba con `npm.cmd run build`.
