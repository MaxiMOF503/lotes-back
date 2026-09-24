package ies.belgrano.lotes.controller;
import ies.belgrano.lotes.service.LoteAdminService;
import ies.belgrano.lotes.dto.request.LoteAdminRequest;
import ies.belgrano.lotes.repository.DepartamentoRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/api/admin")
public class AdminLoteController {
    private final LoteAdminService service; private final DepartamentoRepository departamentos;
    public AdminLoteController(LoteAdminService service,DepartamentoRepository departamentos) {this.service=service;this.departamentos=departamentos;}
    @GetMapping("/lotes") public LoteAdminService.Pagina listar(@RequestParam(defaultValue="0") int pagina) {return service.listar(pagina);}
    @GetMapping("/lotes/{id}") public LoteAdminService.Detalle obtener(@PathVariable Long id) {return service.obtener(id);}
    @PostMapping("/lotes") public ResponseEntity<LoteAdminService.Detalle> crear(@Valid @RequestBody LoteAdminRequest r) {
        var lote=service.crear(r);return ResponseEntity.created(java.net.URI.create("/api/admin/lotes/"+lote.id())).body(lote);
    }
    @PutMapping("/lotes/{id}") public LoteAdminService.Detalle editar(@PathVariable Long id,@Valid @RequestBody LoteAdminRequest r) {return service.editar(id,r);}
    public record Departamento(Long id,String nombre) {}
    @GetMapping("/departamentos") public java.util.List<Departamento> departamentos() {
        return departamentos.findAll(org.springframework.data.domain.Sort.by("nombre")).stream().map(d -> new Departamento(d.getId(),d.getNombre())).toList();
    }
}
