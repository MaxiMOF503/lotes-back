-- Metadatos internos separados de las propiedades originales. No reclasifica datos:
-- volver a ejecutar el importador de catastro después de actualizar el esquema.
-- Idempotente para el preparador local, como V2; no altera migraciones publicadas.
SET @ls_sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE territorio_elementos ADD COLUMN tipo_duplicado varchar(32) DEFAULT NULL', 'DO 0')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='territorio_elementos' AND COLUMN_NAME='tipo_duplicado');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

SET @ls_sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE territorio_elementos ADD COLUMN campos_conflicto json DEFAULT NULL', 'DO 0')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='territorio_elementos' AND COLUMN_NAME='campos_conflicto');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;

SET @ls_sql = (SELECT IF(COUNT(*) = 0,
  'CREATE INDEX territorio_duplicado ON territorio_elementos(capa,duplicado_de)', 'DO 0')
  FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='territorio_elementos' AND INDEX_NAME='territorio_duplicado');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;
