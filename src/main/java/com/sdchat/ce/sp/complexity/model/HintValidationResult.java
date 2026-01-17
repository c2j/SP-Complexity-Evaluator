package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintValidationResult {

    private String hintText;

    private String validationStatus;

    private String category;

    private Integer lineNumber;

    private Integer charOffset;

    private String hintName;

    private String parameters;

    private String errorType;

    private String errorMessage;
}
