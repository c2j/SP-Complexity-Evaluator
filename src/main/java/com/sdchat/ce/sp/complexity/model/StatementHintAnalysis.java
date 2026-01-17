package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementHintAnalysis {

    private String statementId;

    private String statementText;

    private Integer lineNumber;

    private java.util.List<HintValidationResult> hints;

    private HintAnalysisSummary hintSummary;
}
