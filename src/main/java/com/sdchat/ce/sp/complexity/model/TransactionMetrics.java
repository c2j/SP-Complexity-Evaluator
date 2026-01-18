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
public class TransactionMetrics {

    private int transactionCount;

    private int savepointCount;

    private int commitCount;

    private int rollbackCount;

    private int maxNestingDepth;

    private double complexityScore;

    private boolean hasUnbalancedTransactions;

    private List<SavepointInfo> savepointDetails;
}
