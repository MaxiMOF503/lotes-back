package ies.belgrano.lotes.config;
import ies.belgrano.lotes.service.EstadisticaService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component
public class ConsultaEstadisticaFilter extends OncePerRequestFilter {
    private final EstadisticaService estadisticas;
    public ConsultaEstadisticaFilter(EstadisticaService estadisticas) {this.estadisticas=estadisticas;}
    @Override protected boolean shouldNotFilter(HttpServletRequest r) {
        return !"GET".equals(r.getMethod()) || !(r.getContextPath()+"/api/public/v1/lotes/ficha").equals(r.getRequestURI());
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        boolean fallo=false;
        try {chain.doFilter(req,res);} catch(ServletException|IOException|RuntimeException e) {fallo=true;throw e;}
        finally {
            boolean id=req.getParameter("identificador")!=null && !req.getParameter("identificador").isBlank();
            boolean lat=req.getParameter("latitud")!=null, lon=req.getParameter("longitud")!=null;
            String criterio=id && !lat && !lon ? "IDENTIFICADOR" : !id && lat && lon ? "COORDENADAS" : "INVALIDO";
            int status=fallo ? 500 : res.getStatus();
            String resultado=status>=500 ? "ERROR_TECNICO" : status==404 ? "SIN_RESULTADOS" : status>=400 ? "INVALIDA" : "EXITO";
            estadisticas.registrar("CONSULTA",criterio,resultado);
        }
    }
}
