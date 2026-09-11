package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.DisabilityType;
import mx.saludocupacional.portal.catalog.repository.DisabilityTypeRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de tipos de incapacidad. */
@Service
public class DisabilityTypeService extends CatalogService<DisabilityType> {

    public DisabilityTypeService(DisabilityTypeRepository repository, AuditService auditService) {
        super(repository, auditService, "DisabilityType", DisabilityType::new);
    }
}
