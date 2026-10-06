package ies.belgrano.lotes.config;

import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.repository.RolRepository;
import ies.belgrano.lotes.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cuentas de clase optativas. Nunca guarda contraseñas en el repositorio. */
@Component
@ConditionalOnProperty(prefix = "demo.accounts", name = "enabled", havingValue = "true")
public class CuentasDemostrativasInitializer implements ApplicationRunner {
    public static final String ADMIN_EMAIL = "demo.admin@loteseguro.invalid";
    public static final String USUARIO_EMAIL = "demo.usuario@loteseguro.invalid";

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final String adminPassword;
    private final String usuarioPassword;

    public CuentasDemostrativasInitializer(
            UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder,
            @Value("${DEMO_ADMIN_PASSWORD:}") String adminPassword,
            @Value("${DEMO_USER_PASSWORD:}") String usuarioPassword) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
        this.usuarioPassword = usuarioPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validarPassword("DEMO_ADMIN_PASSWORD", adminPassword);
        validarPassword("DEMO_USER_PASSWORD", usuarioPassword);
        crearOActualizar(ADMIN_EMAIL, "Administrador de demostración", RolEntity.NombreRol.ADMIN, adminPassword);
        crearOActualizar(USUARIO_EMAIL, "Usuario de demostración", RolEntity.NombreRol.USUARIO, usuarioPassword);
    }

    private void crearOActualizar(String email, String nombre, RolEntity.NombreRol rolEsperado, String password) {
        var existente = usuarios.findByEmail(email);
        if (existente.isPresent()) {
            UsuarioEntity usuario = existente.get();
            if (usuario.getRol().getNombre() != rolEsperado) {
                throw new IllegalStateException("La cuenta demostrativa " + email + " tiene un rol diferente");
            }
            if (!encoder.matches(password, usuario.getPassword())) {
                usuario.cambiarPassword(encoder.encode(password));
                usuarios.save(usuario);
            }
            return;
        }
        RolEntity rol = roles.findByNombre(rolEsperado)
                .orElseGet(() -> roles.save(new RolEntity(rolEsperado)));
        usuarios.save(new UsuarioEntity(nombre, email, encoder.encode(password), rol));
    }

    private static void validarPassword(String variable, String password) {
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(variable + " debe tener entre 12 caracteres y 72 bytes UTF-8");
        }
    }
}
