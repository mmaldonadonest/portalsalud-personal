package mx.saludocupacional.portal.report.web;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.report.service.ReportService;
import mx.saludocupacional.portal.security.service.PortalUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Descarga de reportes generados desde la capa analítica. */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReportController {

    private static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService service;

    @GetMapping("/ejecutivo.xlsx")
    @PreAuthorize("hasAuthority('reports.export')")
    public ResponseEntity<byte[]> ejecutivo(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId,
            @AuthenticationPrincipal PortalUserDetails usuario) {

        int anioConsultado = anio != null ? anio : LocalDate.now().getYear();
        byte[] contenido = service.ejecutivoExcel(anioConsultado, mes, predioId, usuario.getUsuarioId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-ejecutivo-%d.xlsx\"".formatted(anioConsultado))
                .contentType(MediaType.parseMediaType(EXCEL))
                .body(contenido);
    }

    @GetMapping("/predios.csv")
    @PreAuthorize("hasAuthority('reports.export')")
    public ResponseEntity<byte[]> predios(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @AuthenticationPrincipal PortalUserDetails usuario) {

        int anioConsultado = anio != null ? anio : LocalDate.now().getYear();
        byte[] contenido = service.rankingCsv(anioConsultado, mes, usuario.getUsuarioId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"predios-%d.csv\"".formatted(anioConsultado))
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }
}
