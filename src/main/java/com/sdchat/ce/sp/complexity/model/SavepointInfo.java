package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavepointInfo {

    private String name;

    private int lineNumber;

    private int nestingLevel;

    private boolean hasRollback;
}
