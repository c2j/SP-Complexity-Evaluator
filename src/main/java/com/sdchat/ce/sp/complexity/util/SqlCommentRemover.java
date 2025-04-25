package com.sdchat.ce.sp.complexity.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for removing comments from SQL code.
 */
public class SqlCommentRemover {

    // Pattern for single-line comments (--...)
    private static final Pattern SINGLE_LINE_COMMENT_PATTERN = Pattern.compile("--.*?$", Pattern.MULTILINE);

    // Pattern for multi-line comments (/* ... */)
    private static final Pattern MULTI_LINE_COMMENT_PATTERN = Pattern.compile("/\\*[\\s\\S]*?\\*/");

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

        // First remove multi-line comments
        String result = removeMultiLineComments(sqlCode);

        // Then remove single-line comments
        result = removeSingleLineComments(result);

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
     * Remove multi-line comments (/* ... *\/) from SQL code.
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
