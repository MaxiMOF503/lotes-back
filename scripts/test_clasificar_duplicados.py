"""Clasificación conservadora: no necesita MySQL y no cambia datasets."""
import copy
import unittest
from unittest.mock import patch
import importar_territorio as i

GEOMETRIA = {"type": "MultiPolygon", "coordinates": [[[[0, 0], [1, 0], [1, 1], [0, 0]]]]}


class ClasificacionTest(unittest.TestCase):
    def filas(self, a, b):
        return [("a", GEOMETRIA, {"nomenclatu": "0601", **a}),
                ("b", GEOMETRIA, {"nomenclatu": "0601", **b})]

    def test_identico_ignora_solo_fid(self):
        r = i.clasificar_coincidencias(self.filas({"fid": 1, "ochava": None}, {"fid": 2, "ochava": None}))
        self.assertEqual(r["b"], {"representante": "a", "tipo": i.DUPLICADO_IDENTICO, "campos": []})
        self.assertEqual(i.CAMPOS_TECNICOS, {"fid"})

    def test_conflicto_simple(self):
        r = i.clasificar_coincidencias(self.filas({"ochava": None}, {"ochava": "SI"}))
        self.assertEqual(r["b"]["tipo"], i.CONFLICTO_ATRIBUTOS)
        self.assertEqual(r["b"]["campos"], ["ochava"])

    def test_conflicto_multiple_ordenado(self):
        r = i.clasificar_coincidencias(self.filas({"ochava": None, "ret_front2": 1}, {"ochava": "SI", "ret_front2": 2}))
        self.assertEqual(r["b"]["campos"], ["ochava", "ret_front2"])

    def test_varias_copias_se_comparan_con_el_mismo_representante(self):
        filas = self.filas({"fid": 1}, {"fid": 2}) + [("c", GEOMETRIA, {"nomenclatu": "0601", "ochava": "SI"})]
        r = i.clasificar_coincidencias(filas)
        self.assertEqual(r["b"]["tipo"], i.DUPLICADO_IDENTICO)
        self.assertEqual(r["c"]["tipo"], i.CONFLICTO_ATRIBUTOS)
        self.assertEqual({d["representante"] for d in r.values()}, {"a"})
        resumen = i.resumen_coincidencias(r)
        self.assertEqual(resumen["grupos_conflictivos"], 1)
        self.assertEqual(resumen["registros_en_grupos_conflictivos"], 3)

    def test_no_agrupa_geometrias_distintas(self):
        filas = self.filas({}, {})
        filas[1] = ("b", {"type": "MultiPolygon", "coordinates": []}, filas[1][2])
        self.assertEqual(i.clasificar_coincidencias(filas), {})

    def test_no_agrupa_nomenclaturas_distintas(self):
        self.assertEqual(i.clasificar_coincidencias(self.filas({}, {"nomenclatu": "0602"})), {})

    def test_ausencia_null_y_vacio_son_diferentes(self):
        for a, b in [({}, {"ochava": None}), ({"ochava": None}, {"ochava": ""})]:
            with self.subTest(a=a, b=b):
                self.assertEqual(i.clasificar_coincidencias(self.filas(a, b))["b"]["campos"], ["ochava"])

    def test_no_normaliza_tipos_espacios_ni_mayusculas(self):
        for a, b in [(True, 1), (1, "1"), (1, 1.0), ("SI", "si"), (" SI", "SI")]:
            with self.subTest(a=a, b=b):
                self.assertEqual(i.campos_diferentes({"campo": a}, {"campo": b}), ["campo"])

    def test_json_anidado_no_depende_del_orden_de_claves(self):
        self.assertEqual(i.campos_diferentes({"x": {"b": 2, "a": 1}}, {"x": {"a": 1, "b": 2}}), [])
        self.assertEqual(i.campos_diferentes({"x": [1, 2]}, {"x": [2, 1]}), ["x"])

    def test_cualquier_campo_no_tecnico_genera_conflicto(self):
        for campo in ("alerta_sup", "id_ev", "id_bp", "aluvional", "ochava", "ret_front2", "ret_lat1", "ret_lat2", "ret_post", "nuevo"):
            with self.subTest(campo=campo):
                self.assertEqual(i.campos_diferentes({campo: None}, {campo: "SI"}), [campo])

    def test_no_modifica_propiedades_y_el_sql_no_las_reescribe(self):
        filas = self.filas({"ochava": None}, {"ochava": "SI"})
        copia = copy.deepcopy(filas)
        sql = i.sql_clasificacion(i.clasificar_coincidencias(filas))
        self.assertEqual(filas, copia)
        self.assertNotIn("propiedades=", sql)
        self.assertNotIn("DELETE", sql)
        self.assertIn("tipo_duplicado=CASE", sql)
        self.assertIn("campos_conflicto=CASE", sql)
        self.assertEqual(sql, i.sql_clasificacion(i.clasificar_coincidencias(filas)))

    def test_reclasifica_aunque_la_carga_se_omita_por_checksum(self):
        with patch.object(i, "cargar_capa") as cargar, patch.object(i, "marcar_copias") as marcar:
            i.import_layer("catastro")
            cargar.assert_called_once_with("catastro", False)
            marcar.assert_called_once_with("catastro")

    def test_esquema_reutiliza_v5_sin_modificar_v2(self):
        self.assertIn(i.MIGRACION_CLASIFICACION.read_text(encoding="utf8"), i.SCHEMA)
        self.assertNotIn("tipo_duplicado", i.MIGRACION_TERRITORIO.read_text(encoding="utf8"))

    def test_analisis_no_se_conecta_a_mysql(self):
        with patch.object(i, "mysql") as mysql, patch("sys.argv", ["importar_territorio.py", "--analizar-coincidencias"]), patch("builtins.print"):
            i.main()
            mysql.assert_not_called()


if __name__ == "__main__":
    unittest.main()
