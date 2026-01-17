package com.sdchat.ce.sp.complexity.service;

import com.sdchat.ce.sp.complexity.evaluator.HintExtractor;
import com.sdchat.ce.sp.complexity.model.*;
import com.sdchat.ce.sp.complexity.parser.HintReferenceLoader;
import com.sdchat.ce.sp.complexity.util.HintSyntaxValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HintValidationService {

    private final HintReferenceLoader hintReferenceLoader;
    private final HintExtractor hintExtractor;
    private final HintSyntaxValidator hintSyntaxValidator;

    public HintValidationService(
            HintReferenceLoader hintReferenceLoader,
            HintExtractor hintExtractor,
            HintSyntaxValidator hintSyntaxValidator) {
        this.hintReferenceLoader = hintReferenceLoader;
        this.hintExtractor = hintExtractor;
        this.hintSyntaxValidator = hintSyntaxValidator;
    }

    public HintAnalysisSummary validateSqlStatement(String sql) {
        long startTime = System.currentTimeMillis();

        if (sql == null || sql.trim().isEmpty()) {
            return buildEmptySummary();
        }

        List<HintValidationResult> validHints = new ArrayList<>();
        List<HintValidationResult> invalidHints = new ArrayList<>();
        Map<String, Integer> categoryBreakdown = new HashMap<>();
        Map<String, Integer> errorTypeBreakdown = new HashMap<>();

        List<HintExtractor.ExtractedHint> extractedHints = hintExtractor.extractHints(sql);

        for (HintExtractor.ExtractedHint extracted : extractedHints) {
            HintValidationResult result = validateSingleHint(extracted);
            if (result != null) {
                if ("VALID".equals(result.getValidationStatus())) {
                    validHints.add(result);
                    String category = result.getCategory();
                    if (category != null) {
                        categoryBreakdown.merge(category, 1, Integer::sum);
                    }
                } else {
                    invalidHints.add(result);
                    String errorType = result.getErrorType();
                    if (errorType != null) {
                        errorTypeBreakdown.merge(errorType, 1, Integer::sum);
                    }
                }
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;
        log.debug("Validated SQL statement with {} hints in {}ms", extractedHints.size(), processingTime);

        return HintAnalysisSummary.builder()
                .totalHints(extractedHints.size())
                .validCount(validHints.size())
                .invalidCount(invalidHints.size())
                .validHints(validHints)
                .invalidHints(invalidHints)
                .categoryBreakdown(categoryBreakdown)
                .errorTypeBreakdown(errorTypeBreakdown)
                .build();
    }

    private HintValidationResult validateSingleHint(HintExtractor.ExtractedHint extracted) {
        String hintName = extracted.getHintName();

        if (hintName == null) {
            return HintValidationResult.builder()
                    .hintText(extracted.getHintText())
                    .validationStatus("SYNTAX_ERROR")
                    .lineNumber(extracted.getLineNumber())
                    .charOffset(extracted.getCharOffset())
                    .errorType("MALFORMED_HINT")
                    .errorMessage("Could not parse hint name from: " + extracted.getHintContent())
                    .build();
        }

        HintReference ref = hintReferenceLoader.getHintReference(hintName);

        if (ref == null) {
            StringBuilder sb = new StringBuilder();
            List<String> hintNames = hintReferenceLoader.getAllHints().stream()
                    .map(HintReference::getHint)
                    .sorted()
                    .limit(10)
                    .collect(Collectors.toList());
            sb.append("Unknown hint '").append(hintName).append("'. ");
            if (!hintNames.isEmpty()) {
                sb.append("Valid hints include: ").append(String.join(", ", hintNames));
                if (hintReferenceLoader.getHintCount() > 10) {
                    sb.append(" and ").append(hintReferenceLoader.getHintCount() - 10).append(" more...");
                }
            }

            return HintValidationResult.builder()
                    .hintText(extracted.getHintText())
                    .validationStatus("NOT_IN_REFERENCE")
                    .hintName(hintName)
                    .parameters(extractParameters(extracted.getHintContent()))
                    .lineNumber(extracted.getLineNumber())
                    .charOffset(extracted.getCharOffset())
                    .errorType("UNKNOWN_HINT")
                    .errorMessage(sb.toString())
                    .build();
        }

        HintSyntaxValidator.SyntaxValidationResult syntaxResult =
                hintSyntaxValidator.validateSyntax(extracted.getHintContent(), hintName);

        if (!syntaxResult.isValid()) {
            return HintValidationResult.builder()
                    .hintText(extracted.getHintText())
                    .validationStatus("SYNTAX_ERROR")
                    .hintName(hintName)
                    .parameters(extractParameters(extracted.getHintContent()))
                    .category(ref.getCategory())
                    .lineNumber(extracted.getLineNumber())
                    .charOffset(extracted.getCharOffset())
                    .errorType("SYNTAX_ERROR")
                    .errorMessage(syntaxResult.getErrorMessage())
                    .build();
        }

        return HintValidationResult.builder()
                .hintText(extracted.getHintText())
                .validationStatus("VALID")
                .hintName(hintName)
                .parameters(extractParameters(extracted.getHintContent()))
                .category(ref.getCategory())
                .lineNumber(extracted.getLineNumber())
                .charOffset(extracted.getCharOffset())
                .build();
    }

    private String extractParameters(String hintContent) {
        if (hintContent == null) return null;
        int parenStart = hintContent.indexOf('(');
        int parenEnd = hintContent.lastIndexOf(')');
        if (parenStart >= 0 && parenEnd > parenStart) {
            return hintContent.substring(parenStart + 1, parenEnd).trim();
        }
        return null;
    }

    private HintAnalysisSummary buildEmptySummary() {
        return HintAnalysisSummary.builder()
                .totalHints(0)
                .validCount(0)
                .invalidCount(0)
                .validHints(Collections.emptyList())
                .invalidHints(Collections.emptyList())
                .categoryBreakdown(new HashMap<>())
                .errorTypeBreakdown(new HashMap<>())
                .build();
    }

    public HintValidationResponse buildValidationResponse(
            HintAnalysisSummary analysis,
            String format,
            long processingTimeMs) {

        return HintValidationResponse.builder()
                .success(true)
                .format(format)
                .analysis(analysis)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    public ErrorResponse buildErrorResponse(String errorMessage, String errorCode) {
        return ErrorResponse.builder()
                .success(false)
                .error(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public ErrorResponse buildErrorResponse(String errorMessage, String errorCode, String details) {
        return ErrorResponse.builder()
                .success(false)
                .error(errorMessage)
                .errorCode(errorCode)
                .details(details)
                .build();
    }

    public List<HintReference> getHintReferences() {
        return hintReferenceLoader.getAllHints();
    }

    public int getHintReferenceCount() {
        return hintReferenceLoader.getHintCount();
    }

    public StoredProcedureAnalysis analyzeStoredProcedure(String procedureCode, String procedureName) {
        long startTime = System.currentTimeMillis();

        if (procedureCode == null || procedureCode.trim().isEmpty()) {
            return buildEmptyProcedureAnalysis(procedureName);
        }

        List<StatementHintAnalysis> statementAnalyses = new ArrayList<>();
        List<ParseError> parseErrors = new ArrayList<>();
        List<HintValidationResult> allValidHints = new ArrayList<>();
        List<HintValidationResult> allInvalidHints = new ArrayList<>();
        Map<String, Integer> categoryBreakdown = new HashMap<>();
        Map<String, Integer> errorTypeBreakdown = new HashMap<>();

        String[] lines = procedureCode.split("\\r?\\n");
        int statementCounter = 0;
        int globalOffset = 0;

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum].trim();

            if (line.isEmpty() || isDeclarationLine(line)) {
                continue;
            }

            statementCounter++;
            String statementId = "stmt_" + statementCounter;

            try {
                HintAnalysisSummary summary = validateSqlStatement(line);

                List<HintValidationResult> statementHints = new ArrayList<>();
                statementHints.addAll(summary.getValidHints());
                statementHints.addAll(summary.getInvalidHints());

                statementAnalyses.add(StatementHintAnalysis.builder()
                        .statementId(statementId)
                        .statementText(line.length() > 100 ? line.substring(0, 100) + "..." : line)
                        .lineNumber(lineNum + 1)
                        .hints(statementHints)
                        .hintSummary(summary)
                        .build());

                allValidHints.addAll(summary.getValidHints());
                allInvalidHints.addAll(summary.getInvalidHints());

                for (HintValidationResult hint : summary.getValidHints()) {
                    if (hint.getCategory() != null) {
                        categoryBreakdown.merge(hint.getCategory(), 1, Integer::sum);
                    }
                }
                for (HintValidationResult hint : summary.getInvalidHints()) {
                    if (hint.getErrorType() != null) {
                        errorTypeBreakdown.merge(hint.getErrorType(), 1, Integer::sum);
                    }
                }

            } catch (Exception e) {
                log.warn("Error parsing statement at line {}: {}", lineNum + 1, e.getMessage());
                parseErrors.add(ParseError.builder()
                        .statementId(statementId)
                        .lineNumber(lineNum + 1)
                        .errorMessage("Parse error: " + e.getMessage())
                        .partialText(line.length() > 100 ? line.substring(0, 100) : line)
                        .build());
            }

            globalOffset += lines[lineNum].length() + 1;
        }

        HintAnalysisSummary overallSummary = HintAnalysisSummary.builder()
                .totalHints(allValidHints.size() + allInvalidHints.size())
                .validCount(allValidHints.size())
                .invalidCount(allInvalidHints.size())
                .validHints(allValidHints)
                .invalidHints(allInvalidHints)
                .categoryBreakdown(categoryBreakdown)
                .errorTypeBreakdown(errorTypeBreakdown)
                .build();

        long processingTime = System.currentTimeMillis() - startTime;
        log.debug("Analyzed stored procedure with {} statements, {} hints in {}ms",
                statementCounter, allValidHints.size() + allInvalidHints.size(), processingTime);

        return StoredProcedureAnalysis.builder()
                .procedureName(procedureName)
                .totalStatements(statementCounter)
                .totalHints(allValidHints.size() + allInvalidHints.size())
                .statementAnalyses(statementAnalyses)
                .overallSummary(overallSummary)
                .parseErrors(parseErrors)
                .build();
    }

    private boolean isDeclarationLine(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("declare") ||
                lower.startsWith("variable") ||
                lower.startsWith("begin") ||
                lower.startsWith("end") ||
                lower.startsWith("create procedure") ||
                lower.startsWith("create function") ||
                lower.startsWith("create or replace");
    }

    private StoredProcedureAnalysis buildEmptyProcedureAnalysis(String procedureName) {
        return StoredProcedureAnalysis.builder()
                .procedureName(procedureName)
                .totalStatements(0)
                .totalHints(0)
                .statementAnalyses(new ArrayList<>())
                .overallSummary(buildEmptySummary())
                .parseErrors(new ArrayList<>())
                .build();
    }
}
