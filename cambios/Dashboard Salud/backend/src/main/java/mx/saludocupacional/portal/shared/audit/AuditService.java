package mx.saludocupacional.portal.shared.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escritura de la bitácora.
 *
 * <p>Cada asiento se guarda en una transacción propia: si el registro auditado
 * falla y se revierte, el intento queda documentado de todos modos. Un fallo al
 * auditar nunca interrumpe la operación del usuario, solo deja una advertencia
 * en el registro técnico.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Registra un cambio sobre una entidad.
     *
     * @param sensible cuando es verdadero, el detalle se omite por tratarse de
     *                 información clínica identificable
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(AuditAction accion, String modulo, String entidad, Long entidadId,
                          Object valorAnterior, Object valorNuevo, Long usuarioId, boolean sensible) {
        try {
            AuditLog asiento = new AuditLog();
            asiento.setAccion(accion);
            asiento.setModulo(modulo);
            asiento.setEntidad(entidad);
            asiento.setEntidadId(entidadId);
            asiento.setUserId(usuarioId);
            asiento.setContieneDatosSensibles(sensible);

            if (!sensible) {
                asiento.setValorAnterior(serializar(valorAnterior));
                asiento.setValorNuevo(serializar(valorNuevo));
            }
            repository.save(asiento);
        } catch (Exception ex) {
            log.warn("No fue posible registrar la auditoría de {} sobre {} {}",
                    accion, entidad, entidadId, ex);
        }
    }

    /** Registra el ingreso o la salida de un usuario. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAcceso(AuditAction accion, Long usuarioId, String ipOrigen) {
        try {
            AuditLog asiento = new AuditLog();
            asiento.setAccion(accion);
            asiento.setModulo("seguridad");
            asiento.setEntidad("User");
            asiento.setEntidadId(usuarioId);
            asiento.setUserId(usuarioId);
            asiento.setIpAddress(ipOrigen);
            repository.save(asiento);
        } catch (Exception ex) {
            log.warn("No fue posible registrar el acceso del usuario {}", usuarioId, ex);
        }
    }

    private String serializar(Object valor) throws JsonProcessingException {
        return valor == null ? null : objectMapper.writeValueAsString(valor);
    }
}
