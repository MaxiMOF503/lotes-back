-- LoteSeguro V2: tablas de las capas territoriales oficiales (catastro, AySAM, electricidad).
-- No son entidades JPA: las carga scripts/importar_territorio.py, que ejecuta ESTE mismo
-- archivo antes de importar para que exista una sola definicion. Es idempotente: puede
-- correr sobre una base nueva, sobre una ya importada o repetirse sin tocar ninguna fila.

CREATE TABLE IF NOT EXISTS territorio_elementos (
  capa varchar(24) NOT NULL,
  origen_id varchar(100) NOT NULL,
  geometria geometry NOT NULL SRID 4326,
  propiedades json NOT NULL,
  valida tinyint(1) NOT NULL DEFAULT 1,
  duplicado_de varchar(100) DEFAULT NULL,
  PRIMARY KEY (capa, origen_id),
  SPATIAL KEY territorio_geometria (geometria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS territorio_fuentes (
  capa varchar(24) NOT NULL,
  fuente varchar(255) NOT NULL,
  url varchar(255) NOT NULL,
  sha256 char(64) NOT NULL,
  registros int NOT NULL,
  invalidos int NOT NULL,
  importado_en timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (capa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS territorio_sin_geometria (
  capa varchar(24) NOT NULL,
  origen_id varchar(100) NOT NULL,
  propiedades json NOT NULL,
  PRIMARY KEY (capa, origen_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Bases importadas antes de detectar parcelas repetidas no tienen duplicado_de: se agrega
-- solo si falta (MySQL no tiene ADD COLUMN IF NOT EXISTS).
SET @ls_sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE territorio_elementos ADD COLUMN duplicado_de varchar(100) DEFAULT NULL',
  'DO 0')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'territorio_elementos' AND COLUMN_NAME = 'duplicado_de');
PREPARE ls_paso FROM @ls_sql;
EXECUTE ls_paso;
DEALLOCATE PREPARE ls_paso;
