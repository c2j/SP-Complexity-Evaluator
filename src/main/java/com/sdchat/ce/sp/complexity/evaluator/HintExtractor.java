package com.sdchat.ce.sp.complexity.evaluator;

import com.sdchat.ce.sp.complexity.model.HintValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class HintExtractor {

    private static final Pattern HINT_PATTERN = Pattern.compile(
            "/\\*\\+\\s*(.*?)\\s*\\*/",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern HINT_NAME_PATTERN = Pattern.compile(
            "^([a-zA-Z_][a-zA-Z0-9_]*)\\s*(\\()?",
            Pattern.CASE_INSENSITIVE
    );

    public List<ExtractedHint> extractHints(String sqlText) {
        List<ExtractedHint> hints = new ArrayList<>();

        if (sqlText == null || sqlText.isEmpty()) {
            return hints;
        }

        String[] lines = sqlText.split("\\r?\\n");
        int globalOffset = 0;

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];
            Matcher matcher = HINT_PATTERN.matcher(line);

            while (matcher.find()) {
                int lineStart = line.lastIndexOf("/*+", matcher.start());
                int lineEnd = line.indexOf("*/", matcher.end());

                String hintContent = matcher.group(1).trim();
                int charOffset = lineStart >= 0 ? lineStart : matcher.start();

                ExtractedHint.ExtractedHintBuilder builder = ExtractedHint.builder()
                        .hintText(matcher.group(0))
                        .hintContent(hintContent)
                        .lineNumber(lineNum + 1)
                        .charOffset(globalOffset + charOffset);

                String hintName = extractHintName(hintContent);
                builder.hintName(hintName);

                hints.add(builder.build());
            }

            globalOffset += line.length() + 1;
        }

        log.debug("Extracted {} hints from SQL", hints.size());
        return hints;
    }

    private String extractHintName(String hintContent) {
        if (hintContent == null || hintContent.isEmpty()) {
            return null;
        }

        Matcher matcher = HINT_NAME_PATTERN.matcher(hintContent);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return null;
    }

    public static class ExtractedHint {
        private final String hintText;
        private final String hintContent;
        private final String hintName;
        private final Integer lineNumber;
        private final Integer charOffset;

        public ExtractedHint(ExtractedHintBuilder builder) {
            this.hintText = builder.hintText;
            this.hintContent = builder.hintContent;
            this.hintName = builder.hintName;
            this.lineNumber = builder.lineNumber;
            this.charOffset = builder.charOffset;
        }

        public String getHintText() {
            return hintText;
        }

        public String getHintContent() {
            return hintContent;
        }

        public String getHintName() {
            return hintName;
        }

        public Integer getLineNumber() {
            return lineNumber;
        }

        public Integer getCharOffset() {
            return charOffset;
        }

        public static ExtractedHintBuilder builder() {
            return new ExtractedHintBuilder();
        }

        public static class ExtractedHintBuilder {
            private String hintText;
            private String hintContent;
            private String hintName;
            private Integer lineNumber;
            private Integer charOffset;

            public ExtractedHintBuilder hintText(String hintText) {
                this.hintText = hintText;
                return this;
            }

            public ExtractedHintBuilder hintContent(String hintContent) {
                this.hintContent = hintContent;
                return this;
            }

            public ExtractedHintBuilder hintName(String hintName) {
                this.hintName = hintName;
                return this;
            }

            public ExtractedHintBuilder lineNumber(Integer lineNumber) {
                this.lineNumber = lineNumber;
                return this;
            }

            public ExtractedHintBuilder charOffset(Integer charOffset) {
                this.charOffset = charOffset;
                return this;
            }

            public ExtractedHint build() {
                return new ExtractedHint(this);
            }
        }
    }
}
