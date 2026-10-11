package ies.belgrano.lotes.dto.response;
import java.time.Instant;
import java.util.List;

public final class EspacioTerritorialResponse {
    private EspacioTerritorialResponse(){}
    public record Pagina<T>(List<T> items,int pagina,int totalPaginas,long total){}
    public record Guardada(Long id,String parcelaId,String nomenclatura,String etiqueta,
                           double latitud,double longitud,Instant creadaEn){}
    public record Ficha(String parcelaId,String nomenclatura,String direccion,String observaciones,String fuente,String referencia,
                        String actualizadoPor,Instant actualizadoEn,Long version){}
    public record FichaPublica(String direccion,String observaciones,String fuente,String referencia,Instant actualizadoEn){}
}
