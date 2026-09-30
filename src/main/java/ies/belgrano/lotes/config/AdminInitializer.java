package ies.belgrano.lotes.config;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.repository.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class AdminInitializer implements ApplicationRunner {
    private final UsuarioRepository usuarios; private final RolRepository roles; private final PasswordEncoder encoder;
    private final String email; private final String password;
    private final String usuarioEmail; private final String usuarioPassword;
    public AdminInitializer(UsuarioRepository usuarios,RolRepository roles,PasswordEncoder encoder,
        @Value("${ADMIN_EMAIL:}") String email,@Value("${ADMIN_PASSWORD:}") String password,
        @Value("${USUARIO_EMAIL:}") String usuarioEmail,@Value("${USUARIO_PASSWORD:}") String usuarioPassword) {
        this.usuarios=usuarios;this.roles=roles;this.encoder=encoder;this.email=email;this.password=password;
        this.usuarioEmail=usuarioEmail;this.usuarioPassword=usuarioPassword;
    }
    @Transactional public void run(ApplicationArguments args) {
        if(!email.isBlank() || !password.isBlank()) {
            if(email.isBlank() || password.length()<12) throw new IllegalStateException("Configurar ADMIN_EMAIL y ADMIN_PASSWORD de al menos 12 caracteres");
            if(!usuarios.existsByEmail(email)) {
                var rol=roles.findByNombre(RolEntity.NombreRol.ADMIN).orElseGet(() -> roles.save(new RolEntity(RolEntity.NombreRol.ADMIN)));
                usuarios.save(new UsuarioEntity("Administrador",email,encoder.encode(password),rol));
            }
        }
        crearUsuarioDeConsulta();
    }

    private void crearUsuarioDeConsulta() {
        if(usuarioEmail.isBlank() && usuarioPassword.isBlank()) return;
        if(usuarioEmail.isBlank() || usuarioPassword.length()<12) throw new IllegalStateException("Configurar USUARIO_EMAIL y USUARIO_PASSWORD de al menos 12 caracteres");
        if(usuarios.existsByEmail(usuarioEmail)) return;
        var rol=roles.findByNombre(RolEntity.NombreRol.USUARIO).orElseGet(() -> roles.save(new RolEntity(RolEntity.NombreRol.USUARIO)));
        usuarios.save(new UsuarioEntity("Usuario de consulta",usuarioEmail,encoder.encode(usuarioPassword),rol));
    }
}
