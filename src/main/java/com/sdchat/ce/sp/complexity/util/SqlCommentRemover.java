package com.sdchat.ce.sp.complexity.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for removing comments from SQL code.
 */
public class SqlCommentRemover {

    // Pattern for single-line comments (--...)
    private static final Pattern SINGLE_LINE_COMMENT_PATTERN = Pattern.compile("--.*?$", Pattern.MULTILINE);

    // Pattern for multi-line comments (/* ... */) - but NOT optimizer hints
    private static final Pattern MULTI_LINE_COMMENT_PATTERN = Pattern.compile("/\\*(?!\\+)[\\s\\S]*?\\*/");

    // Pattern for optimizer hints (/*+ ... */)
    private static final Pattern HINT_PATTERN = Pattern.compile("/\\*\\+[\\s\\S]*?\\*/");

    /**
     * Remove all comments from SQL code.
     *
     * @param sqlCode The SQL code with comments
     * @return The SQL code without comments
     */
    public static String removeComments(String sqlCode) {
        if (sqlCode == null || sqlCode.isEmpty()) {
            return sqlCode;
        }

        // First remove multi-line comments (but preserve optimizer hints)
        String result = removeMultiLineComments(sqlCode);

        // Then remove single-line comments
        result = removeSingleLineComments(result);

        return result;
    }

    /**
     * Remove all comments from SQL code EXCEPT optimizer hints.
     * This preserves optimizer hints while removing regular comments.
     *
     * @param sqlCode The SQL code with comments
     * @return The SQL code without regular comments, but with optimizer hints preserved
     */
    public static String removeCommentsPreserveHints(String sqlCode) {
        if (sqlCode == null || sqlCode.isEmpty()) {
            return sqlCode;
        }

        // First extract and temporarily replace optimizer hints with placeholders
        String result = sqlCode;
        String[] hints = new String[100];
        int hintIndex = 0;

        Matcher hintMatcher = HINT_PATTERN.matcher(result);
        while (hintMatcher.find() && hintIndex < hints.length) {
            hints[hintIndex] = hintMatcher.group();
            result = result.substring(0, hintMatcher.start()) + "___HINT_" + hintIndex + "___" + result.substring(hintMatcher.end());
            hintMatcher = HINT_PATTERN.matcher(result);
            hintIndex++;
        }

        // Now remove regular multi-line comments
        result = removeMultiLineComments(result);

        // Remove single-line comments
        result = removeSingleLineComments(result);

        // Restore optimizer hints
        for (int i = hintIndex - 1; i >= 0; i--) {
            result = result.replace("___HINT_" + i + "___", hints[i]);
        }

        return result;
    }

    /**
     * Remove single-line comments (--...) from SQL code.
     *
     * @param sqlCode The SQL code with single-line comments
     * @return The SQL code without single-line comments
     */
    private static String removeSingleLineComments(String sqlCode) {
        if (sqlCode == null || sqlCode.isEmpty()) {
            return sqlCode;
        }

        Matcher matcher = SINGLE_LINE_COMMENT_PATTERN.matcher(sqlCode);
        return matcher.replaceAll("");
    }

    /**
     * Remove multi-line comments from SQL code.
     *
     * @param sqlCode The SQL code with multi-line comments
     * @return The SQL code without multi-line comments
     */
    private static String removeMultiLineComments(String sqlCode) {
        if (sqlCode == null || sqlCode.isEmpty()) {
            return sqlCode;
        }

        Matcher matcher = MULTI_LINE_COMMENT_PATTERN.matcher(sqlCode);
        return matcher.replaceAll("");
    }
}
