"""Preparar y ejecutar LoteSeguro localmente, sin guardar contraseñas.
Python 3.10+, Java 21 y MySQL 8.0.18+ con cliente mysql.
"""
import argparse
import getpass
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
from urllib.parse import urlparse
import importar_territorio as importer

ROOT = Path(__file__).resolve().parents[1]

def configurar_conexion():
    os.environ.setdefault("DB_URL", "jdbc:mysql://127.0.0.1:3306/verificador_lotes?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC")
    if not os.environ.get("DB_USERNAME"):
        os.environ["DB_USERNAME"] = input("Usuario de MySQL [root]: ").strip() or "root"
    if "DB_PASSWORD" not in os.environ:
        os.environ["DB_PASSWORD"] = getpass.getpass("Contraseña de MySQL: ")
    url = urlparse(os.environ["DB_URL"].removeprefix("jdbc:"))
    if url.hostname not in ("localhost", "127.0.0.1", "::1"):
        raise ValueError("Este asistente prepara bases locales. Usar localhost o 127.0.0.1 en DB_URL.")
    database = url.path.strip("/")
    if not re.fullmatch(r"[A-Za-z0-9_]+", database):
        raise ValueError("Nombre de base inválido en DB_URL.")
    command = [arg for arg in importer.mysql_command() if not arg.startswith("--database=")]
    env = os.environ.copy()
    env["MYSQL_PWD"] = env["DB_PASSWORD"]
    sql = f"CREATE DATABASE IF NOT EXISTS `{database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    result = subprocess.run(command, input=sql.encode("utf8"), stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, env=env)
    if result.returncode:
        raise ValueError("No se pudo crear o acceder a la base. Revisá que MySQL esté iniciado y que tus credenciales tengan permiso para crearla.")
    version = importer.mysql("SELECT VERSION();").strip()
    numbers = tuple(int(n) for n in version.split("-")[0].split(".")[:3])
    if "MariaDB" in version or numbers < (8, 0, 18):
        raise ValueError("Se requiere MySQL 8.0.18 o posterior; MariaDB no está soportado por esta importación.")
    print(f"MySQL {version}. Base local: {database}.", flush=True)

def verificar_java():
    home = os.environ.get("JAVA_HOME")
    java = str(Path(home) / "bin" / ("java.exe" if os.name == "nt" else "java")) if home else shutil.which("java")
    if not java or not Path(java).exists():
        raise ValueError("No se encontró Java. Instalar JDK 21 y configurar JAVA_HOME.")
    result = subprocess.run([java, "-XshowSettings:properties", "-version"],
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    output = result.stdout + result.stderr
    version = re.search(r"java.version\s*=\s*(\d+)", output)
    java_home = re.search(r"java.home\s*=\s*(.+)", output)
    if result.returncode or not version or int(version.group(1)) < 21 or not java_home:
        raise ValueError("El backend requiere Java 21. Revisá JAVA_HOME y java --version.")
    jdk = Path(java_home.group(1).strip())
    if not (jdk / "bin" / ("javac.exe" if os.name == "nt" else "javac")).exists():
        raise ValueError("Se requiere un JDK con javac, no solamente un JRE.")
    os.environ["JAVA_HOME"] = str(jdk)
    os.environ["PATH"] = str(jdk / "bin") + os.pathsep + os.environ.get("PATH", "")

def verificar_puerto(puerto):
    with socket.socket() as sock:
        sock.settimeout(1)
        if sock.connect_ex(("127.0.0.1", puerto)) == 0:
            raise ValueError(f"El puerto {puerto} ya está ocupado. Detené tu otra instancia del backend antes de iniciar esta.")

CORREO = re.compile(r"[^@\s]+@[^@\s]+\.[^@\s]+")

def correo_admin():
    correo = os.environ.get("ADMIN_EMAIL", "").strip()
    while not CORREO.fullmatch(correo):
        correo = input("Correo del administrador: ").strip()
        if not CORREO.fullmatch(correo):
            print("Ingresá un correo válido, por ejemplo nombre@organizacion.com.")
    return correo

def password_cuenta(variable, etiqueta):
    password = os.environ.get(variable)
    while not password or len(password) < 12 or len(password.encode("utf8")) > 72:
        password = getpass.getpass(f"Elegí contraseña para {etiqueta} (mínimo 12 caracteres, máximo 72 bytes UTF-8): ")
        if len(password) < 12 or len(password.encode("utf8")) > 72:
            print("Debe tener al menos 12 caracteres y no superar 72 bytes UTF-8.")
    return password

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preparar-solo", action="store_true", help="Crear base e importar capas; no iniciar backend ni crear cuentas")
    parser.add_argument("--sin-admin", action="store_true", help="Iniciar sin pedir ni crear un administrador: se usan las cuentas que ya existan o la configuración ADMIN_*")
    args = parser.parse_args()
    if sys.version_info < (3, 10):
        raise ValueError("Se requiere Python 3.10 o posterior.")
    verificar_java()
    if not args.preparar_solo:
        verificar_puerto(8080)
    configurar_conexion()
    importer.mysql(importer.SCHEMA)
    for layer in importer.DATASETS:
        importer.import_layer(layer)
    if args.preparar_solo:
        print("Base y capas listas. No se crearon cuentas ni se inició el backend.")
        return
    env = os.environ.copy()
    env["JPA_DDL_AUTO"] = "validate"
    env["SERVER_PORT"] = "8080"
    # Escucha local para la presentación; no abre el administrador a otras PCs.
    env["SERVER_ADDRESS"] = "127.0.0.1"
    if not args.sin_admin:
        # Solo viajan al proceso del backend: no se guardan en archivos ni en el entorno de esta ventana.
        env["ADMIN_EMAIL"] = correo_admin()
        env["ADMIN_PASSWORD"] = password_cuenta("ADMIN_PASSWORD", env["ADMIN_EMAIL"])
        print(f"Administrador: {env['ADMIN_EMAIL']}")
        print("Si esa cuenta ya existía se conserva su contraseña actual; si no, se crea con la que acabás de elegir.")
    print("Backend: http://localhost:8080/ · Detener: Ctrl+C", flush=True)
    wrapper = ROOT / ("mvnw.cmd" if os.name == "nt" else "mvnw")
    result = subprocess.run([str(wrapper), "spring-boot:run"], cwd=ROOT, env=env)
    if result.returncode:
        raise RuntimeError("El backend no pudo iniciar. Revisá el error que aparece arriba.")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nEjecución detenida.")
    except (OSError, ValueError, RuntimeError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
