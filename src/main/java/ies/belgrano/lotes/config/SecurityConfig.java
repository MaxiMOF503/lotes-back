package ies.belgrano.lotes.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
@org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
public class SecurityConfig {
    private final tools.jackson.databind.ObjectMapper mapper;
    public SecurityConfig(tools.jackson.databind.ObjectMapper mapper) {this.mapper=mapper;}
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/public/**", "/api/csrf", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().denyAll())
            .formLogin(f -> f.loginProcessingUrl("/api/login")
                .successHandler((req,res,auth) -> res.setStatus(204))
                .failureHandler((req,res,e) -> error(req,res,401,"CREDENCIALES_INVALIDAS")).permitAll())
            .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,x) -> error(req,res,401,"AUTENTICACION_REQUERIDA"))
                .accessDeniedHandler((req,res,x) -> error(req,res,403,"ACCESO_DENEGADO")))
            .build();
    }
    private void error(jakarta.servlet.http.HttpServletRequest req,jakarta.servlet.http.HttpServletResponse res,int status,String code) throws java.io.IOException {
        res.setStatus(status);res.setContentType("application/json");res.setCharacterEncoding("UTF-8");
        res.getWriter().write(mapper.writeValueAsString(new ies.belgrano.lotes.dto.response.ApiErrorResponse(
            java.time.Instant.now(),status,code,"Acceso no autorizado o token CSRF inválido",req.getRequestURI(),java.util.List.of())));
    }
}
