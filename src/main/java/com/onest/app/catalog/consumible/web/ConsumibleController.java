package com.onest.app.catalog.consumible.web;

import com.onest.app.catalog.consumible.dto.ConsumibleDto;
import com.onest.app.catalog.consumible.service.ConsumibleService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Consumibles de antidoping (inventario por PREDIO/mes). Backend aplicado 2026-08-18
 * (docs/ords-antidoping-consumibles.sql). JSON puro (igual criterio que DashboardController) -
 * NO esta asociado al flujo de busqueda por NSS.
 */
@RestController
@RequestMapping("/api/consumibles")
public class ConsumibleController {

    private final ConsumibleService consumibleService;

    public ConsumibleController(ConsumibleService consumibleService) {
        this.consumibleService = consumibleService;
    }

    /** predio es opcional - si no se manda, regresa todos los registros. */
    @GetMapping
    public List<ConsumibleDto> listar(@RequestParam(name = "predio", required = false) String predio) {
        return consumibleService.listar(predio);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = "text/plain;charset=UTF-8")
    public String crear(@ModelAttribute ConsumibleAltaForm form) {
        try {
            String proceso = consumibleService.crear(form);
            return (proceso == null || proceso.isBlank()) ? "Registro de consumibles guardado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
