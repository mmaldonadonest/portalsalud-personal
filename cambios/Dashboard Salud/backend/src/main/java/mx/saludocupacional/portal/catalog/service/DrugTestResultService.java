package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.DrugTestResult;
import mx.saludocupacional.portal.catalog.repository.DrugTestResultRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de resultados de prueba. */
@Service
public class DrugTestResultService extends CatalogService<DrugTestResult> {

    public DrugTestResultService(DrugTestResultRepository repository, AuditService auditService) {
        super(repository, auditService, "DrugTestResult", DrugTestResult::new);
    }
}
