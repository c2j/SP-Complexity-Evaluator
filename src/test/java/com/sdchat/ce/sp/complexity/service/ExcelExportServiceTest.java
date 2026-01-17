package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.model.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelExportServiceTest {

    private ExcelExportService service;

    @BeforeEach
    void setUp() {
        service = new ExcelExportService();
    }

    @Test
    void exportAnalysisToExcel_withValidHints_createsValidWorkbook() throws IOException {
        HintValidationResult validHint = HintValidationResult.builder()
                .hintText("/*+ tablescan(t1) */")
                .validationStatus("VALID")
                .category("Scan")
                .hintName("tablescan")
                .parameters("t1")
                .lineNumber(1)
                .charOffset(16)
                .build();

        HintAnalysisSummary summary = HintAnalysisSummary.builder()
                .totalHints(1)
                .validCount(1)
                .invalidCount(0)
                .validHints(Collections.singletonList(validHint))
                .invalidHints(Collections.emptyList())
                .categoryBreakdown(Collections.singletonMap("Scan", 1))
                .errorTypeBreakdown(Collections.emptyMap())
                .build();

        byte[] result = service.exportAnalysisToExcel(summary, "test_procedure");

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertNotNull(workbook.getSheet("Summary"));
            assertNotNull(workbook.getSheet("Hints"));
            assertNotNull(workbook.getSheet("Statements"));
        }
    }

    @Test
    void exportAnalysisToExcel_withInvalidHints_createsValidWorkbook() throws IOException {
        HintValidationResult invalidHint = HintValidationResult.builder()
                .hintText("/*+ invalid_hint(param) */")
                .validationStatus("NOT_IN_REFERENCE")
                .hintName("invalid_hint")
                .parameters("param")
                .lineNumber(1)
                .charOffset(8)
                .errorType("UNKNOWN_HINT")
                .errorMessage("Unknown hint")
                .build();

        HintAnalysisSummary summary = HintAnalysisSummary.builder()
                .totalHints(1)
                .validCount(0)
                .invalidCount(1)
                .validHints(Collections.emptyList())
                .invalidHints(Collections.singletonList(invalidHint))
                .categoryBreakdown(Collections.emptyMap())
                .errorTypeBreakdown(Collections.singletonMap("UNKNOWN_HINT", 1))
                .build();

        byte[] result = service.exportAnalysisToExcel(summary, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportAnalysisToExcel_withEmptySummary_createsValidWorkbook() throws IOException {
        HintAnalysisSummary summary = HintAnalysisSummary.builder()
                .totalHints(0)
                .validCount(0)
                .invalidCount(0)
                .validHints(Collections.emptyList())
                .invalidHints(Collections.emptyList())
                .categoryBreakdown(new HashMap<>())
                .errorTypeBreakdown(new HashMap<>())
                .build();

        byte[] result = service.exportAnalysisToExcel(summary, "empty_procedure");

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportAnalysisToExcel_withMixedHints_createsValidWorkbook() throws IOException {
        HintValidationResult validHint = HintValidationResult.builder()
                .hintText("/*+ tablescan(t1) */")
                .validationStatus("VALID")
                .category("Scan")
                .hintName("tablescan")
                .parameters("t1")
                .lineNumber(1)
                .charOffset(16)
                .build();

        HintValidationResult invalidHint = HintValidationResult.builder()
                .hintText("/*+ invalid_hint */")
                .validationStatus("NOT_IN_REFERENCE")
                .hintName("invalid_hint")
                .lineNumber(2)
                .charOffset(8)
                .errorType("UNKNOWN_HINT")
                .build();

        HintAnalysisSummary summary = HintAnalysisSummary.builder()
                .totalHints(2)
                .validCount(1)
                .invalidCount(1)
                .validHints(Collections.singletonList(validHint))
                .invalidHints(Collections.singletonList(invalidHint))
                .categoryBreakdown(Collections.singletonMap("Scan", 1))
                .errorTypeBreakdown(Collections.singletonMap("UNKNOWN_HINT", 1))
                .build();

        byte[] result = service.exportAnalysisToExcel(summary, "mixed_procedure");

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertNotNull(workbook.getSheet("Summary"));
            assertNotNull(workbook.getSheet("Hints"));
        }
    }

    @Test
    void exportAnalysisToExcel_withNullProcedureName_createsValidWorkbook() throws IOException {
        HintValidationResult validHint = HintValidationResult.builder()
                .hintText("/*+ indexscan(item i) */")
                .validationStatus("VALID")
                .category("Scan")
                .hintName("indexscan")
                .parameters("item i")
                .lineNumber(1)
                .charOffset(16)
                .build();

        HintAnalysisSummary summary = HintAnalysisSummary.builder()
                .totalHints(1)
                .validCount(1)
                .invalidCount(0)
                .validHints(Collections.singletonList(validHint))
                .invalidHints(Collections.emptyList())
                .categoryBreakdown(Collections.singletonMap("Scan", 1))
                .errorTypeBreakdown(Collections.emptyMap())
                .build();

        byte[] result = service.exportAnalysisToExcel(summary, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
