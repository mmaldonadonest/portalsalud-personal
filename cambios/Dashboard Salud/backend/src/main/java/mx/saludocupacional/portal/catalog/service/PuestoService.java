package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.Puesto;
import mx.saludocupacional.portal.catalog.repository.PuestoRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de puestos. */
@Service
public class PuestoService extends CatalogService<Puesto> {

    public PuestoService(PuestoRepository repository, AuditService auditService) {
        super(repository, auditService, "Puesto", Puesto::new);
    }
}
