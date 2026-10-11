package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.ActividadTerritorialResponse.*;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.Pagina;
import ies.belgrano.lotes.dto.response.TerritorioResponse.Consulta;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.repository.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service @Transactional
public class ActividadTerritorialService {
    private final ConsultaHistorialRepository historial;
    private final ConsultaGuardadaRepository guardadas;
    private final UsuarioRepository usuarios;
    private final TerritorioService territorio;
    private final TerritorioRepository geometria;
    private final EspacioTerritorialService espacio;
    private final ObjectMapper mapper;
    public ActividadTerritorialService(ConsultaHistorialRepository historial,ConsultaGuardadaRepository guardadas,
            UsuarioRepository usuarios,TerritorioService territorio,TerritorioRepository geometria,
            EspacioTerritorialService espacio,ObjectMapper mapper){
        this.historial=historial;this.guardadas=guardadas;this.usuarios=usuarios;this.territorio=territorio;
        this.geometria=geometria;this.espacio=espacio;this.mapper=mapper;
    }
    public Consulta consultar(String email,double latitud,double longitud){
        var usuario=usuarios.findByEmail(email).orElseThrow(()->error(401,"AUTENTICACION_REQUERIDA","Iniciá sesión nuevamente."));
        var consulta=territorio.consultar(latitud,longitud);
        var fecha=Instant.now();
        var copia=informe(consulta,null,fecha);
        String resumen=consulta.parcelas().stream().map(p->p.properties().path("nomenclatu").asText(p.id())).collect(Collectors.joining(" · "));
        if(resumen.isEmpty())resumen="Sin parcela identificada";
        historial.save(new ConsultaHistorial(usuario,latitud,longitud,fecha,resumen,mapper.writeValueAsString(copia)));
        return consulta;
    }
    @Transactional(readOnly=true)
    public Pagina<Historial> historial(String email,int pagina){
        if(pagina<0 || pagina>5000)throw error(400,"PAGINA_INVALIDA","Indicá una página entre 0 y 5000.");
        var rows=historial.findByUsuarioEmail(email,PageRequest.of(pagina,20,Sort.by(Sort.Direction.DESC,"consultadaEn","id")));
        return new Pagina<>(rows.map(h->new Historial(h.getId(),h.getLatitud(),h.getLongitud(),h.getConsultadaEn(),h.getResumen())).getContent(),
                pagina,rows.getTotalPages(),rows.getTotalElements());
    }
    @Transactional(readOnly=true)
    public Informe informeHistorico(String email,Long id){
        var h=historiaPropia(email,id);
        var copia=mapper.readValue(h.getSnapshot(),Informe.class);
        // La copia no se recalcula contra las capas actuales ni contra las fichas editables.
        return new Informe(Instant.now(),h.getConsultadaEn(),true,null,copia.consulta(),copia.aportes());
    }
    public void quitarHistorial(String email,Long id){historial.delete(historiaPropia(email,id));}
    @Transactional(readOnly=true)
    public Informe informeGuardada(String email,Long id){return actual(guardadaPropia(email,id),Instant.now());}
    @Transactional(readOnly=true)
    public List<Informe> comparar(String email,List<Long> ids){
        if(ids==null || ids.size()<2 || ids.size()>3 || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size()!=ids.size())
            throw error(400,"COMPARACION_INVALIDA","Elegí entre dos y tres guardadas distintas de tu cuenta.");
        // Se comprueba la propiedad de toda la selección antes de leer datos territoriales.
        var propias=ids.stream().map(id->guardadaPropia(email,id)).toList();
        var fecha=Instant.now();
        return propias.stream().map(g->actual(g,fecha)).toList();
    }
    private Informe actual(ConsultaGuardada g,Instant fecha){
        territorio.parcela(g.getParcelaId());
        if(!geometria.pertenece(g.getParcelaId(),"POINT("+g.getLongitud()+" "+g.getLatitud()+")"))
            throw error(409,"PARCELA_CAMBIO","El punto guardado ya no coincide con la parcela. Seleccionala nuevamente desde el mapa.");
        var c=territorio.consultar(g.getLatitud(),g.getLongitud());
        if(c.parcelas().stream().noneMatch(p->p.id().equals(g.getParcelaId())))
            throw error(409,"PARCELA_NO_INCLUIDA","La selección tiene demasiadas coincidencias. Elegí un punto interior de la parcela.");
        return informe(c,g.getParcelaId(),fecha);
    }
    private Informe informe(Consulta c,String id,Instant fecha){
        var aportes=c.parcelas().stream().map(p->new Aporte(p.id(),espacio.publica(p.id()))).filter(a->a.ficha()!=null).toList();
        return new Informe(fecha,fecha,false,id,c,aportes);
    }
    private ConsultaGuardada guardadaPropia(String email,Long id){
        return guardadas.findByIdAndUsuarioEmail(id,email).orElseThrow(()->error(404,"GUARDADA_NO_ENCONTRADA","La consulta no está entre tus guardadas."));
    }
    private ConsultaHistorial historiaPropia(String email,Long id){
        return historial.findByIdAndUsuarioEmail(id,email).orElseThrow(()->error(404,"HISTORIAL_NO_ENCONTRADO","La consulta no está en tu historial."));
    }
    private OperacionInvalidaException error(int estado,String codigo,String mensaje){return new OperacionInvalidaException(estado,codigo,mensaje);}
}
