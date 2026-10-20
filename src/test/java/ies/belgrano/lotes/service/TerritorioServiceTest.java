package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.TerritorioResponse.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.TerritorioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerritorioServiceTest {
    @Mock TerritorioRepository repository;
    @InjectMocks TerritorioService service;
    private Fuente fuente(String capa) { return new Fuente(capa,"fuente","https://example.org",2,0,0,"2026-10-11"); }

    @Test void rechazaCoordenadasNoFinitasAntesDeConsultarLaBase() {
        assertThatThrownBy(() -> service.consultar(Double.NaN,-68.9)).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> service.consultar(-33,Double.POSITIVE_INFINITY)).isInstanceOf(OperacionInvalidaException.class);
        verifyNoInteractions(repository);
    }
    @Test void rechazaVentanasInvertidasCapasDesconocidasYLimitesExcesivos() {
        assertThatThrownBy(() -> service.capa("catastro",-68.8,-33.1,-69,-33,16,20)).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> service.capa("usuarios",-69,-33.1,-68.8,-33,16,20)).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> service.capa("catastro",-69,-33.1,-68.8,-33,16,1001)).isInstanceOf(OperacionInvalidaException.class);
        verifyNoInteractions(repository);
    }
    @Test void noDescargaParcelasMasivamenteCuandoElZoomEsBajo() {
        when(repository.fuentes()).thenReturn(List.of(fuente("catastro")));
        Capa result=service.capa("catastro",-69,-33.1,-68.8,-33,11,500);
        assertThat(result.acercar()).isTrue();
        assertThat(result.features()).isEmpty();
        verify(repository,never()).ventana(anyString(),anyString(),anyInt());
    }
    @Test void indicaRecorteEnVezDeAfirmarQueSeMostroTodaLaZona() {
        when(repository.fuentes()).thenReturn(List.of(fuente("catastro")));
        Feature a=new Feature("Feature","1",null,null);
        when(repository.ventana(eq("catastro"),anyString(),eq(3))).thenReturn(List.of(a,a,a));
        Capa result=service.capa("catastro",-68.90,-33.07,-68.89,-33.06,16,2);
        assertThat(result.hayMas()).isTrue();
        assertThat(result.features()).hasSize(2);
    }
    @Test void unaImportacionParcialDevuelveErrorExplicito() {
        when(repository.fuentes()).thenReturn(List.of(fuente("catastro")));
        assertThatThrownBy(() -> service.consultar(-33,-68.9)).isInstanceOf(OperacionInvalidaException.class)
            .hasMessageContaining("no fue importada");
        verify(repository,never()).parcelas(anyString(),anyInt());
    }
    @Test void sinParcelaMantieneElCruceDeServiciosSinInventarDatos() {
        when(repository.fuentes()).thenReturn(List.of(fuente("catastro"),fuente("aysam"),fuente("electricidad")));
        when(repository.parcelas(anyString(),eq(6))).thenReturn(List.of());
        when(repository.electricidad(anyString(),anyString(),eq(1000))).thenReturn(new Electricidad(null,null,1000,"Sin tramo"));
        Consulta result=service.consultar(-33,-68.9);
        assertThat(result.parcelas()).isEmpty();
        assertThat(result.dentroRadioAgua()).isFalse();
        assertThat(result.electricidad().distanciaMetros()).isNull();
        verify(repository).dentroAgua("POINT(-68.9 -33.0)");
        assertThat(result.advertencia()).contains("no confirman una conexión");
    }
    @Test void cercanasRechazaRadiosFueraDeRangoSinConsultarLaBase() {
        assertThatThrownBy(() -> service.cercanas(-33.03,-68.87,4)).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> service.cercanas(-33.03,-68.87,101)).isInstanceOf(OperacionInvalidaException.class);
        verifyNoInteractions(repository);
    }
    @Test void cercanasBuscaAlrededorDelPuntoConUnaVentanaApenasMayorQueElRadio() {
        when(repository.fuentes()).thenReturn(List.of(fuente("catastro")));
        when(repository.cercanas(anyString(),anyString(),eq(30.0),eq(10))).thenReturn(List.of());
        Cercanas result=service.cercanas(-33.0262588845649,-68.8749132362673,30);
        assertThat(result.radioMetros()).isEqualTo(30);
        var ventana=ArgumentCaptor.forClass(String.class);
        verify(repository).cercanas(eq("POINT(-68.8749132362673 -33.0262588845649)"),ventana.capture(),eq(30.0),eq(10));
        // 30 m con margen: unas 3 diezmilésimas de grado de latitud a cada lado, no una ventana de kilómetros.
        assertThat(ventana.getValue()).startsWith("POLYGON((-68.875").contains(" -33.0265");
    }
}
