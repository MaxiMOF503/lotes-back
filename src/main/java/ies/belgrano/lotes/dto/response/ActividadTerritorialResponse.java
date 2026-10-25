package ies.belgrano.lotes.dto.response;
import java.time.Instant;
import java.util.List;
public final class ActividadTerritorialResponse {
    private ActividadTerritorialResponse() {}
    public record Aporte(String parcelaId,EspacioTerritorialResponse.FichaPublica ficha) {}
    public record Informe(Instant generadoEn,Instant fechaConsulta,boolean historico,String parcelaId,
                          TerritorioResponse.Consulta consulta,List<Aporte> aportes,String parcelaIdOriginal) {
        public Informe { if(parcelaIdOriginal==null)parcelaIdOriginal=parcelaId; }
        public Informe(Instant generadoEn,Instant fechaConsulta,boolean historico,String parcelaId,
                       TerritorioResponse.Consulta consulta,List<Aporte> aportes) {
            this(generadoEn,fechaConsulta,historico,parcelaId,consulta,aportes,parcelaId);
        }
    }
    public record Historial(Long id,double latitud,double longitud,Instant consultadaEn,String resumen) {}
}
