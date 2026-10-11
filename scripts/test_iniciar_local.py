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
                self.assertEqual(launcher.password_cuenta("DEMO_ADMIN_PASSWORD", "admin"), "MiClaveLocal12345")

    def test_preparar_solo_no_crea_cuentas_ni_inicia_servidor(self):
        with patch.object(launcher.sys, "argv", ["iniciar_local.py", "--preparar-solo"]), \
             patch.object(launcher, "verificar_java"), patch.object(launcher, "configurar_conexion"), \
             patch.object(launcher, "verificar_puerto") as port, \
             patch.object(launcher.importer, "mysql"), patch.object(launcher.importer, "import_layer") as layer, \
             patch.object(launcher, "password_cuenta") as password, patch.object(launcher.subprocess, "run") as run:
            launcher.main()
            self.assertEqual(layer.call_count, 3)
            port.assert_not_called()
            password.assert_not_called()
            run.assert_not_called()

    def test_arranque_con_cuentas_no_activa_lotes_ficticios_y_preserva_otras_variables(self):
        with patch.dict(os.environ, {"ADMIN_EMAIL": "privado@example.invalid", "DB_PASSWORD": "privada"}, clear=True), \
             patch.object(launcher.sys, "argv", ["iniciar_local.py"]), \
             patch.object(launcher, "verificar_java"), patch.object(launcher, "configurar_conexion"), \
             patch.object(launcher, "verificar_puerto") as port, \
             patch.object(launcher.importer, "mysql"), patch.object(launcher.importer, "import_layer"), \
             patch.object(launcher, "password_cuenta", side_effect=["MiClaveAdmin12345", "MiClaveUsuario12345"]), \
             patch.object(launcher.subprocess, "run", return_value=SimpleNamespace(returncode=0)) as run:
            launcher.main()
            env = run.call_args.kwargs["env"]
            self.assertEqual(env["DEMO_LOTES_ENABLED"], "false")
            self.assertEqual(env["DEMO_SQL_INIT_MODE"], "never")
            self.assertEqual(env["DEMO_ACCOUNTS_ENABLED"], "true")
            self.assertEqual(env["ADMIN_EMAIL"], "")
            self.assertEqual(env["DB_PASSWORD"], "privada")
            self.assertEqual(os.environ["ADMIN_EMAIL"], "privado@example.invalid")
            self.assertEqual(env["SERVER_ADDRESS"], "127.0.0.1")
            port.assert_called_once_with(8080)

    def test_sin_cuentas_preserva_configuracion_admin(self):
        with patch.dict(os.environ, {"ADMIN_EMAIL": "propio@example.invalid"}, clear=True), \
             patch.object(launcher.sys, "argv", ["iniciar_local.py", "--sin-cuentas-prueba"]), \
             patch.object(launcher, "verificar_java"), patch.object(launcher, "configurar_conexion"), \
             patch.object(launcher, "verificar_puerto"), patch.object(launcher.importer, "mysql"), \
             patch.object(launcher.importer, "import_layer"), patch.object(launcher, "password_cuenta") as password, \
             patch.object(launcher.subprocess, "run", return_value=SimpleNamespace(returncode=0)) as run:
            launcher.main()
            env = run.call_args.kwargs["env"]
            self.assertEqual(env["DEMO_ACCOUNTS_ENABLED"], "false")
            self.assertEqual(env["ADMIN_EMAIL"], "propio@example.invalid")
            password.assert_not_called()

if __name__ == "__main__":
    unittest.main()
