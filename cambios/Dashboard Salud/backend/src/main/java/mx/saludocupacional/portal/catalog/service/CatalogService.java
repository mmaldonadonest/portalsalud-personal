package mx.saludocupacional.portal.catalog.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.domain.CatalogEntity;
import mx.saludocupacional.portal.catalog.repository.CatalogRepository;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.CatalogItem;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.CatalogRequest;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

/**
 * Operaciones comunes a todos los catálogos maestros.
 *
 * <p>Los diecisiete catálogos se administran igual: listar, crear, editar y
 * desactivar. Esta clase concentra esa lógica una sola vez; cada catálogo
 * concreto solo aporta su repositorio y su constructor de entidad.
 *
 * <p>Un catálogo nunca se elimina físicamente: se desactiva. Borrarlo rompería
 * los registros históricos que lo referencian.
 *
 * @param <T> entidad de catálogo concreta
 */
@RequiredArgsConstructor
public abstract class CatalogService<T extends CatalogEntity> {

    private final CatalogRepository<T> repository;
    private final AuditService auditService;
    private final String nombreCatalogo;
    private final Supplier<T> constructor;

    @Transactional(readOnly = true)
    public List<CatalogItem> listar(boolean soloActivos) {
        List<T> elementos = soloActivos
                ? repository.findByActivoTrueOrderByOrdenAscNombreAsc()
                : repository.findAllByOrderByOrdenAscNombreAsc();
        return elementos.stream().map(this::aItem).toList();
    }

    @Transactional(readOnly = true)
    public T obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(nombreCatalogo, id));
    }

    @Transactional
    public CatalogItem crear(CatalogRequest request, Long usuarioId) {
        if (repository.existsByNombreIgnoreCase(request.nombre())) {
            throw new BusinessRuleException(
                    "Ya existe un registro con el nombre «%s» en %s".formatted(request.nombre(), nombreCatalogo));
        }
        T entidad = constructor.get();
        aplicar(entidad, request);
        T guardada = repository.save(entidad);

        auditService.registrar(AuditAction.CREATE, "catalogos", nombreCatalogo,
                guardada.getId(), null, aItem(guardada), usuarioId, false);
        return aItem(guardada);
    }

    @Transactional
    public CatalogItem actualizar(Long id, CatalogRequest request, Long usuarioId) {
        T entidad = obtener(id);
        CatalogItem anterior = aItem(entidad);

        repository.findByNombreIgnoreCase(request.nombre())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessRuleException(
                            "Ya existe otro registro con el nombre «%s»".formatted(request.nombre()));
                });

        aplicar(entidad, request);
        T guardada = repository.save(entidad);

        auditService.registrar(AuditAction.UPDATE, "catalogos", nombreCatalogo,
                id, anterior, aItem(guardada), usuarioId, false);
        return aItem(guardada);
    }

    /** Desactiva el elemento; los registros que lo referencian permanecen intactos. */
    @Transactional
    public void desactivar(Long id, Long usuarioId) {
        T entidad = obtener(id);
        if (!entidad.isActivo()) {
            return;
        }
        CatalogItem anterior = aItem(entidad);
        entidad.setActivo(false);
        repository.save(entidad);

        auditService.registrar(AuditAction.UPDATE, "catalogos", nombreCatalogo,
                id, anterior, aItem(entidad), usuarioId, false);
    }

    @Transactional
    public void reactivar(Long id, Long usuarioId) {
        T entidad = obtener(id);
        if (entidad.isActivo()) {
            return;
        }
        entidad.setActivo(true);
        repository.save(entidad);

        auditService.registrar(AuditAction.UPDATE, "catalogos", nombreCatalogo,
                id, null, aItem(entidad), usuarioId, false);
    }

    protected void aplicar(T entidad, CatalogRequest request) {
        entidad.setCodigo(request.codigo());
        entidad.setNombre(request.nombre());
        if (request.activo() != null) {
            entidad.setActivo(request.activo());
        }
        if (request.orden() != null) {
            entidad.setOrden(request.orden());
        }
    }

    protected CatalogItem aItem(T entidad) {
        return new CatalogItem(
                entidad.getId(), entidad.getCodigo(), entidad.getNombre(),
                entidad.isActivo(), entidad.getOrden());
    }

    protected CatalogRepository<T> repositorio() {
        return repository;
    }
}
