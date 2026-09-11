package mx.saludocupacional.portal.drugtest.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.repository.PredioRepository;
import mx.saludocupacional.portal.drugtest.domain.DrugTestBatch;
import mx.saludocupacional.portal.drugtest.domain.InventoryMovement;
import mx.saludocupacional.portal.drugtest.domain.InventoryMovement.Tipo;
import mx.saludocupacional.portal.drugtest.repository.DrugTestBatchRepository;
import mx.saludocupacional.portal.drugtest.repository.InventoryMovementRepository;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.AlertaInventario;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.BatchItem;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.BatchRequest;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.MovementItem;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.MovementRequest;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import mx.saludocupacional.portal.shared.service.ThresholdService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Control del inventario de pruebas de antidoping.
 *
 * <p>La existencia de un lote se calcula sumando sus movimientos, nunca se
 * almacena. De ese modo el inventario siempre puede explicarse: cada unidad que
 * falta corresponde a un consumo registrado.
 *
 * <p>Las alertas de caducidad y de existencia mínima se evalúan contra los
 * umbrales administrables, no contra constantes del código.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String MODULO = "inventario";

    private final DrugTestBatchRepository batches;
    private final InventoryMovementRepository movimientos;
    private final PredioRepository predios;
    private final ThresholdService umbrales;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BatchItem> listarLotes(Long predioId) {
        List<DrugTestBatch> lotes = predioId == null
                ? batches.findByActivoTrueOrderByFechaCaducidadAsc()
                : batches.findByPredioIdAndActivoTrueOrderByFechaCaducidadAsc(predioId);
        return lotes.stream().map(this::aItem).toList();
    }

    @Transactional(readOnly = true)
    public BatchItem detalleLote(Long id) {
        return aItem(obtenerLote(id));
    }

    /** Existencia actual del lote: cantidad inicial más el saldo de movimientos. */
    @Transactional(readOnly = true)
    public int disponibleDe(Long loteId) {
        DrugTestBatch lote = obtenerLote(loteId);
        return lote.getCantidadInicial() + movimientos.saldoDeLote(loteId);
    }

    @Transactional
    public BatchItem crearLote(BatchRequest request, Long usuarioId) {
        batches.findByLote(request.lote()).ifPresent(existente -> {
            throw new BusinessRuleException("El lote «%s» ya está registrado".formatted(request.lote()));
        });
        if (request.fechaCaducidad().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("No se puede registrar un lote ya vencido");
        }

        DrugTestBatch lote = new DrugTestBatch();
        lote.setLote(request.lote().trim());
        lote.setFechaCaducidad(request.fechaCaducidad());
        lote.setCantidadInicial(request.cantidadInicial());
        lote.setPredio(predios.findById(request.predioId())
                .orElseThrow(() -> new ResourceNotFoundException("predio", request.predioId())));

        DrugTestBatch guardado = batches.save(lote);
        auditService.registrar(AuditAction.CREATE, MODULO, "DrugTestBatch",
                guardado.getId(), null, aItem(guardado), usuarioId, false);
        return aItem(guardado);
    }

    /** Registra un movimiento manual: recepción, consumo directo o ajuste por conteo. */
    @Transactional
    public MovementItem registrarMovimiento(MovementRequest request, Long usuarioId) {
        DrugTestBatch lote = obtenerLote(request.loteId());

        if (request.tipo() == Tipo.CONSUMO) {
            verificarExistencia(lote, request.cantidad());
        }

        InventoryMovement movimiento = new InventoryMovement();
        movimiento.setBatch(lote);
        movimiento.setPredio(lote.getPredio());
        movimiento.setTipo(request.tipo());
        movimiento.setCantidad(request.cantidad());
        movimiento.setFecha(request.fecha());
        movimiento.setReferencia(request.referencia());
        movimiento.setCreatedBy(usuarioId);

        InventoryMovement guardado = movimientos.save(movimiento);
        auditService.registrar(AuditAction.CREATE, MODULO, "InventoryMovement",
                guardado.getId(), null, null, usuarioId, false);
        return aMovementItem(guardado);
    }

    /**
     * Descuenta una unidad al aplicar una prueba.
     *
     * <p>Lo invoca el servicio de pruebas dentro de su misma transacción, de
     * modo que registrar la prueba y descontar el inventario ocurren juntos.
     */
    @Transactional
    public void consumirUnidad(DrugTestBatch lote, LocalDate fecha, String referencia, Long usuarioId) {
        verificarExistencia(lote, 1);

        InventoryMovement consumo = new InventoryMovement();
        consumo.setBatch(lote);
        consumo.setPredio(lote.getPredio());
        consumo.setTipo(Tipo.CONSUMO);
        consumo.setCantidad(1);
        consumo.setFecha(fecha);
        consumo.setReferencia(referencia);
        consumo.setCreatedBy(usuarioId);
        movimientos.save(consumo);
    }

    @Transactional(readOnly = true)
    public List<MovementItem> movimientosDe(Long loteId) {
        return movimientos.findByBatchIdOrderByFechaDescIdDesc(loteId)
                .stream().map(this::aMovementItem).toList();
    }

    /**
     * Alertas vigentes de inventario.
     *
     * <p>Combina la proximidad de caducidad con la existencia mínima; ambos
     * criterios se leen de los umbrales configurables.
     */
    @Transactional(readOnly = true)
    public List<AlertaInventario> alertas() {
        int critico = umbrales.valorEntero(ThresholdService.CADUCIDAD_CRITICO);
        int alerta = umbrales.valorEntero(ThresholdService.CADUCIDAD_ALERTA);
        int vigilar = umbrales.valorEntero(ThresholdService.CADUCIDAD_VIGILAR);
        int stockMinimo = umbrales.valorEntero(ThresholdService.STOCK_MINIMO);

        List<AlertaInventario> resultado = new ArrayList<>();

        for (DrugTestBatch lote : batches.findByActivoTrueOrderByFechaCaducidadAsc()) {
            long dias = lote.diasParaCaducar();
            int disponible = lote.getCantidadInicial() + movimientos.saldoDeLote(lote.getId());
            int porcentaje = porcentaje(disponible, lote.getCantidadInicial());

            if (dias < 0) {
                resultado.add(alertaDe(lote, "CRITICA", "El lote venció el %s".formatted(lote.getFechaCaducidad()),
                        dias, disponible));
            } else if (dias <= critico) {
                resultado.add(alertaDe(lote, "CRITICA", "Caduca en %d días".formatted(dias), dias, disponible));
            } else if (dias <= alerta) {
                resultado.add(alertaDe(lote, "ADVERTENCIA", "Caduca en %d días".formatted(dias), dias, disponible));
            } else if (dias <= vigilar) {
                resultado.add(alertaDe(lote, "VIGILAR", "Caduca en %d días".formatted(dias), dias, disponible));
            }

            if (porcentaje < stockMinimo) {
                resultado.add(alertaDe(lote, "ADVERTENCIA",
                        "Quedan %d de %d unidades (%d%%)".formatted(disponible, lote.getCantidadInicial(), porcentaje),
                        dias, disponible));
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public DrugTestBatch obtenerLote(Long id) {
        return batches.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lote", id));
    }

    private void verificarExistencia(DrugTestBatch lote, int cantidad) {
        int disponible = lote.getCantidadInicial() + movimientos.saldoDeLote(lote.getId());
        if (disponible < cantidad) {
            throw new BusinessRuleException(
                    "El lote «%s» solo tiene %d unidades disponibles".formatted(lote.getLote(), disponible));
        }
        if (lote.estaVencido()) {
            throw new BusinessRuleException(
                    "El lote «%s» venció el %s".formatted(lote.getLote(), lote.getFechaCaducidad()));
        }
    }

    private AlertaInventario alertaDe(DrugTestBatch lote, String severidad, String mensaje,
                                      long dias, int disponible) {
        return new AlertaInventario(lote.getId(), lote.getLote(), lote.getPredio().getNombre(),
                severidad, mensaje, dias, disponible);
    }

    private BatchItem aItem(DrugTestBatch lote) {
        int consumo = movimientos.consumoDeLote(lote.getId());
        int disponible = lote.getCantidadInicial() + movimientos.saldoDeLote(lote.getId());
        long dias = lote.diasParaCaducar();

        return new BatchItem(
                lote.getId(), lote.getLote(), lote.getPredio().getNombre(), lote.getPredio().getId(),
                lote.getFechaCaducidad(), dias, lote.getCantidadInicial(), consumo, disponible,
                porcentaje(disponible, lote.getCantidadInicial()), estadoDeCaducidad(dias), lote.isActivo());
    }

    private String estadoDeCaducidad(long dias) {
        if (dias < 0) {
            return "VENCIDO";
        }
        if (dias <= umbrales.valorEntero(ThresholdService.CADUCIDAD_CRITICO)) {
            return "CRITICO";
        }
        if (dias <= umbrales.valorEntero(ThresholdService.CADUCIDAD_ALERTA)) {
            return "ADVERTENCIA";
        }
        if (dias <= umbrales.valorEntero(ThresholdService.CADUCIDAD_VIGILAR)) {
            return "VIGILAR";
        }
        return "VIGENTE";
    }

    private int porcentaje(int disponible, int inicial) {
        return inicial == 0 ? 0 : (int) Math.round(disponible * 100.0 / inicial);
    }

    private MovementItem aMovementItem(InventoryMovement m) {
        return new MovementItem(m.getId(), m.getBatch().getLote(), m.getTipo(),
                m.getCantidad(), m.getFecha(), m.getReferencia());
    }
}
