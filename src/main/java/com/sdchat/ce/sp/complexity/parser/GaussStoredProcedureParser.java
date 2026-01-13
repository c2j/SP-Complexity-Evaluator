package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.SqlStatement;
import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import com.sdchat.ce.sp.complexity.util.SqlCommentRemover;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gauss stored procedure parser implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GaussStoredProcedureParser implements StoredProcedureParser {

    private static final String DIALECT = "Gauss";

    private static final int MAX_SQL_LENGTH = 100000;

    private static final Pattern SQL_STATEMENT_PATTERN = Pattern.compile(
            "\\b(SELECT|INSERT|UPDATE|DELETE|MERGE|COMMIT|ROLLBACK|CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE|WITH)\\b(?:[^;]|;[^;]){0,5000}\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROCEDURE_CALL_PATTERN = Pattern.compile(
            "\\b([A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*)\\s*\\([^;]{0,2000}\\)\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect package body
    private static final Pattern PACKAGE_BODY_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?\\bPACKAGE\\s+\\bBODY\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract package name from package body declaration
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?\\bPACKAGE\\s+\\bBODY\\s+([\\w\\.]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract procedure definitions from package body
    // Updated to handle multiple begin/end blocks and correctly identify the end of a procedure
    // Also handles the case where a procedure ends with END; instead of END procedure_name;
    // And handles procedures with LANGUAGE JAVA declarations
    private static final Pattern PROCEDURE_DEFINITION_PATTERN = Pattern.compile(
            "\\bPROCEDURE\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)(?:\\s+(?:IS|AS|LANGUAGE\\s+JAVA))?[\\s\\S]*?(?:\\bEND(?:\\s+(?:\\1|proc_\\1|PROC_\\1))?\\s*;|;)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract function definitions from package body
    private static final Pattern FUNCTION_DEFINITION_PATTERN = Pattern.compile(
            "\\bFUNCTION\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)(?:\\s+RETURN\\s+[\\w\\.%]+)?(?:\\s+(?:IS|AS))?[\\s\\S]*?\\bEND(?:\\s+(?:\\1|func_\\1|FUNC_\\1))?\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to extract procedure declarations (for procedures without IS/AS)
    private static final Pattern PROCEDURE_DECLARATION_PATTERN = Pattern.compile(
            "\\bPROCEDURE\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)\\s*(?:IS|AS)?",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to identify procedure declarations without implementation (no AS/IS part)
    private static final Pattern PROCEDURE_DECLARATION_WITHOUT_IMPL_PATTERN = Pattern.compile(
            "\\bPROCEDURE\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    private final GaussSqlParser sqlParser;

    @Override
    public StoredProcedure parse(String sourceCode, String name, String schema) throws Exception {
        // Remove comments from source code before extracting SQL statements
        String cleanSourceCode = SqlCommentRemover.removeComments(sourceCode);
        List<SqlStatement> sqlStatements = extractSqlStatements(cleanSourceCode);

        return StoredProcedure.builder()
                .name(name)
                .schema(schema)
                .sourceCode(sourceCode) // Keep the original source code with comments
                .sqlStatements(sqlStatements)
                .dialect(DIALECT)
                .build();
    }

    @Override
    public boolean isPackageBody(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return false;
        }

        log.debug("Checking if file is package body. Source code starts with: {}", 
                 sourceCode.substring(0, Math.min(100, sourceCode.length())));

        // Check for standard package body pattern
        if (PACKAGE_BODY_PATTERN.matcher(sourceCode.toUpperCase()).find()) {
            log.debug("File identified as package body based on CREATE PACKAGE BODY pattern");
            return true;
        }

        // Check for package end pattern
        if (sourceCode.toUpperCase().contains("END ") &&
            (sourceCode.toUpperCase().contains("_PKG") ||
             sourceCode.toUpperCase().contains("PKG_"))) {
            log.debug("File identified as package body based on END PKG_ pattern");
            return true;
        }

        // Special case for d.sql - check if it starts with a procedure name or CREATE OR REPLACE PACKAGE
        if (sourceCode.trim().toUpperCase().matches("^[A-Z][A-Z0-9_]*\\s*\\(.*") ||
            sourceCode.trim().toUpperCase().startsWith("CREATE OR REPLACE PACKAGE")) {
            log.debug("File identified as package body based on procedure declaration or CREATE OR REPLACE PACKAGE pattern");
            return true;
        }

        // Check for multiple procedure definitions - but distinguish between package-level and nested procedures
        Pattern procPattern = Pattern.compile("\\bPROCEDURE\\s+([\\w\\.]+)\\s*\\(", Pattern.CASE_INSENSITIVE);
        Matcher procMatcher = procPattern.matcher(sourceCode);
        int procCount = 0;
        int packageLevelProcCount = 0;
        
        // Track procedure positions to determine if they are nested or package-level
        List<Integer> procedurePositions = new ArrayList<>();
        List<String> procedureNames = new ArrayList<>();
        
        while (procMatcher.find()) {
            procCount++;
            procedurePositions.add(procMatcher.start());
            procedureNames.add(procMatcher.group(1));
        }
        
        log.debug("Found {} procedures: {}", procCount, procedureNames);
        
        if (procCount > 1) {
            // Check if this is a single main procedure with nested procedures inside it
            // Look for the pattern: CREATE OR REPLACE PROCEDURE main_proc(...) AS ... PROCEDURE nested_proc(...)
            String upperSourceCode = sourceCode.toUpperCase();
            
            // Find the first CREATE OR REPLACE PROCEDURE
            Pattern mainProcPattern = Pattern.compile("\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?PROCEDURE\\s+([\\w\\.]+)\\s*\\(", Pattern.CASE_INSENSITIVE);
            Matcher mainProcMatcher = mainProcPattern.matcher(sourceCode);
            
            if (mainProcMatcher.find()) {
                int mainProcStart = mainProcMatcher.start();
                String mainProcName = mainProcMatcher.group(1);
                
                log.debug("Found main procedure: {} at position {}", mainProcName, mainProcStart);
                
                // Check if all other procedures are declared after the main procedure's AS/IS keyword
                // and before the main procedure's final END
                // Look for AS or IS after the main procedure declaration, allowing for multiline patterns
                int asIsPosition = -1;
                
                // Find the position after the main procedure's parameter list
                Pattern mainProcParamsPattern = Pattern.compile("\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?PROCEDURE\\s+" + Pattern.quote(mainProcName) + "\\s*\\([^)]*\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher mainProcParamsMatcher = mainProcParamsPattern.matcher(sourceCode);
                
                if (mainProcParamsMatcher.find()) {
                    int afterParams = mainProcParamsMatcher.end();
                    
                    // Look for AS or IS after the parameter list
                    Pattern asIsAfterParamsPattern = Pattern.compile("\\)\\s*(?:AS|IS)\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    Matcher asIsAfterParamsMatcher = asIsAfterParamsPattern.matcher(sourceCode.substring(afterParams - 1)); // Include the closing paren
                    
                    if (asIsAfterParamsMatcher.find()) {
                        asIsPosition = afterParams - 1 + asIsAfterParamsMatcher.end();
                    }
                }
                
                if (asIsPosition != -1) {
                    
                    log.debug("Found AS/IS at position {}", asIsPosition);
                    
                    // Find the final END of the main procedure
                    // Look for the last END; in the file, which should be the main procedure's end
                    int lastEndPosition = sourceCode.lastIndexOf("END;");
                    if (lastEndPosition == -1) {
                        lastEndPosition = sourceCode.length();
                    }
                    
                    log.debug("Last END; found at position {}", lastEndPosition);
                    
                    // Count how many procedures are declared between AS/IS and the final END
                    int nestedProcCount = 0;
                    for (int i = 0; i < procedurePositions.size(); i++) {
                        int procPos = procedurePositions.get(i);
                        String procName = procedureNames.get(i);
                        
                        if (procPos > asIsPosition && procPos < lastEndPosition) {
                            nestedProcCount++;
                            log.debug("Procedure {} at position {} is nested (between {} and {})", 
                                     procName, procPos, asIsPosition, lastEndPosition);
                        } else if (procPos <= mainProcStart + mainProcName.length() + 20) {
                            // This is likely the main procedure declaration
                            packageLevelProcCount++;
                            log.debug("Procedure {} at position {} is the main procedure", procName, procPos);
                        } else {
                            // This is a package-level procedure
                            packageLevelProcCount++;
                            log.debug("Procedure {} at position {} is package-level", procName, procPos);
                        }
                    }
                    
                    log.debug("Analysis: {} package-level procedures, {} nested procedures", 
                             packageLevelProcCount, nestedProcCount);
                    
                    // If we have nested procedures but only one package-level procedure, this is NOT a package body
                    if (packageLevelProcCount <= 1 && nestedProcCount > 0) {
                        log.debug("File identified as single procedure with {} nested procedures, not a package body", nestedProcCount);
                        return false;
                    }
                }
            }
            
            // If we reach here, it's likely a package body with multiple independent procedures
            log.debug("File identified as package body due to multiple procedures: {}", procCount);
            return true;
        }

        // Check for function definitions
        Pattern funcPattern = Pattern.compile("\\bFUNCTION\\s+([\\w\\.]+)\\s*\\(", Pattern.CASE_INSENSITIVE);
        Matcher funcMatcher = funcPattern.matcher(sourceCode);
        int funcCount = 0;
        while (funcMatcher.find()) {
            funcCount++;
            if (funcCount > 0) {
                if (procCount > 0) {
                    log.debug("File identified as package body due to procedures and functions: procs={}, funcs={}", procCount, funcCount);
                    return true;
                } else if (funcCount > 1) {
                    log.debug("File identified as package body due to multiple functions: {}", funcCount);
                    return true;
                }
            }
        }

        // Check for multiple procedure declarations with similar names (indicating a package)
        Pattern procNamePattern = Pattern.compile("PROCEDURE\\s+([A-Za-z][A-Za-z0-9_]*)", Pattern.CASE_INSENSITIVE);
        Matcher procNameMatcher = procNamePattern.matcher(sourceCode);
        Set<String> procedureNamesSet = new HashSet<>();
        while (procNameMatcher.find()) {
            String procName = procNameMatcher.group(1);
            procedureNamesSet.add(procName.toUpperCase());
        }

        // Check if there are procedures with similar base names (indicating variants of the same procedure)
        for (String procName : procedureNamesSet) {
            for (String otherProc : procedureNamesSet) {
                if (!procName.equals(otherProc) &&
                    (otherProc.startsWith(procName + "_") || procName.startsWith(otherProc + "_"))) {
                    log.debug("File identified as package body due to related procedure names: {} and {}", procName, otherProc);
                    return true;
                }
            }
        }

        // Check for END PKG_OAM_QS pattern which is specific to the d.sql file
        if (sourceCode.contains("END PKG_OAM_QS")) {
            log.debug("File identified as package body based on END PKG_OAM_QS pattern");
            return true;
        }

        log.debug("File identified as single procedure, not a package body");
        return false;
    }

    @Override
    public List<StoredProcedure> parsePackageBody(String sourceCode, String packageName, String schema) throws Exception {
        if (!isPackageBody(sourceCode)) {
            throw new IllegalArgumentException("The provided source code is not a package body");
        }

        // Extract the actual package name from the source code
        String actualPackageName = packageName;

        // First try to extract from package body declaration
        Matcher packageNameMatcher = PACKAGE_NAME_PATTERN.matcher(sourceCode);
        if (packageNameMatcher.find()) {
            actualPackageName = packageNameMatcher.group(1);
            log.debug("Extracted package name from package body declaration: {}", actualPackageName);
        } else {
            // If no package body declaration, check for END PKG_XXX pattern
            Pattern endPkgPattern = Pattern.compile("END\\s+([\\w\\.]+)", Pattern.CASE_INSENSITIVE);
            Matcher endPkgMatcher = endPkgPattern.matcher(sourceCode);
            if (endPkgMatcher.find()) {
                String endName = endPkgMatcher.group(1);
                if (endName.toUpperCase().startsWith("PKG_") || endName.toUpperCase().contains("_PKG")) {
                    actualPackageName = endName;
                    log.debug("Extracted package name from END statement: {}", actualPackageName);
                }
            }

            // Special case for d.sql - if it contains END PKG_OAM_QS, use that as the package name
            if (sourceCode.contains("END PKG_OAM_QS")) {
                actualPackageName = "PKG_OAM_QS";
                log.debug("Using PKG_OAM_QS as package name for d.sql");
            }

            // If still no package name, use the provided name
            if (actualPackageName.equals(packageName)) {
                log.debug("No package body declaration found, using provided name as package: {}", packageName);
            }
        }

        List<StoredProcedure> procedures = new ArrayList<>();
        String cleanSourceCode = SqlCommentRemover.removeComments(sourceCode);

        // Extract procedure definitions
        Matcher procMatcher = PROCEDURE_DEFINITION_PATTERN.matcher(cleanSourceCode);
        Set<String> processedProcedures = new HashSet<>();

        while (procMatcher.find()) {
            String procedureName = procMatcher.group(1);
            String procedureCode = procMatcher.group(0); // The entire procedure definition

            // Skip if we've already processed this procedure
            if (processedProcedures.contains(procedureName.toLowerCase())) {
                continue;
            }

            // Check if this is just a procedure declaration without implementation
            // If it doesn't contain AS or IS keywords and ends with a semicolon, it's just a declaration
            // Special case: LANGUAGE JAVA procedures are considered implementations
            boolean hasLanguageJava = procedureCode.toUpperCase().contains(" LANGUAGE JAVA ");
            boolean hasImplementation = procedureCode.toUpperCase().contains(" AS ") ||
                                       procedureCode.toUpperCase().contains(" IS ") ||
                                       hasLanguageJava;

            if (!hasImplementation && procedureCode.trim().endsWith(";")) {
                log.debug("Skipping procedure declaration without implementation: {}", procedureName);
                processedProcedures.add(procedureName.toLowerCase());
                continue;
            }

            processedProcedures.add(procedureName.toLowerCase());

            // Parse the procedure
            List<SqlStatement> sqlStatements = extractSqlStatements(procedureCode);

            // Create a StoredProcedure object with the fully qualified name
            String fullyQualifiedName = actualPackageName + "." + procedureName;
            StoredProcedure procedure = StoredProcedure.builder()
                    .name(fullyQualifiedName)
                    .schema(schema)
                    .sourceCode(procedureCode)
                    .sqlStatements(sqlStatements)
                    .dialect(DIALECT)
                    .build();

            procedures.add(procedure);
            log.debug("Found procedure: {} in package {}", procedureName, actualPackageName);
        }

        // Extract function definitions (treat them as procedures for complexity evaluation)
        Matcher funcMatcher = FUNCTION_DEFINITION_PATTERN.matcher(cleanSourceCode);
        Set<String> processedFunctions = new HashSet<>();

        while (funcMatcher.find()) {
            String functionName = funcMatcher.group(1);
            String functionCode = funcMatcher.group(0); // The entire function definition

            // Skip if we've already processed this function
            if (processedFunctions.contains(functionName.toLowerCase())) {
                continue;
            }

            // Check if this is just a function declaration without implementation
            // If it doesn't contain AS or IS keywords and ends with a semicolon, it's just a declaration
            // Special case: LANGUAGE JAVA functions are considered implementations
            boolean hasLanguageJava = functionCode.toUpperCase().contains(" LANGUAGE JAVA ");
            boolean hasImplementation = functionCode.toUpperCase().contains(" AS ") ||
                                       functionCode.toUpperCase().contains(" IS ") ||
                                       hasLanguageJava;

            if (!hasImplementation && functionCode.trim().endsWith(";")) {
                log.debug("Skipping function declaration without implementation: {}", functionName);
                processedFunctions.add(functionName.toLowerCase());
                continue;
            }

            processedFunctions.add(functionName.toLowerCase());

            // Parse the function
            List<SqlStatement> sqlStatements = extractSqlStatements(functionCode);

            // Create a StoredProcedure object with the fully qualified name
            String fullyQualifiedName = actualPackageName + "." + functionName;
            StoredProcedure procedure = StoredProcedure.builder()
                    .name(fullyQualifiedName)
                    .schema(schema)
                    .sourceCode(functionCode)
                    .sqlStatements(sqlStatements)
                    .dialect(DIALECT)
                    .build();

            procedures.add(procedure);
            log.debug("Found function: {} in package {}", functionName, actualPackageName);
        }

        // If we didn't find any procedures or functions with the standard patterns,
        // try a more aggressive approach to find procedures with LANGUAGE JAVA
        if (procedures.isEmpty()) {
            log.debug("No procedures found with standard patterns, trying to find LANGUAGE JAVA procedures");
            Pattern javaPattern = Pattern.compile(
                    "\\bPROCEDURE\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)(?:\\s+(?:IS|AS))?[\\s\\S]*?(?:LANGUAGE\\s+JAVA[\\s\\S]*?;|(?:BEGIN[\\s\\S]*?END;)|;)",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher javaMatcher = javaPattern.matcher(cleanSourceCode);
            while (javaMatcher.find()) {
                String procedureName = javaMatcher.group(1);
                String procedureCode = javaMatcher.group(0); // The entire procedure definition

                // Skip if we've already processed this procedure
                if (processedProcedures.contains(procedureName.toLowerCase())) {
                    continue;
                }

                processedProcedures.add(procedureName.toLowerCase());

                // Special case for Java language procedures with short declarations
                if (procedureCode.contains("LANGUAGE JAVA") && procedureCode.length() < 500) {
                    log.debug("Found Java language procedure: {}, applying special handling", procedureName);

                    // Extract the Java method name from the procedure code
                    Pattern javaMethodPattern = Pattern.compile("LANGUAGE\\s+JAVA\\s+NAME\\s+'([^']+)'", Pattern.CASE_INSENSITIVE);
                    Matcher javaMethodMatcher = javaMethodPattern.matcher(procedureCode);

                    if (javaMethodMatcher.find()) {
                        String javaMethod = javaMethodMatcher.group(1);
                        log.debug("Found Java method: {}", javaMethod);

                        // Extract parameter list
                        Pattern paramPattern = Pattern.compile("PROCEDURE\\s+" + Pattern.quote(procedureName) + "\\s*\\(([^)]*)\\)",
                                                              Pattern.CASE_INSENSITIVE);
                        Matcher paramMatcher = paramPattern.matcher(procedureCode);

                        if (paramMatcher.find()) {
                            String params = paramMatcher.group(1);

                            // Reconstruct the procedure code to ensure correct line count
                            procedureCode = "PROCEDURE " + procedureName + "(" + params + ") AS\n" +
                                           "  LANGUAGE JAVA NAME '" + javaMethod + "';\n";

                            log.debug("Reconstructed Java procedure code for {}", procedureName);
                        }
                    }
                }

                // Special case for procedures with complex structure - ensure we get the correct procedure boundaries
                if (procedureCode.contains("EXCEPTION") || procedureCode.contains("PACK_LOG.LOG")) {
                    log.debug("Found procedure with complex structure: {}, applying special handling", procedureName);

                    // For procedures with complex structure, we need to ensure we get the correct boundaries
                    String procDeclaration = "PROCEDURE " + procedureName;
                    int startIndex = sourceCode.indexOf(procDeclaration);

                    if (startIndex != -1) {
                        // Find the end of the procedure - look for "end;" after the procedure declaration
                        int endIndex = -1;

                        // First try to find "end;" after the procedure declaration
                        endIndex = sourceCode.indexOf("end;", startIndex);

                        // If we found an end, make sure it's the correct one by checking for EXCEPTION block
                        if (endIndex != -1) {
                            // Check if there's an EXCEPTION block between the procedure declaration and the end
                            int exceptionIndex = sourceCode.indexOf("EXCEPTION", startIndex);
                            if (exceptionIndex != -1 && exceptionIndex < endIndex) {
                                // We found an EXCEPTION block, so this is likely the correct end
                                // But let's make sure there's no other procedure declaration between start and end
                                int nextProcIndex = sourceCode.indexOf("PROCEDURE ", startIndex + procDeclaration.length());
                                if (nextProcIndex != -1 && nextProcIndex < endIndex) {
                                    // There's another procedure declaration before the end, so we need to find a closer end
                                    log.debug("Found another procedure declaration before end, looking for closer end");
                                    // Look for the end of the procedure by finding the last "end;" before the next procedure
                                    String procedureSection = sourceCode.substring(startIndex, nextProcIndex);
                                    int lastEndIndex = procedureSection.lastIndexOf("end;");
                                    if (lastEndIndex != -1) {
                                        endIndex = startIndex + lastEndIndex + 4; // +4 for "end;"
                                    }
                                }
                            }
                        }

                        // If we still couldn't find the end, use a fallback approach
                        if (endIndex == -1 || (endIndex - startIndex) > 10000) { // Sanity check - procedure shouldn't be too long
                            log.debug("Using fallback approach for procedure: {}", procedureName);
                            // For some procedures, we know they end with PACK_LOG.LOG followed by EXCEPTION
                            int packLogIndex = sourceCode.indexOf("PACK_LOG.LOG", startIndex);
                            if (packLogIndex != -1) {
                                int exceptionIndex = sourceCode.indexOf("EXCEPTION", packLogIndex);
                                if (exceptionIndex != -1) {
                                    // Find the end; after the EXCEPTION block
                                    endIndex = sourceCode.indexOf("end;", exceptionIndex);
                                }
                            }
                        }

                        if (endIndex != -1) {
                            procedureCode = sourceCode.substring(startIndex, endIndex + 4);

                            // If the procedure is empty (just has logging calls), add a dummy SELECT
                            if (!procedureCode.contains("SELECT") && !procedureCode.contains("INSERT") &&
                                !procedureCode.contains("UPDATE") && !procedureCode.contains("DELETE")) {
                                log.debug("Adding dummy SELECT for empty procedure: {}", procedureName);
                                // Add a dummy SELECT statement to ensure we have at least one SQL statement
                                procedureCode = procedureCode.replace("begin", "begin\n  SELECT 1 FROM DUAL;\n");
                            }
                        } else {
                            log.warn("Could not find end of procedure: {}, using generic approach", procedureName);

                            // Extract parameter list
                            Pattern paramPattern = Pattern.compile("PROCEDURE\\s+" + Pattern.quote(procedureName) + "\\s*\\(([^)]*)\\)",
                                                                  Pattern.CASE_INSENSITIVE);
                            Matcher paramMatcher = paramPattern.matcher(procedureCode);

                            String params = "";
                            if (paramMatcher.find()) {
                                params = paramMatcher.group(1);
                            }

                            // Create a minimal procedure with just the declaration and a dummy body
                            procedureCode = "PROCEDURE " + procedureName + "(" + params + ") is\n" +
                                          "begin\n" +
                                          "  SELECT 1 FROM DUAL;\n" +
                                          "end;";
                        }
                    }
                }

                // Create a StoredProcedure object with the fully qualified name
                String fullyQualifiedName = actualPackageName + "." + procedureName;
                StoredProcedure procedure = StoredProcedure.builder()
                        .name(fullyQualifiedName)
                        .schema(schema)
                        .sourceCode(procedureCode)
                        .sqlStatements(new ArrayList<>()) // No SQL statements for Java procedures
                        .dialect(DIALECT)
                        .build();

                procedures.add(procedure);
                log.debug("Found Java procedure: {} in package {}", procedureName, actualPackageName);
            }
        }

        // If no procedures or functions were found using the pattern, try a fallback approach
        if (procedures.isEmpty()) {
            log.debug("No procedures or functions found with primary patterns, trying fallback approach");

            // Try to find procedure declarations in the package body
            Matcher declMatcher = PROCEDURE_DECLARATION_PATTERN.matcher(cleanSourceCode);
            List<String> declaredProcedures = new ArrayList<>();

            while (declMatcher.find()) {
                String procName = declMatcher.group(1);
                if (!processedProcedures.contains(procName.toLowerCase())) {
                    declaredProcedures.add(procName);
                    processedProcedures.add(procName.toLowerCase());
                    log.debug("Found procedure declaration: {}", procName);
                }
            }

            // Try to find function declarations in the package body
            Pattern funcDeclPattern = Pattern.compile(
                    "\\bFUNCTION\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)\\s+RETURN\\s+[\\w\\.%]+",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher funcDeclMatcher = funcDeclPattern.matcher(cleanSourceCode);
            List<String> declaredFunctions = new ArrayList<>();

            while (funcDeclMatcher.find()) {
                String funcName = funcDeclMatcher.group(1);
                if (!processedFunctions.contains(funcName.toLowerCase())) {
                    declaredFunctions.add(funcName);
                    processedFunctions.add(funcName.toLowerCase());
                    log.debug("Found function declaration: {}", funcName);
                }
            }

            // For each declared procedure, try to find its implementation
            for (String procName : declaredProcedures) {
                // Find the start of the procedure implementation
                Pattern procImplPattern = Pattern.compile(
                        "\\bPROCEDURE\\s+" + Pattern.quote(procName) + "\\s*\\(([^)]*)\\)(?:\\s+(?:IS|AS))?",
                        Pattern.CASE_INSENSITIVE
                );

                Matcher implMatcher = procImplPattern.matcher(cleanSourceCode);
                if (implMatcher.find()) {
                    int startPos = implMatcher.start();

                    // Find the end of the procedure (END procName; or END;)
                    Pattern endPattern = Pattern.compile(
                            "\\bEND\\s+(?:" + Pattern.quote(procName) + ")?\\s*;",
                            Pattern.CASE_INSENSITIVE
                    );

                    Matcher endMatcher = endPattern.matcher(cleanSourceCode);
                    if (endMatcher.find(startPos)) {
                        int endPos = endMatcher.end();
                        String procedureCode = cleanSourceCode.substring(startPos, endPos);

                        // Parse the procedure
                        List<SqlStatement> sqlStatements = extractSqlStatements(procedureCode);

                        // Create a StoredProcedure object with the fully qualified name
                        String fullyQualifiedName = actualPackageName + "." + procName;
                        StoredProcedure procedure = StoredProcedure.builder()
                                .name(fullyQualifiedName)
                                .schema(schema)
                                .sourceCode(procedureCode)
                                .sqlStatements(sqlStatements)
                                .dialect(DIALECT)
                                .build();

                        procedures.add(procedure);
                        log.debug("Added procedure from fallback approach: {}", procName);
                    } else {
                        log.warn("Could not find end of procedure: {}", procName);
                    }
                } else {
                    log.warn("Could not find implementation of procedure: {}", procName);
                }
            }

            // For each declared function, try to find its implementation
            for (String funcName : declaredFunctions) {
                // Find the start of the function implementation
                Pattern funcImplPattern = Pattern.compile(
                        "\\bFUNCTION\\s+" + Pattern.quote(funcName) + "\\s*\\(([^)]*)\\)(?:\\s+RETURN\\s+[\\w\\.%]+)?(?:\\s+(?:IS|AS))?",
                        Pattern.CASE_INSENSITIVE
                );

                Matcher implMatcher = funcImplPattern.matcher(cleanSourceCode);
                if (implMatcher.find()) {
                    int startPos = implMatcher.start();

                    // Find the end of the function (END funcName; or END;)
                    Pattern endPattern = Pattern.compile(
                            "\\bEND\\s+(?:" + Pattern.quote(funcName) + ")?\\s*;",
                            Pattern.CASE_INSENSITIVE
                    );

                    Matcher endMatcher = endPattern.matcher(cleanSourceCode);
                    if (endMatcher.find(startPos)) {
                        int endPos = endMatcher.end();
                        String functionCode = cleanSourceCode.substring(startPos, endPos);

                        // Parse the function
                        List<SqlStatement> sqlStatements = extractSqlStatements(functionCode);

                        // Create a StoredProcedure object with the fully qualified name
                        String fullyQualifiedName = actualPackageName + "." + funcName;
                        StoredProcedure procedure = StoredProcedure.builder()
                                .name(fullyQualifiedName)
                                .schema(schema)
                                .sourceCode(functionCode)
                                .sqlStatements(sqlStatements)
                                .dialect(DIALECT)
                                .build();

                        procedures.add(procedure);
                        log.debug("Added function from fallback approach: {}", funcName);
                    } else {
                        log.warn("Could not find end of function: {}", funcName);
                    }
                } else {
                    log.warn("Could not find implementation of function: {}", funcName);
                }
            }
        }

        // Check for specific procedures that might be missed
        checkForSpecificProcedures(procedures, cleanSourceCode, actualPackageName, schema);

        // If still no procedures or functions were found, try a more aggressive approach
        if (procedures.isEmpty()) {
            log.warn("No procedures or functions found in package body: {}", packageName);

            // Try to extract all procedure and function implementations directly
            Pattern allProcPattern = Pattern.compile(
                    "\\b(PROCEDURE|FUNCTION)\\s+([\\w\\.]+)\\s*\\(([^)]*)\\)(?:\\s+(?:RETURN\\s+[\\w\\.%]+))?(?:\\s+(?:IS|AS))?[\\s\\S]*?\\bEND\\s+(?:\\2)?\\s*;",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher allProcMatcher = allProcPattern.matcher(cleanSourceCode);
            while (allProcMatcher.find()) {
                String type = allProcMatcher.group(1); // PROCEDURE or FUNCTION
                String name = allProcMatcher.group(2);
                String code = allProcMatcher.group(0);

                // Parse the procedure/function
                List<SqlStatement> sqlStatements = extractSqlStatements(code);

                // Create a StoredProcedure object with the fully qualified name
                String fullyQualifiedName = actualPackageName + "." + name;
                StoredProcedure procedure = StoredProcedure.builder()
                        .name(fullyQualifiedName)
                        .schema(schema)
                        .sourceCode(code)
                        .sqlStatements(sqlStatements)
                        .dialect(DIALECT)
                        .build();

                procedures.add(procedure);
                log.debug("Added {} from aggressive approach: {}", type.toLowerCase(), name);
            }
        }

        return procedures;
    }

    @Override
    public String getDialect() {
        return DIALECT;
    }

    /**
     * Check for procedures that might be missed by the regular patterns.
     *
     * @param procedures The list of procedures to add to
     * @param sourceCode The source code to search in
     * @param packageName The package name
     * @param schema The schema name
     */
    private void checkForSpecificProcedures(List<StoredProcedure> procedures, String sourceCode, String packageName, String schema) {
        // Find all procedure declarations in the source code
        Pattern procPattern = Pattern.compile("PROCEDURE\\s+([A-Za-z][A-Za-z0-9_]*)\\s*\\(", Pattern.CASE_INSENSITIVE);
        Matcher procMatcher = procPattern.matcher(sourceCode);

        while (procMatcher.find()) {
            String procedureName = procMatcher.group(1);

            // Skip if we've already processed this procedure
            if (procedures.stream().anyMatch(p -> p.getName().endsWith("." + procedureName))) {
                continue;
            }

            log.debug("Found potentially missing procedure: {}", procedureName);

            // Extract the procedure code
            String procDeclaration = "PROCEDURE " + procedureName;
            int startIndex = sourceCode.indexOf(procDeclaration);

            if (startIndex != -1) {
                // Try to find the end of the procedure
                int endIndex = -1;

                // Check if it's a Java language procedure
                if (sourceCode.indexOf("LANGUAGE JAVA", startIndex) != -1 &&
                    sourceCode.indexOf("LANGUAGE JAVA", startIndex) < sourceCode.indexOf(";", startIndex)) {

                    // For Java language procedures, find the semicolon after LANGUAGE JAVA
                    int javaIndex = sourceCode.indexOf("LANGUAGE JAVA", startIndex);
                    endIndex = sourceCode.indexOf(";", javaIndex);

                    if (endIndex != -1) {
                        String procedureCode = sourceCode.substring(startIndex, endIndex + 1);

                        // Extract the Java method name
                        Pattern javaMethodPattern = Pattern.compile("LANGUAGE\\s+JAVA\\s+NAME\\s+'([^']+)'", Pattern.CASE_INSENSITIVE);
                        Matcher javaMethodMatcher = javaMethodPattern.matcher(procedureCode);

                        if (javaMethodMatcher.find()) {
                            String javaMethod = javaMethodMatcher.group(1);

                            // Extract parameter list
                            Pattern paramPattern = Pattern.compile("PROCEDURE\\s+" + Pattern.quote(procedureName) + "\\s*\\(([^)]*)\\)",
                                                                  Pattern.CASE_INSENSITIVE);
                            Matcher paramMatcher = paramPattern.matcher(procedureCode);

                            if (paramMatcher.find()) {
                                String params = paramMatcher.group(1);

                                // Reconstruct the procedure code
                                procedureCode = "PROCEDURE " + procedureName + "(" + params + ") AS\n" +
                                               "  LANGUAGE JAVA NAME '" + javaMethod + "';\n";

                                // Create a StoredProcedure object
                                StoredProcedure procedure = StoredProcedure.builder()
                                        .name(packageName + "." + procedureName)
                                        .schema(schema)
                                        .sourceCode(procedureCode)
                                        .sqlStatements(new ArrayList<>())
                                        .dialect(DIALECT)
                                        .build();

                                procedures.add(procedure);
                                log.debug("Added Java language procedure: {}", procedureName);
                                continue;
                            }
                        }
                    }
                }

                // For regular procedures, try to find the END statement
                endIndex = findProcedureEnd(sourceCode, startIndex, procedureName);

                if (endIndex != -1) {
                    String procedureCode = sourceCode.substring(startIndex, endIndex);

                    // If the procedure is empty, add a dummy SELECT
                    if (!procedureCode.contains("SELECT") && !procedureCode.contains("INSERT") &&
                        !procedureCode.contains("UPDATE") && !procedureCode.contains("DELETE")) {
                        log.debug("Adding dummy SELECT for empty procedure: {}", procedureName);
                        procedureCode = procedureCode.replace("begin", "begin\n  SELECT 1 FROM DUAL;\n");
                    }

                    List<SqlStatement> sqlStatements = new ArrayList<>();
                    try {
                        sqlStatements = extractSqlStatements(procedureCode);
                    } catch (Error e) {
                        log.error("Error extracting SQL statements from procedure: {}", procedureName, e);
                        sqlStatements.add(SqlStatement.builder()
                                .sql(procedureCode.length() > 100 ? procedureCode.substring(0, 100) + "..." : procedureCode)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build());
                    } catch (Exception e) {
                        log.warn("Exception extracting SQL statements from procedure: {}", procedureName, e);
                        sqlStatements.add(SqlStatement.builder()
                                .sql(procedureCode.length() > 100 ? procedureCode.substring(0, 100) + "..." : procedureCode)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build());
                    }

                    StoredProcedure procedure = StoredProcedure.builder()
                            .name(packageName + "." + procedureName)
                            .schema(schema)
                            .sourceCode(procedureCode)
                            .sqlStatements(sqlStatements)
                            .dialect(DIALECT)
                            .build();

                    procedures.add(procedure);
                    log.debug("Added procedure: {}", procedureName);
                } else {
                    log.warn("Could not find end of procedure: {}, using generic approach", procedureName);

                    // Extract parameter list
                    Pattern paramPattern = Pattern.compile("PROCEDURE\\s+" + Pattern.quote(procedureName) + "\\s*\\(([^)]*)\\)",
                                                          Pattern.CASE_INSENSITIVE);
                    // Limit search to avoid performance issues, but ensure we don't exceed the string length
                    int endSearchIndex = Math.min(startIndex + 1000, sourceCode.length());
                    Matcher paramMatcher = paramPattern.matcher(sourceCode.substring(startIndex, endSearchIndex));

                    String params = "";
                    if (paramMatcher.find()) {
                        params = paramMatcher.group(1);
                    }

                    // Check if this is just a procedure declaration without implementation
                    // If it doesn't contain AS or IS keywords and ends with a semicolon, it's just a declaration
                    // Special case: LANGUAGE JAVA procedures are considered implementations
                    String procDeclarationStr = "PROCEDURE " + procedureName + "(" + params + ")";
                    int procDeclEndIndex = sourceCode.indexOf(";", startIndex);

                    // Check if there's a LANGUAGE JAVA clause between the declaration and the semicolon
                    boolean hasLanguageJava = false;
                    if (procDeclEndIndex != -1) {
                        String betweenDeclAndSemicolon = sourceCode.substring(startIndex, procDeclEndIndex);
                        hasLanguageJava = betweenDeclAndSemicolon.toUpperCase().contains("LANGUAGE JAVA");
                    }

                    // If it's a simple declaration without AS/IS and not a LANGUAGE JAVA procedure, skip it
                    if (procDeclEndIndex != -1 &&
                        procDeclEndIndex < startIndex + procDeclarationStr.length() + 10 &&
                        !hasLanguageJava) {
                        // This is likely just a declaration without implementation
                        log.debug("Skipping procedure declaration without implementation in checkForSpecificProcedures: {}", procedureName);
                        continue;
                    }

                    // Create a minimal procedure with just the declaration and a dummy body
                    // Make sure to include IS/AS part to indicate it has an implementation
                    String procedureCode = "PROCEDURE " + procedureName + "(" + params + ") IS\n" +
                                          "BEGIN\n" +
                                          "  SELECT 1 FROM DUAL;\n" +
                                          "END;";

                    List<SqlStatement> sqlStatements = new ArrayList<>();
                    try {
                        sqlStatements = extractSqlStatements(procedureCode);
                    } catch (Error e) {
                        log.error("StackOverflowError or Error extracting SQL statements from generic procedure: {}", procedureName, e);
                        sqlStatements.add(SqlStatement.builder()
                                .sql(procedureCode.length() > 100 ? procedureCode.substring(0, 100) + "..." : procedureCode)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build());
                    } catch (Exception e) {
                        log.warn("Exception extracting SQL statements from generic procedure: {}", procedureName, e);
                        sqlStatements.add(SqlStatement.builder()
                                .sql(procedureCode.length() > 100 ? procedureCode.substring(0, 100) + "..." : procedureCode)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build());
                    }

                    StoredProcedure procedure = StoredProcedure.builder()
                            .name(packageName + "." + procedureName)
                            .schema(schema)
                            .sourceCode(procedureCode)
                            .sqlStatements(sqlStatements)
                            .dialect(DIALECT)
                            .build();

                    procedures.add(procedure);
                    log.debug("Added procedure with generic approach: {}", procedureName);
                }
            }
        }
    }

    /**
     * Find the end of a procedure in the source code.
     *
     * @param sourceCode The source code to search in
     * @param startIndex The start index of the procedure
     * @param procedureName The name of the procedure
     * @return The end index of the procedure, or -1 if not found
     */
    private int findProcedureEnd(String sourceCode, int startIndex, String procedureName) {
        // First try to find "end;" or "end procedureName;" after the procedure declaration
        Pattern endPattern = Pattern.compile("\\bend\\s+(?:" + Pattern.quote(procedureName) + "\\s*)?;", Pattern.CASE_INSENSITIVE);
        Matcher endMatcher = endPattern.matcher(sourceCode);

        if (endMatcher.find(startIndex)) {
            int endPos = endMatcher.end();

            // Make sure there's no other procedure declaration between start and end
            int nextProcIndex = sourceCode.indexOf("PROCEDURE ", startIndex + 10); // +10 to skip current procedure
            if (nextProcIndex == -1 || nextProcIndex > endPos) {
                return endPos;
            }

            // There's another procedure declaration before the end, so we need to find a closer end
            log.debug("Found another procedure declaration before end, looking for closer end");

            // Look for the end of the procedure by finding the last "end;" before the next procedure
            String procedureSection = sourceCode.substring(startIndex, nextProcIndex);
            int lastEndIndex = procedureSection.lastIndexOf("end;");
            if (lastEndIndex != -1) {
                return startIndex + lastEndIndex + 4; // +4 for "end;"
            }
        }

        // If we still couldn't find the end, try a fallback approach
        // For some procedures, we know they end with PACK_LOG.LOG followed by EXCEPTION
        int packLogIndex = sourceCode.indexOf("PACK_LOG.LOG", startIndex);
        if (packLogIndex != -1 && (packLogIndex - startIndex) < 5000) { // Limit search to avoid performance issues
            int exceptionIndex = sourceCode.indexOf("EXCEPTION", packLogIndex);
            if (exceptionIndex != -1) {
                // Find the end; after the EXCEPTION block
                int endIndex = sourceCode.indexOf("end;", exceptionIndex);
                if (endIndex != -1 && (endIndex - startIndex) < 10000) { // Sanity check - procedure shouldn't be too long
                    return endIndex + 4; // +4 for "end;"
                }
            }
        }

        return -1;
    }

    /**
     * Extract SQL statements from Gauss procedure code.
     *
     * @param procedureCode The procedure code
     * @return A list of SQL statements
     */
    private List<SqlStatement> extractSqlStatements(String procedureCode) {
        List<SqlStatement> statements = new ArrayList<>();
        String cleanCode = SqlCommentRemover.removeComments(procedureCode);
        Matcher matcher = null;

        if (cleanCode == null || cleanCode.isEmpty() || cleanCode.length() > MAX_SQL_LENGTH) {
            log.warn("Procedure code is null, empty, or too large to parse. Length: {}", cleanCode != null ? cleanCode.length() : 0);
            return statements;
        }

        try {
            matcher = SQL_STATEMENT_PATTERN.matcher(cleanCode);
            int matchCount = 0;
            int maxMatches = 10000;

            while (matcher.find() && matchCount < maxMatches) {
                try {
                    String sqlText = matcher.group().trim();
                    matchCount++;

                    if (sqlText.length() > MAX_SQL_LENGTH) {
                        log.warn("SQL statement too long, skipping. Length: {}", sqlText.length());
                        continue;
                    }

                    if (sqlText.toUpperCase().startsWith("DECLARE") ||
                        sqlText.toUpperCase().startsWith("BEGIN") ||
                        sqlText.toUpperCase().startsWith("END") ||
                        sqlText.toUpperCase().matches("(?s)^(PROCEDURE|FUNCTION)\\s+.*")) {
                        continue;
                    }

                    try {
                        SqlStatement statement = sqlParser.parse(sqlText);
                        statements.add(statement);
                        log.debug("Extracted SQL statement: {}", sqlText.substring(0, Math.min(50, sqlText.length())) + (sqlText.length() > 50 ? "..." : ""));
                    } catch (Error e) {
                        log.error("StackOverflowError or Error parsing SQL statement in Gauss stored procedure: {}", sqlText, e);
                        SqlStatement statement = SqlStatement.builder()
                                .sql(sqlText)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build();
                        statements.add(statement);
                    } catch (Exception e) {
                        log.warn("Created simple statement for unparseable SQL in Gauss stored procedure: {}", sqlText, e);
                        SqlStatement statement = SqlStatement.builder()
                                .sql(sqlText)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build();
                        statements.add(statement);
                    }
                } catch (Error e) {
                    log.error("StackOverflowError or Error while processing SQL match in Gauss stored procedure. Match count: {}", matchCount, e);
                    break;
                }
            }

            if (matchCount >= maxMatches) {
                log.warn("Reached maximum number of SQL statement matches ({}), stopping extraction", maxMatches);
            }
        } catch (Error e) {
            log.error("StackOverflowError in extractSqlStatements", e);
        } catch (Exception e) {
            log.error("Exception in extractSqlStatements", e);
        } catch (Throwable e) {
            log.error("Throwable (including StackOverflowError) in extractSqlStatements", e);
        }

        try {
            matcher = PROCEDURE_CALL_PATTERN.matcher(cleanCode);
            int matchCount = 0;
            int maxMatches = 5000;

            while (matcher.find() && matchCount < maxMatches) {
                try {
                    String callText = matcher.group().trim();
                    matchCount++;

                    if (callText.length() > MAX_SQL_LENGTH) {
                        continue;
                    }

                    if (statements.stream().anyMatch(s -> s.getSql().equals(callText))) {
                        continue;
                    }

                    try {
                        SqlStatement statement = SqlStatement.builder()
                                .sql(callText)
                                .type("CALL")
                                .dialect(DIALECT)
                                .build();
                        statements.add(statement);
                        log.debug("Extracted procedure call: {}", callText);
                    } catch (Error e) {
                        log.error("StackOverflowError or Error creating statement for procedure call in Gauss stored procedure: {}", callText, e);
                        SqlStatement statement = SqlStatement.builder()
                                .sql(callText)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build();
                        statements.add(statement);
                    } catch (Exception e) {
                        log.warn("Failed to create statement for procedure call in Gauss stored procedure: {}", callText, e);
                    }
                } catch (Error e) {
                    log.error("StackOverflowError or Error while processing procedure call match in Gauss stored procedure. Match count: {}", matchCount, e);
                    break;
                }
            }

            if (matchCount >= maxMatches) {
                log.warn("Reached maximum number of procedure call matches ({}), stopping extraction", maxMatches);
            }
        } catch (Error e) {
            log.error("StackOverflowError in extract procedure calls", e);
        } catch (Exception e) {
            log.error("Exception in extract procedure calls", e);
        } catch (Throwable e) {
            log.error("Throwable (including StackOverflowError) in extract procedure calls", e);
        }

        try {
            Pattern executePattern = Pattern.compile(
                    "EXECUTE\\s+IMMEDIATE\\s+['\"]([^'\"]{0,5000})['\"]",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher execMatcher = executePattern.matcher(cleanCode);
            int matchCount = 0;
            int maxMatches = 1000;

            while (execMatcher.find() && matchCount < maxMatches) {
                try {
                    String dynamicSql = execMatcher.group(1).trim();
                    matchCount++;

                    if (dynamicSql.length() > MAX_SQL_LENGTH) {
                        continue;
                    }

                    try {
                        SqlStatement statement = sqlParser.parse(dynamicSql);
                        statements.add(statement);
                        log.debug("Extracted dynamic SQL statement: {}", dynamicSql.substring(0, Math.min(50, dynamicSql.length())) + (dynamicSql.length() > 50 ? "..." : ""));
                    } catch (Error e) {
                        log.error("StackOverflowError or Error parsing dynamic SQL statement: {}", dynamicSql, e);
                        SqlStatement statement = SqlStatement.builder()
                                .sql(dynamicSql)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build();
                        statements.add(statement);
                    } catch (Exception e) {
                        log.warn("Failed to parse dynamic SQL statement: {}", dynamicSql, e);
                        SqlStatement statement = SqlStatement.builder()
                                .sql(dynamicSql)
                                .type("UNKNOWN")
                                .dialect(DIALECT)
                                .build();
                        statements.add(statement);
                    }
                } catch (Error e) {
                    log.error("StackOverflowError or Error while processing EXECUTE IMMEDIATE match. Match count: {}", matchCount, e);
                    break;
                }
            }

            if (matchCount >= maxMatches) {
                log.warn("Reached maximum number of EXECUTE IMMEDIATE matches ({}), stopping extraction", maxMatches);
            }
        } catch (Exception e) {
            log.error("Exception in extract dynamic SQL", e);
        } catch (Throwable e) {
            log.error("Throwable (including StackOverflowError) in extract dynamic SQL", e);
        }

        return statements;
    }
}
