package ies.belgrano.lotes.service;
import ies.belgrano.lotes.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.*;
@Service
public class UsuarioAutenticacionService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public UsuarioAutenticacionService(UsuarioRepository usuarios) { this.usuarios=usuarios; }
    public UserDetails loadUserByUsername(String email) {
        var usuario=usuarios.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return User.withUsername(usuario.getEmail()).password(usuario.getPassword()).roles(usuario.getRol().getNombre().name()).build();
    }
}
