package ies.belgrano.lotes.controller;

import ies.belgrano.lotes.dto.request.ReporteDatoRequest.*;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.Pagina;
import ies.belgrano.lotes.dto.response.ReporteDatoResponse;
import ies.belgrano.lotes.entity.ReporteDato;
import ies.belgrano.lotes.service.ReporteDatoService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReporteDatoController {
    private final ReporteDatoService service;

    public ReporteDatoController(ReporteDatoService service) { this.service = service; }

    @PostMapping("/api/mi-espacio/reportes")
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteDatoResponse crear(Principal usuario, @Valid @RequestBody Crear solicitud) {
        return service.crear(usuario.getName(), solicitud);
    }

    @GetMapping("/api/mi-espacio/reportes")
    public Pagina<ReporteDatoResponse> propios(Principal usuario,
            @RequestParam(defaultValue = "0") int pagina) {
        return service.propios(usuario.getName(), pagina);
    }

    @GetMapping("/api/admin/reportes")
    public Pagina<ReporteDatoResponse> administrar(
            @RequestParam(required = false) ReporteDato.Estado estado,
            @RequestParam(defaultValue = "0") int pagina) {
        return service.administrar(estado, pagina);
    }

    @PatchMapping("/api/admin/reportes/{id}")
    public ReporteDatoResponse actualizar(Principal administrador, @PathVariable Long id,
            @Valid @RequestBody Actualizar solicitud) {
        return service.actualizar(administrador.getName(), id, solicitud);
    }
}
