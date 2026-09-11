package com.onest.app.catalog.predio.service;

import com.onest.app.catalog.predio.client.PredioClient;
import com.onest.app.catalog.predio.dto.CuentaPredioDto;
import com.onest.app.catalog.predio.dto.PredioDto;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resuelve predio para el dashboard analitico (rol MEDICO_ANALISTA). Dos fuentes,
 * NO intercambiables:
 * <ul>
 *   <li>{@link #predioRealPorNss} - predio REAL de RH (BIO_DATOS_LABORALES_EMPLEADOS via
 *       TAB_PREDIOS), hoy solo 2 valores (MACRO 1/2), retroactivo, cero mantenimiento.</li>
 *   <li>{@link #predioFinoPorCuenta} - predio "fino" (17 sitios, granularidad del Excel de
 *       morbilidad) via el mapeo administrable cuenta-&gt;predio (docs/ords-predio-cuenta.sql).
 *       Nace vacio - cuentas sin asignar quedan como Optional.empty(), el llamador decide
 *       como mostrar "Sin asignar" (nunca se inventa un valor).</li>
 * </ul>
 * Cache corta (mismo TTL que PermissionService) porque el catalogo de predios y el mapeo
 * cuenta-predio cambian con muy poca frecuencia, pero SI pueden cambiar desde la pantalla admin.
 */
@Service
public class PredioService {

    private static final long TTL_MILLIS = 120_000L;

    private final PredioClient client;
    private final AtomicReference<CacheEntry<List<PredioDto>>> catalogoCache = new AtomicReference<>();
    private final AtomicReference<CacheEntry<Map<String, CuentaPredioDto>>> mapeoCache = new AtomicReference<>();

    public PredioService(PredioClient client) {
        this.client = client;
    }

    /** Catalogo de los 17 sitios, para filtros/UI. */
    public List<PredioDto> listarPredios() {
        return cached(catalogoCache, client::listar);
    }

    /** Predio real de RH (2 valores hoy) para un NSS puntual. Nunca falla el flujo si no hay dato. */
    public Optional<PredioDto> predioRealPorNss(String nss) {
        return client.resolverPorNss(nss);
    }

    /**
     * Predio fino (mapeo administrable) para una cuenta. Optional.empty() si esa cuenta
     * todavia no fue asignada - el caso debe agruparse como "Sin asignar", nunca excluirse.
     */
    public Optional<CuentaPredioDto> predioFinoPorCuenta(String cuentaNombre) {
        if (cuentaNombre == null || cuentaNombre.isBlank()) {
            return Optional.empty();
        }
        CuentaPredioDto fila = mapaCuentaPredio().get(cuentaNombre.trim().toUpperCase(Locale.ROOT));
        return Optional.ofNullable(fila).filter(CuentaPredioDto::tienePredioAsignado);
    }

    /** Todas las cuentas reales con su predio vigente o pendiente - alimenta la pantalla admin. */
    public List<CuentaPredioDto> listarCuentaPredio() {
        return mapaCuentaPredio().values().stream()
                .sorted((a, b) -> a.cuentaNombre().compareToIgnoreCase(b.cuentaNombre()))
                .toList();
    }

    /** Asigna/reasigna predio a una cuenta (insert-only del lado ORDS) e invalida la cache local. */
    public String asignar(String cuentaNombre, Long predioId) {
        String resultado = client.asignar(cuentaNombre, predioId, usuarioActualId(), usuarioActualId());
        mapeoCache.set(null);
        return resultado;
    }

    private Map<String, CuentaPredioDto> mapaCuentaPredio() {
        return cached(mapeoCache, () -> client.listarCuentaPredio().stream()
                .collect(Collectors.toMap(
                        d -> d.cuentaNombre().trim().toUpperCase(Locale.ROOT),
                        d -> d,
                        (a, b) -> a)));
    }

    private <T> T cached(AtomicReference<CacheEntry<T>> ref, Supplier<T> loader) {
        long now = System.currentTimeMillis();
        CacheEntry<T> entry = ref.get();
        if (entry != null && entry.expiresAt() > now) {
            return entry.valor();
        }
        T valor = loader.get();
        ref.set(new CacheEntry<>(valor, now + TTL_MILLIS));
        return valor;
    }

    private String usuarioActualId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private record CacheEntry<T>(T valor, long expiresAt) {
    }
}
