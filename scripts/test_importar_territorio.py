"""Regresiones del dump mezclado y de la codificación espacial; sin acceso a MySQL."""
import struct
import unittest
from pathlib import Path
import importar_territorio as importer

class ImportadorTest(unittest.TestCase):
    def test_extraccion_excluye_usuarios_y_recupera_geometria_del_dump(self):
        rows = list(importer.read_electricity(importer.ROOT / "data" / importer.DATASETS["electricidad"][0]))
        self.assertEqual(len(rows), 13494)
        self.assertEqual(len({row[0] for row in rows}), 13494)
        self.assertEqual(set(rows[0][2]), {"tipo", "fuente_datos"})
        self.assertIn("Energía", rows[0][2]["fuente_datos"])
        geometry = bytes.fromhex(rows[0][1].split("0x")[1].split(",")[0])
        lon, lat = struct.unpack("<dd", geometry[18:34])
        self.assertAlmostEqual(lon, -69.05024776679063)
        self.assertAlmostEqual(lat, -33.044458995880476)
        self.assertIn("axis-order=long-lat", rows[0][1])

    def test_catastro_conserva_registros_sin_geometria(self):
        rows = list(importer.read_geojson(importer.ROOT / "data" / importer.DATASETS["catastro"][0]))
        self.assertEqual(len(rows), 65756)
        self.assertEqual(sum(geometry is None for _, geometry, _ in rows), 25)
        self.assertEqual(rows[0][2]["nomenclatu"], "060406000300002300007")

    def test_catastro_detecta_copias_exactas_y_conserva_la_primera(self):
        path = importer.ROOT / "data" / importer.DATASETS["catastro"][0]
        copias = importer.copias_exactas(importer.read_geojson_geometrias(path))
        self.assertEqual(len(copias), 605)
        self.assertEqual(len(set(copias.values())), 94)
        repetidas = sorted(copia for copia, original in copias.items() if original == "Parcelas_visor0.55136")
        self.assertEqual(len(repetidas), 7)
        self.assertNotIn("Parcelas_visor0.55136", copias)

    def test_misma_nomenclatura_con_otra_geometria_no_es_copia(self):
        cuadrado = {"type": "MultiPolygon", "coordinates": [[[[0, 0], [1, 0], [1, 1], [0, 0]]]]}
        otro = {"type": "MultiPolygon", "coordinates": [[[[2, 2], [3, 2], [3, 3], [2, 2]]]]}
        features = [("a", cuadrado, {"nomenclatu": "0601"}), ("b", otro, {"nomenclatu": "0601"}),
                    ("c", cuadrado, {"nomenclatu": "0601"}), ("d", None, {"nomenclatu": "0601"})]
        self.assertEqual(importer.copias_exactas(features), {"c": "a"})

    def test_esquema_territorial_sale_de_la_migracion_de_flyway(self):
        self.assertTrue(importer.MIGRACION_TERRITORIO.is_file())
        for tabla in ("territorio_elementos", "territorio_fuentes", "territorio_sin_geometria"):
            self.assertIn("CREATE TABLE IF NOT EXISTS " + tabla, importer.SCHEMA)
        self.assertIn("duplicado_de", importer.SCHEMA)
        self.assertNotIn("DROP ", importer.SCHEMA.upper())

    def test_escapes_no_alteran_binario_ni_acentos(self):
        self.assertEqual(importer.unescape(b"\\0\\n\\r\\b\\t\\Z\\\\\\'"), bytes([0,10,13,8,9,26,92,39]))
        self.assertEqual(importer.sql_text("Luján"), "CONVERT(0x4c756ac3a16e USING utf8mb4)")

if __name__ == "__main__":
    unittest.main()
