package com.scriptflow.storage.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.storage.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * File upload/download controller.
 */
@Tag(name = "File Storage", description = "File upload, download, and management")
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload file")
    @PostMapping("/upload")
    public R<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "common") String directory) {
        String objectKey = fileStorageService.upload(file, directory);
        Map<String, String> result = new HashMap<>();
        result.put("objectKey", objectKey);
        result.put("originalName", file.getOriginalFilename());
        result.put("size", String.valueOf(file.getSize()));
        return R.success(result);
    }

    @Operation(summary = "Delete file")
    @DeleteMapping("/file/{objectKey}")
    public R<Void> delete(@PathVariable("objectKey") String objectKey) {
        fileStorageService.delete(objectKey);
        return R.success();
    }

    @Operation(summary = "Get file access URL")
    @GetMapping("/file/{objectKey}/url")
    public R<Map<String, String>> getUrl(
            @PathVariable("objectKey") String objectKey,
            @RequestParam(value = "expiryMinutes", defaultValue = "60") int expiryMinutes) {
        String url = fileStorageService.getPresignedUrl(objectKey, expiryMinutes);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("objectKey", objectKey);
        return R.success(result);
    }
}
