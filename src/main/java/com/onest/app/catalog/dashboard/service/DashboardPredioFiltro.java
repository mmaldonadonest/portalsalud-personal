package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.predio.dto.CuentaPredioDto;
import com.onest.app.catalog.predio.service.PredioService;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Corte por predio/cuenta compartido por los 4 dashboards (Consultas, Incapacidades,
 * Accidentes, Examenes). Vive aparte porque los cuatro necesitan exactamente el mismo
 * criterio: si cada servicio lo reimplementara, bastaria con que uno normalizara distinto
 * para que el mismo filtro diera totales que no cuadran entre tarjetas.
 *
 * <p>Los 4 reportes traen CUENTA por registro desde el 10-sep-2026 (los tres WS {@code _cta}
 * de docs/ords-cuenta-en-reportes.sql; Consultas ya la traia). El predio se resuelve contra
 * el mapeo administrable cuenta-&gt;predio via {@link PredioService}, que cachea 2 minutos -
 * no hay llamadas WS extra por fila.
 */
@Component
public class DashboardPredioFiltro {

    /** Bucket de cuentas todavia sin predio asignado en /admin/predios. Es un valor de filtro valido. */
    public static final String SIN_ASIGNAR = "Sin asignar";

    private final PredioService predioService;

    public DashboardPredioFiltro(PredioService predioService) {
        this.predioService = predioService;
    }

    /**
     * Predio "fino" (17 sitios) de una cuenta. Cuenta sin mapear todavia (o sin cuenta en RH)
     * cae en {@link #SIN_ASIGNAR}, nunca se descarta: el analista ve cuanto le falta por
     * mapear en vez de un dato inventado.
     */
    public String predioDe(String cuenta) {
        return predioService.predioFinoPorCuenta(cuenta)
                .map(CuentaPredioDto::predioNombre)
                .orElse(SIN_ASIGNAR);
    }

    /**
     * True si la fila entra en el corte. Filtro vacio = pasa todo, que es como lo llama /home.
     * Cuando se piden ambos, la fila debe cumplir los dos (cuenta manda sobre predio solo en
     * el sentido de que es mas especifica; no se relaja ninguno).
     */
    public boolean coincide(String cuentaFila, String predioFiltro, String cuentaFiltro) {
        String predio = normalizar(predioFiltro);
        String cuenta = normalizar(cuentaFiltro);
        if (predio == null && cuenta == null) {
            return true;
        }
        if (cuenta != null && !cuenta.equals(normalizar(cuentaFila))) {
            return false;
        }
        return predio == null || predio.equals(normalizar(predioDe(cuentaFila)));
    }

    /** Comparacion laxa (trim + mayusculas) para no depender de como venga el texto del WS. */
    private static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
