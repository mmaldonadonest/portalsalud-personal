package com.onest.app.catalog.exameninicial.service;

import com.onest.app.catalog.examen.dto.ContactoEmergenciaDto;
import com.onest.app.catalog.examen.dto.DatosGeneralesExamenDto;
import com.onest.app.catalog.examen.dto.ExamItem;
import com.onest.app.catalog.examen.service.ContactoEmergenciaService;
import com.onest.app.catalog.examen.service.DatosGeneralesExamenService;
import com.onest.app.catalog.examen.repository.ExamenHistoricoRepository;
import com.onest.app.catalog.examen.service.ExamenService;
import com.onest.app.catalog.exameninicial.repository.ExamenBorradorRepository;
import com.onest.app.catalog.exameninicial.repository.ExamenBorradorRepository.Borrador;
import com.onest.app.catalog.exameninicial.repository.ExamenBorradorRepository.Finalizado;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.nss.service.NssSearchService;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Examen medico inicial (FT-SO-04 rev. 04): captura por hojas, en el orden del formato, con
 * borrador que se guarda solo. Es una PANTALLA distinta sobre el MISMO almacen del modulo
 * Examen medico: al finalizar, los campos viajan al WS Servcio/Medico y a SERV_MED_TAG
 * exactamente igual que desde ese modulo (mismo catalogo de secciones, mismos servicios),
 * con el tipo de examen fijo en Admision. Asi el documento impreso, el dashboard y el
 * modulo general ven el mismo examen; lo unico propio de aqui es el borrador.
 *
 * <p>No redefine ni un campo: las secciones y sus claves salen de {@link ExamenService}
 * (CATALOGO); este servicio solo decide en que hoja va cada seccion.
 */
@Service
public class ExamenInicialService {

    private static final Logger log = LoggerFactory.getLogger(ExamenInicialService.class);

    /**
     * Hoja del asistente. {@code corto} es el nombre de la pestana (mismo ancho para las 10);
     * {@code tipo} dice si la plantilla agrega algo ademas de las secciones.
     */
    public record Hoja(int numero, String corto, String titulo, String ayuda, String tipo, List<String> secciones) {
    }

    /** {@code valores} = fieldName -> valor, para las plantillas que pintan campos sueltos por nombre. */
    public record SeccionVista(String nombre, List<ExamItem> items, Map<String, String> valores) {
    }

    public record HojaVista(Hoja hoja, List<SeccionVista> secciones, int capturados, int total) {
    }

    /** Un empleo anterior (hasta 4, como el formato). Claves = CAMPOS_TRABAJO. */
    public record Trabajo(int indice, Map<String, String> campos) {
    }

    public record CampoTrabajo(String clave, String etiqueta) {
    }

    public record Vista(String nss, EmpleadoDto empleado, List<HojaVista> hojas, int hojaActual,
                        List<ContactoEmergenciaDto> contactos, List<Trabajo> trabajos,
                        DatosGeneralesExamenDto datosGenerales, String firmaGuardada,
                        Borrador borrador, Finalizado ultimoFinalizado, int avance, int capturados, int total,
                        boolean tablaBorrador, Candado candado) {
    }

    /**
     * Candado: el inicial es de nuevo ingreso. Si la persona ya tiene un examen capturado desde
     * el modulo general (o del PHP) y nunca uno inicial, finalizar aqui pisaria un examen
     * vigente: se bloquea. Un inicial previo (p.ej. no apto que se vuelve a examinar) NO
     * bloquea. {@code motivo} vacio = sin candado.
     */
    public record Candado(boolean bloqueado, String motivo) {
        static final Candado ABIERTO = new Candado(false, "");
    }

    /** Orden y agrupacion del FT-SO-04 rev. 04 (hoja 1 de 3 .. 3 de 3 del Excel, partidas en 10 pasos). */
    public static final List<Hoja> HOJAS = List.of(
            new Hoja(1, "Ficha", "Ficha de identificación", "Datos del trabajador (vienen del biométrico) y teléfonos de emergencia.", "FICHA", List.of()),
            new Hoja(2, "Laborales", "Antecedentes laborales", "Edad al iniciar, cuántos trabajos, pensión y hasta 4 empleos anteriores.", "LABORAL",
                    List.of("Antecedentes laborales (resumen)")),
            new Hoja(3, "Heredofamiliares", "Antecedentes heredofamiliares", "Marca Sí solo donde haya antecedente; «Todo normal» pone No en el resto.", "SECCIONES",
                    List.of("Neurología", "Cardiopatía", "Neumopatía", "Toxicológico", "Nefropatías", "Endocrinas", "Obesidad", "Mentales", "Generales", "Otras")),
            new Hoja(4, "No patológicos", "No patológicos, inmunizaciones y gineco-obstétricos", "", "SECCIONES",
                    List.of("APNP (no patológicos)", "Inmunizaciones", "Gineco-obstétricos (AGO)")),
            new Hoja(5, "Patológicos", "Antecedentes personales patológicos", "Marca Sí solo donde haya antecedente; «Todo normal» pone No en el resto.", "SECCIONES",
                    List.of("APP: personales patológicos", "APP: oftalmológico", "APP: digestivo", "APP: renal", "APP: sistema nervioso",
                            "APP: músculo-esquelético", "APP: cardiovascular", "APP: toxicológico", "APP: endocrino")),
            new Hoja(6, "Padecimiento", "Padecimiento actual e interrogatorio", "", "SECCIONES",
                    List.of("Padecimiento actual", "Interrogatorio por aparatos")),
            new Hoja(7, "Exploración", "Exploración física", "Signos vitales, cabeza y cuello. «Todo normal» llena con Normal lo que esté vacío.", "EXPLORACION",
                    List.of("Exploración física", "Cráneo", "Agudeza visual", "Oídos", "Cuello", "Nariz", "Tórax")),
            new Hoja(8, "Columna y piel", "Columna, abdomen, extremidades y piel", "«Todo normal» llena con Normal lo que esté vacío.", "SECCIONES",
                    List.of("Columna vertebral", "Columna (movilidad)", "Abdomen", "Genitales", "Urinario", "Extremidades", "Piel")),
            new Hoja(9, "Boca y dientes", "Boca y dientes", "Dientes en notación FDI (11-48). Solo anota los que tengan hallazgo.", "SECCIONES",
                    List.of("Boca", "Dientes")),
            new Hoja(10, "Cierre", "Estudios, diagnóstico, plan y resultado", "Diagnóstico del catálogo ICD, dictamen y firma del trabajador.", "CIERRE",
                    List.of("Estudios realizados", "Diagnóstico", "Plan terapéutico", "Resultado del examen")));

    public static final List<CampoTrabajo> CAMPOS_TRABAJO = List.of(
            new CampoTrabajo("empresa", "Nombre de la empresa"), new CampoTrabajo("giro", "Giro industrial"),
            new CampoTrabajo("puesto", "Puesto"), new CampoTrabajo("turno", "Turno"),
            new CampoTrabajo("antiguedad", "Antigüedad"), new CampoTrabajo("salida", "Salida (año)"),
            new CampoTrabajo("descripcion", "Descripción de puesto"), new CampoTrabajo("riesgos", "Riesgos"),
            new CampoTrabajo("epp", "EPP utilizado"), new CampoTrabajo("observaciones", "Obs."));

    private static final int MAX_TRABAJOS = 4;
    private static final String TIPO_ADMISION = "1";

    private final ExamenService examenService;
    private final ContactoEmergenciaService contactoEmergenciaService;
    private final DatosGeneralesExamenService datosGeneralesExamenService;
    private final NssSearchService nssSearchService;
    private final MedTagRepository tags;
    private final ExamenBorradorRepository borradores;
    private final ExamenHistoricoRepository historico;

    public ExamenInicialService(ExamenService examenService, ContactoEmergenciaService contactoEmergenciaService,
                                DatosGeneralesExamenService datosGeneralesExamenService, NssSearchService nssSearchService,
                                MedTagRepository tags, ExamenBorradorRepository borradores, ExamenHistoricoRepository historico) {
        this.examenService = examenService;
        this.contactoEmergenciaService = contactoEmergenciaService;
        this.datosGeneralesExamenService = datosGeneralesExamenService;
        this.nssSearchService = nssSearchService;
        this.tags = tags;
        this.borradores = borradores;
        this.historico = historico;
    }

    /**
     * Regla del candado (3-oct-2026): permitido si ya hubo un inicial (historico o constancia
     * del borrador); bloqueado si no lo hubo y la persona ya tiene examen con dictamen, sea
     * por el historico (origen GENERAL) o por el WS (capturado antes de existir el historico).
     */
    public Candado candado(String nss, boolean huboInicialSegunBorrador) {
        boolean huboInicial = huboInicialSegunBorrador;
        ExamenHistoricoRepository.Registro ultimo = null;
        try {
            huboInicial = huboInicial || historico.existeInicial(nss);
            ultimo = historico.ultimo(nss).orElse(null);
        } catch (org.springframework.dao.DataAccessException ex) {
            log.warn("[examen-inicial] SERV_MED_EXAMEN_HISTORICO no disponible: {}", ex.getMostSpecificCause().getMessage());
        }
        if (huboInicial) {
            return Candado.ABIERTO;
        }
        if (ultimo != null && ExamenHistoricoRepository.ORIGEN_GENERAL.equals(ultimo.origen())) {
            return new Candado(true, "Este trabajador ya tiene un examen médico capturado en el módulo Examen médico el "
                    + ultimo.fechaExamen().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " ("
                    + etiquetaDictamen(ultimo.dictamen()) + "). El examen inicial es solo de nuevo ingreso.");
        }
        String dictamenWs = examenService.dictamenVigente(nss);
        if (!dictamenWs.isBlank()) {
            return new Candado(true, "Este trabajador ya tiene un examen médico vigente con dictamen "
                    + etiquetaDictamen(dictamenWs) + ". El examen inicial es solo de nuevo ingreso; captura en Examen médico.");
        }
        return Candado.ABIERTO;
    }

    private static String etiquetaDictamen(String d) {
        return switch (d == null ? "" : d) {
            case "apto" -> "Apto";
            case "no_apto" -> "No apto";
            case "apto_condicionado" -> "Apto condicionado";
            case "apto_restringido" -> "Apto restringido";
            default -> d == null ? "" : d;
        };
    }

    /**
     * Arma las 10 hojas: lo guardado (WS + tags) como base, y encima lo que haya en el borrador
     * (el borrador siempre gana: es lo mas reciente que escribio el medico).
     */
    public Vista cargar(String nss) {
        String limpio = normalizeNss(nss);
        // Sin la tabla (db/sql/app_domain/examen-inicial.sql sin aplicar) la pantalla abre igual,
        // en modo solo lectura del WS, y avisa que el autoguardado no funciona todavia.
        Optional<Borrador> borrador = Optional.empty();
        Optional<Finalizado> ultimo = Optional.empty();
        boolean tablaBorrador = true;
        try {
            borrador = borradores.buscar(limpio);
            ultimo = borradores.ultimoFinalizado(limpio);
        } catch (org.springframework.dao.DataAccessException ex) {
            tablaBorrador = false;
            log.warn("[examen-inicial] SERV_MED_EXAMEN_BORRADOR no disponible: {}", ex.getMostSpecificCause().getMessage());
        }
        Map<String, String> b = borrador.map(Borrador::datos).orElse(Map.of());

        Map<String, List<ExamItem>> todas = examenService.itemsDeTodas(limpio);
        List<HojaVista> hojas = new ArrayList<>();
        int capturadosTotal = 0;
        int total = 0;
        for (Hoja h : HOJAS) {
            List<SeccionVista> secciones = new ArrayList<>();
            int capturados = 0;
            int n = 0;
            for (String nombre : h.secciones()) {
                List<ExamItem> base = todas.get(nombre);
                if (base == null) {
                    log.warn("[examen-inicial] seccion '{}' no existe en el catalogo del examen", nombre);
                    continue;
                }
                List<ExamItem> items = new ArrayList<>();
                Map<String, String> valores = new LinkedHashMap<>();
                for (ExamItem it : base) {
                    String value = b.containsKey(it.fieldName()) ? b.get(it.fieldName()) : it.value();
                    String obs = it.obsName() != null && b.containsKey(it.obsName()) ? b.get(it.obsName()) : it.obsValue();
                    items.add(new ExamItem(it.type(), it.label(), it.fieldName(), value, it.obsName(), obs));
                    valores.put(it.fieldName(), value == null ? "" : value);
                    n++;
                    if (value != null && !value.isBlank()) {
                        capturados++;
                    }
                }
                secciones.add(new SeccionVista(nombre, items, valores));
            }
            hojas.add(new HojaVista(h, secciones, capturados, n));
            capturadosTotal += capturados;
            total += n;
        }

        EmpleadoDto empleado = null;
        try {
            empleado = nssSearchService.findByNss(limpio).map(r -> r.empleado()).orElse(null);
        } catch (RuntimeException ex) {
            log.warn("[examen-inicial] sin ficha del empleado {}: {}", limpio, ex.getMessage());
        }

        Map<String, String> t;
        try {
            t = tags.latestByNssAndTypeSuffix(limpio, "");
        } catch (RuntimeException ex) {
            t = Map.of();
        }

        List<ContactoEmergenciaDto> contactos = new ArrayList<>();
        for (ContactoEmergenciaDto c : contactoEmergenciaService.cargar(limpio)) {
            String p = "contactoEmer";
            String i = String.valueOf(c.indice());
            contactos.add(new ContactoEmergenciaDto(c.indice(),
                    b.getOrDefault(p + "nombre" + i, c.nombre()),
                    b.getOrDefault(p + "apellido_paterno" + i, c.apellidoPaterno()),
                    b.getOrDefault(p + "apellido_materno" + i, c.apellidoMaterno()),
                    b.getOrDefault(p + "parentesco" + i, c.parentesco()),
                    b.getOrDefault(p + "telefono" + i, c.telefono())));
        }

        // Empleos anteriores: borrador > tags del legacy (nombre1..4 = empresa, giro1..4, ...).
        // El WS de lectura no expone SERV_MED_DET_ANT_LABORALES, por eso la base son los tags.
        List<Trabajo> trabajos = new ArrayList<>();
        for (int i = 1; i <= MAX_TRABAJOS; i++) {
            Map<String, String> campos = new LinkedHashMap<>();
            for (CampoTrabajo c : CAMPOS_TRABAJO) {
                String clave = claveTrabajo(i, c.clave());
                String deTag = t.get(("empresa".equals(c.clave()) ? "nombre" : c.clave()) + i);
                campos.put(c.clave(), b.getOrDefault(clave, deTag == null ? "" : deTag.trim()));
            }
            trabajos.add(new Trabajo(i, campos));
        }

        DatosGeneralesExamenDto dg = datosGeneralesExamenService.cargar(limpio);
        DatosGeneralesExamenDto datosGenerales = new DatosGeneralesExamenDto(TIPO_ADMISION,
                b.getOrDefault("spo2", dg.spo2()), b.getOrDefault("cintura", dg.cintura()), b.getOrDefault("cadera", dg.cadera()));

        int hojaActual = borrador.map(Borrador::hojaActual).filter(x -> x >= 1 && x <= HOJAS.size()).orElse(1);
        int avance = total == 0 ? 0 : (int) Math.round(100.0 * capturadosTotal / total);
        Candado candado = candado(limpio, ultimo.isPresent());
        return new Vista(limpio, empleado, hojas, hojaActual, contactos, trabajos, datosGenerales,
                examenService.firmaGuardada(limpio), borrador.orElse(null),
                ultimo.orElse(null), avance, capturadosTotal, total, tablaBorrador, candado);
    }

    /** Autoguardado: funde los campos recibidos con el borrador existente y recuerda la hoja. */
    public void guardarBorrador(String nss, int hoja, Map<String, String> datos) {
        String limpio = normalizeNss(nss);
        Map<String, String> fusion = new LinkedHashMap<>(borradores.buscar(limpio).map(Borrador::datos).orElse(Map.of()));
        if (datos != null) {
            datos.forEach((k, v) -> {
                if (k != null && !k.isBlank() && !"nss".equals(k) && !"_csrf".equals(k)) {
                    fusion.put(k, v == null ? "" : v);
                }
            });
        }
        int h = hoja < 1 || hoja > HOJAS.size() ? 1 : hoja;
        borradores.guardar(limpio, h, fusion, usuarioActual());
    }

    /**
     * Finaliza: reparte el borrador entre los mismos destinos que usa el modulo Examen medico y
     * cierra el borrador. Devuelve el mensaje del WS.
     */
    public String finalizar(String nss, String firma) {
        String limpio = normalizeNss(nss);
        Borrador borrador = borradores.buscar(limpio)
                .orElseThrow(() -> new IllegalArgumentException("No hay nada capturado para finalizar"));
        Map<String, String> b = borrador.datos();

        // El candado se vuelve a evaluar aqui, no solo en pantalla
        Candado candado = candado(limpio, borradores.ultimoFinalizado(limpio).isPresent());
        if (candado.bloqueado()) {
            throw new IllegalArgumentException(candado.motivo());
        }

        String dictamen = b.getOrDefault("SERV_MED_RESULTADO_EXAMEN.DICTAMEN", "");
        if (dictamen.isBlank()) {
            throw new IllegalArgumentException("Selecciona el resultado del examen (hoja 10) antes de finalizar");
        }
        // Regla del PO (misma que en el modulo general): dictamen sin diagnostico ICD no se guarda
        if (b.getOrDefault("SERV_MED_DIAGNOSTICO.OBSERVACIONES", "").isBlank()) {
            throw new IllegalArgumentException("Captura el diagnóstico desde el catálogo ICD (hoja 10) antes de finalizar");
        }

        // 1) WS Servcio/Medico: claves del catalogo + empleos anteriores como trabajos[i].campo
        Set<String> clavesWs = ExamenService.clavesEscritura();
        Map<String, String> camposWs = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : b.entrySet()) {
            if (clavesWs.contains(e.getKey()) || e.getKey().endsWith(".DICTAMEN")) {
                camposWs.put(e.getKey(), e.getValue());
            }
        }
        Map<String, String> tagsTrabajo = new LinkedHashMap<>();
        for (int i = 1; i <= MAX_TRABAJOS; i++) {
            boolean vacio = true;
            for (CampoTrabajo c : CAMPOS_TRABAJO) {
                String v = b.getOrDefault(claveTrabajo(i, c.clave()), "");
                if (!v.isBlank()) {
                    vacio = false;
                }
            }
            if (vacio) {
                continue;
            }
            int idx = i - 1;
            // El WS no tiene columna para el nombre de la empresa: "nombre" ahi es el numero de trabajo
            camposWs.put("trabajos[" + idx + "].nombre", String.valueOf(i));
            for (CampoTrabajo c : CAMPOS_TRABAJO) {
                String v = b.getOrDefault(claveTrabajo(i, c.clave()), "");
                if (!"empresa".equals(c.clave())) {
                    camposWs.put("trabajos[" + idx + "]." + c.clave(), v);
                }
                // tags del legacy (nombre{i} = empresa): es lo que el documento impreso lee
                tagsTrabajo.put(("empresa".equals(c.clave()) ? "nombre" : c.clave()) + i, v);
            }
        }
        String firmaFinal = firma != null && !firma.isBlank() ? firma : null;
        // El tag de tipo se escribe ANTES del guardado para que la foto del historico lo lleve
        datosGeneralesExamenService.guardar(limpio, Map.of("tipoExamen", TIPO_ADMISION,
                "spo2", b.getOrDefault("spo2", ""), "cintura", b.getOrDefault("cintura", ""), "cadera", b.getOrDefault("cadera", "")));
        String proceso = examenService.guardar(limpio, camposWs, firmaFinal, ExamenHistoricoRepository.ORIGEN_INICIAL);

        // 2) SERV_MED_TAG: tipo Admision + SpO2/cintura/cadera ya se escribieron antes del guardado (arriba)

        // 3) Contactos de emergencia: solo si la hoja 1 se toco (el servicio pisa los 15 campos)
        if (b.keySet().stream().anyMatch(k -> k.startsWith("contactoEmer"))) {
            Map<String, String> contactos = new LinkedHashMap<>();
            b.forEach((k, v) -> {
                if (k.startsWith("contactoEmer")) {
                    contactos.put(k, v);
                }
            });
            contactoEmergenciaService.guardar(limpio, contactos);
        }

        // 4) Tags de antecedentes laborales que lee el impreso (resumen + 4 empleos)
        String usuario = usuarioActual();
        copiarTag(b, "SERV_ANTECEDENTESLAB.edad_inicio_laboral", "edad_inicio_laborar", limpio, usuario);
        copiarTag(b, "SERV_ANTECEDENTESLAB.cantidad_trabajos", "cantidad_trabajos", limpio, usuario);
        copiarTag(b, "SERV_ANTECEDENTESLAB.pension", "pension", limpio, usuario);
        tagsTrabajo.forEach((type, v) -> tags.upsert(limpio, type, v, "HISTORIA_LABORAL", usuario));

        borradores.finalizar(limpio, usuario);
        return proceso;
    }

    private void copiarTag(Map<String, String> b, String claveBorrador, String type, String nss, String usuario) {
        if (b.containsKey(claveBorrador)) {
            tags.upsert(nss, type, b.get(claveBorrador), "HISTORIA_LABORAL", usuario);
        }
    }

    public static String claveTrabajo(int indice, String campo) {
        return "trabajo" + indice + "." + campo;
    }

    private String normalizeNss(String nss) {
        if (nss == null || nss.isBlank()) {
            throw new IllegalArgumentException("El NSS es obligatorio");
        }
        return nss.trim();
    }

    private String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
