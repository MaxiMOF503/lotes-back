package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.response.TerritorioResponse.*;
import ies.belgrano.lotes.dto.response.DireccionResponse.Direcciones;
import ies.belgrano.lotes.service.DireccionService;
import ies.belgrano.lotes.service.TerritorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public/v1/territorio")
@Tag(name = "Territorio real", description = "Archivos publicados de catastro, electricidad y radio de AYSAM")
public class TerritorioController {
    private final TerritorioService service;
    private final DireccionService direcciones;
    public TerritorioController(TerritorioService service, DireccionService direcciones) {
        this.service = service;
        this.direcciones = direcciones;
    }

    @GetMapping("/fuentes")
    public List<Fuente> fuentes() { return service.fuentes(); }

    @GetMapping("/capas/{capa}")
    @Operation(summary = "GeoJSON de la zona visible, hasta 1000 elementos",
        description = "Oeste/este son longitudes; sur/norte son latitudes WGS84. "
            + "Catastro requiere zoom 15; electricidad zoom 12. hayMas indica recorte.")
    public Capa capa(@PathVariable String capa, @RequestParam double oeste, @RequestParam double sur,
                     @RequestParam double este, @RequestParam double norte,
                     @RequestParam(defaultValue = "15") int zoom,
                     @RequestParam(defaultValue = "500") int limite) {
        return service.capa(capa, oeste, sur, este, norte, zoom, limite);
    }

    @GetMapping("/consulta")
    @Operation(summary = "Consultar parcelas y servicios publicados de un punto",
        description = "Incluye coincidencias en límites. Electricidad: tramo más cercano hasta 1000 metros. "
            + "Agua: pertenencia a los radios generales de AYSAM y de Aguas Luján, sin confirmar acometida.")
    public Consulta consultar(@RequestParam double latitud, @RequestParam double longitud) {
        return service.consultar(latitud, longitud);
    }
    @GetMapping("/parcelas")
    public TerritorioService.BusquedaParcelas buscarParcelas(@RequestParam String nomenclatura,
            @RequestParam(defaultValue="0") int pagina) {
        return service.buscarParcelas(nomenclatura,pagina);
    }
    @GetMapping("/direcciones")
    @Operation(summary = "Normalizar una dirección de Luján de Cuyo con Georef",
        description = "Calle y número (\"San Martín 1500\") o esquina (\"San Martín esquina Sáenz Peña\"). "
            + "La ubicación es aproximada, sobre el eje de la calle. Incluye direcciones relevadas en fichas.")
    public Direcciones direcciones(@RequestParam String texto) { return direcciones.buscar(texto); }
    @GetMapping("/parcelas/cercanas")
    @Operation(summary = "Parcelas a menos de un radio de un punto, de la más cercana a la más lejana",
        description = "Radio entre 5 y 100 metros (por defecto 30). Pensado para el punto aproximado de una dirección.")
    public Cercanas cercanas(@RequestParam double latitud, @RequestParam double longitud,
                             @RequestParam(defaultValue = "30") int radio) {
        return service.cercanas(latitud, longitud, radio);
    }
    @GetMapping("/parcelas/{id}")
    public Feature parcela(@PathVariable String id){return service.parcela(id);}
}
