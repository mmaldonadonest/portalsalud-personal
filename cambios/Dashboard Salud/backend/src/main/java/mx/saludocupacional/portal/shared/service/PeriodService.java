package mx.saludocupacional.portal.shared.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import mx.saludocupacional.portal.shared.repository.PeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Resuelve el periodo mensual al que pertenece cada registro.
 *
 * <p>Todos los módulos operativos derivan su periodo de la fecha del hecho, de
 * modo que nadie captura el mes a mano ni puede equivocarse al elegirlo. Si el
 * periodo aún no existe, se crea: incorporar un año nuevo no requiere
 * intervención.
 */
@Service
@RequiredArgsConstructor
public class PeriodService {

    private final PeriodRepository repository;

    /** Periodo correspondiente a la fecha, creándolo si hiciera falta. */
    @Transactional
    public Period resolver(LocalDate fecha) {
        return repository.findByAnioAndMes(fecha.getYear(), fecha.getMonthValue())
                .orElseGet(() -> crear(fecha.getYear(), fecha.getMonthValue()));
    }

    /**
     * Periodo de la fecha, rechazando la captura si ya está cerrado.
     *
     * @throws BusinessRuleException si el periodo no admite modificaciones
     */
    @Transactional
    public Period resolverParaCaptura(LocalDate fecha) {
        Period periodo = resolver(fecha);
        if (periodo.isCerrado()) {
            throw new BusinessRuleException(
                    "El periodo %s está cerrado y no admite capturas".formatted(periodo.getEtiqueta()));
        }
        return periodo;
    }

    @Transactional(readOnly = true)
    public List<Period> listarPorAnio(Integer anio) {
        return repository.findByAnioOrderByMesAsc(anio);
    }

    @Transactional(readOnly = true)
    public List<Integer> aniosDisponibles() {
        return repository.findAniosDisponibles();
    }

    @Transactional
    public Period cerrar(Long periodoId) {
        Period periodo = repository.findById(periodoId)
                .orElseThrow(() -> new BusinessRuleException("El periodo indicado no existe"));
        periodo.cerrar();
        return repository.save(periodo);
    }

    @Transactional
    public Period reabrir(Long periodoId) {
        Period periodo = repository.findById(periodoId)
                .orElseThrow(() -> new BusinessRuleException("El periodo indicado no existe"));
        periodo.setCerrado(false);
        periodo.setFechaCierre(null);
        return repository.save(periodo);
    }

    private Period crear(int anio, int mes) {
        Period periodo = new Period();
        periodo.setAnio(anio);
        periodo.setMes(mes);
        return repository.save(periodo);
    }
}
