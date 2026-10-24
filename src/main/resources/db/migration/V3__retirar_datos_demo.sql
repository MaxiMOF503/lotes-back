-- LoteSeguro V3: retira los datos y las tablas de demostracion (lotes ficticios, cuentas de clase,
-- linea electrica y departamento demo). La aplicacion trabaja solo con las capas oficiales importadas.
--
-- NUNCA toca: territorio_elementos, territorio_fuentes, territorio_sin_geometria, cuentas reales,
-- fichas_parcelas, consultas_guardadas ni consultas_historial de cuentas reales, estadisticas_diarias.
--
-- Pensada para repetirse sin error: cada paso comprueba antes que la tabla exista. Primero va todo el
-- DML (Flyway lo confirma o lo revierte junto) y al final los DROP TABLE, que MySQL confirma solos.
-- Una tabla con filas que no son demo (por ejemplo un lote cargado por un administrador) NO se borra:
-- queda sin usar para que alguien la revise (ver scripts/diagnostico-datos-demo.sql).
-- Hacer una copia de seguridad antes de aplicarla (docs/migraciones-flyway.md).

-- 1. Cuentas de clase y todo lo que dependia de ellas.
DELETE FROM consultas_guardadas WHERE usuario_id IN (SELECT id FROM usuarios WHERE email IN
  ('usuario.demo@loteseguro.invalid', 'demo.admin@loteseguro.invalid', 'demo.usuario@loteseguro.invalid'));
DELETE FROM consultas_historial WHERE usuario_id IN (SELECT id FROM usuarios WHERE email IN
  ('usuario.demo@loteseguro.invalid', 'demo.admin@loteseguro.invalid', 'demo.usuario@loteseguro.invalid'));
DELETE FROM usuarios WHERE email IN
  ('usuario.demo@loteseguro.invalid', 'demo.admin@loteseguro.invalid', 'demo.usuario@loteseguro.invalid');

-- 2. Filas demo de lineas_electricas, lotes y departamentos (si esas tablas todavia existen).
SET @hay_lineas = (SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lineas_electricas');
SET @hay_lotes = (SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lotes');
SET @hay_departamentos = (SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departamentos');

SET @ls_sql = IF(@hay_lineas > 0,
  'DELETE FROM lineas_electricas WHERE tipo = ''RED DEMOSTRATIVA'' AND fuente_datos = ''Dataset demostrativo B-01''',
  'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

SET @ls_sql = IF(@hay_lotes > 0,
  'DELETE FROM lotes WHERE identificador LIKE ''LOT-DEMO-%'' OR es_dato_simulado = 1 OR lote_fuente LIKE ''Dataset demostrativo%'' OR lote_fuente LIKE ''Escenarios did%cticos locales%''',
  'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

-- El departamento demo se borra solo si ningun lote que quedo lo referencia.
SET @ls_sql = IF(@hay_departamentos > 0,
  IF(@hay_lotes > 0,
    'DELETE FROM departamentos WHERE fuente_datos = ''Dataset demostrativo B-01'' AND NOT EXISTS (SELECT 1 FROM lotes l WHERE l.departamento_id = departamentos.id)',
    'DELETE FROM departamentos WHERE fuente_datos = ''Dataset demostrativo B-01'''),
  'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

-- 3. Retirar las tablas del modelo anterior, solo si quedaron vacias.
SET @filas_lineas = 0;
SET @filas_lotes = 0;
SET @filas_departamentos = 0;

SET @ls_sql = IF(@hay_lineas > 0, 'SELECT COUNT(*) INTO @filas_lineas FROM lineas_electricas', 'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

SET @ls_sql = IF(@hay_lotes > 0, 'SELECT COUNT(*) INTO @filas_lotes FROM lotes', 'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

SET @ls_sql = IF(@hay_departamentos > 0, 'SELECT COUNT(*) INTO @filas_departamentos FROM departamentos', 'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

-- lotes primero: tiene la clave foranea hacia departamentos.
SET @ls_sql = IF(@hay_lotes > 0 AND @filas_lotes = 0, 'DROP TABLE lotes', 'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

SET @ls_sql = IF(@hay_departamentos > 0 AND @filas_departamentos = 0 AND (@hay_lotes = 0 OR @filas_lotes = 0),
  'DROP TABLE departamentos', 'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

SET @ls_sql = IF(@hay_lineas > 0 AND @filas_lineas = 0, 'DROP TABLE lineas_electricas', 'DO 0');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;
