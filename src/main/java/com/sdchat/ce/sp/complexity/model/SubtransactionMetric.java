package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtransactionMetric {

    private String name;
    private SubtransactionType type;
    private List<String> dmlStatements;
    private List<String> savepointOperations;
    private Integer nestingLevel;
    private String sourceProcedure;
    private Integer sourceLine;
}
