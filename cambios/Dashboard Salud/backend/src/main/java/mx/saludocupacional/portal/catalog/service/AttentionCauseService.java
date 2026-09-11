package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.AttentionCause;
import mx.saludocupacional.portal.catalog.repository.AttentionCauseRepository;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.AttentionCauseItem;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administración del catálogo de causas de atención.
 *
 * <p>La categoría agrupa causas afines para que el dashboard pueda presentar
 * bloques comparables sin interpretar el nombre de cada una.
 */
@Service
public class AttentionCauseService extends CatalogService<AttentionCause> {

    private final AttentionCauseRepository causeRepository;

    public AttentionCauseService(AttentionCauseRepository repository, AuditService auditService) {
        super(repository, auditService, "AttentionCause", AttentionCause::new);
        this.causeRepository = repository;
    }

    @Transactional(readOnly = true)
    public List<AttentionCauseItem> listarConCategoria(boolean soloActivos) {
        List<AttentionCause> causas = soloActivos
                ? causeRepository.findByActivoTrueOrderByOrdenAscNombreAsc()
                : causeRepository.findAllByOrderByOrdenAscNombreAsc();
        return causas.stream().map(this::aCauseItem).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarCategorias() {
        return causeRepository.findCategorias();
    }

    @Transactional
    public AttentionCauseItem asignarCategoria(Long id, String categoria, Long usuarioId) {
        AttentionCause causa = obtener(id);
        causa.setCategoria(categoria);
        return aCauseItem(causeRepository.save(causa));
    }

    private AttentionCauseItem aCauseItem(AttentionCause causa) {
        return new AttentionCauseItem(
                causa.getId(), causa.getCodigo(), causa.getNombre(),
                causa.getCategoria(), causa.isActivo(), causa.getOrden());
    }
}
