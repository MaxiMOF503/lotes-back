package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.response.TerritorioResponse.*;
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
    public TerritorioController(TerritorioService service) { this.service = service; }

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
            + "Agua: pertenencia al radio general, sin confirmar acometida.")
    public Consulta consultar(@RequestParam double latitud, @RequestParam double longitud) {
        return service.consultar(latitud, longitud);
    }
}
