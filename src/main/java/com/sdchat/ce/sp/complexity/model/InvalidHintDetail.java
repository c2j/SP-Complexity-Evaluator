package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidHintDetail {
    private String hintText;
    private int lineNumber;
    private String hintName;
    private String errorType;
    private String errorMessage;
}
