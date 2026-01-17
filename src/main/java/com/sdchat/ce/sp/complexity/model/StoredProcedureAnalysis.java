package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredProcedureAnalysis {

    private String procedureName;

    private Integer totalStatements;

    private Integer totalHints;

    private List<StatementHintAnalysis> statementAnalyses;

    private HintAnalysisSummary overallSummary;

    private List<ParseError> parseErrors;
}
