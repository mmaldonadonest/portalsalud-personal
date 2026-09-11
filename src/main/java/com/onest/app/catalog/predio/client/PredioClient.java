package com.onest.app.catalog.predio.client;

import com.onest.app.catalog.predio.dto.CuentaPredioDto;
import com.onest.app.catalog.predio.dto.PredioDto;
import java.util.List;
import java.util.Optional;

/**
 * Gateway hacia los WS ORDS del modulo de predios (dashboard analitico, rol
 * MEDICO_ANALISTA). Backend aplicado 08-sep-2026, ver docs/ords-predio-empleado.sql
 * y docs/ords-predio-cuenta.sql.
 */
public interface PredioClient {

    /**
     * POST .../Servcio/predio_empleado. Predio REAL (RH, ligado a BIO_DATOS_LABORALES_EMPLEADOS
     * -> TAB_PREDIOS) - hoy solo distingue 2 valores (MACRO 1/2), pero es retroactivo y de
     * mantenimiento cero. Optional.empty() si el NSS no tiene fila laboral o predio nulo.
     */
    Optional<PredioDto> resolverPorNss(String nss);

    /** POST .../Servcio/predios. Catalogo de los 17 sitios (predio "fino") para filtros/UI. */
    List<PredioDto> listar();

    /**
     * POST .../Servcio/cuenta_predio_lista. Todas las cuentas reales de biometrico_cuenta_SAP
     * con su predio fino vigente (o pendiente) segun el mapeo de SERV_MED_CUENTA_PREDIO.
     */
    List<CuentaPredioDto> listarCuentaPredio();

    /**
     * POST .../Servcio/cuenta_predio_asignar. Insert-only - cada llamada es una fila nueva,
     * la vigente es la mas reciente por cuenta. Devuelve el mensaje "Proceso" del WS.
     */
    String asignar(String cuentaNombre, Long predioId, String idUsuario, String nombreUsuario);
}
