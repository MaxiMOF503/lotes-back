package ies.belgrano.lotes.controller;
import ies.belgrano.lotes.dto.request.EspacioTerritorialRequest.*;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse;
import ies.belgrano.lotes.service.EspacioTerritorialService;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class EspacioTerritorialController {
    private final EspacioTerritorialService service;
    public EspacioTerritorialController(EspacioTerritorialService service){this.service=service;}
    @GetMapping("/api/mi-espacio/guardadas")
    public EspacioTerritorialResponse.Pagina<EspacioTerritorialResponse.Guardada> guardadas(Principal usuario,@RequestParam(defaultValue="0") int pagina){
        return service.guardadas(usuario.getName(),pagina);
    }
    @PutMapping("/api/mi-espacio/guardadas/{parcelaId}")
    public EspacioTerritorialResponse.Guardada guardar(Principal usuario,@PathVariable String parcelaId,@Valid @RequestBody Guardar r){
        return service.guardar(usuario.getName(),parcelaId,r);
    }
    @DeleteMapping("/api/mi-espacio/guardadas/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void quitar(Principal usuario,@PathVariable Long id){service.quitar(usuario.getName(),id);}
    @GetMapping("/api/admin/parcelas/fichas")
    public EspacioTerritorialResponse.Pagina<EspacioTerritorialResponse.Ficha> fichas(@RequestParam(defaultValue="0") int pagina){return service.fichas(pagina);}
    @GetMapping("/api/admin/parcelas/{parcelaId}/ficha")
    public EspacioTerritorialResponse.Ficha ficha(@PathVariable String parcelaId){return service.ficha(parcelaId);}
    @PutMapping("/api/admin/parcelas/{parcelaId}/ficha")
    public EspacioTerritorialResponse.Ficha guardarFicha(Principal usuario,@PathVariable String parcelaId,@Valid @RequestBody Ficha r){
        return service.guardarFicha(usuario.getName(),parcelaId,r);
    }
    @GetMapping("/api/public/v1/territorio/parcelas/{parcelaId}/ficha")
    public EspacioTerritorialResponse.FichaPublica publica(@PathVariable String parcelaId){return service.publica(parcelaId);}
}
