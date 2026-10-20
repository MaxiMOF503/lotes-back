package ies.belgrano.lotes.dto.response;

import java.util.List;
import tools.jackson.databind.JsonNode;

public final class TerritorioResponse {
    private TerritorioResponse() {}

    public record Fuente(String capa, String fuente, String url, int registros,
                         int invalidos, int sinGeometria, String importadoEn) {}
    public record Feature(String type, String id, JsonNode geometry, JsonNode properties) {}
    public record Capa(String type, List<Feature> features, Fuente fuente, boolean hayMas,
                       boolean acercar, String mensaje) {}
    public record Electricidad(Double distanciaMetros, String tipo, int radioBusquedaMetros,
                               String alcance) {}
    /** Parcela a cierta distancia de un punto, en metros sobre WGS84. */
    public record Cercana(Feature parcela, double distanciaMetros) {}
    public record Cercanas(double latitud, double longitud, int radioMetros, List<Cercana> parcelas) {}
    public record Consulta(double latitud, double longitud, List<Feature> parcelas,
                           boolean hayMasParcelas, boolean dentroRadioAgua,
                           Electricidad electricidad, List<Fuente> fuentes, String advertencia) {}
}
