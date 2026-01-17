package com.sdchat.ce.sp.complexity.util;

import com.sdchat.ce.sp.complexity.model.HintParameter;
import com.sdchat.ce.sp.complexity.model.HintReference;
import com.sdchat.ce.sp.complexity.parser.HintReferenceLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class HintSyntaxValidator {

    private final HintReferenceLoader hintReferenceLoader;

    public HintSyntaxValidator(HintReferenceLoader hintReferenceLoader) {
        this.hintReferenceLoader = hintReferenceLoader;
    }

    public SyntaxValidationResult validateSyntax(String hintContent, String hintName) {
        List<String> errors = new ArrayList<>();

        if (hintContent == null || hintContent.trim().isEmpty()) {
            errors.add("Empty hint content");
            return new SyntaxValidationResult(false, errors);
        }

        if (hintName == null || hintName.isEmpty()) {
            errors.add("Could not extract hint name");
            return new SyntaxValidationResult(false, errors);
        }

        HintReference ref = hintReferenceLoader.getHintReference(hintName);
        if (ref == null) {
            errors.add("Unknown hint: " + hintName);
            return new SyntaxValidationResult(false, errors);
        }

        String syntax = ref.getSyntax();
        if (syntax != null && !syntax.isEmpty()) {
            SyntaxAnalysis analysis = analyzeSyntax(hintContent, syntax, ref, hintName);

            if (!analysis.isValid()) {
                errors.addAll(analysis.getErrors());
            }

            return new SyntaxValidationResult(errors.isEmpty(), errors, analysis.getWarning());
        }

        return new SyntaxValidationResult(true, errors);
    }

    private SyntaxAnalysis analyzeSyntax(String hintContent, String syntax, HintReference ref, String hintName) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        int openParens = countChar(hintContent, '(');
        int closeParens = countChar(hintContent, ')');

        if (openParens != closeParens) {
            errors.add("Unbalanced parentheses: " + openParens + " '(' and " + closeParens + " ')'");
        }

        if (syntax.startsWith("[no]")) {
            String baseName = hintName.replaceFirst("^(no_)", "");
            if (!hintName.toLowerCase().startsWith("no_") && !hintName.toLowerCase().equals(baseName.toLowerCase())) {
                warnings.add("Hint may expect 'no_' prefix for negation");
            }
        }

        if (ref.getParameters() != null) {
            List<HintParameter> requiredParams = new ArrayList<>();
            for (HintParameter param : ref.getParameters()) {
                if (param.getRequired() != null && param.getRequired()) {
                    requiredParams.add(param);
                }
            }

            if (!requiredParams.isEmpty()) {
                boolean hasParams = hintContent.contains("(") && hintContent.contains(")");
                if (!hasParams) {
                    errors.add("Required parameters missing: " + requiredParams.stream()
                            .map(HintParameter::getName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("unknown"));
                }
            }
        }

        if (hintContent.contains("{") || hintContent.contains("}")) {
            warnings.add("Syntax uses braces which may not be supported in all contexts");
        }

        return new SyntaxAnalysis(errors.isEmpty(), errors, warnings);
    }

    private int countChar(String str, char ch) {
        if (str == null) return 0;
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) count++;
        }
        return count;
    }

    public static class SyntaxValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final String warning;

        public SyntaxValidationResult(boolean valid, List<String> errors) {
            this(valid, errors, null);
        }

        public SyntaxValidationResult(boolean valid, List<String> errors, String warning) {
            this.valid = valid;
            this.errors = errors;
            this.warning = warning;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getWarning() {
            return warning;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    private static class SyntaxAnalysis {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public SyntaxAnalysis(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getWarning() {
            return warnings != null && !warnings.isEmpty() ? String.join("; ", warnings) : null;
        }
    }
}
