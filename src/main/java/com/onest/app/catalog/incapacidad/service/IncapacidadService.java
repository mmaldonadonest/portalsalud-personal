package com.onest.app.catalog.incapacidad.service;

import com.onest.app.catalog.incapacidad.client.IncapacidadClient;
import com.onest.app.catalog.incapacidad.client.dto.BiowsIncapacidadAltaRequest;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDetalleDto;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDto;
import com.onest.app.catalog.incapacidad.dto.IncapacidadReporteDto;
import com.onest.app.catalog.incapacidad.web.IncapacidadAltaForm;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Incapacidades por NSS (lista + detalle). Replica el flujo viewArchivoInc /
 * showDetailsIncap de php-old (WS consulta_incapacidad).
 */
@Service
public class IncapacidadService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");
    private static final String USUARIO_FIJO = "747849849";
    // Formato exacto que espera el WS para el reporte por fecha (anio de 2 digitos) - ver
    // BiowsIncapacidadReporteRequest, confirmado contra php-old/rest/dashboard/generarExcelIncapa.php.
    private static final DateTimeFormatter FECHA_REPORTE = DateTimeFormatter.ofPattern("dd/MM/yy");

    /** C-046: folio del certificado IMSS, 2 letras + 6 numeros (asi vienen 96 de 97 folios IMSS en produccion). */
    static final java.util.regex.Pattern MASCARA_FOLIO = java.util.regex.Pattern.compile("^[A-Z]{2}[0-9]{6}$");
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IncapacidadService.class);

    private final IncapacidadClient client;
    private final com.onest.app.catalog.incapacidad.repository.FolioIncapacidadRepository folios;
    private final CadenaIncapacidadService cadenas;
    // Tomcat de produccion corre en UTC (memoria zona-horaria-prod-utc): "hoy" es el de la Ciudad de Mexico
    private static final java.time.ZoneId ZONA = java.time.ZoneId.of("America/Mexico_City");

    // C-057: nombre del PDF de la incapacidad con la nomenclatura de SM
    private final com.onest.app.catalog.file.service.NomenclaturaService nomenclatura;

    public IncapacidadService(IncapacidadClient client,
                              com.onest.app.catalog.incapacidad.repository.FolioIncapacidadRepository folios,
                              CadenaIncapacidadService cadenas,
                              com.onest.app.catalog.file.service.NomenclaturaService nomenclatura) {
        this.client = client;
        this.folios = folios;
        this.cadenas = cadenas;
        this.nomenclatura = nomenclatura;
    }

    /**
     * Fechas de la incapacidad ya resueltas: lo que se manda al WS. {@code marca} = I (inicial) o S1, S2... (la
     * subsecuente que sigue en su cadena), para el nombre del PDF (C-057, P-008); null en tipos viejos.
     */
    record Fechas(String primera, String inicio, String termino, Long anterior, String marca) {
    }

    /**
     * C-045 / C-050 / C-051 / C-053 (18-nov-2026). Resuelve las fechas que se guardan:
     * <ul>
     *   <li>Inicial: fecha inicial = fecha de la primera incapacidad.</li>
     *   <li>Subsecuente: hay que elegir la incapacidad que continua, su cadena debe seguir abierta (sin Alta = Si)
     *       y la fecha de la primera incapacidad la pone el portal (inicio de la inicial de la cadena; en el
     *       Excel de SM se teclea y hay 14 que no cuadran). La subsecuente empieza despues del termino anterior.</li>
     *   <li>Termino = inicio + dias autorizados - 1 (el dia de inicio cuenta), como el Excel de SM.</li>
     *   <li>E-021: la fecha de inicio no puede ser futura (el termino si: la incapacidad sigue corriendo).</li>
     * </ul>
     * Los tipos viejos del PHP (enfermedad_general, accidente_trabajo...) no pasan por aqui: el formulario
     * solo ofrece Inicial y Subsecuente; si llegara otro, se guarda como venia.
     */
    Fechas resolverFechas(String nss, IncapacidadAltaForm form) {
        String tipo = form.getTipo() == null ? "" : form.getTipo().trim().toLowerCase(java.util.Locale.ROOT);
        boolean sub = "subsecuente".equals(tipo);
        if (tipo.isEmpty()) {
            throw new IllegalArgumentException("Elige el tipo de incapacidad: Inicial o Subsecuente.");
        }
        if (!sub && !"inicial".equals(tipo)) {
            String inicio = blankToNull(form.getFechaInicio()) != null ? form.getFechaInicio() : form.getFechaPrimera();
            return new Fechas(form.getFechaPrimera(), inicio, form.getFechaTermino(), null, null);
        }
        // P-008 (SM 18-nov-2026): «Maternidad es única inicial, no hay más»
        if (sub && "maternidad".equalsIgnoreCase(form.getRamo() == null ? "" : form.getRamo().trim())) {
            throw new IllegalArgumentException("Maternidad es una sola incapacidad inicial: no lleva subsecuentes.");
        }
        Integer dias = enteroPositivo(form.getDiasAutorizados());
        if (dias == null) {
            throw new IllegalArgumentException("Captura los días autorizados (número entero mayor que 0): con ellos se calcula el término.");
        }
        LocalDate inicio;
        LocalDate primera;
        Long anterior = null;
        String marca = "I";
        if (sub) {
            CadenaIncapacidadService.Cadena c = cadenas.paraSubsecuente(nss, form.getAnterior());
            // la cadena ya trae la inicial y las subsecuentes anteriores: esta es la S<cuantas hay>
            marca = "S" + c.eslabones().size();
            anterior = Long.valueOf(form.getAnterior().trim());
            inicio = fechaObligatoria(form.getFechaInicio(), "Fecha subsecuente");
            LocalDate terminoAnterior = c.termino();
            if (terminoAnterior != null && !inicio.isAfter(terminoAnterior)) {
                throw new IllegalArgumentException("La subsecuente debe empezar después del término de la anterior ("
                        + CadenaIncapacidadService.fmt(terminoAnterior) + "); normalmente el día siguiente.");
            }
            primera = c.primera() != null ? c.primera() : inicio;
        } else {
            String f = blankToNull(form.getFechaPrimera()) != null ? form.getFechaPrimera() : form.getFechaInicio();
            inicio = fechaObligatoria(f, "Fecha inicial");
            primera = inicio;
        }
        if (inicio.isAfter(LocalDate.now(ZONA))) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser futura (" + CadenaIncapacidadService.fmt(inicio) + ").");
        }
        LocalDate termino = inicio.plusDays(dias - 1L);
        return new Fechas(primera.toString(), inicio.toString(), termino.toString(), anterior, marca);
    }

    private static LocalDate fechaObligatoria(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La " + campo + " es obligatoria.");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La " + campo + " no es una fecha válida: " + valor);
        }
    }

    private static Integer enteroPositivo(String s) {
        if (s == null || !s.trim().matches("\\d{1,4}")) {
            return null;
        }
        int n = Integer.parseInt(s.trim());
        return n > 0 ? n : null;
    }

    /**
     * C-046 (17-nov-2026, decision del usuario: «avisa y no deja guardar»). Rubro IMSS: folio obligatorio, con la
     * mascara AA999999 y que no exista ya en el sistema (portal o Excel, cualquier NSS); tampoco en una subsecuente,
     * que lleva su propio folio. Contexto: los folios falsos llegan a Nominas. Rubro Interna no pide folio (C-055).
     * Los repetidos viejos (21 en produccion, casi todos doble guardado) no se tocan: la regla es para lo nuevo.
     *
     * @return el folio normalizado (sin espacios, en mayusculas) que se guarda; el mismo valor si no es IMSS
     */
    String validarFolio(String rubro, String folio) {
        if (rubro == null || !"IMSS".equalsIgnoreCase(rubro.trim())) {
            return folio;
        }
        String f = folio == null ? "" : folio.replace(" ", "").trim().toUpperCase(java.util.Locale.ROOT);
        if (f.isEmpty()) {
            throw new IllegalArgumentException("El folio es obligatorio en rubro IMSS.");
        }
        if (!MASCARA_FOLIO.matcher(f).matches()) {
            throw new IllegalArgumentException("El folio IMSS debe ser 2 letras y 6 números (ejemplo SE105771). Capturado: " + f);
        }
        java.util.Optional<com.onest.app.catalog.incapacidad.repository.FolioIncapacidadRepository.Existente> ya;
        try {
            ya = folios.buscar(f);
        } catch (org.springframework.dao.DataAccessException ex) {
            // Sin poder revisar no se frena la captura clinica: se guarda y queda en el log para revisarlo
            log.warn("[incapacidad] no se pudo revisar si el folio {} ya existe: {}", f, ex.getMostSpecificCause().getMessage());
            return f;
        }
        if (ya.isPresent()) {
            var e = ya.get();
            throw new IllegalArgumentException("El folio " + f + " ya está registrado en otra incapacidad (NSS " + e.nss()
                    + (e.fecha() == null ? "" : ", " + e.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    + "). No se guardó: revisa el folio del certificado.");
        }
        return f;
    }

    public List<IncapacidadDto> byNss(String nss) {
        return client.findIncapacidades(normalizeNss(nss));
    }

    public Optional<IncapacidadDetalleDto> detalle(String nss, String idConsulta) {
        if (idConsulta == null || idConsulta.isBlank()) {
            throw new IllegalArgumentException("El id de incapacidad es obligatorio");
        }
        return client.findIncapacidadDetail(normalizeNss(nss), idConsulta.trim());
    }

    /** Alta de incapacidad (addIncapacidad -> /Servcio/incapacidades). Devuelve el mensaje Proceso. */
    public String crearIncapacidad(IncapacidadAltaForm form) {
        String nss = normalizeNss(form.getNss());
        String relacion = (form.getIdArchivoRel() != null && !form.getIdArchivoRel().isBlank())
                ? form.getIdArchivoRel().trim()
                : UUID.randomUUID().toString().replace("-", "");

        // incap.php: si salario_acumulado vacio -> 0
        String salarioAcumulado = blankToNull(form.getSalarioAcumulado()) != null ? form.getSalarioAcumulado() : "0";
        // C-045/C-051/C-053 y C-046: todo se valida antes de llamar al WS; si algo no pasa, no se guarda nada
        Fechas fechas = resolverFechas(nss, form);
        String folio = validarFolio(form.getRubro(), form.getFolio());

        BiowsIncapacidadAltaRequest request = new BiowsIncapacidadAltaRequest(
                LocalDateTime.now().format(FECHA),
                nss,
                folio,
                form.getRamo(),
                form.getTipo(),
                fechas.inicio(),
                fechas.termino(),
                form.getDiasAutorizados(),
                form.getSalarioIntegrado(),
                form.getCosto(),
                form.getImputable(),
                form.getEstadoDictamen(),
                form.getGoceSueldo(),
                form.getComplementoSalarial(),
                relacion,
                USUARIO_FIJO,
                usuarioActual(),
                form.getFirma(),
                form.getRubro(),
                form.getFechaAlta(),
                fechas.primera(),
                form.getSt2(),
                salarioAcumulado,
                form.getAlta()
        );
        String proceso = client.crearIncapacidad(request);
        // C-051/C-052: liga y diagnostico en SERV_MED_TAG. La incapacidad ya quedo en el WS: si esto falla no se
        // pierde la captura (la subsecuente se vuelve a pegar sola por fechas), solo queda en el log
        try {
            cadenas.guardarExtras(nss, relacion, fechas.anterior(), form.getDiagnostico());
        } catch (org.springframework.dao.DataAccessException ex) {
            log.warn("[incapacidad] guardada en el WS, pero sin liga/diagnostico (rel {}): {}", relacion,
                    ex.getMostSpecificCause().getMessage());
        }
        // C-057 (P-008, P-009): el PDF que el navegador sube enseguida con este rel se llamara
        // INCAP-MES-AAAA-INICIALES-I (o -S1, -S2...), con la fecha de inicio = fecha del documento
        if (fechas.marca() != null) {
            try {
                nomenclatura.apartarNombre(nss, relacion, nomenclatura.base("INCAP", LocalDate.parse(fechas.inicio()), nss)
                        + "-" + fechas.marca());
            } catch (RuntimeException ex) {
                log.warn("[incapacidad] sin nombre de SM para el PDF (rel {}): {}", relacion, ex.getMessage());
            }
        }
        return proceso;
    }

    /** Cadenas del NSS para la lista (C-054) y el formulario (C-051); vacio si no se pueden leer. */
    public List<CadenaIncapacidadService.Cadena> cadenas(String nss) {
        try {
            return cadenas.cadenas(normalizeNss(nss));
        } catch (org.springframework.dao.DataAccessException ex) {
            log.warn("[incapacidad] no se pudieron armar las cadenas: {}", ex.getMostSpecificCause().getMessage());
            return List.of();
        }
    }

    /**
     * Reporte administrativo por rango de fechas (todas las NSS). fechaInicial/fechaFinal llegan
     * en formato ISO (yyyy-MM-dd, lo que manda un &lt;input type="date"&gt;) y se reformatean a
     * "dd/MM/yy" antes de mandarlas al WS (ver FECHA_REPORTE).
     */
    public List<IncapacidadReporteDto> reportePorFecha(String fechaInicial, String fechaFinal) {
        LocalDate desde = parseFechaIso(fechaInicial, "inicial");
        LocalDate hasta = parseFechaIso(fechaFinal, "final");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        return client.reportePorFecha(desde.format(FECHA_REPORTE), hasta.format(FECHA_REPORTE));
    }

    private static LocalDate parseFechaIso(String valor, String etiqueta) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La fecha " + etiqueta + " es obligatoria");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha " + etiqueta + " no es valida", ex);
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

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
