package ies.belgrano.lotes.dto.response;

import tools.jackson.databind.JsonNode;
import java.util.List;

/** Calidad de origen, sin fusionar propiedades ni seleccionar valores verdaderos. */
public final class CalidadCatastroResponse {
    private CalidadCatastroResponse() {}
    public record Resumen(long gruposCoincidentes, long gruposIdenticos, long gruposConConflicto,
                          long copiasIdenticas, long copiasConConflicto, long registrosInvolucrados,
                          long registrosEnGruposConConflicto, long copiasSinClasificar,
                          boolean clasificacionCompleta) {}
    public record Registro(String parcelaId, String duplicadoDe, String tipoDuplicado,
                           List<String> camposConflicto, JsonNode propiedades) {}
    public record GrupoResumen(String representante,long copias,long copiasConConflicto,long copiasSinClasificar) {}
    public record PaginaGrupos(List<GrupoResumen> items,int pagina,boolean hayMas) {}
    public record Grupo(String idSolicitado, String representante, String clasificacion,
                        List<String> camposConflicto, List<Registro> registros) {}
}
