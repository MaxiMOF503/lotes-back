package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.response.CalidadCatastroResponse.*;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.CalidadCatastroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class CalidadCatastroService {
    private final CalidadCatastroRepository repository;
    public CalidadCatastroService(CalidadCatastroRepository repository){this.repository=repository;}
    public Resumen resumen(){return repository.resumen();}
    public PaginaGrupos grupos(boolean soloConflictos,int pagina){
        if(pagina<0||pagina>5000)throw new OperacionInvalidaException(400,"PAGINA_INVALIDA","Indicá una página entre 0 y 5000.");
        var filas=repository.grupos(soloConflictos,21,pagina*20);
        return new PaginaGrupos(filas.stream().limit(20).toList(),pagina,filas.size()>20);
    }
    public Grupo grupo(String id){
        if(id==null||!id.matches("[A-Za-z0-9._-]{1,100}"))
            throw new OperacionInvalidaException(400,"CONSULTA_TERRITORIAL_INVALIDA","Identificador de parcela inválido.");
        String representante=repository.representante(id).orElseThrow(()->new OperacionInvalidaException(404,
            "PARCELA_NO_ENCONTRADA","La parcela no está en el catastro importado."));
        var registros=repository.grupo(representante);
        if(registros.stream().noneMatch(r->r.parcelaId().equals(representante)&&r.duplicadoDe()==null))
            throw new OperacionInvalidaException(409,"REFERENCIA_PARCELA_INVALIDA",
                "El grupo no tiene un representante directo válido. Revisá la importación.");
        var copias=registros.stream().filter(r->r.duplicadoDe()!=null).toList();
        var campos=copias.stream().flatMap(r->r.camposConflicto().stream()).distinct().sorted().toList();
        boolean pendiente=copias.stream().anyMatch(r->!java.util.Set.of("DUPLICADO_IDENTICO","CONFLICTO_ATRIBUTOS").contains(
            r.tipoDuplicado()==null?"":r.tipoDuplicado()));
        String tipo=copias.isEmpty()?"SIN_COINCIDENCIAS":pendiente?"PENDIENTE_CLASIFICACION":
            copias.stream().anyMatch(r->"CONFLICTO_ATRIBUTOS".equals(r.tipoDuplicado()))?"COINCIDENCIA_CON_CONFLICTO":"DUPLICADO_IDENTICO";
        return new Grupo(id,representante,tipo,campos,registros);
    }
}
