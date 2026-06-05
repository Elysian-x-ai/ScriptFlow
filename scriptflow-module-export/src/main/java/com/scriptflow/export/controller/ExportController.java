package com.scriptflow.export.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Export controller for script format conversion.
 */
@Tag(name = "Script Export", description = "Multi-format script export (PDF, DOCX, FDX)")
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "Export script to specified format")
    @PostMapping("/{scriptId}")
    public ResponseEntity<byte[]> export(
            @PathVariable Long scriptId,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestBody(required = false) String yamlContent) {
        byte[] data = exportService.export(scriptId, yamlContent, format);

        String filename = "script_" + scriptId + "." + format;
        MediaType mediaType = switch (format.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "docx" -> MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "fdx" -> MediaType.valueOf("application/xml");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }
}
