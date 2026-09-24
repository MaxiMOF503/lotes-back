package ies.belgrano.lotes.controller;
import ies.belgrano.lotes.service.EstadisticaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
@RestController
public class EstadisticaController {
    private final EstadisticaService service;
    public EstadisticaController(EstadisticaService service) {this.service=service;}
    @PostMapping("/api/public/v1/visitas") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void visita() {service.registrar("VISITA","NINGUNO","EXITO");}
    @GetMapping("/api/admin/estadisticas")
    public EstadisticaService.Resumen estadisticas(@RequestParam LocalDate desde,@RequestParam LocalDate hasta) {return service.consultar(desde,hasta);}
}
