-- Ejemplos ficticios para clase. Los puntos representan localidades, no parcelas reales.
-- Reejecutable: conserva los registros existentes y solo agrega identificadores faltantes.
INSERT INTO departamentos (nombre, contacto_oficial, direccion_oficina, como_consultar, fuente_datos)
SELECT 'Luján de Cuyo', 'Contacto demostrativo no oficial', 'Oficina demostrativa',
       'Consultar la zonificación ante el organismo municipal competente', 'Dataset demostrativo B-01'
WHERE NOT EXISTS (SELECT 1 FROM departamentos WHERE nombre = 'Luján de Cuyo');

INSERT INTO lotes (
    identificador, ubicacion, departamento_id, direccion_aproximada, zonificacion,
    zonificacion_verificada, distancia_red_electrica_mts, tiene_acceso_electricidad,
    tiene_cobertura_agua, titular_simulado, situacion_dominial_simulada, es_dato_simulado,
    eliminado, lote_tipo, lote_fuente, lote_referencia, zonificacion_tipo,
    zonificacion_fuente, zonificacion_referencia, electricidad_tipo, electricidad_fuente,
    electricidad_referencia, agua_tipo, agua_fuente, agua_referencia
)
SELECT demo.identificador, ST_SRID(POINT(demo.longitud, demo.latitud), 4326), departamento.id,
       demo.direccion, demo.zonificacion, 0, demo.distancia_electricidad,
       demo.electricidad, demo.agua, 'Titular ficticio para demostración',
       'Situación simulada, sin valor legal', 1, 0,
       'SIMULADO', 'Escenarios didácticos locales, octubre 2026',
       'Punto de referencia de localidad basado en listado INDEC. No identifica parcela ni domicilio real.',
       'SIMULADO', 'Escenarios didácticos locales, octubre 2026',
       'Categoría inventada para mostrar la ficha; no es zonificación municipal.',
       'SIMULADO', 'Escenarios didácticos locales, octubre 2026',
       'Disponibilidad y distancia inventadas para clase; no verificadas con prestadora.',
       'SIMULADO', 'Escenarios didácticos locales, octubre 2026',
       'Disponibilidad inventada para clase; no verificada con prestadora.'
FROM (
    SELECT 'LOT-DEMO-LUJ-002' AS identificador, -33.0339984 AS latitud, -68.88837416 AS longitud,
           'Zona demostrativa - Ciudad de Luján de Cuyo (sin domicilio real)' AS direccion,
           'Residencial (simulada)' AS zonificacion, 45.0 AS distancia_electricidad,
           1 AS electricidad, 1 AS agua
    UNION ALL SELECT 'LOT-DEMO-LUJ-003', -33.07561204, -68.89378323,
           'Zona demostrativa - Perdriel, Luján de Cuyo (sin domicilio real)', 'Mixta (simulada)', 120.0, 1, 0
    UNION ALL SELECT 'LOT-DEMO-LUJ-004', -33.11738444, -68.89602354,
           'Zona demostrativa - Agrelo, Luján de Cuyo (sin domicilio real)', 'Rural (simulada)', 380.0, 0, 0
    UNION ALL SELECT 'LOT-DEMO-LUJ-005', -33.21094516, -68.8972088,
           'Zona demostrativa - Ugarteche, Luján de Cuyo (sin domicilio real)', 'Residencial (simulada)', 70.0, 1, 1
    UNION ALL SELECT 'LOT-DEMO-LUJ-006', -33.03439536, -68.97572014,
           'Zona demostrativa - Las Compuertas, Luján de Cuyo (sin domicilio real)', 'Residencial (simulada)', 260.0, 0, 1
    UNION ALL SELECT 'LOT-DEMO-LUJ-007', -33.07532823, -68.9250404,
           'Zona demostrativa - Barrio Perdriel IV, Luján de Cuyo (sin domicilio real)', 'Residencial (simulada)', 95.0, 1, 0
    UNION ALL SELECT 'LOT-DEMO-LUJ-008', -33.03671044, -69.11590683,
           'Zona demostrativa - Cacheuta, Luján de Cuyo (sin domicilio real)', 'Turística (simulada)', 210.0, 0, 1
    UNION ALL SELECT 'LOT-DEMO-LUJ-009', -33.07051845, -68.93432149,
           'Zona demostrativa - Costa Flores, Luján de Cuyo (sin domicilio real)', 'Rural (simulada)', 340.0, 0, 0
    UNION ALL SELECT 'LOT-DEMO-LUJ-010', -33.30374336, -68.7554952,
           'Zona demostrativa - El Carrizal, Luján de Cuyo (sin domicilio real)', 'Mixta (simulada)', 155.0, 1, 0
    UNION ALL SELECT 'LOT-DEMO-LUJ-011', -32.94548681, -69.20864491,
           'Zona demostrativa - Potrerillos, Luján de Cuyo (sin domicilio real)', 'Turística (simulada)', 280.0, 0, 1
) AS demo
JOIN departamentos AS departamento ON departamento.nombre = 'Luján de Cuyo'
WHERE NOT EXISTS (SELECT 1 FROM lotes AS existente WHERE existente.identificador = demo.identificador);
