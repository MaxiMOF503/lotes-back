package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.CalidadCatastroResponse.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.CalidadCatastroRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalidadCatastroServiceTest {
    final CalidadCatastroRepository repository=mock(CalidadCatastroRepository.class);
    final CalidadCatastroService service=new CalidadCatastroService(repository);
    Registro registro(String id,String padre,String tipo,List<String> campos,String json){
        return new Registro(id,padre,tipo,campos,JsonMapper.builder().build().readTree(json));
    }
    void grupo(Registro... registros){
        when(repository.representante("b")).thenReturn(Optional.of("a"));
        when(repository.grupo("a")).thenReturn(List.of(registros));
    }
    @Test void conservaTodasLasVariantesYOrdenaCamposSinMezclarValores(){
        grupo(registro("a",null,null,List.of(),"{\"ochava\":null}"),
            registro("b","a","DUPLICADO_IDENTICO",List.of(),"{\"ochava\":null}"),
            registro("c","a","CONFLICTO_ATRIBUTOS",List.of("ret_front2","ochava"),"{\"ochava\":\"SI\"}"));
        var r=service.grupo("b");
        assertThat(r.representante()).isEqualTo("a");assertThat(r.idSolicitado()).isEqualTo("b");
        assertThat(r.clasificacion()).isEqualTo("COINCIDENCIA_CON_CONFLICTO");
        assertThat(r.camposConflicto()).containsExactly("ochava","ret_front2");
        assertThat(r.registros()).extracting(Registro::parcelaId).containsExactly("a","b","c");
        assertThat(r.registros().getFirst().propiedades().get("ochava").isNull()).isTrue();
        assertThat(r.registros().getLast().propiedades().get("ochava").asText()).isEqualTo("SI");
        verify(repository).representante("b");verify(repository).grupo("a");verifyNoMoreInteractions(repository);
    }
    @Test void noConfundeClasificacionPendienteConIdentica(){
        grupo(registro("a",null,null,List.of(),"{}"),registro("b","a",null,List.of(),"{}"));
        assertThat(service.grupo("b").clasificacion()).isEqualTo("PENDIENTE_CLASIFICACION");
    }
    @Test void todosIdenticos(){
        grupo(registro("a",null,null,List.of(),"{}"),registro("b","a","DUPLICADO_IDENTICO",List.of(),"{}"));
        assertThat(service.grupo("b").clasificacion()).isEqualTo("DUPLICADO_IDENTICO");
    }
    @Test void validaIdAntesDeConsultarYConserva404(){
        assertThatThrownBy(()->service.grupo("no válido")).isInstanceOfSatisfying(OperacionInvalidaException.class,e->assertThat(e.status).isEqualTo(400));
        verifyNoInteractions(repository);
        assertThatThrownBy(()->service.grupo("ausente")).isInstanceOfSatisfying(OperacionInvalidaException.class,e->assertThat(e.status).isEqualTo(404));
    }
    @Test void noInventaRepresentanteSiElEnlaceEsInconsistente(){
        grupo(registro("b","a","DUPLICADO_IDENTICO",List.of(),"{}"));
        assertThatThrownBy(()->service.grupo("b")).isInstanceOfSatisfying(OperacionInvalidaException.class,e->assertThat(e.status).isEqualTo(409));
    }
    @Test void parcelaSinCopiasNoEsUnDuplicado(){
        grupo(registro("a",null,null,List.of(),"{}"));
        assertThat(service.grupo("b").clasificacion()).isEqualTo("SIN_COINCIDENCIAS");
    }
    @Test void listadoPermiteLocalizarConflictosSinCargarTodosLosValores(){
        var filas=java.util.stream.IntStream.range(0,21).mapToObj(i->new GrupoResumen("p."+i,2,1,0)).toList();
        when(repository.grupos(true,21,20)).thenReturn(filas);
        var pagina=service.grupos(true,1);
        assertThat(pagina.items()).hasSize(20);assertThat(pagina.hayMas()).isTrue();assertThat(pagina.pagina()).isEqualTo(1);
        verify(repository).grupos(true,21,20);verifyNoMoreInteractions(repository);
    }
    @Test void listadoRechazaPaginasFueraDeRango(){
        for(int pagina:new int[]{-1,5001})assertThatThrownBy(()->service.grupos(false,pagina))
            .isInstanceOfSatisfying(OperacionInvalidaException.class,e->assertThat(e.codigo).isEqualTo("PAGINA_INVALIDA"));
        verifyNoInteractions(repository);
    }
}
