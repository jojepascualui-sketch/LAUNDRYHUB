package com.laundryhub.backend.web.admin;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.laundryhub.backend.service.ReportService;
import com.laundryhub.backend.web.dto.ReportResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A5 Sales reports (role ADMIN only). */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin reports", description = "A5 Sales reports (ADMIN)")
public class AdminReportController {

    /** Excel opens UTF-8 CSV files correctly only with a byte-order mark. */
    private static final String UTF8_BOM = "\uFEFF";

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportService reports;

    /**
     * @param reports report logic
     */
    public AdminReportController(ReportService reports) {
        this.reports = reports;
    }

    /**
     * @param from first day (yyyy-MM-dd), default first day of this month
     * @param to   last day (yyyy-MM-dd), default today
     * @return Cash vs GCash sales, delivery vs walk-in, sales by soap
     */
    @GetMapping
    @Operation(summary = "A5 Sales report for a date range")
    public ReportResponse report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.report(from, to);
    }

    /**
     * @param from first day, default first day of this month
     * @param to   last day, default today
     * @return a CSV download, one row per order
     */
    @GetMapping("/export.csv")
    @Operation(summary = "A5 Export CSV")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String csv = UTF8_BOM + reports.exportCsv(from, to);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(reports.csvFileName(from, to)).build().toString())
                .body(csv);
    }
}
