package com.sdchat.ce.sp.complexity.controller;

import com.sdchat.ce.sp.complexity.model.*;
import com.sdchat.ce.sp.complexity.service.ExcelExportService;
import com.sdchat.ce.sp.complexity.service.HintValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HintValidationControllerTest {

    @Mock
    private HintValidationService hintValidationService;

    @Mock
    private ExcelExportService excelExportService;

    @InjectMocks
    private HintValidationController controller;

    private HintAnalysisSummary sampleValidSummary;
    private HintAnalysisSummary sampleInvalidSummary;

    @BeforeEach
    void setUp() {
        HintValidationResult validHint = HintValidationResult.builder()
                .hintText("/*+ tablescan(t1) */")
                .validationStatus("VALID")
                .category("Scan")
                .hintName("tablescan")
                .parameters("t1")
                .lineNumber(1)
                .charOffset(16)
                .build();

        sampleValidSummary = HintAnalysisSummary.builder()
                .totalHints(1)
                .validCount(1)
                .invalidCount(0)
                .validHints(Collections.singletonList(validHint))
                .invalidHints(Collections.emptyList())
                .categoryBreakdown(Collections.singletonMap("Scan", 1))
                .errorTypeBreakdown(Collections.emptyMap())
                .build();

        HintValidationResult invalidHint = HintValidationResult.builder()
                .hintText("/*+ invalid_hint(param) */")
                .validationStatus("NOT_IN_REFERENCE")
                .hintName("invalid_hint")
                .parameters("param")
                .lineNumber(1)
                .charOffset(8)
                .errorType("UNKNOWN_HINT")
                .errorMessage("Unknown hint 'invalid_hint'")
                .build();

        sampleInvalidSummary = HintAnalysisSummary.builder()
                .totalHints(1)
                .validCount(0)
                .invalidCount(1)
                .validHints(Collections.emptyList())
                .invalidHints(Collections.singletonList(invalidHint))
                .categoryBreakdown(Collections.emptyMap())
                .errorTypeBreakdown(Collections.singletonMap("UNKNOWN_HINT", 1))
                .build();
    }

    @Test
    void validateSqlStatement_withValidHint_returnsOkWithValidResult() {
        ValidateSqlRequest request = ValidateSqlRequest.builder()
                .sql("SELECT /*+ tablescan(t1) */ * FROM t1")
                .build();

        when(hintValidationService.validateSqlStatement(anyString())).thenReturn(sampleValidSummary);
        when(hintValidationService.buildValidationResponse(any(), anyString(), anyLong()))
                .thenAnswer(invocation -> HintValidationResponse.builder()
                        .success(true)
                        .format("json")
                        .analysis(invocation.getArgument(0))
                        .processingTimeMs(invocation.getArgument(2))
                        .build());

        ResponseEntity<?> response = controller.validateSqlStatement(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(((HintValidationResponse)response.getBody()).getSuccess());
        assertEquals(1, ((HintValidationResponse)response.getBody()).getAnalysis().getValidCount());

        verify(hintValidationService).validateSqlStatement("SELECT /*+ tablescan(t1) */ * FROM t1");
    }

    @Test
    void validateSqlStatement_withInvalidHint_returnsOkWithInvalidResult() {
        ValidateSqlRequest request = ValidateSqlRequest.builder()
                .sql("SELECT /*+ invalid_hint(param) */ * FROM t1")
                .build();

        when(hintValidationService.validateSqlStatement(anyString())).thenReturn(sampleInvalidSummary);
        when(hintValidationService.buildValidationResponse(any(), anyString(), anyLong()))
                .thenAnswer(invocation -> HintValidationResponse.builder()
                        .success(true)
                        .format("json")
                        .analysis(invocation.getArgument(0))
                        .processingTimeMs(invocation.getArgument(2))
                        .build());

        ResponseEntity<?> response = controller.validateSqlStatement(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(((HintValidationResponse)response.getBody()).getSuccess());
        assertEquals(0, ((HintValidationResponse)response.getBody()).getAnalysis().getValidCount());
        assertEquals(1, ((HintValidationResponse)response.getBody()).getAnalysis().getInvalidCount());
    }

    @Test
    void validateSqlStatement_withException_returnsInternalServerError() {
        ValidateSqlRequest request = ValidateSqlRequest.builder()
                .sql("SELECT * FROM t1")
                .build();

        when(hintValidationService.validateSqlStatement(anyString()))
                .thenThrow(new RuntimeException("Test exception"));
        when(hintValidationService.buildErrorResponse(anyString(), anyString()))
                .thenAnswer(invocation -> ErrorResponse.builder()
                        .success(false)
                        .error(invocation.getArgument(0))
                        .errorCode(invocation.getArgument(1))
                        .build());

        ResponseEntity<?> response = controller.validateSqlStatement(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(((ErrorResponse)response.getBody()).isSuccess());
    }

    @Test
    void getHintReferences_returnsHintList() {
        List<HintReference> hints = Arrays.asList(
                HintReference.builder().hint("tablescan").category("Scan").build(),
                HintReference.builder().hint("indexscan").category("Scan").build()
        );

        when(hintValidationService.getHintReferences()).thenReturn(hints);

        ResponseEntity response = controller.getHintReferences();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(hintValidationService).getHintReferences();
    }

    @Test
    void validateProcedure_withJsonFormat_returnsJsonResponse() {
        ValidateProcedureRequest request = ValidateProcedureRequest.builder()
                .procedure("SELECT /*+ tablescan(t1) */ * FROM t1")
                .procedureName("test_proc")
                .format("json")
                .build();

        when(hintValidationService.validateSqlStatement(anyString())).thenReturn(sampleValidSummary);
        when(hintValidationService.buildValidationResponse(any(), anyString(), anyLong()))
                .thenAnswer(invocation -> HintValidationResponse.builder()
                        .success(true)
                        .format("json")
                        .analysis(invocation.getArgument(0))
                        .processingTimeMs(invocation.getArgument(2))
                        .build());

        ResponseEntity<?> response = controller.validateProcedure(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof HintValidationResponse);

        HintValidationResponse body = (HintValidationResponse) response.getBody();
        assertTrue(body.getSuccess());
        assertEquals("json", body.getFormat());
    }

    @Test
    void validateProcedure_withExcelFormat_returnsExcelFile() throws Exception {
        ValidateProcedureRequest request = ValidateProcedureRequest.builder()
                .procedure("SELECT /*+ tablescan(t1) */ * FROM t1")
                .procedureName("test_proc")
                .format("excel")
                .build();

        byte[] excelContent = "mock excel content".getBytes();

        when(hintValidationService.validateSqlStatement(anyString())).thenReturn(sampleValidSummary);
        when(excelExportService.exportAnalysisToExcel(any(), anyString())).thenReturn(excelContent);

        ResponseEntity<?> response = controller.validateProcedure(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().containsKey("Content-Disposition"));
    }

    @Test
    void validateBatch_withMultipleItems_returnsAggregatedResults() {
        BatchItem item1 = BatchItem.builder()
                .type("sql")
                .content("SELECT /*+ tablescan(t1) */ * FROM t1")
                .name("Query 1")
                .build();

        BatchItem item2 = BatchItem.builder()
                .type("sql")
                .content("SELECT /*+ invalid_hint */ * FROM t2")
                .name("Query 2")
                .build();

        BatchValidateRequest request = BatchValidateRequest.builder()
                .items(Arrays.asList(item1, item2))
                .format("json")
                .build();

        when(hintValidationService.validateSqlStatement("SELECT /*+ tablescan(t1) */ * FROM t1"))
                .thenReturn(sampleValidSummary);
        when(hintValidationService.validateSqlStatement("SELECT /*+ invalid_hint */ * FROM t2"))
                .thenReturn(sampleInvalidSummary);

        ResponseEntity<?> response = controller.validateBatch(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(((BatchValidationResponse)response.getBody()).getSuccess());
        assertEquals(2, ((BatchValidationResponse)response.getBody()).getResults().size());
        assertEquals(1, ((BatchValidationResponse)response.getBody()).getSummary().getValidCount());
        assertEquals(1, ((BatchValidationResponse)response.getBody()).getSummary().getInvalidCount());
    }
}
