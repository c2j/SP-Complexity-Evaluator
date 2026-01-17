package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.evaluator.HintExtractor;
import com.sdchat.ce.sp.complexity.model.*;
import com.sdchat.ce.sp.complexity.parser.HintReferenceLoader;
import com.sdchat.ce.sp.complexity.util.HintSyntaxValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HintValidationServiceTest {

    @Mock
    private HintReferenceLoader hintReferenceLoader;

    @Mock
    private HintExtractor hintExtractor;

    @Mock
    private HintSyntaxValidator hintSyntaxValidator;

    private HintValidationService service;

    @BeforeEach
    void setUp() {
        service = new HintValidationService(hintReferenceLoader, hintExtractor, hintSyntaxValidator);
    }

    @Test
    void validateSqlStatement_withValidHint_returnsValidResult() {
        String sql = "SELECT /*+ tablescan(t1) */ * FROM t1";

        HintExtractor.ExtractedHint extractedHint = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ tablescan(t1) */")
                .hintContent("tablescan(t1)")
                .hintName("tablescan")
                .lineNumber(1)
                .charOffset(16)
                .build();

        HintReference hintRef = HintReference.builder()
                .hint("tablescan")
                .category("Scan")
                .syntax("[no] tablescan([@queryblock] table)")
                .parameters(Collections.emptyList())
                .build();

        HintSyntaxValidator.SyntaxValidationResult syntaxResult =
                new HintSyntaxValidator.SyntaxValidationResult(true, Collections.emptyList());

        when(hintExtractor.extractHints(sql)).thenReturn(Collections.singletonList(extractedHint));
        when(hintReferenceLoader.getHintReference("tablescan")).thenReturn(hintRef);
        when(hintSyntaxValidator.validateSyntax(anyString(), anyString())).thenReturn(syntaxResult);

        HintAnalysisSummary result = service.validateSqlStatement(sql);

        assertNotNull(result);
        assertEquals(1, result.getTotalHints());
        assertEquals(1, result.getValidCount());
        assertEquals(0, result.getInvalidCount());
        assertEquals(1, result.getValidHints().size());
        assertEquals("tablescan", result.getValidHints().get(0).getHintName());
        assertEquals("VALID", result.getValidHints().get(0).getValidationStatus());
        assertEquals("Scan", result.getValidHints().get(0).getCategory());
    }

    @Test
    void validateSqlStatement_withInvalidHint_returnsInvalidResult() {
        String sql = "SELECT /*+ invalid_hint(param) */ * FROM t1";

        HintExtractor.ExtractedHint extractedHint = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ invalid_hint(param) */")
                .hintContent("invalid_hint(param)")
                .hintName("invalid_hint")
                .lineNumber(1)
                .charOffset(8)
                .build();

        when(hintExtractor.extractHints(sql)).thenReturn(Collections.singletonList(extractedHint));
        when(hintReferenceLoader.getHintReference("invalid_hint")).thenReturn(null);
        when(hintReferenceLoader.getAllHints()).thenReturn(Collections.emptyList());

        HintAnalysisSummary result = service.validateSqlStatement(sql);

        assertNotNull(result);
        assertEquals(1, result.getTotalHints());
        assertEquals(0, result.getValidCount());
        assertEquals(1, result.getInvalidCount());
        assertEquals("NOT_IN_REFERENCE", result.getInvalidHints().get(0).getValidationStatus());
        assertEquals("invalid_hint", result.getInvalidHints().get(0).getHintName());
        assertEquals("UNKNOWN_HINT", result.getInvalidHints().get(0).getErrorType());
    }

    @Test
    void validateSqlStatement_withSyntaxError_returnsSyntaxErrorResult() {
        String sql = "SELECT /*+ tablescan( */ * FROM t1";

        HintExtractor.ExtractedHint extractedHint = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ tablescan( */")
                .hintContent("tablescan(")
                .hintName("tablescan")
                .lineNumber(1)
                .charOffset(16)
                .build();

        HintReference hintRef = HintReference.builder()
                .hint("tablescan")
                .category("Scan")
                .syntax("[no] tablescan([@queryblock] table)")
                .parameters(Collections.emptyList())
                .build();

        HintSyntaxValidator.SyntaxValidationResult syntaxResult =
                new HintSyntaxValidator.SyntaxValidationResult(false, Collections.singletonList("Unbalanced parentheses"));

        when(hintExtractor.extractHints(sql)).thenReturn(Collections.singletonList(extractedHint));
        when(hintReferenceLoader.getHintReference("tablescan")).thenReturn(hintRef);
        when(hintSyntaxValidator.validateSyntax(anyString(), anyString())).thenReturn(syntaxResult);

        HintAnalysisSummary result = service.validateSqlStatement(sql);

        assertNotNull(result);
        assertEquals(1, result.getTotalHints());
        assertEquals(0, result.getValidCount());
        assertEquals(1, result.getInvalidCount());
        assertEquals("SYNTAX_ERROR", result.getInvalidHints().get(0).getValidationStatus());
        assertEquals("SYNTAX_ERROR", result.getInvalidHints().get(0).getErrorType());
    }

    @Test
    void validateSqlStatement_withEmptySql_returnsEmptySummary() {
        HintAnalysisSummary result = service.validateSqlStatement("");

        assertNotNull(result);
        assertEquals(0, result.getTotalHints());
        assertEquals(0, result.getValidCount());
        assertEquals(0, result.getInvalidCount());
        assertTrue(result.getValidHints().isEmpty());
        assertTrue(result.getInvalidHints().isEmpty());
    }

    @Test
    void validateSqlStatement_withNullSql_returnsEmptySummary() {
        HintAnalysisSummary result = service.validateSqlStatement(null);

        assertNotNull(result);
        assertEquals(0, result.getTotalHints());
    }

    @Test
    void validateSqlStatement_withMultipleHints_returnsCorrectCounts() {
        String sql = "SELECT /*+ tablescan(t1) */ * FROM t1 JOIN t2 /*+ nestloop(t1 t2) */ ON t1.id = t2.id";

        HintExtractor.ExtractedHint hint1 = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ tablescan(t1) */")
                .hintContent("tablescan(t1)")
                .hintName("tablescan")
                .lineNumber(1)
                .charOffset(16)
                .build();

        HintExtractor.ExtractedHint hint2 = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ nestloop(t1 t2) */")
                .hintContent("nestloop(t1 t2)")
                .hintName("nestloop")
                .lineNumber(1)
                .charOffset(50)
                .build();

        HintReference tablescanRef = HintReference.builder()
                .hint("tablescan")
                .category("Scan")
                .build();

        HintReference nestloopRef = HintReference.builder()
                .hint("nestloop")
                .category("Join Method")
                .build();

        HintSyntaxValidator.SyntaxValidationResult syntaxResult =
                new HintSyntaxValidator.SyntaxValidationResult(true, Collections.emptyList());

        when(hintExtractor.extractHints(sql)).thenReturn(Arrays.asList(hint1, hint2));
        when(hintReferenceLoader.getHintReference("tablescan")).thenReturn(tablescanRef);
        when(hintReferenceLoader.getHintReference("nestloop")).thenReturn(nestloopRef);
        when(hintSyntaxValidator.validateSyntax(anyString(), anyString())).thenReturn(syntaxResult);

        HintAnalysisSummary result = service.validateSqlStatement(sql);

        assertNotNull(result);
        assertEquals(2, result.getTotalHints());
        assertEquals(2, result.getValidCount());
        assertEquals(0, result.getInvalidCount());
        assertEquals(2, result.getCategoryBreakdown().size());
        assertEquals(1, result.getCategoryBreakdown().get("Scan"));
        assertEquals(1, result.getCategoryBreakdown().get("Join Method"));
    }

    @Test
    void getHintReferences_returnsAllHints() {
        List<HintReference> hints = Arrays.asList(
                HintReference.builder().hint("tablescan").category("Scan").build(),
                HintReference.builder().hint("indexscan").category("Scan").build()
        );

        when(hintReferenceLoader.getAllHints()).thenReturn(hints);

        List<HintReference> result = service.getHintReferences();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(hintReferenceLoader).getAllHints();
    }

    @Test
    void getHintReferenceCount_returnsCorrectCount() {
        when(hintReferenceLoader.getHintCount()).thenReturn(53);

        int count = service.getHintReferenceCount();

        assertEquals(53, count);
        verify(hintReferenceLoader).getHintCount();
    }

    @Test
    void buildValidationResponse_createsCorrectResponse() {
        HintAnalysisSummary summary = HintAnalysisSummary.builder()
                .totalHints(1)
                .validCount(1)
                .invalidCount(0)
                .validHints(Collections.emptyList())
                .invalidHints(Collections.emptyList())
                .categoryBreakdown(Collections.emptyMap())
                .errorTypeBreakdown(Collections.emptyMap())
                .build();

        HintValidationResponse response = service.buildValidationResponse(summary, "json", 15L);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("json", response.getFormat());
        assertEquals(15L, response.getProcessingTimeMs());
        assertEquals(summary, response.getAnalysis());
    }

    @Test
    void buildErrorResponse_createsErrorResponse() {
        ErrorResponse response = service.buildErrorResponse("Test error", "ERROR_CODE");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Test error", response.getError());
        assertEquals("ERROR_CODE", response.getErrorCode());
    }
}
