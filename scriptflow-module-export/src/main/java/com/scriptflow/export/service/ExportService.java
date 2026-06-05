package com.scriptflow.export.service;

import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Multi-format export service.
 * Supports PDF, Word (docx), and Fountain (FDX) format export.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    /**
     * Export script content to the specified format.
     *
     * @param scriptId  script ID
     * @param yamlContent the YAML script content
     * @param format    export format: pdf, docx, fdx
     * @return byte array of the exported file
     */
    public byte[] export(Long scriptId, String yamlContent, String format) {
        return switch (format.toLowerCase()) {
            case "pdf" -> exportToPdf(scriptId, yamlContent);
            case "docx" -> exportToDocx(scriptId, yamlContent);
            case "fdx" -> exportToFdx(scriptId, yamlContent);
            default -> throw new BusinessException(ResultCode.PARAM_ERROR, "Unsupported format: " + format);
        };
    }

    private byte[] exportToPdf(Long scriptId, String yamlContent) {
        // TODO: Implement PDF export using iText or Apache PDFBox
        log.info("PDF export requested for script: {}", scriptId);
        throw new BusinessException(ResultCode.EXPORT_ERROR, "PDF export not yet implemented");
    }

    private byte[] exportToDocx(Long scriptId, String yamlContent) {
        // TODO: Implement DOCX export using Apache POI
        log.info("DOCX export requested for script: {}", scriptId);
        throw new BusinessException(ResultCode.EXPORT_ERROR, "DOCX export not yet implemented");
    }

    private byte[] exportToFdx(Long scriptId, String yamlContent) {
        // TODO: Implement FDX (Final Draft) export
        log.info("FDX export requested for script: {}", scriptId);
        throw new BusinessException(ResultCode.EXPORT_ERROR, "FDX export not yet implemented");
    }
}
