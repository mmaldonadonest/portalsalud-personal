package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.Area;
import mx.saludocupacional.portal.catalog.repository.AreaRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de áreas. */
@Service
public class AreaService extends CatalogService<Area> {

    public AreaService(AreaRepository repository, AuditService auditService) {
        super(repository, auditService, "Area", Area::new);
    }
}
