package ies.belgrano.lotes.dto.response;
import java.time.Instant;
import java.util.List;

public final class EspacioTerritorialResponse {
    private EspacioTerritorialResponse(){}
    public record Pagina<T>(List<T> items,int pagina,int totalPaginas,long total){}
    public record Guardada(Long id,String parcelaId,String nomenclatura,String etiqueta,
                           double latitud,double longitud,Instant creadaEn){}
    public record Ficha(String parcelaId,String nomenclatura,String direccion,String observaciones,String fuente,String referencia,
                        String actualizadoPor,Instant actualizadoEn,Long version,String idCanonico,List<Ficha> aportesRelacionados){
        public Ficha(String parcelaId,String nomenclatura,String direccion,String observaciones,String fuente,String referencia,
                     String actualizadoPor,Instant actualizadoEn,Long version) {
            this(parcelaId,nomenclatura,direccion,observaciones,fuente,referencia,actualizadoPor,actualizadoEn,version,parcelaId,List.of());
        }
    }
    public record AporteRelacionado(String parcelaId,String direccion,String observaciones,String fuente,
                                   String referencia,Instant actualizadoEn){}
    public record FichaPublica(String direccion,String observaciones,String fuente,String referencia,Instant actualizadoEn,
                               String idCanonico,Boolean multiplesAportes,List<AporteRelacionado> aportesRelacionados){
        public FichaPublica {
            multiplesAportes = Boolean.TRUE.equals(multiplesAportes);
            aportesRelacionados = aportesRelacionados == null ? List.of() : List.copyOf(aportesRelacionados);
        }
        public FichaPublica(String direccion,String observaciones,String fuente,String referencia,Instant actualizadoEn) {
            this(direccion,observaciones,fuente,referencia,actualizadoEn,null,false,List.of());
        }
    }
}
