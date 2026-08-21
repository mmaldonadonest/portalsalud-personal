package com.onest.app.catalog.consumible.service;

import com.onest.app.catalog.consumible.client.ConsumibleClient;
import com.onest.app.catalog.consumible.client.dto.BiowsConsumibleAltaRequest;
import com.onest.app.catalog.consumible.dto.ConsumibleDto;
import com.onest.app.catalog.consumible.web.ConsumibleAltaForm;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Consumibles de antidoping (inventario por PREDIO/mes, lista + alta). Backend
 * aplicado 2026-08-18 contra el WS ORDS real (docs/ords-antidoping-consumibles.sql).
 * NO esta asociado a NSS/empleado - identificado sin bloqueador de negocio revisando
 * salud-ocupacional-v2 (ver docs/plan-tareas-concretas.html).
 */
@Service
public class ConsumibleService {

    private static final int ANIO_MINIMO = 2000;

    private final ConsumibleClient client;

    public ConsumibleService(ConsumibleClient client) {
        this.client = client;
    }

    /** predio null/vacio regresa todos los registros. */
    public List<ConsumibleDto> listar(String predio) {
        String value = (predio == null || predio.isBlank()) ? null : predio.trim();
        return client.listar(value);
    }

    public String crear(ConsumibleAltaForm form) {
        String predio = normalizePredio(form.getPredio());
        Integer anio = normalizeAnio(form.getAnio());
        Integer mes = normalizeMes(form.getMes());
        Integer cantidadInicial = normalizeCantidad(form.getCantidadInicial(), "cantidad inicial");
        Integer entregaMensual = normalizeCantidad(form.getEntregaMensual(), "entrega mensual");
        Integer consumoMensual = normalizeCantidad(form.getConsumoMensual(), "consumo mensual");

        BiowsConsumibleAltaRequest request = new BiowsConsumibleAltaRequest(
                predio, anio, mes, cantidadInicial, entregaMensual, consumoMensual, form.getObservaciones());
        return client.crear(request);
    }

    private static String normalizePredio(String predio) {
        if (predio == null || predio.isBlank()) {
            throw new IllegalArgumentException("El predio es obligatorio");
        }
        String value = predio.trim();
        if (value.length() > 50) {
            throw new IllegalArgumentException("El predio excede la longitud permitida");
        }
        return value;
    }

    private static Integer normalizeAnio(Integer anio) {
        if (anio == null) {
            throw new IllegalArgumentException("El año es obligatorio");
        }
        int maximo = LocalDate.now().getYear() + 1;
        if (anio < ANIO_MINIMO || anio > maximo) {
            throw new IllegalArgumentException("El año no es valido");
        }
        return anio;
    }

    private static Integer normalizeMes(Integer mes) {
        if (mes == null || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }
        return mes;
    }

    private static Integer normalizeCantidad(Integer valor, String etiqueta) {
        if (valor == null) {
            return null;
        }
        if (valor < 0) {
            throw new IllegalArgumentException("La " + etiqueta + " no puede ser negativa");
        }
        return valor;
    }
}
