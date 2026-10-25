package ies.belgrano.lotes.config;

import ies.belgrano.lotes.service.EstadoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class AvisoDeCapasTest {
	private final EstadoService estado = mock(EstadoService.class);

	@Test void consultaLasCapasFaltantesSinTocarLaBase() {
		when(estado.capasFaltantes()).thenReturn(List.of("aguas_lujan"));
		new AvisoDeCapas(estado).run(new DefaultApplicationArguments());
		verify(estado).capasFaltantes();
		verify(estado, never()).estado();   // no llama a Georef al arrancar
	}

	@Test void unaFallaAlComprobarNuncaImpideElArranque() {
		when(estado.capasFaltantes()).thenThrow(new IllegalStateException("base caída"));
		assertThatCode(() -> new AvisoDeCapas(estado).run(new DefaultApplicationArguments())).doesNotThrowAnyException();
	}
}
