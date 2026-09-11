package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.MedicalExamResult;
import mx.saludocupacional.portal.catalog.repository.MedicalExamResultRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de resultados de examen médico. */
@Service
public class MedicalExamResultService extends CatalogService<MedicalExamResult> {

    public MedicalExamResultService(MedicalExamResultRepository repository, AuditService auditService) {
        super(repository, auditService, "MedicalExamResult", MedicalExamResult::new);
    }
}
