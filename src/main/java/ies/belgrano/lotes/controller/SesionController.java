package ies.belgrano.lotes.controller;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController
public class SesionController {
    @GetMapping("/api/csrf") public Map<String,String> csrf(CsrfToken csrf) {
        return Map.of("headerName",csrf.getHeaderName(),"token",csrf.getToken());
    }
    @GetMapping("/api/admin/me") public Map<String,String> me(java.security.Principal principal) {
        return Map.of("email",principal.getName(),"rol","ADMIN");
    }
}
