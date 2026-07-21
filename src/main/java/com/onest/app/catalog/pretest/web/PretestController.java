package com.onest.app.catalog.pretest.web;

import com.onest.app.catalog.pretest.service.PretestService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pre-Test (U03, id 11). Replica pretest.php (form pre-cargado) y savePretest.php (guardado).
 */
@Controller
@RequestMapping("/api/nss")
public class PretestController {

    private final PretestService pretestService;

    public PretestController(PretestService pretestService) {
        this.pretestService = pretestService;
    }

    @PostMapping(
            path = "/pretest",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String form(@RequestParam("data") String data, Model model) {
        try {
            model.addAttribute("nss", data == null ? "" : data.trim());
            model.addAttribute("p", pretestService.load(data));
            return "fragments/pretest-form :: form";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping(
            path = "/pretest/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String save(@RequestParam MultiValueMap<String, String> params) {
        try {
            String nss = params.getFirst("nss");
            Map<String, String> fields = new HashMap<>();
            params.forEach((key, values) -> {
                if (!"nss".equals(key) && !"_csrf".equals(key)) {
                    fields.put(key, (values == null || values.isEmpty()) ? null : values.get(0));
                }
            });
            pretestService.save(nss, fields);
            return "Pre-Test guardado.";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
