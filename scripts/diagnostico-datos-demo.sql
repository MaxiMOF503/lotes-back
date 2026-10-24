-- Solo lectura: muestra que datos de demostracion va a retirar la migracion V3 y que va a conservar.
-- Ejecutar ANTES de actualizar, sobre la base de la aplicacion (por ejemplo en MySQL Workbench).
-- Si una tabla ya no existe, esa consulta falla: es normal en una base que ya se limpio.

SELECT 'cuentas demo que se borran' AS que, id, email FROM usuarios WHERE email IN
  ('usuario.demo@loteseguro.invalid', 'demo.admin@loteseguro.invalid', 'demo.usuario@loteseguro.invalid');

SELECT 'consultas guardadas de cuentas demo' AS que, COUNT(*) AS filas FROM consultas_guardadas WHERE usuario_id IN
  (SELECT id FROM usuarios WHERE email IN ('usuario.demo@loteseguro.invalid', 'demo.admin@loteseguro.invalid', 'demo.usuario@loteseguro.invalid'));
SELECT 'historial de cuentas demo' AS que, COUNT(*) AS filas FROM consultas_historial WHERE usuario_id IN
  (SELECT id FROM usuarios WHERE email IN ('usuario.demo@loteseguro.invalid', 'demo.admin@loteseguro.invalid', 'demo.usuario@loteseguro.invalid'));

SELECT 'lotes demo que se borran' AS que, id, identificador FROM lotes
  WHERE identificador LIKE 'LOT-DEMO-%' OR es_dato_simulado = 1 OR lote_fuente LIKE 'Dataset demostrativo%' OR lote_fuente LIKE 'Escenarios did%cticos locales%';

-- Lotes que NO son demo: si aparece alguno, la tabla lotes no se borra y hay que revisarlo a mano.
SELECT 'lotes que NO son demo (se conservan)' AS que, id, identificador FROM lotes
  WHERE NOT (identificador LIKE 'LOT-DEMO-%' OR es_dato_simulado = 1 OR lote_fuente LIKE 'Dataset demostrativo%' OR lote_fuente LIKE 'Escenarios did%cticos locales%');

SELECT 'linea electrica demo que se borra' AS que, id, tipo FROM lineas_electricas
  WHERE tipo = 'RED DEMOSTRATIVA' AND fuente_datos = 'Dataset demostrativo B-01';

SELECT 'departamentos' AS que, id, nombre, fuente_datos FROM departamentos;

-- Se conservan siempre: cuentas reales, fichas, capas importadas y estadisticas.
SELECT 'cuentas reales (se conservan)' AS que, id, email FROM usuarios WHERE email NOT IN
  ('usuario.demo@loteseguro.invalid', 'demo.admin@loteseguro.invalid', 'demo.usuario@loteseguro.invalid');
SELECT 'fichas_parcelas (se conservan)' AS que, COUNT(*) AS filas FROM fichas_parcelas;
