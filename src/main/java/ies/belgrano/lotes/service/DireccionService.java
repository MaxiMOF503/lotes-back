package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.DireccionResponse.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.FichaParcelaRepository;
import ies.belgrano.lotes.repository.TerritorioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Búsqueda por dirección con datos oficiales: Georef (Gobierno nacional) normaliza calle y número
 * o esquina y devuelve un punto aproximado. Se suman las direcciones que el equipo relevó en las fichas.
 * El texto buscado no se guarda ni se escribe en el log.
 */
@Service
@Transactional(readOnly = true)
public class DireccionService {
    static final String FUENTE = "Georef · Servicio de Normalización de Datos Geográficos de Argentina";
    static final String ADVERTENCIA = "Ubicación aproximada según Georef: se calcula sobre el eje de la calle. "
        + "Elegí en el mapa la parcela que corresponde; la numeración no indica de qué lado de la calle está.";
    private final RestClient georef;
    private final FichaParcelaRepository fichas;
    private final TerritorioRepository territorio;
    private final ObjectMapper mapper;

    public DireccionService(RestClient georef, FichaParcelaRepository fichas, TerritorioRepository territorio,
                            ObjectMapper mapper) {
        this.georef = georef;
        this.fichas = fichas;
        this.territorio = territorio;
        this.mapper = mapper;
    }

    public Direcciones buscar(String texto) {
        String direccion = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
        if (direccion.length() < 3 || direccion.length() > 150)
            throw new OperacionInvalidaException(400, "DIRECCION_INVALIDA",
                "Escribí entre 3 y 150 caracteres: calle y número, o calle esquina calle.");
        List<Direccion> resultados = new ArrayList<>(relevadas(direccion));
        try {
            resultados.addAll(oficiales(direccion));
        } catch (RestClientException | JacksonException error) {
            // Sin la causa: su mensaje incluye la URL con el texto buscado.
            if (resultados.isEmpty())
                throw new OperacionInvalidaException(503, "DIRECCIONES_NO_DISPONIBLES",
                    "El servicio oficial de direcciones no respondió. Probá en unos minutos o buscá por nomenclatura.");
            return new Direcciones(resultados, FUENTE, "El servicio oficial de direcciones no respondió; "
                + "se muestran solo las direcciones relevadas por el equipo.");
        }
        return new Direcciones(resultados, FUENTE, ADVERTENCIA);
    }

    private List<Direccion> relevadas(String direccion) {
        return fichas.findTop5ByDireccionContainingIgnoreCaseOrderByDireccion(direccion).stream()
            .flatMap(f -> territorio.parcela(f.getParcelaId()).stream().map(p -> new Direccion(Origen.FICHA,
                f.getDireccion(), null, null, null, null, null, null, f.getParcelaId(),
                p.properties().path("nomenclatu").asString(f.getParcelaId()))))
            .toList();
    }

    private List<Direccion> oficiales(String direccion) {
        String cuerpo = georef.get()
            .uri(uri -> uri.path("/direcciones")
                .queryParam("direccion", direccion)
                .queryParam("provincia", "Mendoza")
                .queryParam("departamento", "Lujan de Cuyo")
                .queryParam("max", 5)
                .build())
            .retrieve()
            .body(String.class);
        List<Direccion> resultados = new ArrayList<>();
        for (JsonNode d : mapper.readTree(cuerpo == null ? "{}" : cuerpo).path("direcciones")) {
            JsonNode ubicacion = d.path("ubicacion");
            // Sin coordenadas no se puede ubicar en el mapa ni buscar parcelas cercanas.
            if (!ubicacion.path("lat").isNumber() || !ubicacion.path("lon").isNumber()) continue;
            JsonNode altura = d.path("altura").path("valor");
            resultados.add(new Direccion(Origen.GEOREF, texto(d.path("nomenclatura")),
                texto(d.path("calle").path("nombre")), altura.isNumber() ? altura.asInt() : null,
                texto(d.path("calle_cruce_1").path("nombre")), texto(d.path("localidad_censal").path("nombre")),
                ubicacion.path("lat").asDouble(), ubicacion.path("lon").asDouble(), null, null));
        }
        return resultados;
    }

    private static String texto(JsonNode valor) {
        return valor.isMissingNode() || valor.isNull() ? null : valor.asString();
    }
}
