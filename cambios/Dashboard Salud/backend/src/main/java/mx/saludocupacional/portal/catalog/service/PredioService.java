package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.catalog.domain.PredioAlias;
import mx.saludocupacional.portal.catalog.repository.PredioRepository;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.PredioItem;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Administración del catálogo de predios.
 *
 * <p>Además de las operaciones comunes, resuelve los nombres alternativos que
 * el importador necesita para reconocer las abreviaturas del archivo origen.
 */
@Service
public class PredioService extends CatalogService<Predio> {

    private final PredioRepository predioRepository;

    public PredioService(PredioRepository repository, AuditService auditService) {
        super(repository, auditService, "Predio", Predio::new);
        this.predioRepository = repository;
    }

    @Transactional(readOnly = true)
    public List<PredioItem> listarConAliases(boolean soloActivos) {
        List<Predio> predios = soloActivos
                ? predioRepository.findByActivoTrueOrderByOrdenAscNombreAsc()
                : predioRepository.findAllByOrderByOrdenAscNombreAsc();
        return predios.stream().map(this::aPredioItem).toList();
    }

    /**
     * Busca un predio por su nombre oficial o por cualquiera de sus alias.
     *
     * <p>El importador lo usa para que «M2» y «MACRO II» apunten al mismo
     * registro en lugar de crear un predio duplicado.
     */
    @Transactional(readOnly = true)
    public Predio resolverPorNombreOAlias(String nombre) {
        return predioRepository.findByNombreOrAlias(nombre.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se reconoce el predio «%s»".formatted(nombre)));
    }

    /** Predios que un usuario puede consultar; lista vacía significa alcance global. */
    @Transactional(readOnly = true)
    public List<PredioItem> listarAccesibles(Long usuarioId, Set<Long> prediosPermitidos) {
        if (prediosPermitidos.isEmpty()) {
            return listarConAliases(true);
        }
        return predioRepository.findAccesiblesPorUsuario(usuarioId).stream()
                .map(this::aPredioItem)
                .toList();
    }

    @Transactional
    public void agregarAlias(Long predioId, String alias, Long usuarioId) {
        Predio predio = obtener(predioId);
        boolean yaExiste = predio.getAliases().stream()
                .anyMatch(a -> a.getAlias().equalsIgnoreCase(alias));
        if (!yaExiste) {
            predio.addAlias(alias.trim().toUpperCase());
            predioRepository.save(predio);
        }
    }

    private PredioItem aPredioItem(Predio predio) {
        return new PredioItem(
                predio.getId(), predio.getCodigo(), predio.getNombre(),
                predio.isActivo(), predio.getOrden(),
                predio.getAliases().stream().map(PredioAlias::getAlias).toList());
    }
}
