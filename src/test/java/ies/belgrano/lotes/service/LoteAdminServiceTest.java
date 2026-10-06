package ies.belgrano.lotes.service;

import ies.belgrano.lotes.entity.LoteEntity;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.DepartamentoRepository;
import ies.belgrano.lotes.repository.LoteRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoteAdminServiceTest {
	@Mock LoteRepository lotes;
	@Mock DepartamentoRepository departamentos;
	@InjectMocks LoteAdminService service;

	@Test
	void marcaComoEliminadoElLoteSeleccionado() {
		LoteEntity lote = mock(LoteEntity.class);
		when(lotes.findByIdAndEliminadoFalse(7L)).thenReturn(Optional.of(lote));

		service.eliminar(7L);

		var orden = inOrder(lotes,lote);
		orden.verify(lotes).findByIdAndEliminadoFalse(7L);
		orden.verify(lote).eliminar();
		orden.verify(lotes).saveAndFlush(lote);
	}

	@Test
	void noEliminaUnLoteInexistente() {
		when(lotes.findByIdAndEliminadoFalse(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.eliminar(99L))
				.isInstanceOfSatisfying(OperacionInvalidaException.class,
						error -> assertThat(error.status).isEqualTo(404));
		verify(lotes, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(LoteEntity.class));
	}
}
