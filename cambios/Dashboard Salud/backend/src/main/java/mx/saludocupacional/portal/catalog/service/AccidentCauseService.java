package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.AccidentCause;
import mx.saludocupacional.portal.catalog.repository.AccidentCauseRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de causas de accidente. */
@Service
public class AccidentCauseService extends CatalogService<AccidentCause> {

    public AccidentCauseService(AccidentCauseRepository repository, AuditService auditService) {
        super(repository, auditService, "AccidentCause", AccidentCause::new);
    }
}
