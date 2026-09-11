package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.AccidentStatus;
import mx.saludocupacional.portal.catalog.repository.AccidentStatusRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de estados de trámite de accidente. */
@Service
public class AccidentStatusService extends CatalogService<AccidentStatus> {

    public AccidentStatusService(AccidentStatusRepository repository, AuditService auditService) {
        super(repository, auditService, "AccidentStatus", AccidentStatus::new);
    }
}
