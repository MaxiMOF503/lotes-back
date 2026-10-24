package ies.belgrano.lotes.config;
import ies.belgrano.lotes.service.EstadisticaService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Cuenta de forma anónima las consultas reales sobre el catastro, según cómo se llegó a la parcela:
 * clic en el mapa (COORDENADAS), búsqueda por nomenclatura (NOMENCLATURA) o por dirección (DIRECCION,
 * al elegir una dirección y buscar las parcelas cercanas). No guarda parámetros ni datos personales.
 */
@Component
public class ConsultaEstadisticaFilter extends OncePerRequestFilter {
    private static final String BASE = "/api/public/v1/territorio";
    private final EstadisticaService estadisticas;
    public ConsultaEstadisticaFilter(EstadisticaService estadisticas) {this.estadisticas=estadisticas;}

    static String criterio(HttpServletRequest r) {
        return switch (r.getRequestURI().substring(r.getContextPath().length())) {
            case BASE + "/consulta" -> "COORDENADAS";
            case BASE + "/parcelas" -> "NOMENCLATURA";
            case BASE + "/parcelas/cercanas" -> "DIRECCION";
            default -> null;
        };
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest r) {
        return !"GET".equals(r.getMethod()) || criterio(r)==null;
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        boolean fallo=false;
        try {chain.doFilter(req,res);} catch(ServletException|IOException|RuntimeException e) {fallo=true;throw e;}
        finally {
            int status=fallo ? 500 : res.getStatus();
            String resultado=status>=500 ? "ERROR_TECNICO" : status==404 ? "SIN_RESULTADOS" : status>=400 ? "INVALIDA" : "EXITO";
            estadisticas.registrar("CONSULTA",criterio(req),resultado);
        }
    }
}
