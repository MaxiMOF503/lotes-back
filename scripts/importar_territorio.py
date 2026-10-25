"""Importa solo las tres capas territoriales; nunca ejecuta el dump completo.
Python 3.10+ y cliente mysql 8.0.18+. Credenciales en DB_* (no argumentos).
"""
import argparse
from collections import Counter
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import tempfile
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
DATASETS = {
    "catastro": ("catastro-parcelas/parcelas_lujan_de_cuyo.geojson.gz", 65756,
                "Geoportal municipal de Luján de Cuyo · Parcelas_visor0",
                "https://geoportal.lujandecuyo.gob.ar"),
    "aysam": ("aysam-agua/radio_servicio_aysam_lujan_de_cuyo.geojson", 2,
              "Geoportal municipal de Luján de Cuyo · radio_servicio_aysam",
              "https://geoportal.lujandecuyo.gob.ar"),
    "aguas_lujan": ("aguas-lujan/radio_servicio_lujan.geojson", 3,
                    "Geoportal municipal de Luján de Cuyo · radio_servicio_lujan (Aguas Luján)",
                    "https://geoportal.lujandecuyo.gob.ar"),
    "electricidad": ("lineas-electricas/lineas_electricas_lujan_de_cuyo.sql", 13494,
                     "EDEMSA · Portal de Datos Abiertos de Energía · recorte Luján de Cuyo",
                     "https://datos.energia.gob.ar"),
}
# Una sola definicion: la migracion V2 de Flyway. Es idempotente, asi que se puede ejecutar
# antes de importar aunque el backend todavia no haya arrancado o la base ya tenga las tablas.
MIGRACION_TERRITORIO = ROOT / "src" / "main" / "resources" / "db" / "migration" / "V2__capas_territoriales.sql"
SCHEMA = MIGRACION_TERRITORIO.read_text(encoding="utf-8")
MIGRACION_CLASIFICACION = MIGRACION_TERRITORIO.with_name("V5__clasificar_duplicados_catastrales.sql")
SCHEMA += "\n" + MIGRACION_CLASIFICACION.read_text(encoding="utf-8")
# fid identifica una fila del exportador, no un atributo territorial. Ningún otro
# campo se ignora (incluidos id_ev, id_bp y todos los retiros).
CAMPOS_TECNICOS = frozenset({"fid"})
DUPLICADO_IDENTICO = "DUPLICADO_IDENTICO"
CONFLICTO_ATRIBUTOS = "CONFLICTO_ATRIBUTOS"
ESCAPES = {ord("0"): 0, ord("n"): 10, ord("r"): 13, ord("b"): 8,
           ord("t"): 9, ord("Z"): 26}
ROW = re.compile(rb"\((\d+),'((?:[^'\\]|\\.)*)','((?:[^'\\]|\\.)*)','((?:[^'\\]|\\.)*)'\)", re.S)

def unescape(value):
    return re.sub(rb"\\(.)", lambda m: bytes([ESCAPES.get(m[1][0], m[1][0])]), value, flags=re.S)

def sql_text(value):
    # Hexadecimal evita escapes dependientes de sql_mode y conserva UTF-8.
    return "CONVERT(0x" + str(value).encode("utf8").hex() + " USING utf8mb4)"

def read_electricity(path):
    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        # Este dump fue pasado por PowerShell: OEM CP850 -> UTF-8.
        raw = raw.decode("utf-8-sig").encode("cp850")
    count = 0
    for block in re.finditer(rb"INSERT INTO `lineas_electricas` VALUES (.*?);\s*(?=INSERT|/\*!|UNLOCK)", raw, re.S):
        for row in ROW.finditer(block[1]):
            geometry = unescape(row[4])
            if len(geometry) < 22 or struct.unpack("<I", geometry[:4])[0] != 4326:
                raise ValueError("Geometría eléctrica con SRID inválido")
            if geometry[4] != 1 or struct.unpack("<I", geometry[5:9])[0] != 5:
                raise ValueError("Se esperaba MultiLineString WKB little endian")
            # El dump guarda WKB como longitud/latitud. MySQL debe conocer ese orden.
            expression = "ST_GeomFromWKB(0x" + geometry[4:].hex() + ",4326,'axis-order=long-lat')"
            properties = {"tipo": unescape(row[3]).decode("utf8"),
                          "fuente_datos": unescape(row[2]).decode("utf8")}
            count += 1
            yield row[1].decode("ascii"), expression, properties
    if count != DATASETS["electricidad"][1]:
        raise ValueError(f"Se extrajeron {count} líneas; se esperaban 13494. No se ejecutó el dump.")

def read_geojson(path):
    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8-sig") as source:
        data = json.load(source)
    seen = set()
    for feature in data["features"]:
        props = feature.get("properties") or {}
        key = str(feature.get("id") or props.get("fid") or props.get("id"))
        if key in seen or key == "None":
            raise ValueError(f"Identificador de origen duplicado o ausente: {key}")
        seen.add(key)
        geometry = feature["geometry"]
        if geometry is None:
            yield key, None, props
            continue
        if geometry["type"] != "MultiPolygon":
            raise ValueError("Se esperaba MultiPolygon")
        expression = "ST_GeomFromGeoJSON(" + sql_text(json.dumps(geometry, separators=(",", ":"))) + ",1,4326)"
        yield key, expression, props

def copias_exactas(features):
    """Compatibilidad: coincidencia geométrica, no necesariamente igualdad de atributos."""
    return {copia: dato["representante"] for copia, dato in clasificar_coincidencias(features).items()}


def valor_estructural(valor):
    # JSON ordenado, sin trim ni conversiones: distingue null/ausencia, bool/número,
    # cadenas/números y el orden de arrays; el orden de claves no afecta.
    return json.dumps(valor, sort_keys=True, ensure_ascii=False, separators=(",", ":"), allow_nan=False)


def campos_diferentes(original, copia):
    return sorted(campo for campo in (original.keys() | copia.keys()) - CAMPOS_TECNICOS
                  if campo not in original or campo not in copia
                  or valor_estructural(original[campo]) != valor_estructural(copia[campo]))


def clasificar_coincidencias(features):
    """Mismo grupo y representante que antes; cada copia se compara con el primero.

    No modifica propiedades ni elige un valor verdadero. El grupo es conflictivo
    si alguna copia difiere, aunque otras sean idénticas al representante.
    """
    originales, copias = {}, {}
    for key, geometry, props in features:
        if geometry is None:
            continue
        clave = (props.get("nomenclatu") or "", json.dumps(geometry, separators=(",", ":"), sort_keys=True))
        if clave in originales:
            representante, propiedades = originales[clave]
            campos = campos_diferentes(propiedades, props)
            copias[key] = {"representante": representante,
                           "tipo": CONFLICTO_ATRIBUTOS if campos else DUPLICADO_IDENTICO,
                           "campos": campos}
        else:
            originales[clave] = (key, props)
    return copias


def resumen_coincidencias(clasificacion):
    grupos, campos = {}, Counter()
    for dato in clasificacion.values():
        grupo = grupos.setdefault(dato["representante"], {"copias": 0, "campos": set()})
        grupo["copias"] += 1
        grupo["campos"].update(dato["campos"])
    for grupo in grupos.values():
        campos.update(grupo["campos"])
    conflictos = [g for g in grupos.values() if g["campos"]]
    return {"grupos_coincidentes": len(grupos), "grupos_identicos": len(grupos) - len(conflictos),
            "grupos_conflictivos": len(conflictos), "copias_identicas": sum(
                d["tipo"] == DUPLICADO_IDENTICO for d in clasificacion.values()),
            "copias_conflictivas": sum(d["tipo"] == CONFLICTO_ATRIBUTOS for d in clasificacion.values()),
            "registros_involucrados": len(grupos) + len(clasificacion),
            "registros_en_grupos_conflictivos": sum(g["copias"] + 1 for g in conflictos),
            "campos_conflictivos_por_grupo": dict(sorted(campos.items(), key=lambda p: (-p[1], p[0])))}

def read_geojson_geometrias(path):
    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8-sig") as source:
        data = json.load(source)
    for feature in data["features"]:
        props = feature.get("properties") or {}
        yield str(feature.get("id") or props.get("fid") or props.get("id")), feature["geometry"], props

def sql_clasificacion(clasificacion, layer="catastro"):
    if layer != "catastro":
        raise ValueError("La clasificación de duplicados solo corresponde a catastro")
    sql = ["START TRANSACTION;"]
    # Solo registros de esta importación; no reconcilia versiones ni toca filas
    # ausentes del archivo actual (esa política corresponde a #50).
    filas = {}
    for copia, dato in clasificacion.items():
        filas.setdefault(dato["representante"], (None, None, None))
        filas[copia] = (dato["representante"], dato["tipo"], dato["campos"])
    pares = sorted(filas.items())
    for inicio in range(0, len(pares), 200):
        lote = pares[inicio:inicio + 200]
        asignaciones = []
        for posicion, columna in enumerate(("duplicado_de", "tipo_duplicado", "campos_conflicto")):
            casos = " ".join("WHEN " + sql_text(key) + " THEN " + (
                "NULL" if valores[posicion] is None else sql_text(
                    json.dumps(valores[posicion], ensure_ascii=False) if posicion == 2 else valores[posicion]))
                for key, valores in lote)
            asignaciones.append(columna + "=CASE origen_id " + casos + " END")
        claves = ",".join(sql_text(key) for key, _ in lote)
        sql.append("UPDATE territorio_elementos SET " + ",".join(asignaciones)
                   + " WHERE capa=" + sql_text(layer) + " AND origen_id IN (" + claves + ");")
    sql.append("COMMIT;")
    return "\n".join(sql)


def marcar_copias(layer):
    """Idempotente: se ejecuta en cada importación del catastro, aunque el archivo no cambie."""
    path = ROOT / "data" / DATASETS[layer][0]
    clasificacion = clasificar_coincidencias(read_geojson_geometrias(path))
    mysql(sql_clasificacion(clasificacion, layer))
    print(json.dumps(resumen_coincidencias(clasificacion), ensure_ascii=False), flush=True)

def mysql_command():
    jdbc = os.environ.get("DB_URL", "")
    url = urlparse(jdbc.removeprefix("jdbc:"))
    if url.scheme != "mysql" or not url.hostname or not url.path.strip("/"):
        raise ValueError("Configurar DB_URL como jdbc:mysql://host:puerto/base")
    if not os.environ.get("DB_USERNAME") or "DB_PASSWORD" not in os.environ:
        raise ValueError("Configurar DB_USERNAME y DB_PASSWORD")
    exe = os.environ.get("MYSQL_EXE") or shutil.which("mysql")
    if not exe and os.name == "nt":
        candidate = Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "MySQL/MySQL Server 8.0/bin/mysql.exe"
        if candidate.exists():
            exe = str(candidate)
    if not exe:
        raise ValueError("No se encontró mysql. Configurar MYSQL_EXE.")
    return [exe, "--batch", "--skip-column-names", "--default-character-set=utf8mb4",
            "--max-allowed-packet=64M", "--host=" + url.hostname,
            "--port=" + str(url.port or 3306), "--user=" + os.environ["DB_USERNAME"],
            "--database=" + url.path.strip("/")]

def mysql(sql, file=None):
    env = os.environ.copy()
    env["MYSQL_PWD"] = env["DB_PASSWORD"]
    result = subprocess.run(mysql_command(), input=None if file else sql.encode("utf8"),
                            stdin=file, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env)
    if result.returncode:
        # Nunca volcar el SQL/dump o las credenciales en errores.
        message = result.stderr.decode("utf8", errors="replace").strip()
        raise RuntimeError("MySQL rechazó la importación: " + message[:400])
    return result.stdout.decode("utf8")

def import_layer(layer, force=False):
    # También lo usa iniciar_local.py: el marcado de copias va acá para que ningún camino lo saltee.
    cargar_capa(layer, force)
    if layer == "catastro":
        marcar_copias(layer)

def cargar_capa(layer, force=False):
    relative, expected, source, url = DATASETS[layer]
    path = ROOT / "data" / relative
    checksum = hashlib.sha256(path.read_bytes()).hexdigest()
    current = mysql("SELECT sha256,registros,(SELECT (SELECT COUNT(*) FROM territorio_elementos WHERE capa="
                    + sql_text(layer) + ")+(SELECT COUNT(*) FROM territorio_sin_geometria WHERE capa=" + sql_text(layer) + ")) FROM territorio_fuentes WHERE capa=" + sql_text(layer) + ";").strip()
    if not force and current == f"{checksum}\t{expected}\t{expected}":
        print(f"{layer}: ya importada ({expected} registros), sin cambios.", flush=True)
        return
    reader = read_electricity(path) if layer == "electricidad" else read_geojson(path)
    # Primero preparar/validar toda la entrada. Una única transacción por capa.
    with tempfile.TemporaryFile() as sqlfile:
        sqlfile.write(b"START TRANSACTION;\n")
        batch = []
        count = 0
        for key, geometry, props in reader:
            count += 1
            if geometry is None:
                sqlfile.write(("INSERT INTO territorio_sin_geometria(capa,origen_id,propiedades) VALUES ("
                               + sql_text(layer) + "," + sql_text(key) + ","
                               + sql_text(json.dumps(props, ensure_ascii=False))
                               + ") ON DUPLICATE KEY UPDATE propiedades=VALUES(propiedades);\n").encode("utf8"))
                continue
            batch.append("(" + sql_text(layer) + "," + sql_text(key) + "," + geometry + ","
                         + sql_text(json.dumps(props, ensure_ascii=False, separators=(",", ":"))) + ")")
            if len(batch) == 100:
                write_batch(sqlfile, batch)
                batch = []
        if batch:
            write_batch(sqlfile, batch)
        if count != expected:
            raise ValueError(f"{layer}: {count} registros, se esperaban {expected}. Sin cambios.")
        tail = "UPDATE territorio_elementos SET valida=ST_IsValid(geometria) WHERE capa=" + sql_text(layer) + ";\n"
        tail += ("INSERT INTO territorio_fuentes(capa,fuente,url,sha256,registros,invalidos) VALUES ("
                 + ",".join(sql_text(v) for v in (layer, source, url, checksum))
                 + f",{count},(SELECT COUNT(*) FROM territorio_elementos WHERE capa="
                 + sql_text(layer) + " AND valida=FALSE)) ON DUPLICATE KEY UPDATE fuente=VALUES(fuente),"
                 "url=VALUES(url),sha256=VALUES(sha256),registros=VALUES(registros),"
                 "invalidos=VALUES(invalidos),importado_en=CURRENT_TIMESTAMP;\nCOMMIT;\n")
        sqlfile.write(tail.encode("utf8"))
        sqlfile.seek(0)
        print(f"{layer}: importando {count} registros…", flush=True)
        mysql("", file=sqlfile)
    print(mysql("SELECT capa,registros,invalidos FROM territorio_fuentes WHERE capa=" + sql_text(layer) + ";").strip(), flush=True)

def write_batch(file, rows):
    statement = ("INSERT INTO territorio_elementos(capa,origen_id,geometria,propiedades) VALUES "
                 + ",".join(rows) + " ON DUPLICATE KEY UPDATE geometria=VALUES(geometria),"
                 "propiedades=VALUES(propiedades);\n")
    file.write(statement.encode("utf8"))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capa", choices=list(DATASETS))
    parser.add_argument("--forzar", action="store_true", help="Reimportar aun con el mismo SHA256")
    parser.add_argument("--analizar-coincidencias", action="store_true",
                        help="Analizar el archivo catastral sin conectarse a MySQL ni escribir datos")
    args = parser.parse_args()
    if args.analizar_coincidencias:
        clasificacion = clasificar_coincidencias(read_geojson_geometrias(ROOT / "data" / DATASETS["catastro"][0]))
        print(json.dumps(resumen_coincidencias(clasificacion), ensure_ascii=False, indent=2))
        return
    mysql(SCHEMA)
    for layer in [args.capa] if args.capa else DATASETS:
        import_layer(layer, args.forzar)

if __name__ == "__main__":
    try:
        main()
    except (ValueError, RuntimeError, OSError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
