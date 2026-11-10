package com.onest.app.catalog.formato.service;

import com.onest.app.catalog.examen.service.ExamenDocumentoService.Encabezado;
import com.onest.app.catalog.expediente.service.DocumentoImpresoService;
import com.onest.app.catalog.firma.repository.DocumentoFirmadoRepository;
import com.onest.app.catalog.firma.repository.DocumentoFirmadoRepository.Autor;
import com.onest.app.catalog.firma.service.FirmaService;
import com.onest.app.catalog.firma.service.FirmaService.FirmaImpresa;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * Lo comun de los formatos de Salud Ocupacional que se imprimen sueltos (formatos B, 20-oct-2026):
 * encabezado de calidad de cada formato y las dos firmas que pidio el Dr. Ceron en TODOS los formatos:
 * la suya (con cedula) y la de quien elaboro el documento, con su nombre al lado.
 *
 * <p>Quien elaboro y quien era el medico responsable se fijan por documento (SERV_MED_DOC_AUTOR) al
 * crearlo o, para documentos anteriores, en su primera impresion: al reimprimir se usan esos, no quien
 * imprime. Cada impresion que lleva al menos una firma recibe folio y codigo de verificacion.
 * Si las tablas del modulo aun no existen, los impresos salen como antes (nombre y linea a mano).
 */
@Service
public class FormatosImpresosService {

    private static final Logger log = LoggerFactory.getLogger(FormatosImpresosService.class);

    /** Pie de firma del medico responsable: firma (data URI) o null para la linea a mano. */
    public record MedicoResponsable(String nombre, String cedula, String firmaDataUri) {
    }

    /** Segunda firma: quien elaboro el documento. */
    public record Elaboro(String nombre, String firmaDataUri) {
    }

    /** Folio y codigo de verificacion de una impresion con firma. */
    public record Folio(long folio, String codigo) {
    }

    /**
     * Encabezados de los formatos 2026 (filas 1-4 de cada Excel en docs/formatosnuevos/10202026,
     * descargados del OneDrive de Salud Ocupacional el 20-oct-2026).
     */
    private static final Map<String, Encabezado> ENCABEZADOS = Map.of(
            "FT-SO-25", new Encabezado("CARTA COMPROMISO", "14 de octubre de 2026", "05", "FT-SO-25", "04",
                    "Actualización por vigencia"),
            "FT-SO-22", new Encabezado("ACEPTACIÓN A CECOSAMA", "14 de octubre de 2026", "05", "FT-SO-22", "04",
                    "Actualización de titulo"),
            "FT-SO-14", new Encabezado("CADENA DE CUSTODIA", "14 de octubre de 2026", "05", "FT-SO-14", "04",
                    "Actualización por vigencia"),
            "FT-SO-02", new Encabezado("PASE DE SALIDA", "14 de octubre de 2026", "05", "FT-SO-02", "04",
                    "Actualización por vigencia"),
            "FT-SO-29", new Encabezado("PASE DE MEDICAMENTO", "14 de octubre de 2026", "04", "FT-SO-29", "03",
                    "Actualización de la vigencia"),
            "FT-SO-23", new Encabezado("CARTA DE CONSENTIMIENTO PARA PRUEBAS ESPECIALES", "14 de octubre de 2026", "05",
                    "FT-SO-23", "04", "Actualización por vigencia"),
            // C-065 (9-nov-2026): encabezado de «NOTA DE FISIO plataforma.xlsx»
            "FT-SO-38", new Encabezado("NOTA DE FISIOTERAPIA", "14 de octubre del 2026", "00", "FT-SO-38", "N/A",
                    "Implementación"),
            "FT-SO-31", new Encabezado("DICTAMEN DE CERTIFICACIÓN DE APTITUD MEDICA LABORAL", "14 de octubre de 2026", "03",
                    "FT-SO-31", "02", "Consideración de puestos clave y maquila, actualización por vigencia"));

    /** Sin I, O, 0 ni 1: el codigo se lee y se teclea sin confundirse. */
    private static final char[] ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final FirmaService firmas;
    private final DocumentoFirmadoRepository documentos;
    private final String nombreMedico;
    private final String cedulaMedico;
    private final String usuarioMedico;
    private final Set<String> rolesConFirma;
    private final SecureRandom azar = new SecureRandom();

    public FormatosImpresosService(FirmaService firmas, DocumentoFirmadoRepository documentos,
                                   @Value("${portal.formatos.medico-responsable.nombre:}") String nombreMedico,
                                   @Value("${portal.formatos.medico-responsable.cedula:}") String cedulaMedico,
                                   @Value("${portal.formatos.medico-responsable.usuario:}") String usuarioMedico,
                                   @Value("${portal.firmas.roles:}") String rolesConFirma) {
        this.firmas = firmas;
        this.documentos = documentos;
        this.nombreMedico = nombreMedico;
        this.cedulaMedico = cedulaMedico;
        this.usuarioMedico = usuarioMedico == null ? "" : usuarioMedico.trim();
        // vacio = todos los que ya imprimen (decision del usuario 20-oct-2026); se puede acotar despues
        this.rolesConFirma = Arrays.stream((rolesConFirma == null ? "" : rolesConFirma).split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(FormatosImpresosService::sinPrefijo)
                .collect(Collectors.toSet());
    }

    public Encabezado encabezado(String clave) {
        Encabezado e = ENCABEZADOS.get(clave);
        if (e == null) {
            throw new IllegalArgumentException("Formato sin encabezado registrado: " + clave);
        }
        return e;
    }

    /** Medico responsable sin firma (pantallas que solo muestran su nombre y cedula). */
    public MedicoResponsable medicoResponsable() {
        return new MedicoResponsable(nombreMedico, cedulaMedico, null);
    }

    /**
     * Fija el autor de un documento al crearlo (consulta, antidoping, dictamen): quien lo elaboro es el
     * usuario en sesion y el medico responsable es el configurado hoy. Nunca lanza.
     */
    public void registrarAutor(String registro, String nss) {
        try {
            documentos.fijarAutor(autorActual(registro, "ALTA"), nss);
        } catch (DataAccessException ex) {
            log.warn("[firmas] autor de {} no registrado: {}", registro, ex.getMessage());
        }
    }

    /**
     * Pone en el modelo las firmas del impreso: "medico", "elaboro" y, si salio al menos una firma, "folioFirma".
     * {@code registro} identifica el documento (CONSULTA:rel, ANTIDOPING:id, EXAMEN:nss:fecha, FTSO31:nss:fecha).
     */
    public void aplicarFirmas(Model model, String formato, String registro, String nss) {
        Autor autor;
        try {
            autor = documentos.autor(registro).orElseGet(() -> documentos.fijarAutor(autorActual(registro, "PRIMERA_IMPRESION"), nss));
        } catch (DataAccessException ex) {
            // modulo sin tablas (SQL aun no corrido): como antes, sin firmas ni folio
            log.warn("[firmas] sin tablas del modulo de firmas ({}); impreso sin firmas", ex.getMessage());
            model.addAttribute("medico", medicoResponsable());
            model.addAttribute("elaboro", new Elaboro(DocumentoImpresoService.usuarioActual(), null));
            return;
        }
        boolean puede = puedeImprimirConFirma();
        Optional<FirmaImpresa> fMedico = puede ? firmas.paraImprimir(autor.medicoUsuario()) : Optional.empty();
        Optional<FirmaImpresa> fElaboro = puede ? firmas.paraImprimir(autor.elaboroUsuario()) : Optional.empty();
        MedicoResponsable medico = new MedicoResponsable(texto(autor.medicoNombre(), nombreMedico),
                texto(autor.medicoCedula(), cedulaMedico), fMedico.map(FirmaImpresa::dataUri).orElse(null));
        Elaboro elaboro = new Elaboro(texto(autor.elaboroNombre(), ""), fElaboro.map(FirmaImpresa::dataUri).orElse(null));
        model.addAttribute("medico", medico);
        model.addAttribute("elaboro", elaboro);
        if (fMedico.isPresent() || fElaboro.isPresent()) {
            try {
                String codigo = codigo();
                long folio = documentos.registrarImpreso(codigo, formato, registro, nss,
                        fMedico.map(FirmaImpresa::id).orElse(null), fElaboro.map(FirmaImpresa::id).orElse(null),
                        elaboro.nombre(), medico.nombre(), medico.cedula(), usuarioEnSesion());
                model.addAttribute("folioFirma", new Folio(folio, codigo));
            } catch (DataAccessException ex) {
                // sin folio no se entregan firmas: un impreso firmado siempre debe poder verificarse
                log.warn("[firmas] no se pudo registrar el folio de {} {}: {}", formato, registro, ex.getMessage());
                model.addAttribute("medico", new MedicoResponsable(medico.nombre(), medico.cedula(), null));
                model.addAttribute("elaboro", new Elaboro(elaboro.nombre(), null));
            }
        }
    }

    // ---- internos ----

    private Autor autorActual(String registro, String origen) {
        return new Autor(registro, usuarioEnSesion(), DocumentoImpresoService.usuarioActual(), usuarioMedico,
                nombreMedico, cedulaMedico, origen);
    }

    private boolean puedeImprimirConFirma() {
        if (rolesConFirma.isEmpty()) {
            return true;
        }
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .map(FormatosImpresosService::sinPrefijo).anyMatch(rolesConFirma::contains);
    }

    private String codigo() {
        char[] c = new char[8];
        for (int i = 0; i < c.length; i++) {
            c[i] = ALFABETO[azar.nextInt(ALFABETO.length)];
        }
        return new String(c);
    }

    private static String usuarioEnSesion() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }

    private static String sinPrefijo(String rol) {
        String r = rol.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r.substring(5) : r;
    }

    private static String texto(String v, String siVacio) {
        return v == null || v.isBlank() ? siVacio : v;
    }
}
