package com.onest.app.catalog.firma.service;

import com.onest.app.audit.service.AuditoriaService;
import com.onest.app.catalog.file.repository.FsFileRepository;
import com.onest.app.catalog.file.storage.StorageProvider;
import com.onest.app.catalog.file.storage.StoredBinary;
import com.onest.app.catalog.firma.repository.FirmaRepository;
import com.onest.app.catalog.firma.repository.FirmaRepository.Aviso;
import com.onest.app.catalog.firma.repository.FirmaRepository.Firma;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Firmas digitalizadas (paso 8, 20-oct-2026): cada persona registra SU firma (dibujada o imagen),
 * acepta el aviso de privacidad y puede revocarla; un administrador puede cargar una firma existente,
 * que queda PENDIENTE hasta que su dueno acepte el aviso, y puede desactivar firmas. La imagen se
 * limpia (ImagenFirma), se cifra (CifradoFirma) y va al almacen de archivos; nunca tiene direccion
 * propia: solo sale como data URI dentro de la pagina o del impreso.
 *
 * <p>Una firma se imprime solo si: hay llave, esta VIGENTE, su dueno acepto la version vigente del
 * aviso y sigue activo en el biometrico (EMP_STATUS = 0). Si algo falla, el impreso sale con la linea
 * para firmar a mano: nunca se rompe la impresion por la firma.
 */
@Service
public class FirmaService {

    private static final Logger log = LoggerFactory.getLogger(FirmaService.class);
    public static final String FILE_TYPE = "firma_digitalizada";
    private static final String MODULO = "Firmas";

    /** Lo que ve una persona en «Mi firma digitalizada». */
    public record MiFirma(boolean habilitado, Optional<Aviso> aviso, Optional<Firma> vigente, Optional<Firma> pendiente,
                          String imagenVigente, String imagenPendiente, boolean empleadoActivo, boolean debeReaceptar) {
    }

    /** Firma lista para imprimir: id (para el folio) e imagen como data URI. */
    public record FirmaImpresa(long id, String dataUri) {
    }

    private final FirmaRepository repo;
    private final CifradoFirma cifrado;
    private final StorageProvider storage;
    private final FsFileRepository archivos;
    private final AuditoriaService auditoria;

    public FirmaService(FirmaRepository repo, CifradoFirma cifrado, StorageProvider storage, FsFileRepository archivos,
                        AuditoriaService auditoria) {
        this.repo = repo;
        this.cifrado = cifrado;
        this.storage = storage;
        this.archivos = archivos;
        this.auditoria = auditoria;
    }

    public boolean habilitado() {
        return cifrado.habilitado();
    }

    public Optional<Aviso> aviso() {
        return repo.avisoVigente();
    }

    public MiFirma miFirma(String usuario) {
        Optional<Aviso> aviso = repo.avisoVigente();
        Optional<Firma> vigente = repo.enEstado(usuario, "VIGENTE");
        Optional<Firma> pendiente = repo.enEstado(usuario, "PENDIENTE");
        boolean reaceptar = vigente.isPresent() && aviso.isPresent()
                && !Integer.valueOf(aviso.get().version()).equals(vigente.get().avisoVersion());
        return new MiFirma(cifrado.habilitado(), aviso, vigente, pendiente,
                vigente.map(this::dataUriSegura).orElse(null), pendiente.map(this::dataUriSegura).orElse(null),
                activoSeguro(usuario), reaceptar);
    }

    /** La persona registra su propia firma (dibujo o imagen) aceptando el aviso vigente. */
    @Transactional
    public void registrarPropia(String usuario, String nombre, byte[] original, String origen, int avisoVersionLeida,
                                HttpServletRequest request) {
        Aviso aviso = exigirAviso(avisoVersionLeida);
        long fileId = guardarCifrada(usuario, ImagenFirma.normalizar(original), usuario);
        repo.enEstado(usuario, "PENDIENTE").ifPresent(f -> repo.cambiarEstado(f.id(), "PENDIENTE", "SUSTITUIDA", usuario,
                "La persona registro su propia firma"));
        repo.enEstado(usuario, "VIGENTE").ifPresent(f -> repo.cambiarEstado(f.id(), "VIGENTE", "SUSTITUIDA", usuario,
                "La persona registro una firma nueva"));
        long id = repo.insertar(usuario, nombre, fileId, origen, "VIGENTE", aviso.version(), ip(request), usuario);
        auditoria.registrar(MODULO, "create", "Firma digitalizada", usuario,
                "Registro su firma (" + origen + ", id " + id + ") aceptando el aviso v" + aviso.version(), request);
    }

    /** Un administrador carga una firma existente (escaneo); queda PENDIENTE hasta que su dueno acepte el aviso. */
    @Transactional
    public void cargarPorAdmin(String usuarioDestino, String nombreDestino, byte[] original, String admin,
                               HttpServletRequest request) {
        if (usuarioDestino == null || usuarioDestino.isBlank()) {
            throw new IllegalArgumentException("Indica el usuario dueño de la firma.");
        }
        String destino = usuarioDestino.trim();
        long fileId = guardarCifrada(destino, ImagenFirma.normalizar(original), admin);
        repo.enEstado(destino, "PENDIENTE").ifPresent(f -> repo.cambiarEstado(f.id(), "PENDIENTE", "SUSTITUIDA", admin,
                "Un administrador cargo otra firma pendiente"));
        long id = repo.insertar(destino, nombreDestino, fileId, "CARGA_ADMIN", "PENDIENTE", null, null, admin);
        auditoria.registrar(MODULO, "create", "Firma digitalizada", destino,
                "Firma cargada por administrador (id " + id + "); pendiente de que su dueno acepte el aviso", request);
    }

    /** El dueno acepta el aviso: su firma pendiente pasa a VIGENTE, o su vigente queda al dia con el aviso nuevo. */
    @Transactional
    public void aceptar(String usuario, int avisoVersionLeida, HttpServletRequest request) {
        Aviso aviso = exigirAviso(avisoVersionLeida);
        Optional<Firma> pendiente = repo.enEstado(usuario, "PENDIENTE");
        if (pendiente.isPresent()) {
            repo.enEstado(usuario, "VIGENTE").ifPresent(f -> repo.cambiarEstado(f.id(), "VIGENTE", "SUSTITUIDA", usuario,
                    "Activo la firma cargada por un administrador"));
            repo.aceptar(pendiente.get().id(), aviso.version(), ip(request), usuario, "VIGENTE");
            auditoria.registrar(MODULO, "update", "Firma digitalizada", usuario,
                    "Acepto el aviso v" + aviso.version() + " y activo la firma " + pendiente.get().id(), request);
            return;
        }
        Firma vigente = repo.enEstado(usuario, "VIGENTE")
                .orElseThrow(() -> new IllegalArgumentException("No tienes una firma registrada."));
        repo.aceptar(vigente.id(), aviso.version(), ip(request), usuario, "VIGENTE");
        auditoria.registrar(MODULO, "update", "Firma digitalizada", usuario,
                "Acepto el aviso v" + aviso.version() + " para la firma " + vigente.id(), request);
    }

    /** El dueno retira su permiso: su firma deja de salir en formatos nuevos. */
    @Transactional
    public void revocar(String usuario, HttpServletRequest request) {
        Firma vigente = repo.enEstado(usuario, "VIGENTE")
                .orElseThrow(() -> new IllegalArgumentException("No tienes una firma vigente."));
        repo.cambiarEstado(vigente.id(), "VIGENTE", "REVOCADA", usuario, "La persona retiro su permiso");
        auditoria.registrar(MODULO, "update", "Firma digitalizada", usuario, "Revoco su firma " + vigente.id(), request);
    }

    /** Un administrador apaga una firma vigente o pendiente (p.ej. la persona dejo de trabajar ahi). */
    @Transactional
    public void desactivar(long id, String admin, String motivo, HttpServletRequest request) {
        Firma f = repo.porId(id).orElseThrow(() -> new IllegalArgumentException("No existe la firma " + id + "."));
        if (!"VIGENTE".equals(f.estado()) && !"PENDIENTE".equals(f.estado())) {
            throw new IllegalArgumentException("La firma " + id + " ya no está activa.");
        }
        String m = motivo == null || motivo.isBlank() ? "Desactivada por un administrador" : motivo.trim();
        repo.cambiarEstado(id, f.estado(), "DESACTIVADA", admin, m);
        auditoria.registrar(MODULO, "update", "Firma digitalizada", f.usuario(), "Desactivo la firma " + id + ": " + m, request);
    }

    public List<Firma> todas() {
        return repo.todas();
    }

    /**
     * Firma para imprimir, solo si cumple todo (llave, VIGENTE, aviso vigente aceptado, empleado activo).
     * Nunca lanza: ante cualquier problema el impreso sale con la linea para firmar a mano.
     */
    public Optional<FirmaImpresa> paraImprimir(String usuario) {
        if (usuario == null || usuario.isBlank() || !cifrado.habilitado()) {
            return Optional.empty();
        }
        try {
            Optional<Firma> f = repo.enEstado(usuario.trim(), "VIGENTE");
            Optional<Aviso> aviso = repo.avisoVigente();
            if (f.isEmpty() || aviso.isEmpty() || !Integer.valueOf(aviso.get().version()).equals(f.get().avisoVersion())
                    || !repo.empleadoActivo(usuario.trim())) {
                return Optional.empty();
            }
            return Optional.ofNullable(dataUriSegura(f.get())).map(uri -> new FirmaImpresa(f.get().id(), uri));
        } catch (DataAccessException | IllegalStateException ex) {
            log.warn("[firmas] firma de {} no disponible para imprimir: {}", usuario, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Revision diaria: apaga las firmas vigentes de quien ya no es empleado activo en el biometrico. */
    public int revisarVigencias() {
        int apagadas = 0;
        for (Firma f : repo.vigentes()) {
            if (!repo.empleadoActivo(f.usuario())) {
                apagadas += repo.cambiarEstado(f.id(), "VIGENTE", "DESACTIVADA", "SISTEMA",
                        "Revision diaria: ya no es empleado activo en el biometrico (EMP_STATUS distinto de 0)");
                auditoria.registrar(MODULO, "update", "Firma digitalizada", f.usuario(),
                        "Revision diaria: firma " + f.id() + " desactivada, ya no es empleado activo", null);
            }
        }
        return apagadas;
    }

    // ---- internos ----

    private Aviso exigirAviso(int versionLeida) {
        Aviso aviso = repo.avisoVigente()
                .orElseThrow(() -> new IllegalArgumentException("Todavía no hay aviso de privacidad publicado; no se pueden registrar firmas."));
        if (aviso.version() != versionLeida) {
            throw new IllegalArgumentException("El aviso de privacidad cambió mientras lo leías; vuelve a cargar la página.");
        }
        if (!cifrado.habilitado()) {
            throw new IllegalArgumentException("Las firmas digitalizadas están apagadas en este servidor (falta la llave).");
        }
        return aviso;
    }

    private long guardarCifrada(String dueno, byte[] png, String quien) {
        if (!cifrado.habilitado()) {
            throw new IllegalArgumentException("Las firmas digitalizadas están apagadas en este servidor (falta la llave).");
        }
        byte[] cifrada = cifrado.cifrar(png, dueno);
        StoredBinary s = storage.store(cifrada, "enc");
        return archivos.insert(dueno, UUID.randomUUID().toString(), "firma-" + dueno + ".enc", "enc",
                "application/octet-stream", s.sizeBytes(), s.checksumSha256(), s.storagePath(), s.storageProvider(),
                FILE_TYPE, quien);
    }

    /** data URI de la firma descifrada, o null si no se pudo (llave distinta, archivo faltante...). */
    private String dataUriSegura(Firma f) {
        try {
            byte[] guardado = archivos.findStoragePathById(f.fileId()).map(storage::read).orElse(null);
            if (guardado == null) {
                return null;
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(cifrado.descifrar(guardado, f.usuario()));
        } catch (RuntimeException ex) {
            log.warn("[firmas] no se pudo leer la firma {} de {}: {}", f.id(), f.usuario(), ex.getMessage());
            return null;
        }
    }

    private boolean activoSeguro(String usuario) {
        try {
            return repo.empleadoActivo(usuario);
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private static String ip(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
