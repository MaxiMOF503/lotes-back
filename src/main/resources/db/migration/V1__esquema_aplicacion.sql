-- LoteSeguro V1: tablas de la aplicacion (las nueve entidades JPA).
-- Es el esquema que Hibernate venia creando con ddl-auto=update; los nombres de indices y
-- claves foraneas se conservan para que coincidan con los de las bases ya instaladas.
-- Todas las sentencias son CREATE TABLE IF NOT EXISTS: en una base que ya tiene las tablas
-- no se recrea ni se modifica nada y no se toca ninguna fila.

CREATE TABLE IF NOT EXISTS `departamentos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `como_consultar` varchar(1000) DEFAULT NULL,
  `contacto_oficial` varchar(255) DEFAULT NULL,
  `direccion_oficina` varchar(255) DEFAULT NULL,
  `fuente_datos` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9vfgma390a16fkev3ryhnyrvh` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` enum('ADMIN','USUARIO') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKldv0v52e0udsh2h1rs0r0gw1n` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rol_id` bigint NOT NULL,
  `email` varchar(150) NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkfsp0s1tflm1cwlj8idhqsad0` (`email`),
  KEY `FKqf5elo4jcq7qrt83oi0qmenjo` (`rol_id`),
  CONSTRAINT `FKqf5elo4jcq7qrt83oi0qmenjo` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `lineas_electricas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tipo` varchar(50) DEFAULT NULL,
  `fuente_datos` varchar(255) DEFAULT NULL,
  `trazado` multilinestring NOT NULL SRID 4326,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `lotes` (
  `distancia_red_electrica_mts` double DEFAULT NULL,
  `eliminado` tinyint(1) NOT NULL DEFAULT '0',
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
  `ubicacion` point NOT NULL SRID 4326,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm0hwiom4j3a9d4aj00qd9i3tc` (`identificador`),
  KEY `FKk5fge6xrjenfmhxft4glylim5` (`departamento_id`),
  CONSTRAINT `FKk5fge6xrjenfmhxft4glylim5` FOREIGN KEY (`departamento_id`) REFERENCES `departamentos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `estadisticas_diarias` (
  `fecha` date NOT NULL,
  `cantidad` bigint NOT NULL,
  `criterio` varchar(20) NOT NULL,
  `resultado` varchar(20) NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `clave` varchar(100) NOT NULL,
  PRIMARY KEY (`clave`),
  KEY `idx_estadistica_fecha` (`fecha`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `consultas_guardadas` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `consultas_historial` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `fichas_parcelas` (
  `actualizado_en` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  `parcela_id` varchar(100) NOT NULL,
  `actualizado_por` varchar(150) NOT NULL,
  `referencia` varchar(1000) NOT NULL,
  `observaciones` varchar(2000) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `fuente` varchar(255) NOT NULL,
  PRIMARY KEY (`parcela_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
