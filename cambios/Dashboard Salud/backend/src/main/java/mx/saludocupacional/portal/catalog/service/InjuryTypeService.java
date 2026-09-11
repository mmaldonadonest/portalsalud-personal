package mx.saludocupacional.portal.catalog.service;

import mx.saludocupacional.portal.catalog.domain.InjuryType;
import mx.saludocupacional.portal.catalog.repository.InjuryTypeRepository;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.InjuryTypeItem;
import mx.saludocupacional.portal.shared.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administración del catálogo de lesiones musculoesqueléticas.
 *
 * <p>La región corporal permite agrupar por miembro torácico, miembro pélvico,
 * columna, cabeza o tronco sin depender de abreviaturas dentro del nombre.
 */
@Service
public class InjuryTypeService extends CatalogService<InjuryType> {

    private final InjuryTypeRepository injuryRepository;

    public InjuryTypeService(InjuryTypeRepository repository, AuditService auditService) {
        super(repository, auditService, "InjuryType", InjuryType::new);
        this.injuryRepository = repository;
    }

    @Transactional(readOnly = true)
    public List<InjuryTypeItem> listarConRegion(boolean soloActivos) {
        List<InjuryType> tipos = soloActivos
                ? injuryRepository.findByActivoTrueOrderByOrdenAscNombreAsc()
                : injuryRepository.findAllByOrderByOrdenAscNombreAsc();
        return tipos.stream().map(this::aInjuryItem).toList();
    }

    private InjuryTypeItem aInjuryItem(InjuryType tipo) {
        return new InjuryTypeItem(
                tipo.getId(), tipo.getCodigo(), tipo.getNombre(),
                tipo.getRegionCorporal(), tipo.isActivo(), tipo.getOrden());
    }
}
