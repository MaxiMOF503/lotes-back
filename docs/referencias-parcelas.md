# Identidad de parcelas y referencias históricas (#48)

`TerritorioService.resolverParcela(id)` centraliza la resolución: devuelve
`idOriginal`, `idCanonico`, `esCopia` e `idsRelacionados`. Sigue `duplicado_de`,
incluyendo cadenas; rechaza ciclos y representantes inválidos con 409
`REFERENCIA_PARCELA_INVALIDA`. Un ID solicitado inexistente conserva 404
`PARCELA_NO_ENCONTRADA`. No cambia el criterio de duplicados ni el importador.

La resolución es interna: no se agrega un endpoint `/referencia` sin consumidores actuales.
`GET /parcelas/{id}` sigue devolviendo la geometría y el ID originales. Mapa,
búsqueda y cercanas mantienen el filtro `duplicado_de IS NULL`.

## Guardadas e informes

Las nuevas Guardadas usan ID canónico. Antes de crear se busca la más antigua
de la cuenta entre todos los IDs relacionados. Si existe se actualiza punto y
etiqueta sin renombrarla, cambiar su fecha ni borrar otras Guardadas históricas.
La selección usa el menor ID autoincremental (orden de alta). Una etiqueta omitida
conserva la existente; una cadena vacía explícita permite quitarla. Un punto o una
etiqueta enviados se actualizan como en el contrato previo. Se conserva el HTTP 200
del guardado idempotente. No se fusionan ni eliminan otras filas equivalentes antiguas.
La restricción única usuario/parcela impide duplicar nuevas filas canónicas ante
escrituras simultáneas (una colisión concurrente puede devolver 409).

En informes actuales `parcelaId` es el representante incluido en `consulta` y
`parcelaIdOriginal` conserva el ID de la Guardada. Se valida pertenencia del punto
al representante y su inclusión en la consulta; otros casos siguen dando
`PARCELA_CAMBIO` o `PARCELA_NO_INCLUIDA` según corresponda.

Comparar exige dos o tres Guardadas propias de parcelas canónicas distintas.
Representante y copia en la misma comparación producen 400 `COMPARACION_INVALIDA`
con explicación; no se deduplican silenciosamente.

## Fichas relacionadas

Se siguen editando por ID exacto y con su propia versión. No se fusionan ni borran
fichas de copias al editar al representante. La lectura administrativa por ID
conserva la ficha exacta y añade `idCanonico` y las otras fichas en `aportesRelacionados`.

La lectura pública localiza todas las fichas del grupo: primero representante,
después copias ordenadas por ID. Añade `idCanonico`, `multiplesAportes` y
`aportesRelacionados`, con ID, datos, fuente, referencia y fecha de cada aporte;
nunca expone autor, correo ni versión de edición.

- Sin aportes: respuesta vacía como antes.
- Un aporte: mantiene también los campos planos anteriores, aunque sea de una copia.
- Solo representante: campos planos originales y origen en `aportesRelacionados`, sin conflicto.
- Solo copia: los mismos campos planos muestran su aporte completo; el origen identifica la copia.
- Representante y copia: `multiplesAportes=true`; se conservan ambos, ordenados por
  representante y luego ID. No se escoge uno como verdad.
- Varias copias sin ficha representante: misma política, copias ordenadas por ID.

En ambos casos múltiples, `observaciones` incluye **todos** los campos de cada aporte,
rotulados con su ID original, también para el frontend/exportador anteriores que no
leen la lista nueva. `direccion` es null porque no hay una dirección compartida;
`fuente` y `referencia` remiten expresamente al detalle individual, sin sustituir sus
valores. `actualizadoEn` es la fecha máxima del grupo (última modificación de sus
aportes), no la elección de una versión verdadera. La lista estructurada conserva
los valores y fechas individuales. No se modifica el frontend ni se persiste este texto.

Los informes actuales incorporan la misma lectura pública, una vez por parcela.

## Reportes, historial y esquema

`reportes_datos.parcela_id` conserva el ID original, incluso para nuevos reportes
de copias. Las respuestas añaden `parcelaIdCanonico`; si el catastro no está
disponible, la parcela no existe o su relación es inconsistente, ese campo es null
y el reporte sigue legible. La variante central `canonicoDisponible` permite
esa lectura histórica sin cargar geometrías ni recorrer todos los hermanos; tolera
tablas territoriales ausentes, sin ocultar otros errores SQL. No se corrige la validación de
espacios en la descripción: es un defecto independiente.

Los snapshots no se reescriben ni se resuelven contra capas actuales al leerlos.
Los JSON anteriores, sin campos nuevos, siguen deserializándose y conservan IDs
y aportes originales. No se necesita V5: ya existen las columnas y relaciones.
V1–V4 permanecen inmutables.

En importaciones antiguas sin `duplicado_de`, cada ID conserva su propia identidad:
no se adivinan equivalencias. Se reutiliza la detección de columna del repositorio.
Las cadenas se resuelven por enlace y los grupos por una consulta recursiva; la
respuesta elimina repeticiones y ordena IDs. No se cachean identidades entre pedidos.

## Validación

`ReferenciasParcelasServiceTest` prueba resolución, ciclos, Guardadas, informes,
comparación, fichas, reportes y JSON histórico anterior. `ReferenciasParcelasMySqlIT`
usa el caso real `060103004400002700000` / `Parcelas_visor0.55136` / `.55137`,
con capas previamente importadas y fixtures personales con rollback. Usar una base
aislada y seguir `integracion-mysql.md`, nunca una base de producción.

```powershell
.\mvnw.cmd test
py -3 -m unittest discover -s scripts -p "test_*.py" -v
.\mvnw.cmd verify
# Sólo con credenciales autorizadas y base aislada preparada:
$env:TERRITORIO_IT='true'
.\mvnw.cmd -Pmysql-it "-Dit.test=ReferenciasParcelasMySqlIT" verify
```
