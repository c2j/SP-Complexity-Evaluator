package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintValidationResponse {

    private Boolean success;

    private String format;

    private HintAnalysisSummary analysis;

    private StoredProcedureAnalysis storedProcedureAnalysis;

    private String downloadUrl;

    private String errorMessage;

    private Long processingTimeMs;
}
