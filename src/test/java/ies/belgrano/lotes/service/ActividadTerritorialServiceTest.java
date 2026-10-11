package ies.belgrano.lotes.service;
import ies.belgrano.lotes.repository.*;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.dto.response.ActividadTerritorialResponse.*;
import ies.belgrano.lotes.dto.response.TerritorioResponse.*;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.FichaPublica;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActividadTerritorialServiceTest {
 ConsultaHistorialRepository historial=mock(ConsultaHistorialRepository.class);
 ConsultaGuardadaRepository guardadas=mock(ConsultaGuardadaRepository.class);
 UsuarioRepository usuarios=mock(UsuarioRepository.class);
 TerritorioService territorio=mock(TerritorioService.class);
 TerritorioRepository geometria=mock(TerritorioRepository.class);
 EspacioTerritorialService espacio=mock(EspacioTerritorialService.class);
 JsonMapper mapper=JsonMapper.builder().build();
 ActividadTerritorialService service=new ActividadTerritorialService(historial,guardadas,usuarios,territorio,geometria,espacio,mapper);
 UsuarioEntity usuario=mock(UsuarioEntity.class);
 Consulta consulta(){return new Consulta(-33.0,-68.8,List.of(new Feature("Feature","real.1",mapper.readTree("{}"),mapper.readTree("{\"nomenclatu\":\"060406\",\"suptotal\":200}"))),false,true,
    new Electricidad(80.0,"Linea publicada",1000,"No acredita conexión"),List.of(new Fuente("catastro","Municipal","https://ejemplo.invalid",1,0,0,"2026-10-01")),"Verificar factibilidad");}
 @Test void historialPreservaSnapshotAunqueCambienFuentesYAportes(){
   var c=consulta();var fecha=Instant.parse("2026-10-01T12:00:00Z");
   var aporte=new FichaPublica(null,"Aporte original","Fuente inicial","Acta original",fecha);
   var original=new Informe(fecha,fecha,false,null,c,List.of(new Aporte("real.1",aporte)));
   var h=new ConsultaHistorial(usuario,-33,-68.8,fecha,"060406",mapper.writeValueAsString(original));
   when(historial.findByIdAndUsuarioEmail(4L,"ana")).thenReturn(Optional.of(h));
   var r=service.informeHistorico("ana",4L);
   assertThat(r.historico()).isTrue();assertThat(r.fechaConsulta()).isEqualTo(fecha);
   assertThat(r.consulta()).isEqualTo(c);assertThat(r.aportes().getFirst().ficha().observaciones()).isEqualTo("Aporte original");
   verifyNoInteractions(territorio,geometria,espacio,usuarios,guardadas);
 }
 @Test void propietarioAjenoNoPuedeLeerNiQuitarHistorial(){
   when(historial.findByIdAndUsuarioEmail(4L,"bea")).thenReturn(Optional.empty());
   assertThatThrownBy(()->service.informeHistorico("bea",4L)).isInstanceOf(OperacionInvalidaException.class);
   assertThatThrownBy(()->service.quitarHistorial("bea",4L)).isInstanceOf(OperacionInvalidaException.class);
   verify(historial,never()).delete(any());verifyNoInteractions(territorio,espacio);
 }
 @Test void compararExigeDosOTresIdsDistintos(){
   for(var ids:List.of(List.of(1L),List.of(1L,1L),List.of(1L,2L,3L,4L)))
      assertThatThrownBy(()->service.comparar("ana",ids)).isInstanceOf(OperacionInvalidaException.class);
   verifyNoInteractions(guardadas,territorio,geometria,espacio);
 }
 @Test void compararValidaTodaLaPropiedadAntesDeConsultarFuentes(){
   when(guardadas.findByIdAndUsuarioEmail(1L,"ana")).thenReturn(Optional.of(mock(ConsultaGuardada.class)));
   when(guardadas.findByIdAndUsuarioEmail(2L,"ana")).thenReturn(Optional.empty());
   assertThatThrownBy(()->service.comparar("ana",List.of(1L,2L))).isInstanceOf(OperacionInvalidaException.class);
   verifyNoInteractions(territorio,geometria,espacio);
 }
 @Test void guardadaConPuntoDesactualizadoNoGeneraUnInformeDeOtraParcela(){
   var g=new ConsultaGuardada(usuario,"real.1","060406");g.actualizar(-33,-68.8,null);
   when(guardadas.findByIdAndUsuarioEmail(1L,"ana")).thenReturn(Optional.of(g));
   assertThatThrownBy(()->service.informeGuardada("ana",1L)).isInstanceOf(OperacionInvalidaException.class);
   verify(territorio,never()).consultar(anyDouble(),anyDouble());
 }
 @Test void consultaFallidaNoGeneraHistorial(){
   when(usuarios.findByEmail("ana")).thenReturn(Optional.of(usuario));
   when(territorio.consultar(-33,-68.8)).thenThrow(new OperacionInvalidaException(503,"CAPA_NO_IMPORTADA","No disponible"));
   assertThatThrownBy(()->service.consultar("ana",-33,-68.8)).isInstanceOf(OperacionInvalidaException.class);
   verifyNoInteractions(historial);
 }
 @Test void consultaSinParcelaSeConservaConSuResultadoReal(){
   var c=new Consulta(-33,-68.8,List.of(),false,false,new Electricidad(null,null,1000,"Sin tramo"),List.of(),"Sin conexión acreditada");
   when(usuarios.findByEmail("ana")).thenReturn(Optional.of(usuario));when(territorio.consultar(-33,-68.8)).thenReturn(c);
   assertThat(service.consultar("ana",-33,-68.8)).isEqualTo(c);
   var captor=org.mockito.ArgumentCaptor.forClass(ConsultaHistorial.class);verify(historial).save(captor.capture());
   assertThat(captor.getValue().getResumen()).isEqualTo("Sin parcela identificada");
   assertThat(mapper.readValue(captor.getValue().getSnapshot(),Informe.class).consulta()).isEqualTo(c);
 }
}
