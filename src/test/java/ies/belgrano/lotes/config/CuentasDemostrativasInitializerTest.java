package ies.belgrano.lotes.config;

import ies.belgrano.lotes.entity.RolEntity;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.repository.RolRepository;
import ies.belgrano.lotes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CuentasDemostrativasInitializerTest {
    private static final String ADMIN_PASSWORD = "ClaveAdminDeClase2026!";
    private static final String USUARIO_PASSWORD = "ClaveUsuarioDeClase2026!";

    @Mock UsuarioRepository usuarios;
    @Mock RolRepository roles;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void creaCuentasConRolesYPasswordsCodificadas() {
        given(roles.findByNombre(RolEntity.NombreRol.ADMIN))
                .willReturn(Optional.of(new RolEntity(RolEntity.NombreRol.ADMIN)));
        given(roles.findByNombre(RolEntity.NombreRol.USUARIO))
                .willReturn(Optional.of(new RolEntity(RolEntity.NombreRol.USUARIO)));
        var initializer = new CuentasDemostrativasInitializer(usuarios, roles, encoder,
                ADMIN_PASSWORD, USUARIO_PASSWORD);

        initializer.run(null);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarios, times(2)).save(captor.capture());
        var cuentas = captor.getAllValues();
        assertThat(cuentas).extracting(UsuarioEntity::getEmail).containsExactly(
                CuentasDemostrativasInitializer.ADMIN_EMAIL,
                CuentasDemostrativasInitializer.USUARIO_EMAIL);
        assertThat(cuentas).extracting(c -> c.getRol().getNombre()).containsExactly(
                RolEntity.NombreRol.ADMIN, RolEntity.NombreRol.USUARIO);
        assertThat(encoder.matches(ADMIN_PASSWORD, cuentas.get(0).getPassword())).isTrue();
        assertThat(encoder.matches(USUARIO_PASSWORD, cuentas.get(1).getPassword())).isTrue();
    }

    @Test
    void noReescribeCuentasCuandoConservanLasMismasClaves() {
        given(usuarios.findByEmail(CuentasDemostrativasInitializer.ADMIN_EMAIL))
                .willReturn(Optional.of(new UsuarioEntity("Admin",
                        CuentasDemostrativasInitializer.ADMIN_EMAIL, encoder.encode(ADMIN_PASSWORD),
                        new RolEntity(RolEntity.NombreRol.ADMIN))));
        given(usuarios.findByEmail(CuentasDemostrativasInitializer.USUARIO_EMAIL))
                .willReturn(Optional.of(new UsuarioEntity("Usuario",
                        CuentasDemostrativasInitializer.USUARIO_EMAIL, encoder.encode(USUARIO_PASSWORD),
                        new RolEntity(RolEntity.NombreRol.USUARIO))));

        new CuentasDemostrativasInitializer(usuarios, roles, encoder,
                ADMIN_PASSWORD, USUARIO_PASSWORD).run(null);

        verify(usuarios, never()).save(any(UsuarioEntity.class));
    }

    @Test
    void actualizaLaClaveDeUnaCuentaDemoExistente() {
        UsuarioEntity admin = new UsuarioEntity("Admin",
                CuentasDemostrativasInitializer.ADMIN_EMAIL, encoder.encode("ClaveAnterior2026!"),
                new RolEntity(RolEntity.NombreRol.ADMIN));
        given(usuarios.findByEmail(CuentasDemostrativasInitializer.ADMIN_EMAIL))
                .willReturn(Optional.of(admin));
        given(roles.findByNombre(RolEntity.NombreRol.USUARIO))
                .willReturn(Optional.of(new RolEntity(RolEntity.NombreRol.USUARIO)));

        new CuentasDemostrativasInitializer(usuarios, roles, encoder,
                ADMIN_PASSWORD, USUARIO_PASSWORD).run(null);

        verify(usuarios).save(admin);
        assertThat(encoder.matches(ADMIN_PASSWORD, admin.getPassword())).isTrue();
    }

    @Test
    void exigeClavesValidasAntesDeModificarLaBase() {
        var initializer = new CuentasDemostrativasInitializer(usuarios, roles, encoder,
                "corta", USUARIO_PASSWORD);
        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_ADMIN_PASSWORD");
        verify(usuarios, never()).save(any(UsuarioEntity.class));
    }
}
