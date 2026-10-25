"""Verifica el arranque local sin alterar servidores ni cuentas reales."""
import os
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import iniciar_local as launcher

class ArranqueLocalTest(unittest.TestCase):
    def test_rechaza_servidor_remoto_antes_de_ejecutar_mysql(self):
        with patch.dict(os.environ, {"DB_URL": "jdbc:mysql://servidor-remoto/base",
                                     "DB_USERNAME": "root", "DB_PASSWORD": "no-es-un-secreto"}, clear=True):
            with patch.object(launcher.subprocess, "run") as run:
                with self.assertRaisesRegex(ValueError, "bases locales"):
                    launcher.configurar_conexion()
                run.assert_not_called()

    def test_password_respeta_limite_utf8_de_bcrypt(self):
        with patch.dict(os.environ, {}, clear=True):
            with patch.object(launcher.getpass, "getpass", side_effect=["x"*11, "á"*37, "MiClaveLocal12345"]):
                self.assertEqual(launcher.password_cuenta("ADMIN_PASSWORD", "admin"), "MiClaveLocal12345")

    def test_preparar_solo_no_crea_cuentas_ni_inicia_servidor(self):
        with patch.object(launcher.sys, "argv", ["iniciar_local.py", "--preparar-solo"]), \
             patch.object(launcher, "verificar_java"), patch.object(launcher, "configurar_conexion"), \
             patch.object(launcher, "verificar_puerto") as port, \
             patch.object(launcher.importer, "mysql"), patch.object(launcher.importer, "import_layer") as layer, \
             patch.object(launcher, "password_cuenta") as password, patch.object(launcher.subprocess, "run") as run:
            launcher.main()
            self.assertEqual(layer.call_count, len(launcher.importer.DATASETS))
            port.assert_not_called()
            password.assert_not_called()
            run.assert_not_called()

    def test_arranque_pide_el_administrador_real_y_no_define_variables_demo(self):
        with patch.dict(os.environ, {"DB_PASSWORD": "privada"}, clear=True), \
             patch.object(launcher.sys, "argv", ["iniciar_local.py"]), \
             patch.object(launcher, "verificar_java"), patch.object(launcher, "configurar_conexion"), \
             patch.object(launcher, "verificar_puerto") as port, \
             patch.object(launcher.importer, "mysql"), patch.object(launcher.importer, "import_layer"), \
             patch("builtins.input", side_effect=["no-es-un-correo", "ana@ejemplo.com"]), \
             patch.object(launcher, "password_cuenta", return_value="MiClaveAdmin12345") as password, \
             patch.object(launcher.subprocess, "run", return_value=SimpleNamespace(returncode=0)) as run:
            launcher.main()
            env = run.call_args.kwargs["env"]
            self.assertEqual(env["ADMIN_EMAIL"], "ana@ejemplo.com")
            self.assertEqual(env["ADMIN_PASSWORD"], "MiClaveAdmin12345")
            self.assertEqual(env["JPA_DDL_AUTO"], "validate")
            self.assertEqual(env["DB_PASSWORD"], "privada")
            self.assertEqual(env["SERVER_ADDRESS"], "127.0.0.1")
            self.assertFalse([k for k in env if k.startswith("DEMO_")])
            self.assertNotIn("ADMIN_PASSWORD", os.environ)
            password.assert_called_once_with("ADMIN_PASSWORD", "ana@ejemplo.com")
            port.assert_called_once_with(8080)

    def test_usa_el_correo_del_entorno_sin_preguntarlo(self):
        with patch.dict(os.environ, {"ADMIN_EMAIL": "propio@ejemplo.com"}, clear=True), \
             patch.object(launcher.sys, "argv", ["iniciar_local.py"]), \
             patch.object(launcher, "verificar_java"), patch.object(launcher, "configurar_conexion"), \
             patch.object(launcher, "verificar_puerto"), patch.object(launcher.importer, "mysql"), \
             patch.object(launcher.importer, "import_layer"), \
             patch("builtins.input", side_effect=AssertionError("no debe preguntar el correo")), \
             patch.object(launcher, "password_cuenta", return_value="MiClaveAdmin12345"), \
             patch.object(launcher.subprocess, "run", return_value=SimpleNamespace(returncode=0)) as run:
            launcher.main()
            self.assertEqual(run.call_args.kwargs["env"]["ADMIN_EMAIL"], "propio@ejemplo.com")

    def test_sin_admin_preserva_la_configuracion_y_no_pide_nada(self):
        with patch.dict(os.environ, {"ADMIN_EMAIL": "propio@example.invalid"}, clear=True), \
             patch.object(launcher.sys, "argv", ["iniciar_local.py", "--sin-admin"]), \
             patch.object(launcher, "verificar_java"), patch.object(launcher, "configurar_conexion"), \
             patch.object(launcher, "verificar_puerto"), patch.object(launcher.importer, "mysql"), \
             patch.object(launcher.importer, "import_layer"), patch.object(launcher, "password_cuenta") as password, \
             patch("builtins.input", side_effect=AssertionError("no debe preguntar")), \
             patch.object(launcher.subprocess, "run", return_value=SimpleNamespace(returncode=0)) as run:
            launcher.main()
            env = run.call_args.kwargs["env"]
            self.assertEqual(env["ADMIN_EMAIL"], "propio@example.invalid")
            self.assertNotIn("ADMIN_PASSWORD", env)
            password.assert_not_called()

if __name__ == "__main__":
    unittest.main()
