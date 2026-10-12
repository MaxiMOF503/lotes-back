package ies.belgrano.lotes.service;

import ies.belgrano.lotes.entity.LoteEntity;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.DepartamentoRepository;
import ies.belgrano.lotes.repository.LoteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

	@Test
	void listarRechazaPaginaNegativa() {
		assertThatThrownBy(() -> service.listar(-1, 20, null, null, null, "identificador", "ASC"))
				.isInstanceOfSatisfying(OperacionInvalidaException.class,
						error -> assertThat(error.status).isEqualTo(400));
	}

	@Test
	void listarRechazaTamanioFueraDeRango() {
		assertThatThrownBy(() -> service.listar(0, 0, null, null, null, "identificador", "ASC"))
				.isInstanceOfSatisfying(OperacionInvalidaException.class,
						error -> assertThat(error.status).isEqualTo(400));
		assertThatThrownBy(() -> service.listar(0, 101, null, null, null, "identificador", "ASC"))
				.isInstanceOfSatisfying(OperacionInvalidaException.class,
						error -> assertThat(error.status).isEqualTo(400));
	}

	@Test
	void listarRechazaCampoDeOrdenInvalido() {
		assertThatThrownBy(() -> service.listar(0, 20, null, null, null, "zonificacion", "ASC"))
				.isInstanceOfSatisfying(OperacionInvalidaException.class,
						error -> assertThat(error.status).isEqualTo(400));
	}

	@Test
	void listarRechazaDireccionDeOrdenInvalida() {
		assertThatThrownBy(() -> service.listar(0, 20, null, null, null, "identificador", "DESCENDENTE"))
				.isInstanceOfSatisfying(OperacionInvalidaException.class,
						error -> assertThat(error.status).isEqualTo(400));
	}

	@Test
	void listarAplicaFiltrosYDevuelveResumenPaginado() {
		LoteEntity lote = mock(LoteEntity.class);
		when(lote.getId()).thenReturn(5L);
		when(lote.getIdentificador()).thenReturn("B-05");
		when(lote.getDireccionAproximada()).thenReturn("Calle Falsa 123");
		Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "identificador").and(Sort.by(Sort.Direction.ASC, "id")));
		when(lotes.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(lote), pageable, 1));

		var resultado = service.listar(0, 20, "b-05", null, 3L, "identificador", "ASC");

		assertThat(resultado.items()).hasSize(1);
		assertThat(resultado.items().get(0).id()).isEqualTo(5L);
		assertThat(resultado.items().get(0).identificador()).isEqualTo("B-05");
		assertThat(resultado.total()).isEqualTo(1);
		assertThat(resultado.totalPaginas()).isEqualTo(1);
	}
}
