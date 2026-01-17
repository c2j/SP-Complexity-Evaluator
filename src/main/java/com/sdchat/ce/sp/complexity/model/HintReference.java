package com.sdchat.ce.sp.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintReference {

    private String hint;

    private String category;

    private String description;

    private String syntax;

    private java.util.List<HintParameter> parameters;

    private String example;

    private String notes;
}
