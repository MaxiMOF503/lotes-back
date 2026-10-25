package ies.belgrano.lotes.service;

import ies.belgrano.lotes.dto.request.ReporteDatoRequest.*;
import ies.belgrano.lotes.dto.response.EspacioTerritorialResponse.Pagina;
import ies.belgrano.lotes.dto.response.ReporteDatoResponse;
import ies.belgrano.lotes.dto.response.ReporteDatoResponse.Persona;
import ies.belgrano.lotes.entity.ReporteDato;
import ies.belgrano.lotes.entity.UsuarioEntity;
import ies.belgrano.lotes.exception.OperacionInvalidaException;
import ies.belgrano.lotes.repository.ReporteDatoRepository;
import ies.belgrano.lotes.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@Transactional
public class ReporteDatoService {
    private final ReporteDatoRepository reportes;
    private final UsuarioRepository usuarios;
    private final TerritorioService territorio;

    public ReporteDatoService(ReporteDatoRepository reportes, UsuarioRepository usuarios, TerritorioService territorio) {
        this.reportes = reportes;
        this.usuarios = usuarios;
        this.territorio = territorio;
    }

    public ReporteDatoResponse crear(String email, Crear solicitud) {
        UsuarioEntity usuario = usuario(email);
        var parcela = territorio.parcela(solicitud.parcelaId().trim());
        String nomenclatura = parcela.properties().path("nomenclatu").asText(parcela.id());
        var reporte = new ReporteDato(usuario, parcela.id(), nomenclatura,
            solicitud.seccion(), solicitud.tipo(), solicitud.descripcion().trim(), Instant.now());
        return respuesta(reportes.save(reporte));
    }

    @Transactional(readOnly = true)
    public Pagina<ReporteDatoResponse> propios(String email, int pagina) {
        validarPagina(pagina);
        var resultado = reportes.findByCreadoPorEmail(email, pagina(pagina));
        return pagina(resultado, pagina);
    }

    @Transactional(readOnly = true)
    public Pagina<ReporteDatoResponse> administrar(ReporteDato.Estado estado, int pagina) {
        validarPagina(pagina);
        var resultado = estado == null ? reportes.findAll(pagina(pagina)) : reportes.findByEstado(estado, pagina(pagina));
        return pagina(resultado, pagina);
    }

    public ReporteDatoResponse actualizar(String emailAdministrador, Long id, Actualizar solicitud) {
        UsuarioEntity administrador = usuario(emailAdministrador);
        ReporteDato reporte = reportes.findById(id).orElseThrow(() ->
            error(404, "REPORTE_NO_ENCONTRADO", "El reporte solicitado no existe."));
        String respuesta = textoOpcional(solicitud.respuesta());
        if ((solicitud.estado() == ReporteDato.Estado.RESUELTO || solicitud.estado() == ReporteDato.Estado.DESCARTADO)
                && (respuesta == null || respuesta.length() < 10)) {
            throw error(400, "RESPUESTA_REQUERIDA",
                "Para cerrar o descartar un reporte, escribí una respuesta de al menos 10 caracteres.");
        }
        reporte.actualizar(solicitud.estado(), respuesta, administrador, Instant.now());
        return respuesta(reporte);
    }

    private UsuarioEntity usuario(String email) {
        return usuarios.findByEmail(email).orElseThrow(() ->
            error(401, "AUTENTICACION_REQUERIDA", "Iniciá sesión nuevamente."));
    }

    private PageRequest pagina(int pagina) {
        return PageRequest.of(pagina, 20, Sort.by(Sort.Direction.DESC, "creadoEn", "id"));
    }

    private void validarPagina(int pagina) {
        if (pagina < 0 || pagina > 5000)
            throw error(400, "PAGINA_INVALIDA", "Indicá una página entre 0 y 5000.");
    }

    private Pagina<ReporteDatoResponse> pagina(org.springframework.data.domain.Page<ReporteDato> resultado, int pagina) {
        return new Pagina<>(resultado.map(this::respuesta).getContent(), pagina,
            resultado.getTotalPages(), resultado.getTotalElements());
    }

    private ReporteDatoResponse respuesta(ReporteDato reporte) {
        return new ReporteDatoResponse(reporte.getId(), reporte.getParcelaId(), reporte.getNomenclatura(),
            reporte.getSeccion(), reporte.getTipo(), reporte.getDescripcion(), reporte.getEstado(), reporte.getRespuesta(),
            persona(reporte.getCreadoPor()), persona(reporte.getRespondidoPor()), reporte.getCreadoEn(),
            reporte.getActualizadoEn(), reporte.getRespondidoEn(), canonicoDisponible(reporte.getParcelaId()));
    }

    private String canonicoDisponible(String id) {
        return territorio.canonicoDisponible(id).orElse(null);
    }

    private Persona persona(UsuarioEntity usuario) {
        return usuario == null ? null : new Persona(usuario.getNombre(), usuario.getEmail());
    }

    private String textoOpcional(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return texto.trim();
    }

    private OperacionInvalidaException error(int estado, String codigo, String mensaje) {
        return new OperacionInvalidaException(estado, codigo, mensaje);
    }
}
