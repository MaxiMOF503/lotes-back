package ies.belgrano.lotes.integration;

import ies.belgrano.lotes.dto.request.ReporteDatoRequest.*;
import ies.belgrano.lotes.entity.*;
import ies.belgrano.lotes.repository.*;
import ies.belgrano.lotes.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "TERRITORIO_IT", matches = "true")
class ReporteDatoMySqlIT {
    @Autowired ReporteDatoService service;
    @Autowired TerritorioService territorio;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    String usuario;
    String otro;
    String admin;
    String parcelaId;

    @BeforeEach void fixture() {
        String sufijo = UUID.randomUUID().toString();
        usuario = "reporte-" + sufijo + "@ejemplo.invalid";
        otro = "otro-" + sufijo + "@ejemplo.invalid";
        admin = "admin-" + sufijo + "@ejemplo.invalid";
        var rolUsuario = roles.findByNombre(RolEntity.NombreRol.USUARIO).orElseThrow();
        var rolAdmin = roles.findByNombre(RolEntity.NombreRol.ADMIN).orElseThrow();
        usuarios.saveAndFlush(new UsuarioEntity("Usuario", usuario, "hash-no-autenticable", rolUsuario));
        usuarios.saveAndFlush(new UsuarioEntity("Otro", otro, "hash-no-autenticable", rolUsuario));
        usuarios.saveAndFlush(new UsuarioEntity("Administrador", admin, "hash-no-autenticable", rolAdmin));
        parcelaId = territorio.consultar(-33.0688, -68.8929).parcelas().getFirst().id();
    }

    @Test void cicloCompletoEsPrivadoYLaRespuestaVuelveAlUsuario() {
        var creado = service.crear(usuario, new Crear(parcelaId, ReporteDato.Seccion.ZONIFICACION,
            ReporteDato.Tipo.INCORRECTO, "La zonificación difiere del documento municipal."));
        assertThat(creado.estado()).isEqualTo(ReporteDato.Estado.PENDIENTE);
        assertThat(service.propios(usuario, 0).items()).extracting(r -> r.id()).containsExactly(creado.id());
        assertThat(service.propios(otro, 0).items()).isEmpty();
        assertThat(service.administrar(ReporteDato.Estado.PENDIENTE, 0).items())
            .extracting(r -> r.id()).contains(creado.id());

        service.actualizar(admin, creado.id(), new Actualizar(ReporteDato.Estado.EN_REVISION, null));
        var resuelto = service.actualizar(admin, creado.id(), new Actualizar(ReporteDato.Estado.RESUELTO,
            "Se contrastó la fuente y se actualizó la información."));

        assertThat(resuelto.respondidoPor().email()).isEqualTo(admin);
        var visible = service.propios(usuario, 0).items().getFirst();
        assertThat(visible.estado()).isEqualTo(ReporteDato.Estado.RESUELTO);
        assertThat(visible.respuesta()).contains("Se contrastó la fuente");
        assertThat(visible.respondidoEn()).isNotNull();
    }

    @Test void parcelaInexistenteNoGeneraReporte() {
        assertThatThrownBy(() -> service.crear(usuario, new Crear("parcela-inexistente",
            ReporteDato.Seccion.OTRO, ReporteDato.Tipo.FALTANTE, "Descripción válida para la prueba.")))
            .isInstanceOf(ies.belgrano.lotes.exception.OperacionInvalidaException.class);
        assertThat(service.propios(usuario, 0).total()).isZero();
    }
}
