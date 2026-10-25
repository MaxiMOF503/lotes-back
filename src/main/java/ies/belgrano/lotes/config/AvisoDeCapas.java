package ies.belgrano.lotes.config;

import ies.belgrano.lotes.service.EstadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Al arrancar avisa en el log si falta importar alguna capa territorial. Cuando alguien actualiza el código
 * (git pull) y el equipo suma una capa, los datos no llegan solos: hay que ejecutar el importador.
 * Solo avisa: nunca modifica la base ni impide el arranque.
 */
@Component
public class AvisoDeCapas implements ApplicationRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(AvisoDeCapas.class);
	private final EstadoService estado;

	public AvisoDeCapas(EstadoService estado) {
		this.estado = estado;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			List<String> faltantes = estado.capasFaltantes();
			if (!faltantes.isEmpty()) {
				LOGGER.warn("Faltan capas territoriales por importar: {}. Ejecutá 'iniciar-local.cmd --preparar-solo' "
					+ "o 'py -3 scripts/importar_territorio.py' (ver AGENTS.md).", String.join(", ", faltantes));
			}
		} catch (RuntimeException e) {
			LOGGER.warn("No se pudo comprobar qué capas territoriales están importadas ({}).", e.getClass().getSimpleName());
		}
	}
}
