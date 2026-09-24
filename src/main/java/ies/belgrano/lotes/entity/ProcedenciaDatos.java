package ies.belgrano.lotes.entity;
import jakarta.persistence.*;
import ies.belgrano.lotes.dto.request.ProcedenciaRequest;
import ies.belgrano.lotes.dto.response.ProcedenciaResponse;
@Embeddable
public class ProcedenciaDatos {
    private String tipo; private String fuente;
    @Column(length=1000) private String referencia;
    protected ProcedenciaDatos() {}
    public ProcedenciaDatos(ProcedenciaRequest request) {tipo=request.tipo().name();fuente=request.fuente().trim();referencia=request.referencia();}
    public ProcedenciaResponse response() {return new ProcedenciaResponse(tipo,fuente,"OFICIAL".equals(tipo));}
    public ProcedenciaRequest request() {return new ProcedenciaRequest(ProcedenciaRequest.Tipo.valueOf(tipo),fuente,referencia);}
}
