package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.Agencia;
import mx.saludocupacional.portal.catalog.repository.AgenciaRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de agencias. */
@Service
public class AgenciaService extends CatalogService<Agencia> {

    public AgenciaService(AgenciaRepository repository, AuditService auditService) {
        super(repository, auditService, "Agencia", Agencia::new);
    }
}
