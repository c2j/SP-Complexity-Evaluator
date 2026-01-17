package com.sdchat.ce.sp.complexity.controller;

import com.sdchat.ce.sp.complexity.model.*;
import com.sdchat.ce.sp.complexity.service.ExcelExportService;
import com.sdchat.ce.sp.complexity.service.HintValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/hint-validation")
public class HintValidationController {

    private final HintValidationService hintValidationService;
    private final ExcelExportService excelExportService;

    public HintValidationController(
            HintValidationService hintValidationService,
            ExcelExportService excelExportService) {
        this.hintValidationService = hintValidationService;
        this.excelExportService = excelExportService;
    }

    @PostMapping(value = "/validate/sql", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateSqlStatement(
            @RequestBody ValidateSqlRequest request) {

        if (request == null || request.getSql() == null) {
            log.warn("Received null or empty SQL validation request");
            return ResponseEntity.badRequest()
                    .body(hintValidationService.buildErrorResponse("SQL content is required", "INVALID_REQUEST", "Request body or sql field cannot be null"));
        }

        log.debug("Validating SQL statement");
        long startTime = System.currentTimeMillis();

        try {
            HintAnalysisSummary analysis = hintValidationService.validateSqlStatement(request.getSql());
            long processingTime = System.currentTimeMillis() - startTime;

            HintValidationResponse response = hintValidationService.buildValidationResponse(
                    analysis, "json", processingTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating SQL statement: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(hintValidationService.buildErrorResponse("Validation failed: " + e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @GetMapping("/reference")
    public ResponseEntity<?> getHintReferences() {
        log.debug("Fetching hint references");

        try {
            List<HintReference> hints = hintValidationService.getHintReferences();

            Map<String, Object> response = new HashMap<>();
            response.put("version", "1.0.0");
            response.put("hintCount", hints.size());
            response.put("hints", hints);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching hint references: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(hintValidationService.buildErrorResponse("Failed to load hint references", "REFERENCE_LOAD_ERROR"));
        }
    }

    @PostMapping(value = "/validate/procedure", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"})
    public ResponseEntity<?> validateProcedure(@RequestBody ValidateProcedureRequest request) {

        if (request == null || request.getProcedure() == null) {
            log.warn("Received null or empty procedure validation request");
            return ResponseEntity.badRequest()
                    .body(hintValidationService.buildErrorResponse("Procedure content is required", "INVALID_REQUEST", "Request body or procedure field cannot be null"));
        }

        log.debug("Validating stored procedure with format: {}", request.getFormat());
        long startTime = System.currentTimeMillis();

        try {
            HintAnalysisSummary analysis = hintValidationService.validateSqlStatement(request.getProcedure());
            long processingTime = System.currentTimeMillis() - startTime;

            if ("excel".equalsIgnoreCase(request.getFormat())) {
                byte[] excelContent = excelExportService.exportAnalysisToExcel(analysis, request.getProcedureName());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "hint-analysis.xlsx");
                headers.setContentLength(excelContent.length);

                log.debug("Excel export completed in {}ms, size: {} bytes", processingTime, excelContent.length);

                return new ResponseEntity<>(excelContent, headers, 200);

            } else {
                HintValidationResponse response = hintValidationService.buildValidationResponse(
                        analysis, "json", processingTime);
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Error validating procedure: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(hintValidationService.buildErrorResponse("Validation failed: " + e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @PostMapping(value = "/validate/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateBatch(
            @RequestBody BatchValidateRequest request) {

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            log.warn("Received null or empty batch validation request");
            return ResponseEntity.badRequest()
                    .body(hintValidationService.buildErrorResponse("Batch items are required", "INVALID_REQUEST", "Request body or items field cannot be null or empty"));
        }

        log.debug("Processing batch validation request with {} items",
                request.getItems() != null ? request.getItems().size() : 0);
        long startTime = System.currentTimeMillis();

        try {
            List<BatchResultItem> results = new java.util.ArrayList<>();
            int totalValid = 0;
            int totalInvalid = 0;

            if (request.getItems() != null) {
                for (BatchItem item : request.getItems()) {
                    HintAnalysisSummary analysis = hintValidationService.validateSqlStatement(item.getContent());

                    totalValid += analysis.getValidCount();
                    totalInvalid += analysis.getInvalidCount();

                    results.add(BatchResultItem.builder()
                            .name(item.getName())
                            .type(item.getType())
                            .success(true)
                            .analysis(analysis)
                            .build());
                }
            }

            HintAnalysisSummary overallSummary = HintAnalysisSummary.builder()
                    .totalHints(totalValid + totalInvalid)
                    .validCount(totalValid)
                    .invalidCount(totalInvalid)
                    .validHints(new java.util.ArrayList<>())
                    .invalidHints(new java.util.ArrayList<>())
                    .categoryBreakdown(new java.util.HashMap<>())
                    .errorTypeBreakdown(new java.util.HashMap<>())
                    .build();

            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Batch validation completed in {}ms", processingTime);

            BatchValidationResponse response = BatchValidationResponse.builder()
                    .success(true)
                    .results(results)
                    .summary(overallSummary)
                    .processingTimeMs(processingTime)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in batch validation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    hintValidationService.buildErrorResponse("Batch validation failed: " + e.getMessage(), "BATCH_VALIDATION_ERROR"));
        }
    }
}
