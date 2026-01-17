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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoredProcedureValidationTest {

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
    void analyzeStoredProcedure_withSingleStatement_returnsCorrectAnalysis() {
        String procedure = "SELECT /*+ tablescan(t1) */ * FROM t1 WHERE id = 1";

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

        when(hintExtractor.extractHints(procedure)).thenReturn(Collections.singletonList(extractedHint));
        when(hintReferenceLoader.getHintReference("tablescan")).thenReturn(hintRef);
        when(hintSyntaxValidator.validateSyntax(anyString(), anyString())).thenReturn(syntaxResult);

        StoredProcedureAnalysis result = service.analyzeStoredProcedure(procedure, "test_proc");

        assertNotNull(result);
        assertEquals("test_proc", result.getProcedureName());
        assertEquals(1, result.getTotalStatements());
        assertEquals(1, result.getTotalHints());
        assertEquals(1, result.getOverallSummary().getValidCount());
        assertEquals(0, result.getOverallSummary().getInvalidCount());
        assertTrue(result.getParseErrors().isEmpty());
        assertEquals(1, result.getStatementAnalyses().size());
    }

    @Test
    void analyzeStoredProcedure_withMultipleStatements_returnsMultipleAnalyses() {
        String procedure = "SELECT /*+ tablescan(t1) */ * FROM t1;\n" +
                "SELECT /*+ indexscan(item i) */ * FROM item WHERE i_item_sk = 100";

        HintExtractor.ExtractedHint hint1 = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ tablescan(t1) */")
                .hintContent("tablescan(t1)")
                .hintName("tablescan")
                .lineNumber(1)
                .charOffset(16)
                .build();

        HintExtractor.ExtractedHint hint2 = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ indexscan(item i) */")
                .hintContent("indexscan(item i)")
                .hintName("indexscan")
                .lineNumber(3)
                .charOffset(16)
                .build();

        HintReference scanRef = HintReference.builder()
                .hint("tablescan")
                .category("Scan")
                .build();

        HintReference indexRef = HintReference.builder()
                .hint("indexscan")
                .category("Scan")
                .build();

        HintSyntaxValidator.SyntaxValidationResult syntaxResult =
                new HintSyntaxValidator.SyntaxValidationResult(true, Collections.emptyList());

        when(hintExtractor.extractHints(anyString()))
                .thenReturn(Collections.singletonList(hint1))
                .thenReturn(Collections.singletonList(hint2));
        when(hintReferenceLoader.getHintReference("tablescan")).thenReturn(scanRef);
        when(hintReferenceLoader.getHintReference("indexscan")).thenReturn(indexRef);
        when(hintSyntaxValidator.validateSyntax(anyString(), anyString())).thenReturn(syntaxResult);

        StoredProcedureAnalysis result = service.analyzeStoredProcedure(procedure, "multi_stmt_proc");

        assertNotNull(result);
        assertEquals(2, result.getTotalStatements());
        assertEquals(2, result.getTotalHints());
        assertEquals(2, result.getOverallSummary().getValidCount());
        assertEquals(2, result.getStatementAnalyses().size());
    }

    @Test
    void analyzeStoredProcedure_withEmptyProcedure_returnsEmptyAnalysis() {
        StoredProcedureAnalysis result = service.analyzeStoredProcedure("", "empty_proc");

        assertNotNull(result);
        assertEquals("empty_proc", result.getProcedureName());
        assertEquals(0, result.getTotalStatements());
        assertEquals(0, result.getTotalHints());
        assertEquals(0, result.getOverallSummary().getTotalHints());
    }

    @Test
    void analyzeStoredProcedure_withNullProcedure_returnsEmptyAnalysis() {
        StoredProcedureAnalysis result = service.analyzeStoredProcedure(null, "null_proc");

        assertNotNull(result);
        assertEquals("null_proc", result.getProcedureName());
        assertEquals(0, result.getTotalStatements());
    }

    @Test
    void analyzeStoredProcedure_withInvalidHint_reportsInvalidHint() {
        String procedure = "SELECT /*+ bad_hint */ * FROM t1";

        HintExtractor.ExtractedHint extractedHint = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ bad_hint */")
                .hintContent("bad_hint")
                .hintName("bad_hint")
                .lineNumber(1)
                .charOffset(8)
                .build();

        when(hintExtractor.extractHints(procedure)).thenReturn(Collections.singletonList(extractedHint));
        when(hintReferenceLoader.getHintReference("bad_hint")).thenReturn(null);
        when(hintReferenceLoader.getAllHints()).thenReturn(Collections.emptyList());

        StoredProcedureAnalysis result = service.analyzeStoredProcedure(procedure, "bad_hint_proc");

        assertNotNull(result);
        assertEquals(1, result.getTotalHints());
        assertEquals(0, result.getOverallSummary().getValidCount());
        assertEquals(1, result.getOverallSummary().getInvalidCount());
        assertEquals("NOT_IN_REFERENCE", result.getOverallSummary().getInvalidHints().get(0).getValidationStatus());
    }

    @Test
    void analyzeStoredProcedure_withDeclarationLines_skipsDeclarations() {
        String procedure = "DECLARE v_id INT;\nBEGIN\nSELECT /*+ tablescan(t1) */ * FROM t1;\nEND";

        HintExtractor.ExtractedHint extractedHint = HintExtractor.ExtractedHint.builder()
                .hintText("/*+ tablescan(t1) */")
                .hintContent("tablescan(t1)")
                .hintName("tablescan")
                .lineNumber(3)
                .charOffset(16)
                .build();

        HintReference hintRef = HintReference.builder()
                .hint("tablescan")
                .category("Scan")
                .build();

        HintSyntaxValidator.SyntaxValidationResult syntaxResult =
                new HintSyntaxValidator.SyntaxValidationResult(true, Collections.emptyList());

        when(hintExtractor.extractHints(anyString())).thenReturn(Collections.singletonList(extractedHint));
        when(hintReferenceLoader.getHintReference("tablescan")).thenReturn(hintRef);
        when(hintSyntaxValidator.validateSyntax(anyString(), anyString())).thenReturn(syntaxResult);

        StoredProcedureAnalysis result = service.analyzeStoredProcedure(procedure, "decl_proc");

        assertNotNull(result);
        assertEquals(1, result.getTotalStatements());
    }
}
