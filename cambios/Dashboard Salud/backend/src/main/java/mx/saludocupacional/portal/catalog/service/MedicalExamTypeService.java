package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.MedicalExamType;
import mx.saludocupacional.portal.catalog.repository.MedicalExamTypeRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de tipos de examen médico. */
@Service
public class MedicalExamTypeService extends CatalogService<MedicalExamType> {

    public MedicalExamTypeService(MedicalExamTypeRepository repository, AuditService auditService) {
        super(repository, auditService, "MedicalExamType", MedicalExamType::new);
    }
}
