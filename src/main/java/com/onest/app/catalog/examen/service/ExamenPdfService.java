package com.onest.app.catalog.examen.service;

import com.onest.app.catalog.examen.repository.ExamenHistoricoRepository;
import com.onest.app.catalog.file.repository.FsFileRepository;
import com.onest.app.catalog.file.service.FileStoreService;
import com.onest.app.catalog.file.service.NomenclaturaService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * C-008 (nomenclatura de SM, 17 y 18-nov-2026): cada examen guardado con dictamen, y cada Pretest, deja su PDF en
 * Historico E.M. Antes solo el mas reciente tenia PDF, porque el impreso se arma con los datos vigentes; ahora el
 * PDF se genera justo despues de guardar (los datos vigentes SON los de ese registro) y se queda como archivo.
 *
 * <p>Nombre: PREFIJO-MES-AAAA-INICIALES.pdf con la fecha del registro (P-009). Prefijo por tipo de examen (P-005):
 * EMNI nuevo ingreso, EMP periodico, EMCP cambio de puesto, EMPI post incapacidad, EME especial, EM-EGR egreso,
 * EMMTTO inicial mantenimiento, EMPMT periodico mantenimiento; Pretest PRTS (P-006).
 *
 * <p>El archivo va con FILE_TYPE "&lt;tipo&gt;&lt;clave del registro&gt;": examen_pdf_&lt;ID de SERV_MED_EXAMEN_HISTORICO&gt; y
 * pretest_pdf_&lt;sello de la copia PRETESTHIST&gt;. Asi cada renglon del historial encuentra su PDF. El mismo
 * registro se guarda varias veces el mismo dia y el historial de examenes muestra solo el ultimo de cada dia: al
 * archivar, los PDF anteriores de ese dia se dan de baja (borrado logico) para que el nombre no crezca a -2, -3
 * por simples correcciones.
 */
@Service
public class ExamenPdfService {

    private static final Logger log = LoggerFactory.getLogger(ExamenPdfService.class);

    public static final String TIPO_ARCHIVO = "examen_pdf_";
    public static final String TIPO_ARCHIVO_PRETEST = "pretest_pdf_";
    public static final String PREFIJO_PRETEST = "PRTS";

    /** Tipo de examen del portal (tag tipoExamenInputO) -> prefijo de SM. */
    public static final Map<String, String> PREFIJOS = Map.of(
            "1", "EMNI", "2", "EMP", "3", "EMCP", "4", "EMPI", "5", "EME", "6", "EM-EGR", "7", "EMMTTO", "8", "EMPMT");
    /** Sin tipo capturado (examenes de antes de que el tipo fuera obligatorio): SM no definio prefijo. */
    static final String PREFIJO_SIN_TIPO = "EM";

    private final ExamenHistoricoRepository historico;
    private final FileStoreService archivos;
    private final NomenclaturaService nomenclatura;

    public ExamenPdfService(ExamenHistoricoRepository historico, FileStoreService archivos,
                            NomenclaturaService nomenclatura) {
        this.historico = historico;
        this.archivos = archivos;
        this.nomenclatura = nomenclatura;
    }

    public static String prefijo(String tipoExamen) {
        return PREFIJOS.getOrDefault(tipoExamen == null ? "" : tipoExamen.trim(), PREFIJO_SIN_TIPO);
    }

    /** ID del registro del historico -> su PDF archivado, para el historial de Historico E.M. */
    public Map<Long, FsFileRepository.ConTipo> pdfsPorExamen(String nss) {
        Map<Long, FsFileRepository.ConTipo> r = new HashMap<>();
        porClave(nss, TIPO_ARCHIVO).forEach((clave, f) -> {
            try {
                r.put(Long.parseLong(clave), f);
            } catch (NumberFormatException ex) {
                // FILE_TYPE ajeno: no es de un examen
            }
        });
        return r;
    }

    /** Sello de la copia del Pretest -> su PDF archivado. */
    public Map<String, FsFileRepository.ConTipo> pdfsPorPretest(String nss) {
        return porClave(nss, TIPO_ARCHIVO_PRETEST);
    }

    /** Clave del registro -> su PDF mas nuevo. */
    private Map<String, FsFileRepository.ConTipo> porClave(String nss, String tipo) {
        Map<String, FsFileRepository.ConTipo> r = new HashMap<>();
        for (FsFileRepository.ConTipo f : archivos.listByNssAndTypePrefix(nss, tipo)) {
            if (f.fileType() != null && f.fileType().startsWith(tipo) && f.fileType().length() > tipo.length()) {
                r.putIfAbsent(f.fileType().substring(tipo.length()), f);   // viene por ID desc: gana el mas nuevo
            }
        }
        return r;
    }

    /**
     * Archiva el PDF del ultimo examen del NSS si aun no lo tiene. {@code generador} arma el PDF del impreso
     * (lo hace el controlador: necesita la peticion web para las plantillas).
     *
     * @return mensaje para el usuario
     */
    public String archivarUltimo(String nss, Supplier<byte[]> generador) {
        String n = nss == null ? "" : nss.trim();
        Optional<ExamenHistoricoRepository.Registro> ultimo = historico.ultimo(n);
        if (ultimo.isEmpty()) {
            throw new IllegalArgumentException("No hay examen guardado con dictamen.");
        }
        ExamenHistoricoRepository.Registro reg = ultimo.get();
        Map<String, LocalDate> dias = new HashMap<>();
        for (ExamenHistoricoRepository.Registro r : historico.listar(n, 500)) {
            dias.put(String.valueOf(r.id()), r.fechaExamen().toLocalDate());
        }
        return archivar(n, TIPO_ARCHIVO, String.valueOf(reg.id()), reg.fechaExamen().toLocalDate(), dias::get,
                prefijo(reg.tipoExamen()), generador);
    }

    /**
     * Archiva el PDF del registro {@code clave} (si ya lo tiene, no hace nada).
     *
     * @param diaDeClave dia de los otros registros del NSS: los del mismo dia se reemplazan
     */
    public String archivar(String nss, String tipo, String clave, LocalDate dia, Function<String, LocalDate> diaDeClave,
                           String prefijo, Supplier<byte[]> generador) {
        Map<String, FsFileRepository.ConTipo> yaArchivados = porClave(nss, tipo);
        FsFileRepository.ConTipo propio = yaArchivados.get(clave);
        if (propio != null) {
            return "El PDF ya estaba guardado como " + propio.meta().name() + ".";
        }
        byte[] pdf = generador.get();
        List<String> existentes = new java.util.ArrayList<>();
        for (Map.Entry<String, FsFileRepository.ConTipo> e : yaArchivados.entrySet()) {
            if (dia.equals(diaDeClave.apply(e.getKey()))) {
                archivos.reemplazado(e.getValue().meta().id());
            } else {
                existentes.add(e.getValue().meta().name());
            }
        }
        String base = nomenclatura.base(prefijo, dia, nss);
        String nombre = NomenclaturaService.nombres(base, "pdf", 1, existentes).get(0);
        archivos.storePdfGenerado(pdf, nss, tipo + clave, nombre);
        log.info("[pdf-archivado] {} archivado para {}{}", nombre, tipo, clave);
        return "PDF guardado en Histórico E.M. como " + nombre + ".";
    }
}
