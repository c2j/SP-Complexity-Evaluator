package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtransactionContext {

    @Builder.Default
    private Stack<String> activeSavepoints = new Stack<>();
    @Builder.Default
    private int implicitDmlCount = 0;
    @Builder.Default
    private int loopMultiplier = 1;
    @Builder.Default
    private Set<String> calledProcedures = new HashSet<>();
    @Builder.Default
    private int maxNestingLevel = 0;
}
