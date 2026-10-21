package com.onest.app.catalog.firma.web;

import com.onest.app.catalog.firma.repository.DocumentoFirmadoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Verificar un folio (20-oct-2026): dice si un formato impreso con firma de verdad salio del portal.
 * Pide folio Y codigo (el codigo es aleatorio: con solo el folio se podrian recorrer todos). Muestra el
 * NSS enmascarado. Requiere sesion, como todo el portal.
 */
@Controller
public class VerificarFolioController {

    private final DocumentoFirmadoRepository documentos;

    public VerificarFolioController(DocumentoFirmadoRepository documentos) {
        this.documentos = documentos;
    }

    @GetMapping("/firma/verificar")
    public String verificar(@RequestParam(name = "folio", required = false) String folio,
                            @RequestParam(name = "codigo", required = false) String codigo, Model model) {
        model.addAttribute("folio", folio == null ? "" : folio.trim());
        model.addAttribute("codigo", codigo == null ? "" : codigo.trim().toUpperCase());
        if (folio != null && !folio.isBlank()) {
            String resultado = "NO_VALIDO";
            try {
                var imp = documentos.impreso(Long.parseLong(folio.trim()));
                if (imp.isPresent() && imp.get().codigo().equalsIgnoreCase(codigo == null ? "" : codigo.trim())) {
                    resultado = "VALIDO";
                    model.addAttribute("impreso", imp.get());
                    model.addAttribute("nssMascara", mascara(imp.get().nss()));
                }
            } catch (NumberFormatException ex) {
                // folio no numerico: no valido
            } catch (org.springframework.dao.DataAccessException ex) {
                resultado = "SIN_MODULO";
            }
            model.addAttribute("resultado", resultado);
        }
        return "pages/firma-verificar";
    }

    private static String mascara(String nss) {
        if (nss == null || nss.length() < 4) {
            return "****";
        }
        return "*".repeat(nss.length() - 4) + nss.substring(nss.length() - 4);
    }
}
