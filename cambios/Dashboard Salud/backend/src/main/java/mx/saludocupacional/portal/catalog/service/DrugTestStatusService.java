package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.DrugTestStatus;
import mx.saludocupacional.portal.catalog.repository.DrugTestStatusRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de estados de seguimiento de prueba. */
@Service
public class DrugTestStatusService extends CatalogService<DrugTestStatus> {

    public DrugTestStatusService(DrugTestStatusRepository repository, AuditService auditService) {
        super(repository, auditService, "DrugTestStatus", DrugTestStatus::new);
    }
}
