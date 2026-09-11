package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.Cuenta;
import mx.saludocupacional.portal.catalog.repository.CuentaRepository;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;

/** Administración del catálogo de cuentas o clientes. */
@Service
public class CuentaService extends CatalogService<Cuenta> {

    public CuentaService(CuentaRepository repository, AuditService auditService) {
        super(repository, auditService, "Cuenta", Cuenta::new);
    }
}
