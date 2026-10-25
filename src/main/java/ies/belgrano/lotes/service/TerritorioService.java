package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.TerritorioResponse.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.TerritorioRepository;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TerritorioService {
    private static final Set<String> CAPAS_BASE = Set.of("catastro", "aysam", "electricidad");
    /** Aguas Luján es opcional: una base importada antes de esta capa sigue consultando sin ella. */
    private static final Set<String> CAPAS = Set.of("catastro", "aysam", "aguas_lujan", "electricidad");
    private final TerritorioRepository repository;
    public TerritorioService(TerritorioRepository repository) { this.repository = repository; }

    public List<Fuente> fuentes() {
        try { return repository.fuentes(); }
        catch (BadSqlGrammarException error) {
            if (error.getSQLException() != null && error.getSQLException().getErrorCode() == 1146)
                throw new OperacionInvalidaException(503, "TERRITORIO_NO_IMPORTADO",
                    "Las capas territoriales todavía no fueron importadas en esta base.");
            throw error;
        }
    }

    public Capa capa(String capa, double oeste, double sur, double este, double norte, int zoom, int limite) {
        if (!CAPAS.contains(capa)) throw invalid("La capa debe ser catastro, aysam, aguas_lujan o electricidad.");
        validarPunto(sur, oeste); validarPunto(norte, este);
        if (oeste >= este || sur >= norte) throw invalid("Los límites de la ventana están invertidos.");
        if (zoom < 0 || zoom > 20 || limite < 1 || limite > 1000)
            throw invalid("El zoom debe estar entre 0 y 20 y el límite entre 1 y 1000.");
        Fuente fuente = fuente(capa, fuentes());
        int zoomMinimo = capa.equals("catastro") ? 15 : capa.equals("electricidad") ? 12 : 0;
        double anchoMaximo = capa.equals("catastro") ? 0.12 : capa.equals("electricidad") ? 0.8 : 10;
        if (zoom < zoomMinimo || este - oeste > anchoMaximo || norte - sur > anchoMaximo)
            return new Capa("FeatureCollection", List.of(), fuente, false, true,
                "Acercá el mapa para ver " + (capa.equals("catastro") ? "las parcelas." : "esta capa."));
        List<Feature> features = repository.ventana(capa, bounds(oeste, sur, este, norte), limite + 1);
        boolean hayMas = features.size() > limite;
        return new Capa("FeatureCollection", features.stream().limit(limite).toList(), fuente, hayMas, false,
            hayMas ? "Hay más elementos en esta zona. Acercá el mapa para verlos." : "Datos del archivo de origen importado.");
    }

    public Consulta consultar(double latitud, double longitud) {
        validarPunto(latitud, longitud);
        List<Fuente> fuentes = fuentes();
        CAPAS_BASE.forEach(capa -> fuente(capa, fuentes));
        String punto = "POINT(" + longitud + " " + latitud + ")";
        List<Feature> parcelas = repository.parcelas(punto, 6);
        int radio = 1000;
        // Ventana conservadora para 1 km; la distancia final se calcula sobre WGS84.
        double margenLat = 0.011;
        double margenLon = margenLat / Math.max(0.01, Math.cos(Math.toRadians(latitud)));
        String bounds = bounds(Math.max(-179.999999, longitud - margenLon),
            Math.max(-89.999999, latitud - margenLat),
            Math.min(179.999999, longitud + margenLon), Math.min(89.999999, latitud + margenLat));
        return new Consulta(latitud, longitud, parcelas.stream().limit(5).toList(), parcelas.size() > 5,
            repository.dentroRadio("aysam", punto),
            fuentes.stream().anyMatch(f -> f.capa().equals("aguas_lujan")) ? repository.dentroRadio("aguas_lujan", punto) : null,
            repository.electricidad(punto, bounds, radio), fuentes,
            "Datos publicados por las fuentes indicadas, correspondientes a los archivos importados. "
            + "Los radios de servicio de agua y la proximidad eléctrica no confirman una conexión. "
            + "La parcela no informa titularidad ni dominio. Verificá vigencia y factibilidad ante cada organismo.");
    }

    public record BusquedaParcelas(List<Feature> items,int pagina,boolean hayMas) {}
    public BusquedaParcelas buscarParcelas(String nomenclatura,int pagina) {
        String prefijo=nomenclatura==null?"":nomenclatura.trim();
        if(!prefijo.matches("[0-9]{3,30}") || pagina<0 || pagina>5000)
            throw invalid("Ingresá entre 3 y 30 dígitos de nomenclatura y una página válida.");
        fuente("catastro",fuentes());
        var rows=repository.buscarParcelas(prefijo,21,pagina*20);
        return new BusquedaParcelas(rows.stream().limit(20).toList(),pagina,rows.size()>20);
    }
    /** Parcelas alrededor de un punto aproximado (por ejemplo, el que devuelve Georef sobre el eje de la calle). */
    public Cercanas cercanas(double latitud, double longitud, int radio) {
        validarPunto(latitud, longitud);
        if (radio < 5 || radio > 100) throw invalid("El radio debe estar entre 5 y 100 metros.");
        fuente("catastro", fuentes());
        // Ventana un poco mayor que el radio para usar el índice espacial; la distancia final es en metros sobre WGS84.
        double margenLat = radio * 1.2 / 111_320.0;
        double margenLon = margenLat / Math.max(0.01, Math.cos(Math.toRadians(latitud)));
        String bounds = bounds(longitud - margenLon, latitud - margenLat, longitud + margenLon, latitud + margenLat);
        return new Cercanas(latitud, longitud, radio,
            repository.cercanas("POINT(" + longitud + " " + latitud + ")", bounds, radio, 10));
    }
    public Feature parcela(String id) {
        if(id==null || !id.matches("[A-Za-z0-9._-]{1,100}"))throw invalid("Identificador de parcela inválido.");
        fuente("catastro",fuentes());
        return repository.parcela(id).orElseThrow(()->new OperacionInvalidaException(404,
            "PARCELA_NO_ENCONTRADA","La parcela no tiene geometría válida en la importación actual."));
    }

    private Fuente fuente(String capa, List<Fuente> fuentes) {
        return fuentes.stream().filter(f -> f.capa().equals(capa)).findFirst().orElseThrow(() ->
            new OperacionInvalidaException(503, "CAPA_NO_IMPORTADA", "La capa " + capa + " no fue importada."));
    }

    static void validarPunto(double latitud, double longitud) {
        if (!Double.isFinite(latitud) || !Double.isFinite(longitud) || latitud <= -90 || latitud >= 90
            || longitud <= -180 || longitud >= 180) throw invalid("Coordenadas WGS84 inválidas.");
    }
    private static String bounds(double oeste, double sur, double este, double norte) {
        return "POLYGON((" + oeste + " " + sur + "," + este + " " + sur + "," + este + " " + norte
            + "," + oeste + " " + norte + "," + oeste + " " + sur + "))";
    }
    private static OperacionInvalidaException invalid(String mensaje) {
        return new OperacionInvalidaException(400, "CONSULTA_TERRITORIAL_INVALIDA", mensaje);
    }
}
