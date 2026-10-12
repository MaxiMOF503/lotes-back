package ies.belgrano.lotes.service;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.repository.*;
import ies.belgrano.lotes.dto.request.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Set;
@Service
@Transactional
public class LoteAdminService {
    private static final int TAMANIO_MAXIMO=100;
    private static final Set<String> CAMPOS_ORDENABLES=Set.of("identificador","direccionAproximada");
    private final LoteRepository lotes; private final DepartamentoRepository departamentos;
    public LoteAdminService(LoteRepository lotes,DepartamentoRepository departamentos) {this.lotes=lotes;this.departamentos=departamentos;}
    public record Detalle(Long id,LoteAdminRequest datos) {}
    public record Resumen(Long id,String identificador,String direccionAproximada) {}
    public record Pagina(java.util.List<Resumen> items,int pagina,int totalPaginas,long total) {}
    @Transactional(readOnly=true) public Pagina listar(int pagina,int tamanio,String identificador,String direccion,Long departamentoId,String ordenarPor,String direccionOrden) {
        if(pagina<0) throw new OperacionInvalidaException(400,"DATOS_INVALIDOS","La página no puede ser negativa");
        if(tamanio<1 || tamanio>TAMANIO_MAXIMO) throw new OperacionInvalidaException(400,"DATOS_INVALIDOS","El tamaño de página debe estar entre 1 y "+TAMANIO_MAXIMO);
        if(!CAMPOS_ORDENABLES.contains(ordenarPor)) throw new OperacionInvalidaException(400,"DATOS_INVALIDOS","El campo de ordenamiento no es válido");
        var direccionOrdenamiento=direccionOrdenamiento(direccionOrden);
        var orden=Sort.by(direccionOrdenamiento,ordenarPor).and(Sort.by(Sort.Direction.ASC,"id"));
        var filtro=filtro(identificador,direccion,departamentoId);
        var result=lotes.findAll(filtro,PageRequest.of(pagina,tamanio,orden));
        return new Pagina(result.map(l -> new Resumen(l.getId(),l.getIdentificador(),l.getDireccionAproximada())).getContent(),pagina,result.getTotalPages(),result.getTotalElements());
    }
    private Sort.Direction direccionOrdenamiento(String direccionOrden) {
        if("ASC".equalsIgnoreCase(direccionOrden)) return Sort.Direction.ASC;
        if("DESC".equalsIgnoreCase(direccionOrden)) return Sort.Direction.DESC;
        throw new OperacionInvalidaException(400,"DATOS_INVALIDOS","La dirección de ordenamiento debe ser ASC o DESC");
    }
    private Specification<LoteEntity> filtro(String identificador,String direccion,Long departamentoId) {
        Specification<LoteEntity> filtro=(raiz,consulta,cb) -> cb.isFalse(raiz.get("eliminado"));
        if(identificador!=null && !identificador.isBlank()) {
            var valor="%"+identificador.trim().toLowerCase()+"%";
            filtro=filtro.and((raiz,consulta,cb) -> cb.like(cb.lower(raiz.get("identificador")),valor));
        }
        if(direccion!=null && !direccion.isBlank()) {
            var valor="%"+direccion.trim().toLowerCase()+"%";
            filtro=filtro.and((raiz,consulta,cb) -> cb.like(cb.lower(raiz.get("direccionAproximada")),valor));
        }
        if(departamentoId!=null) {
            filtro=filtro.and((raiz,consulta,cb) -> cb.equal(raiz.get("departamento").get("id"),departamentoId));
        }
        return filtro;
    }
    public Detalle crear(LoteAdminRequest r) {verificarDuplicado(r,null);return detalle(lotes.saveAndFlush(LoteEntity.crear(r,departamento(r))));}
    public Detalle editar(Long id,LoteAdminRequest r) {var lote=buscar(id);verificarDuplicado(r,id);lote.actualizar(r,departamento(r));return detalle(lotes.saveAndFlush(lote));}
    public void eliminar(Long id) {var lote=buscar(id);lote.eliminar();lotes.saveAndFlush(lote);}
    @Transactional(readOnly=true) public Detalle obtener(Long id) {return detalle(buscar(id));}
    private LoteEntity buscar(Long id) {return lotes.findByIdAndEliminadoFalse(id).orElseThrow(() -> new OperacionInvalidaException(404,"LOTE_NO_ENCONTRADO","No existe el lote indicado"));}
    private DepartamentoEntity departamento(LoteAdminRequest r) {
        return r.departamentoId()==null ? null : departamentos.findById(r.departamentoId()).orElseThrow(() -> new OperacionInvalidaException(400,"DEPARTAMENTO_INVALIDO","El departamento no existe"));
    }
    private void verificarDuplicado(LoteAdminRequest r,Long id) {
        lotes.findByIdentificador(r.identificador().trim()).filter(l -> !l.getId().equals(id)).ifPresent(l -> {throw new OperacionInvalidaException(409,"IDENTIFICADOR_DUPLICADO","Ya existe un lote con ese identificador");});
    }
    private ProcedenciaRequest procedencia(ProcedenciaDatos p) {return p==null ? new ProcedenciaRequest(ProcedenciaRequest.Tipo.SIMULADO,"Dataset demostrativo B-01",null) : p.request();}
    private Detalle detalle(LoteEntity l) {return new Detalle(l.getId(),new LoteAdminRequest(l.getIdentificador(),BigDecimal.valueOf(l.getUbicacion().getY()),BigDecimal.valueOf(l.getUbicacion().getX()),
        l.getDepartamento()==null ? null : l.getDepartamento().getId(),l.getDireccionAproximada(),l.getZonificacion(),l.isZonificacionVerificada(),l.getDistanciaRedElectricaMts(),l.isTieneAccesoElectricidad(),l.isTieneCoberturaAgua(),
        procedencia(l.getProcedenciaLote()),procedencia(l.getProcedenciaZonificacion()),procedencia(l.getProcedenciaElectricidad()),procedencia(l.getProcedenciaAgua())));}
}
