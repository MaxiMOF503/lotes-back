package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.response.CalidadCatastroResponse.*;
import ies.belgrano.lotes.service.CalidadCatastroService;
import org.springframework.web.bind.annotation.*;

/** SecurityConfig ya restringe /api/admin/** a ADMIN. No amplía la API pública. */
@RestController
@RequestMapping("/api/admin/territorio/calidad")
public class CalidadCatastroController {
    private final CalidadCatastroService service;
    public CalidadCatastroController(CalidadCatastroService service){this.service=service;}
    @GetMapping public Resumen resumen(){return service.resumen();}
    @GetMapping("/coincidencias") public PaginaGrupos grupos(@RequestParam(defaultValue="true") boolean soloConflictos,
            @RequestParam(defaultValue="0") int pagina){return service.grupos(soloConflictos,pagina);}
    @GetMapping("/parcelas/{id}") public Grupo grupo(@PathVariable String id){return service.grupo(id);}
}
