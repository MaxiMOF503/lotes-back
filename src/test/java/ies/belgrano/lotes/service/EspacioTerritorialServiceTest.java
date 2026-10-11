package ies.belgrano.lotes.service;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.repository.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.dto.request.EspacioTerritorialRequest.*;
import ies.belgrano.lotes.dto.response.TerritorioResponse.Feature;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EspacioTerritorialServiceTest {
 ConsultaGuardadaRepository guardadas=mock(ConsultaGuardadaRepository.class);
 FichaParcelaRepository fichas=mock(FichaParcelaRepository.class);
 UsuarioRepository usuarios=mock(UsuarioRepository.class);
 TerritorioService territorio=mock(TerritorioService.class);
 TerritorioRepository geo=mock(TerritorioRepository.class);
 EspacioTerritorialService service=new EspacioTerritorialService(guardadas,fichas,usuarios,territorio,geo);
 @BeforeEach void setup(){
   when(territorio.parcela("123")).thenReturn(new Feature("Feature","123",null,
     new ObjectMapper().readTree("{\"nomenclatu\":\"060406000300002300007\"}")));
 }
 @Test void noGuardaPuntoFueraDeLaParcelaNiInventado(){
   assertThatThrownBy(()->service.guardar("ana","123",new Guardar(-33.0,-68.0,null)))
      .isInstanceOf(OperacionInvalidaException.class).hasMessageContaining("no pertenece");
   verifyNoInteractions(guardadas,usuarios);
 }
 @Test void rechazaCoordenadasNoFinitasAntesDeConsultarGeometria(){
   assertThatThrownBy(()->service.guardar("ana","123",new Guardar(Double.NaN,-68.0,null)))
      .isInstanceOf(OperacionInvalidaException.class);
   verifyNoInteractions(territorio,geo,guardadas);
 }
 @Test void repetirGuardarActualizaElMismoRegistroSinCambiarLaFechaOriginal(){
   var usuario=new UsuarioEntity("Ana","ana","hash",new RolEntity(RolEntity.NombreRol.USUARIO));
   var previa=new ConsultaGuardada(usuario,"123","Nomenclatura");
   var fecha=previa.getCreadaEn();
   when(geo.pertenece(eq("123"),anyString())).thenReturn(true);
   when(usuarios.findByEmail("ana")).thenReturn(Optional.of(usuario));
   when(guardadas.findByUsuarioEmailAndParcelaId("ana","123")).thenReturn(Optional.of(previa));
   when(guardadas.saveAndFlush(previa)).thenReturn(previa);
   var result=service.guardar("ana","123",new Guardar(-33.0688,-68.8929," Mi consulta "));
   assertThat(result.etiqueta()).isEqualTo("Mi consulta");
   assertThat(result.creadaEn()).isEqualTo(fecha);
   verify(guardadas).saveAndFlush(previa);
 }
 @Test void noPuedeQuitarGuardadaDeOtraCuenta(){
   when(guardadas.findByIdAndUsuarioEmail(9L,"ana")).thenReturn(Optional.empty());
   assertThatThrownBy(()->service.quitar("ana",9L)).isInstanceOf(OperacionInvalidaException.class);
   verify(guardadas,never()).delete(any());
 }
 @Test void conflictoDeVersionNoSobreescribeFicha(){
   var existente=new FichaParcela("123");
   when(fichas.findById("123")).thenReturn(Optional.of(existente));
   assertThatThrownBy(()->service.guardarFicha("admin","123",new Ficha("Dir","Obs","Fuente","Ref",4L)))
      .isInstanceOf(OperacionInvalidaException.class).hasMessageContaining("Otra persona");
   verify(fichas,never()).saveAndFlush(any());
 }
 @Test void fuenteOriginalNuncaSeEscribeCuandoSeCompletaFicha(){
   when(fichas.findById("123")).thenReturn(Optional.empty());
   when(fichas.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
   var result=service.guardarFicha("admin","123",new Ficha(" Calle real "," Observación "," Fuente "," Acta ",null));
   assertThat(result.actualizadoPor()).isEqualTo("admin");
   assertThat(result.direccion()).isEqualTo("Calle real");
   verifyNoInteractions(guardadas,usuarios);
   verify(geo).parcela("123");
 }
}
