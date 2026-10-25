package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.request.ReporteDatoRequest.*;
import ies.belgrano.lotes.dto.response.TerritorioResponse.Feature;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteDatoServiceTest {
    ReporteDatoRepository reportes = mock(ReporteDatoRepository.class);
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    TerritorioService territorio = mock(TerritorioService.class);
    ReporteDatoService service = new ReporteDatoService(reportes, usuarios, territorio);
    JsonMapper mapper = JsonMapper.builder().build();
    UsuarioEntity usuario = new UsuarioEntity("Ana", "ana@ejemplo.invalid", "hash",
        new RolEntity(RolEntity.NombreRol.USUARIO));
    UsuarioEntity admin = new UsuarioEntity("Admin", "admin@ejemplo.invalid", "hash",
        new RolEntity(RolEntity.NombreRol.ADMIN));

    @BeforeEach void referencias() {
        when(territorio.canonicoDisponible(anyString())).thenAnswer(i -> {
            String id=i.getArgument(0);
            return Optional.of(id);
        });
    }

    @Test void creaPendienteSoloParaUnaParcelaRealYConservaNomenclatura() {
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(territorio.parcela("parcela.1")).thenReturn(new Feature("Feature", "parcela.1",
            mapper.readTree("{}"), mapper.readTree("{\"nomenclatu\":\"06040601\"}")));
        when(reportes.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = service.crear(usuario.getEmail(), new Crear(" parcela.1 ",
            ReporteDato.Seccion.AGUA, ReporteDato.Tipo.FALTANTE, "  Falta indicar el prestador.  "));

        assertThat(respuesta.estado()).isEqualTo(ReporteDato.Estado.PENDIENTE);
        assertThat(respuesta.nomenclatura()).isEqualTo("06040601");
        assertThat(respuesta.descripcion()).isEqualTo("Falta indicar el prestador.");
        var captor = ArgumentCaptor.forClass(ReporteDato.class);
        verify(reportes).save(captor.capture());
        assertThat(captor.getValue().getCreadoPor()).isSameAs(usuario);
    }

    @Test void unaFallaAlValidarLaParcelaNoGuardaElReporte() {
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(territorio.parcela("inexistente")).thenThrow(new OperacionInvalidaException(404,
            "PARCELA_NO_ENCONTRADA", "No existe"));
        assertThatThrownBy(() -> service.crear(usuario.getEmail(), new Crear("inexistente",
            ReporteDato.Seccion.PARCELA, ReporteDato.Tipo.INCORRECTO, "Descripción suficientemente larga")))
            .isInstanceOf(OperacionInvalidaException.class);
        verifyNoInteractions(reportes);
    }

    @Test void laListaPropiaConsultaSoloElEmailAutenticado() {
        when(reportes.findByCreadoPorEmail(eq("ana@ejemplo.invalid"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        assertThat(service.propios("ana@ejemplo.invalid", 0).items()).isEmpty();
        verify(reportes).findByCreadoPorEmail(eq("ana@ejemplo.invalid"), any(Pageable.class));
        verify(reportes, never()).findAll(any(Pageable.class));
    }

    @Test void adminPuedeFiltrarPorEstado() {
        when(reportes.findByEstado(eq(ReporteDato.Estado.PENDIENTE), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        service.administrar(ReporteDato.Estado.PENDIENTE, 0);
        verify(reportes).findByEstado(eq(ReporteDato.Estado.PENDIENTE), any(Pageable.class));
    }

    @Test void cerrarExigeRespuestaYRegistraAlAdministrador() {
        var reporte = new ReporteDato(usuario, "parcela.1", "0604", ReporteDato.Seccion.PARCELA,
            ReporteDato.Tipo.INCORRECTO, "La superficie no coincide.", java.time.Instant.now());
        when(reportes.findById(7L)).thenReturn(Optional.of(reporte));
        when(usuarios.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.actualizar(admin.getEmail(), 7L,
            new Actualizar(ReporteDato.Estado.RESUELTO, "corta")))
            .isInstanceOfSatisfying(OperacionInvalidaException.class,
                error -> assertThat(error.codigo).isEqualTo("RESPUESTA_REQUERIDA"));

        var respuesta = service.actualizar(admin.getEmail(), 7L,
            new Actualizar(ReporteDato.Estado.RESUELTO, "  Se verificó y corrigió el dato.  "));
        assertThat(respuesta.estado()).isEqualTo(ReporteDato.Estado.RESUELTO);
        assertThat(respuesta.respuesta()).isEqualTo("Se verificó y corrigió el dato.");
        assertThat(respuesta.respondidoPor().email()).isEqualTo(admin.getEmail());
        assertThat(respuesta.respondidoEn()).isNotNull();
    }

    @Test void reporteInexistenteUsaCodigoEstable() {
        when(reportes.findById(99L)).thenReturn(Optional.empty());
        when(usuarios.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> service.actualizar(admin.getEmail(), 99L,
            new Actualizar(ReporteDato.Estado.EN_REVISION, null)))
            .isInstanceOfSatisfying(OperacionInvalidaException.class,
                error -> assertThat(error.codigo).isEqualTo("REPORTE_NO_ENCONTRADO"));
    }

    @Test void rechazaPaginasFueraDelLimite() {
        assertThatThrownBy(() -> service.propios(usuario.getEmail(), -1))
            .isInstanceOfSatisfying(OperacionInvalidaException.class,
                error -> assertThat(error.codigo).isEqualTo("PAGINA_INVALIDA"));
        assertThatThrownBy(() -> service.administrar(null, 5001))
            .isInstanceOf(OperacionInvalidaException.class);
        verifyNoInteractions(reportes);
    }
}
