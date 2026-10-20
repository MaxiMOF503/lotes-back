package ies.belgrano.lotes.dto.response;

import java.util.List;

public final class DireccionResponse {
    private DireccionResponse() {}

    /** GEOREF: dirección normalizada con ubicación aproximada. FICHA: dirección relevada por el equipo para una parcela. */
    public enum Origen { GEOREF, FICHA }

    public record Direccion(Origen origen, String texto, String calle, Integer altura, String cruce,
                            String localidad, Double latitud, Double longitud,
                            String parcelaId, String nomenclatura) {}

    public record Direcciones(List<Direccion> resultados, String fuente, String advertencia) {}
}
