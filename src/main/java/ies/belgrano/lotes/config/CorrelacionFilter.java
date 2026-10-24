package ies.belgrano.lotes.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Asigna un identificador a cada solicitud: aparece en el log, en el encabezado X-Correlation-Id
 * y en el cuerpo de los errores. Corre antes que Spring Security para que también lo tengan los 401 y 403.
 * Solo acepta uno entrante si tiene un formato inocuo; nunca se registran cookies, encabezados ni contraseñas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacionFilter extends OncePerRequestFilter {
	public static final String CABECERA = "X-Correlation-Id";
	public static final String CLAVE = "correlacionId";
	private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9._-]{8,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String entrante = request.getHeader(CABECERA);
		String id = entrante != null && VALIDO.matcher(entrante).matches() ? entrante : UUID.randomUUID().toString();
		MDC.put(CLAVE, id);
		response.setHeader(CABECERA, id);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(CLAVE);
		}
	}
}
