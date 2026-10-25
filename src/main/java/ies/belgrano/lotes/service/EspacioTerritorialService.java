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
import java.util.List;
import java.util.Comparator;

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
        var referencia=territorio.resolverParcela(parcelaId);
        var parcela=territorio.parcela(referencia.idCanonico());
        String punto="POINT("+r.longitud()+" "+r.latitud()+")";
        if(!geometria.pertenece(referencia.idCanonico(),punto))
            throw new OperacionInvalidaException(400,"PUNTO_FUERA_PARCELA","El punto no pertenece a la parcela seleccionada.");
        var usuario=usuarios.findByEmail(email).orElseThrow(()->new OperacionInvalidaException(401,"AUTENTICACION_REQUERIDA","Iniciá sesión nuevamente."));
        // Reutiliza la más antigua del grupo; no borra ni renombra Guardadas históricas.
        var registro=guardadas.findFirstByUsuarioEmailAndParcelaIdInOrderByIdAsc(email,referencia.idsRelacionados()).orElseGet(()->
            new ConsultaGuardada(usuario,referencia.idCanonico(),parcela.properties().path("nomenclatu").asText(referencia.idCanonico())));
        registro.actualizar(r.latitud(),r.longitud(),r.etiqueta()==null?registro.getEtiqueta():limpiar(r.etiqueta()));
        return guardada(guardadas.saveAndFlush(registro));
    }
    public void quitar(String email,Long id){
        guardadas.delete(guardadas.findByIdAndUsuarioEmail(id,email).orElseThrow(()->
            new OperacionInvalidaException(404,"GUARDADA_NO_ENCONTRADA","La consulta no está entre tus guardadas.")));
    }
    @Transactional(readOnly=true)
    public Ficha ficha(String parcelaId){
        var referencia=territorio.resolverParcela(parcelaId);
        var exacta=fichas.findById(parcelaId).map(this::ficha)
            .orElseGet(()->new Ficha(parcelaId,territorio.parcela(parcelaId).properties().path("nomenclatu").asText(parcelaId),null,null,null,null,null,null,null));
        return new Ficha(exacta.parcelaId(),exacta.nomenclatura(),exacta.direccion(),exacta.observaciones(),
            exacta.fuente(),exacta.referencia(),exacta.actualizadoPor(),exacta.actualizadoEn(),exacta.version(),
            referencia.idCanonico(),relacionadas(referencia).stream().filter(f->!f.getParcelaId().equals(parcelaId)).map(this::ficha).toList());
    }
    @Transactional(readOnly=true)
    public FichaPublica publica(String parcelaId){
        var referencia=territorio.resolverParcela(parcelaId);
        var aportes=relacionadas(referencia).stream().map(f->new AporteRelacionado(f.getParcelaId(),
            f.getDireccion(),f.getObservaciones(),f.getFuente(),f.getReferencia(),f.getActualizadoEn())).toList();
        if(aportes.isEmpty())return null;
        if(aportes.size()==1){
            var f=aportes.getFirst();
            return new FichaPublica(f.direccion(),f.observaciones(),f.fuente(),f.referencia(),f.actualizadoEn(),
                referencia.idCanonico(),false,aportes);
        }
        // El cliente anterior consume sólo campos planos: recibe todos los aportes rotulados,
        // sin elegir uno como verdad ni inventar una dirección compartida.
        String detalle="Existen varios aportes complementarios. Revisar cada origen por separado.\n\n"+
            aportes.stream().map(f->"Parcela de origen: "+f.parcelaId()+"\nDirección: "+texto(f.direccion())+
                "\nObservaciones: "+texto(f.observaciones())+"\nFuente: "+texto(f.fuente())+
                "\nReferencia: "+texto(f.referencia())+"\nActualizado: "+f.actualizadoEn())
                .collect(java.util.stream.Collectors.joining("\n\n"));
        var ultima=aportes.stream().map(AporteRelacionado::actualizadoEn).filter(Objects::nonNull)
            .max(java.time.Instant::compareTo).orElse(null);
        return new FichaPublica(null,detalle,"Múltiples fuentes; ver detalle por parcela de origen",
            "Referencias individuales en el detalle",ultima,referencia.idCanonico(),true,aportes);
    }
    private String texto(String valor){return valor==null?"No informado":valor;}
    private List<FichaParcela> relacionadas(TerritorioService.ReferenciaParcela referencia){
        return fichas.findAllById(referencia.idsRelacionados()).stream()
            .sorted(Comparator.comparing((FichaParcela f)->!f.getParcelaId().equals(referencia.idCanonico()))
                .thenComparing(FichaParcela::getParcelaId)).toList();
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
    private Ficha ficha(FichaParcela f){
        String canonico=territorio.canonicoDisponible(f.getParcelaId()).orElse(null);
        return new Ficha(f.getParcelaId(),geometria.parcela(f.getParcelaId()).map(p->p.properties().path("nomenclatu").asText(f.getParcelaId())).orElse(f.getParcelaId()),f.getDireccion(),f.getObservaciones(),f.getFuente(),
            f.getReferencia(),f.getActualizadoPor(),f.getActualizadoEn(),f.getVersion(),canonico,List.of());
    }
}
