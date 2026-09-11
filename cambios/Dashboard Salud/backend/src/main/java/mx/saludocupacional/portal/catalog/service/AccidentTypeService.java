package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.AccidentType;
import mx.saludocupacional.portal.catalog.repository.AccidentTypeRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de tipos de riesgo de accidente. */
@Service
public class AccidentTypeService extends CatalogService<AccidentType> {

    public AccidentTypeService(AccidentTypeRepository repository, AuditService auditService) {
        super(repository, auditService, "AccidentType", AccidentType::new);
    }
}
