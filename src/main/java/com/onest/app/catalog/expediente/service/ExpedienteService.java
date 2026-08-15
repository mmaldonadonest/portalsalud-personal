package com.onest.app.catalog.expediente.service;

import com.onest.app.catalog.expediente.client.ExpedienteClient;
import com.onest.app.catalog.expediente.client.dto.BiowsConsultaAltaRequest;
import com.onest.app.catalog.expediente.dto.ConsultaDetalleDto;
import com.onest.app.catalog.expediente.dto.ConsultaDto;
import com.onest.app.catalog.expediente.dto.IcdDto;
import com.onest.app.catalog.expediente.web.ConsultaAltaForm;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.nss.dto.NssSearchResponse;
import com.onest.app.catalog.nss.service.NssSearchService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Expediente general: historial de consultas medicas por NSS.
 * Replica el flujo viewExpediente -> expedienteShowConsults?type=expedienteConsulta de php-old.
 */
@Service
public class ExpedienteService {

    // php usa date("d/m/y H:i:s")
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");
    // En php-old addConsultM envia USUARIO fijo "747849849" (NOMBRE_USUARIO si es el logueado).
    private static final String USUARIO_FIJO = "747849849";

    private final ExpedienteClient client;
    private final NssSearchService nssSearchService;

    public ExpedienteService(ExpedienteClient client, NssSearchService nssSearchService) {
        this.client = client;
        this.nssSearchService = nssSearchService;
    }

    public List<ConsultaDto> consultasByNss(String nss) {
        return client.findConsultas(normalizeNss(nss));
    }

    /**
     * CUENTA (cliente externo) del empleado, reusando la busqueda por NSS ya conectada
     * (Catalogo/usuario) - sin agregar ningun WS nuevo. Vacio si no es un NSS de empleado
     * o si el WS no trae el dato.
     */
    public String cuentaDe(String nss) {
        return nssSearchService.findByNss(normalizeNss(nss))
                .map(NssSearchResponse::empleado)
                .map(EmpleadoDto::cuenta)
                .orElse("");
    }

    public Optional<ConsultaDetalleDto> consultaDetalle(String nss, String idConsulta) {
        if (idConsulta == null || idConsulta.isBlank()) {
            throw new IllegalArgumentException("El id de consulta es obligatorio");
        }
        return client.findConsultaDetail(normalizeNss(nss), idConsulta.trim());
    }

    /** Alta de consulta medica (addConsultM -> /Servcio/consulta). Devuelve el mensaje Proceso. */
    public String crearConsulta(ConsultaAltaForm form) {
        String nss = normalizeNss(form.getNss()); 
        // CONSULTA_RELACIONADA/ID_ARCHIVOS: id que relaciona la consulta con sus adjuntos.
        // En php-old es md5(rand+fecha); si el form no lo trae, generamos uno.
        String relacion = (form.getIdArchivoRel() != null && !form.getIdArchivoRel().isBlank())
                ? form.getIdArchivoRel().trim()
                : UUID.randomUUID().toString().replace("-", "");

        BiowsConsultaAltaRequest request = new BiowsConsultaAltaRequest(
                LocalDateTime.now().format(FECHA),
                nss,
                form.getTipoConsulta(),
                form.getAreaAccidente(),
                form.getAreaAnatomica(),
                form.getCausa(),
                form.getPeso(),
                form.getTalla(),
                form.getImc(),
                form.getFc(),
                form.getFr(),
                form.getTa(),
                form.getTemperatura(),
                form.getMotivo(),
                form.getExploracion(),
                form.getDiagnostico(),
                form.getTratamiento(),
                relacion,
                relacion,
                USUARIO_FIJO,
                usuarioActual(),
                form.getFirma()
        );
        return client.crearConsulta(request);
    }

    public List<IcdDto> buscarIcd(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return client.buscarIcd(texto.trim());
    }

    /** NOMBRE_USUARIO. En php-old era $_SESSION['nombre']; aqui el principal autenticado. */
    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private String normalizeNss(String nss) {
        if (nss == null || nss.isBlank()) {
            throw new IllegalArgumentException("El NSS es obligatorio");
        }
        String value = nss.trim();
        if (value.length() > 50) {
            throw new IllegalArgumentException("El NSS excede la longitud permitida");
        }
        return value;
    }
}
