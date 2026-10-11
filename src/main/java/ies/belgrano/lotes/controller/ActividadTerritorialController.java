package ies.belgrano.lotes.controller;
import ies.belgrano.lotes.service.ActividadTerritorialService;
import ies.belgrano.lotes.dto.request.ConsultaPersonalRequest;
import ies.belgrano.lotes.dto.response.ActividadTerritorialResponse.*;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.Pagina;
import ies.belgrano.lotes.dto.response.TerritorioResponse.Consulta;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController @RequestMapping("/api/mi-espacio")
public class ActividadTerritorialController {
    private final ActividadTerritorialService service;
    public ActividadTerritorialController(ActividadTerritorialService service){this.service=service;}
    @PostMapping("/consultas")
    public Consulta consultar(Principal usuario,@Valid @RequestBody ConsultaPersonalRequest r){return service.consultar(usuario.getName(),r.latitud(),r.longitud());}
    @GetMapping("/historial")
    public Pagina<Historial> historial(Principal usuario,@RequestParam(defaultValue="0") int pagina){return service.historial(usuario.getName(),pagina);}
    @GetMapping("/historial/{id}/informe")
    public Informe historico(Principal usuario,@PathVariable Long id){return service.informeHistorico(usuario.getName(),id);}
    @DeleteMapping("/historial/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(Principal usuario,@PathVariable Long id){service.quitarHistorial(usuario.getName(),id);}
    @GetMapping("/guardadas/{id}/informe")
    public Informe informe(Principal usuario,@PathVariable Long id){return service.informeGuardada(usuario.getName(),id);}
    @GetMapping("/comparacion")
    public List<Informe> comparar(Principal usuario,@RequestParam List<Long> ids){return service.comparar(usuario.getName(),ids);}
}
