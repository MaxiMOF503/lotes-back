package ies.belgrano.lotes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

/** Reloj inyectable: permite probar sin esperar el tiempo real. */
@Configuration
public class TiempoConfig {
	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}
}
