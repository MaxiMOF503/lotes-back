-- LoteSeguro: instalacion NUEVA y sin registros. MySQL 8.0.16+ (no MariaDB).
-- Fuente: origin/main 0f8e476814f61b80053755a95cb01909f9c19bb9 (19/10/2026).
-- Nueve entidades JPA y DDL territorial de scripts/importar_territorio.py.
-- Elegir un nombre que NO exista y reemplazarlo en CREATE DATABASE y USE.
-- Ejecutar el archivo COMPLETO; detenerse al primer error y no usar --force.
-- No ejecutar fragmentos sueltos ni continuar manualmente tras un error.
-- El guard evita crear tablas si CREATE DATABASE fallo, aun si el cliente continua.

CREATE DATABASE `verificador_lotes` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Leer el resultado inmediatamente: una base existente produce ERROR 1007.
SET @ls_base_creada = (@@error_count = 0);
USE `verificador_lotes`;

-- Las claves foraneas usan NO ACTION para DELETE/UPDATE (equivale a RESTRICT en InnoDB).
-- Indice espacial territorio_geometria sobre GEOMETRY SRID 4326.
-- No hay vistas, triggers ni rutinas propias en las fuentes inspeccionadas.
-- Cada DDL solo se ejecuta si esta ejecucion creo la base.

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `departamentos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `como_consultar` varchar(1000) DEFAULT NULL,
  `contacto_oficial` varchar(255) DEFAULT NULL,
  `direccion_oficina` varchar(255) DEFAULT NULL,
  `fuente_datos` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9vfgma390a16fkev3ryhnyrvh` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` enum(''ADMIN'',''USUARIO'') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKldv0v52e0udsh2h1rs0r0gw1n` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rol_id` bigint NOT NULL,
  `email` varchar(150) NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkfsp0s1tflm1cwlj8idhqsad0` (`email`),
  KEY `FKqf5elo4jcq7qrt83oi0qmenjo` (`rol_id`),
  CONSTRAINT `FKqf5elo4jcq7qrt83oi0qmenjo` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `lineas_electricas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tipo` varchar(50) DEFAULT NULL,
  `fuente_datos` varchar(255) DEFAULT NULL,
  `trazado` multilinestring NOT NULL /*!80003 SRID 4326 */,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `lotes` (
  `distancia_red_electrica_mts` double DEFAULT NULL,
  `eliminado` tinyint(1) NOT NULL DEFAULT ''0'',
  `es_dato_simulado` bit(1) NOT NULL,
  `tiene_acceso_electricidad` bit(1) NOT NULL,
  `tiene_cobertura_agua` bit(1) NOT NULL,
  `zonificacion_verificada` bit(1) NOT NULL,
  `departamento_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `identificador` varchar(100) NOT NULL,
  `agua_referencia` varchar(1000) DEFAULT NULL,
  `electricidad_referencia` varchar(1000) DEFAULT NULL,
  `lote_referencia` varchar(1000) DEFAULT NULL,
  `zonificacion_referencia` varchar(1000) DEFAULT NULL,
  `agua_fuente` varchar(255) DEFAULT NULL,
  `agua_tipo` varchar(255) DEFAULT NULL,
  `direccion_aproximada` varchar(255) DEFAULT NULL,
  `electricidad_fuente` varchar(255) DEFAULT NULL,
  `electricidad_tipo` varchar(255) DEFAULT NULL,
  `lote_fuente` varchar(255) DEFAULT NULL,
  `lote_tipo` varchar(255) DEFAULT NULL,
  `situacion_dominial_simulada` varchar(255) DEFAULT NULL,
  `titular_simulado` varchar(255) DEFAULT NULL,
  `zonificacion` varchar(255) DEFAULT NULL,
  `zonificacion_fuente` varchar(255) DEFAULT NULL,
  `zonificacion_tipo` varchar(255) DEFAULT NULL,
  `ubicacion` point NOT NULL /*!80003 SRID 4326 */,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm0hwiom4j3a9d4aj00qd9i3tc` (`identificador`),
  KEY `FKk5fge6xrjenfmhxft4glylim5` (`departamento_id`),
  CONSTRAINT `FKk5fge6xrjenfmhxft4glylim5` FOREIGN KEY (`departamento_id`) REFERENCES `departamentos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `estadisticas_diarias` (
  `fecha` date NOT NULL,
  `cantidad` bigint NOT NULL,
  `criterio` varchar(20) NOT NULL,
  `resultado` varchar(20) NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `clave` varchar(100) NOT NULL,
  PRIMARY KEY (`clave`),
  KEY `idx_estadistica_fecha` (`fecha`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

-- Consultas personales y fichas complementarias (JPA actual).

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `consultas_guardadas` (
  `latitud` double NOT NULL,
  `longitud` double NOT NULL,
  `creada_en` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `etiqueta` varchar(100) DEFAULT NULL,
  `parcela_id` varchar(100) NOT NULL,
  `nomenclatura` varchar(150) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8xpw96v8py7lndbbpupt2c22d` (`usuario_id`,`parcela_id`),
  CONSTRAINT `FKijkqobxiibm0judtr3bm8wmey` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `consultas_historial` (
  `latitud` double NOT NULL,
  `longitud` double NOT NULL,
  `consultada_en` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `resumen` varchar(800) NOT NULL,
  `snapshot` longtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_historial_usuario_fecha` (`usuario_id`,`consultada_en`),
  CONSTRAINT `FKgauao8cl46w3aagk4k30ng1gw` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `fichas_parcelas` (
  `actualizado_en` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `parcela_id` varchar(100) NOT NULL,
  `actualizado_por` varchar(150) NOT NULL,
  `referencia` varchar(1000) NOT NULL,
  `observaciones` varchar(2000) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `fuente` varchar(255) NOT NULL,
  PRIMARY KEY (`parcela_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

-- Tablas externas a JPA: DDL del importador territorial, SIN sus datos.

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `territorio_elementos` (
  `capa` varchar(24) NOT NULL,
  `origen_id` varchar(100) NOT NULL,
  `geometria` geometry NOT NULL /*!80003 SRID 4326 */,
  `propiedades` json NOT NULL,
  `valida` tinyint(1) NOT NULL DEFAULT ''1'',
  PRIMARY KEY (`capa`,`origen_id`),
  SPATIAL KEY `territorio_geometria` (`geometria`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `territorio_fuentes` (
  `capa` varchar(24) NOT NULL,
  `fuente` varchar(255) NOT NULL,
  `url` varchar(255) NOT NULL,
  `sha256` char(64) NOT NULL,
  `registros` int NOT NULL,
  `invalidos` int NOT NULL,
  `importado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`capa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;

SET @ls_ddl = IF(@ls_base_creada,
'CREATE TABLE `territorio_sin_geometria` (
  `capa` varchar(24) NOT NULL,
  `origen_id` varchar(100) NOT NULL,
  `propiedades` json NOT NULL,
  PRIMARY KEY (`capa`,`origen_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
'SELECT ''Instalacion detenida: CREATE DATABASE no tuvo exito'' AS estado');
PREPARE ls_tabla FROM @ls_ddl;
EXECUTE ls_tabla;
DEALLOCATE PREPARE ls_tabla;
