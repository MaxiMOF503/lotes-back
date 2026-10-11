# Datos territoriales reales de Luján de Cuyo

Se conectan los archivos existentes de `data/` a MySQL y al mapa actual. No se consulta un servicio remoto en cada clic: se trabaja sobre la copia importada, cuya fecha de importación no es la fecha de actualización del organismo.

## Qué está integrado

- **Catastro municipal:** 65.756 registros. 65.731 contienen geometría; 110 de esas geometrías son inválidas según MySQL. Las consultas precisas y las capas muestran las 65.621 geometrías válidas. Los 25 registros sin geometría se conservan en una tabla aparte.
- **Electricidad:** 13.494 tramos EDEMSA (el 13.495 del README original corresponde al siguiente AUTO_INCREMENT). Se muestra la traza y el tramo más cercano **hasta 1.000 metros** del punto consultado, con distancia geodésica WGS84. No confirma acometida, potencia disponible ni factibilidad.
- **Agua:** dos polígonos del radio general de AYSAM. Un punto dentro o en el límite pertenece al radio publicado. Estar fuera no significa carecer de agua: puede haber otros operadores. No se dispone de cañerías calle por calle.

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

Se crean `territorio_elementos`, `territorio_sin_geometria` y `territorio_fuentes`. Hay índice espacial, claves de origen y propiedades JSON. Cada capa se carga en una transacción. Repetir la ejecución reconoce el SHA256 y no duplica datos. `--capa catastro`, `--capa electricidad` o `--capa aysam` permiten importar una capa; `--forzar` reimporta el mismo archivo.

**No ejecutar el dump eléctrico completo:** contiene DROP TABLE y otras tablas de la aplicación. El importador extrae exclusivamente las filas eléctricas, recupera la codificación OEM del dump incluido y establece explícitamente el orden longitud/latitud. Los lotes registrados, usuarios y roles permanecen en sus tablas originales.

Si falta la importación, la nueva API devuelve 503 con CAPA_NO_IMPORTADA o TERRITORIO_NO_IMPORTADO; el contrato anterior sigue disponible.

## Usar la página

Iniciar el backend con las variables DB_* habituales y luego:

```powershell
cd C:\LoteSeguro\lotes-front
npm.cmd start
```

Abrir http://localhost:4200/. Hacer clic en Luján para identificar una parcela y cruzar servicios; el contorno aparece aunque la capa de parcelas esté apagada. En **Capas del territorio** activar parcelas, electricidad o agua. Catastro requiere zoom 15; electricidad zoom 12. Las capas se refrescan al mover el mapa, cancelan respuestas anteriores y tienen un máximo de 700 elementos por ventana desde la interfaz. Si hay más, se avisa que hay que acercarse; no se afirma que el dibujo esté completo.

Los buscadores por identificador/dirección y el mantenedor siguen consultando **lotes registrados**; los archivos catastrales no contienen direcciones confiables ni equivalen a registros editables del mantenedor. Las consultas reales del mapa cuentan en las estadísticas como consultas por coordenadas; mover o activar capas no cuenta como consulta.

Los datos ficticios no se insertan en el arranque normal. Para una clase demostrativa: `DEMO_LOTES_ENABLED=true` y `DEMO_SQL_INIT_MODE=always`, o usar `examples/iniciar-demo.ps1`. Los ejemplos existentes no se borran y no se usan para identificar las parcelas reales.

## API adicional (pública)

- `GET /api/public/v1/territorio/fuentes`: fuentes, recuentos, calidad y fecha de importación.
- `GET /api/public/v1/territorio/capas/{catastro|electricidad|aysam}?oeste=-68.9&sur=-33.074&este=-68.888&norte=-33.065&zoom=16&limite=500`: FeatureCollection, `hayMas`, `acercar`, fuente y mensaje. Límite máximo 1.000.
- `GET /api/public/v1/territorio/consulta?latitud=-33.0688&longitud=-68.8929`: hasta cinco parcelas coincidentes (incluye límites), radio de agua y distancia eléctrica.

Las geometrías usan SRID 4326; GeoJSON sale como [longitud, latitud]. Los parámetros se validan y las consultas SQL usan argumentos enlazados. No se agregan APIs de escritura para estas capas.

## Verificación

```powershell
.\mvnw.cmd test
py -3 -m unittest discover -s scripts -p 'test_*.py' -v
$env:TERRITORIO_IT='true'
.\mvnw.cmd -Pmysql-it "-Dit.test=TerritorioMySqlIT" verify
```

La última prueba requiere los archivos ya importados y DB_* configuradas; solo lee esas capas. El frontend se comprueba con `npm.cmd run build`.
