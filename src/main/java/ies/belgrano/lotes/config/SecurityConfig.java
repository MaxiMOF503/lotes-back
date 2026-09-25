package ies.belgrano.lotes.config;

import ies.belgrano.lotes.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final ObjectMapper mapper;

	public SecurityConfig(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain security(HttpSecurity http) throws Exception {
		return http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
						"/api/public/**",
						"/api/csrf",
						"/error",
						"/v3/api-docs/**",
						"/swagger-ui/**",
						"/swagger-ui.html")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/me")
				.authenticated()
				.requestMatchers(HttpMethod.GET,
						"/api/admin/lotes",
						"/api/admin/lotes/*",
						"/api/admin/departamentos")
				.hasAnyRole("ADMIN", "USUARIO")
				.requestMatchers(HttpMethod.POST, "/api/admin/lotes")
				.hasRole("ADMIN")
				.requestMatchers(HttpMethod.PUT, "/api/admin/lotes/*")
				.hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/admin/estadisticas")
				.hasRole("ADMIN")
				.requestMatchers("/api/admin/**")
				.hasRole("ADMIN")
				.anyRequest()
				.denyAll())
			.formLogin(form -> form
					.loginProcessingUrl("/api/login")
					.successHandler((request, response, authentication) -> response.setStatus(204))
					.failureHandler((request, response, exception) -> error(
							request, response, 401, "CREDENCIALES_INVALIDAS", "Las credenciales no son válidas"))
					.permitAll())
			.logout(logout -> logout
					.logoutUrl("/api/logout")
					.logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
			.exceptionHandling(exceptions -> exceptions
					.authenticationEntryPoint((request, response, exception) -> error(
							request, response, 401, "AUTENTICACION_REQUERIDA", "Se requiere iniciar sesión"))
					.accessDeniedHandler((request, response, exception) -> error(
							request, response, 403, "ACCESO_DENEGADO", "No tenés permiso o el token CSRF es inválido")))
			.build();
	}

	private void error(
			HttpServletRequest request,
			HttpServletResponse response,
			int status,
			String codigo,
			String mensaje) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(mapper.writeValueAsString(new ApiErrorResponse(
				Instant.now(), status, codigo, mensaje, request.getRequestURI(), List.of())));
	}
}
