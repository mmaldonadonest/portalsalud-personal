package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.DrugTestType;
import mx.saludocupacional.portal.catalog.repository.DrugTestTypeRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de tipos de prueba. */
@Service
public class DrugTestTypeService extends CatalogService<DrugTestType> {

    public DrugTestTypeService(DrugTestTypeRepository repository, AuditService auditService) {
        super(repository, auditService, "DrugTestType", DrugTestType::new);
    }
}
