-- Avisos de datos incorrectos o faltantes enviados por personas autenticadas.
CREATE TABLE IF NOT EXISTS `reportes_datos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parcela_id` varchar(100) NOT NULL,
  `nomenclatura` varchar(150) NOT NULL,
  `seccion` varchar(30) NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `descripcion` varchar(1000) NOT NULL,
  `estado` varchar(20) NOT NULL,
  `respuesta` varchar(1000) DEFAULT NULL,
  `creado_por_id` bigint NOT NULL,
  `respondido_por_id` bigint DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL,
  `actualizado_en` datetime(6) NOT NULL,
  `respondido_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reporte_usuario_fecha` (`creado_por_id`,`creado_en`),
  KEY `idx_reporte_estado_fecha` (`estado`,`creado_en`),
  KEY `idx_reporte_parcela` (`parcela_id`),
  KEY `idx_reporte_respondido_por` (`respondido_por_id`),
  CONSTRAINT `fk_reporte_creado_por` FOREIGN KEY (`creado_por_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_reporte_respondido_por` FOREIGN KEY (`respondido_por_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `chk_reporte_seccion` CHECK (`seccion` IN
    ('PARCELA','ZONIFICACION','AGUA','ELECTRICIDAD','FICHA_COMPLEMENTARIA','OTRO')),
  CONSTRAINT `chk_reporte_tipo` CHECK (`tipo` IN ('INCORRECTO','FALTANTE')),
  CONSTRAINT `chk_reporte_estado` CHECK (`estado` IN ('PENDIENTE','EN_REVISION','RESUELTO','DESCARTADO')),
  CONSTRAINT `chk_reporte_descripcion` CHECK (CHAR_LENGTH(TRIM(`descripcion`)) BETWEEN 10 AND 1000),
  CONSTRAINT `chk_reporte_respuesta` CHECK (`respuesta` IS NULL OR CHAR_LENGTH(TRIM(`respuesta`)) BETWEEN 1 AND 1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
