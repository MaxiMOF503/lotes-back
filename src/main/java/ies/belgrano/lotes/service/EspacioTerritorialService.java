package ies.belgrano.lotes.service;
import ies.belgrano.lotes.dto.request.EspacioTerritorialRequest.Guardar;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.*;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service @Transactional
public class EspacioTerritorialService {
    private final ConsultaGuardadaRepository guardadas;
    private final FichaParcelaRepository fichas;
    private final UsuarioRepository usuarios;
    private final TerritorioService territorio;
    private final TerritorioRepository geometria;
    public EspacioTerritorialService(ConsultaGuardadaRepository guardadas,FichaParcelaRepository fichas,
            UsuarioRepository usuarios,TerritorioService territorio,TerritorioRepository geometria){
        this.guardadas=guardadas;this.fichas=fichas;this.usuarios=usuarios;this.territorio=territorio;this.geometria=geometria;
    }
    @Transactional(readOnly=true)
    public Pagina<Guardada> guardadas(String email,int pagina){
        var result=guardadas.findByUsuarioEmail(email,pagina(pagina,"creadaEn"));
        return new Pagina<>(result.map(this::guardada).getContent(),pagina,result.getTotalPages(),result.getTotalElements());
    }
    public Guardada guardar(String email,String parcelaId,Guardar r){
        TerritorioService.validarPunto(r.latitud(),r.longitud());
        var parcela=territorio.parcela(parcelaId);
        String punto="POINT("+r.longitud()+" "+r.latitud()+")";
        if(!geometria.pertenece(parcelaId,punto))
            throw new OperacionInvalidaException(400,"PUNTO_FUERA_PARCELA","El punto no pertenece a la parcela seleccionada.");
        var usuario=usuarios.findByEmail(email).orElseThrow(()->new OperacionInvalidaException(401,"AUTENTICACION_REQUERIDA","Iniciá sesión nuevamente."));
        var registro=guardadas.findByUsuarioEmailAndParcelaId(email,parcelaId).orElseGet(()->
            new ConsultaGuardada(usuario,parcelaId,parcela.properties().path("nomenclatu").asText(parcelaId)));
        registro.actualizar(r.latitud(),r.longitud(),limpiar(r.etiqueta()));
        return guardada(guardadas.saveAndFlush(registro));
    }
    public void quitar(String email,Long id){
        guardadas.delete(guardadas.findByIdAndUsuarioEmail(id,email).orElseThrow(()->
            new OperacionInvalidaException(404,"GUARDADA_NO_ENCONTRADA","La consulta no está entre tus guardadas.")));
    }
    @Transactional(readOnly=true)
    public Ficha ficha(String parcelaId){
        territorio.parcela(parcelaId);
        return fichas.findById(parcelaId).map(this::ficha)
            .orElse(new Ficha(parcelaId,territorio.parcela(parcelaId).properties().path("nomenclatu").asText(parcelaId),null,null,null,null,null,null,null));
    }
    @Transactional(readOnly=true)
    public FichaPublica publica(String parcelaId){
        territorio.parcela(parcelaId);
        return fichas.findById(parcelaId).map(f->new FichaPublica(f.getDireccion(),f.getObservaciones(),
            f.getFuente(),f.getReferencia(),f.getActualizadoEn())).orElse(null);
    }
    public Ficha guardarFicha(String email,String parcelaId,ies.belgrano.lotes.dto.request.EspacioTerritorialRequest.Ficha r){
        territorio.parcela(parcelaId);
        var ficha=fichas.findById(parcelaId).orElseGet(()->new FichaParcela(parcelaId));
        if(!Objects.equals(ficha.getVersion(),r.version()))
            throw new OperacionInvalidaException(409,"FICHA_MODIFICADA","Otra persona modificó la ficha. Recargala antes de guardar.");
        ficha.actualizar(limpiar(r.direccion()),limpiar(r.observaciones()),r.fuente().trim(),r.referencia().trim(),email);
        return ficha(fichas.saveAndFlush(ficha));
    }
    @Transactional(readOnly=true)
    public Pagina<Ficha> fichas(int pagina){
        var result=fichas.findAll(pagina(pagina,"actualizadoEn"));
        return new Pagina<>(result.map(this::ficha).getContent(),pagina,result.getTotalPages(),result.getTotalElements());
    }
    private PageRequest pagina(int pagina,String orden){
        if(pagina<0)throw new OperacionInvalidaException(400,"PAGINA_INVALIDA","La página no puede ser negativa.");
        return PageRequest.of(pagina,20,Sort.by(Sort.Direction.DESC,orden).and(Sort.by("id".equals(orden)?"id":orden.equals("creadaEn")?"id":"parcelaId")));
    }
    private String limpiar(String valor){return valor==null||valor.isBlank()?null:valor.trim();}
    private Guardada guardada(ConsultaGuardada f){return new Guardada(f.getId(),f.getParcelaId(),f.getNomenclatura(),
        f.getEtiqueta(),f.getLatitud(),f.getLongitud(),f.getCreadaEn());}
    private Ficha ficha(FichaParcela f){return new Ficha(f.getParcelaId(),geometria.parcela(f.getParcelaId()).map(p->p.properties().path("nomenclatu").asText(f.getParcelaId())).orElse(f.getParcelaId()),f.getDireccion(),f.getObservaciones(),f.getFuente(),
        f.getReferencia(),f.getActualizadoPor(),f.getActualizadoEn(),f.getVersion());}
}
