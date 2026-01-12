import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

public class test_package_detection {
    
    private static final Pattern PACKAGE_BODY_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:OR\\s+REPLACE\\s+)?\\bPACKAGE\\s+\\bBODY\\b",
            Pattern.CASE_INSENSITIVE
    );
    
    public static boolean isPackageBody(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return false;
        }

        System.out.println("Checking if file is package body. Source code starts with: " + 
                 sourceCode.substring(0, Math.min(100, sourceCode.length())));

        // Check for standard package body pattern
        if (PACKAGE_BODY_PATTERN.matcher(sourceCode.toUpperCase()).find()) {
            System.out.println("File identified as package body based on CREATE PACKAGE BODY pattern");
            return true;
        }

        // Check for package end pattern
        if (sourceCode.toUpperCase().contains("END ") &&
            (sourceCode.toUpperCase().contains("_PKG") ||
             sourceCode.toUpperCase().contains("PKG_"))) {
            System.out.println("File identified as package body based on END PKG_ pattern");
            return true;
        }

        // Special case for d.sql - check if it starts with a procedure name or CREATE OR REPLACE PACKAGE
        if (sourceCode.trim().toUpperCase().matches("^[A-Z][A-Z0-9_]*\\s*\\(.*") ||
            sourceCode.trim().toUpperCase().startsWith("CREATE OR REPLACE PACKAGE")) {
            System.out.println("File identified as package body based on procedure declaration or CREATE OR REPLACE PACKAGE pattern");
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
        
        System.out.println("Found " + procCount + " procedures: " + procedureNames);
        
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
                
                System.out.println("Found main procedure: " + mainProcName + " at position " + mainProcStart);
                
                // Check if all other procedures are declared after the main procedure's AS/IS keyword
                // and before the main procedure's final END
                Pattern asIsPattern = Pattern.compile("\\bPROCEDURE\\s+" + Pattern.quote(mainProcName) + "\\s*\\([^)]*\\)\\s*(?:AS|IS)\\b", Pattern.CASE_INSENSITIVE);
                Matcher asIsMatcher = asIsPattern.matcher(sourceCode);
                
                if (asIsMatcher.find()) {
                    int asIsPosition = asIsMatcher.end();
                    
                    System.out.println("Found AS/IS at position " + asIsPosition);
                    
                    // Find the final END of the main procedure
                    // Look for the last END; in the file, which should be the main procedure's end
                    int lastEndPosition = sourceCode.lastIndexOf("END;");
                    if (lastEndPosition == -1) {
                        lastEndPosition = sourceCode.length();
                    }
                    
                    System.out.println("Last END; found at position " + lastEndPosition);
                    
                    // Count how many procedures are declared between AS/IS and the final END
                    int nestedProcCount = 0;
                    for (int i = 0; i < procedurePositions.size(); i++) {
                        int procPos = procedurePositions.get(i);
                        String procName = procedureNames.get(i);
                        
                        if (procPos > asIsPosition && procPos < lastEndPosition) {
                            nestedProcCount++;
                            System.out.println("Procedure " + procName + " at position " + procPos + " is nested (between " + asIsPosition + " and " + lastEndPosition + ")");
                        } else if (procPos <= mainProcStart + mainProcName.length() + 20) {
                            // This is likely the main procedure declaration
                            packageLevelProcCount++;
                            System.out.println("Procedure " + procName + " at position " + procPos + " is the main procedure");
                        } else {
                            // This is a package-level procedure
                            packageLevelProcCount++;
                            System.out.println("Procedure " + procName + " at position " + procPos + " is package-level");
                        }
                    }
                    
                    System.out.println("Analysis: " + packageLevelProcCount + " package-level procedures, " + nestedProcCount + " nested procedures");
                    
                    // If we have nested procedures but only one package-level procedure, this is NOT a package body
                    if (packageLevelProcCount <= 1 && nestedProcCount > 0) {
                        System.out.println("File identified as single procedure with " + nestedProcCount + " nested procedures, not a package body");
                        return false;
                    }
                }
            }
            
            // If we reach here, it's likely a package body with multiple independent procedures
            System.out.println("File identified as package body due to multiple procedures: " + procCount);
            return true;
        }

        System.out.println("File identified as single procedure, not a package body");
        return false;
    }
    
    public static void main(String[] args) {
        String sourceCode = "CREATE OR REPLACE PROCEDURE process_monthly_payroll(\n" +
                "    p_month IN NUMBER,\n" +
                "    p_year IN NUMBER,\n" +
                "    p_department_id IN NUMBER DEFAULT NULL,\n" +
                "    p_result OUT NUMBER\n" +
                ") AS\n" +
                "    -- Constants\n" +
                "    c_tax_rate CONSTANT NUMBER := 0.25;\n" +
                "    \n" +
                "    -- Nested procedure to process department payroll\n" +
                "    PROCEDURE process_department_payroll(\n" +
                "        p_dept_id IN NUMBER,\n" +
                "        p_dept_name IN VARCHAR2,\n" +
                "        p_start_date IN DATE,\n" +
                "        p_end_date IN DATE,\n" +
                "        p_dept_result OUT NUMBER\n" +
                "    ) AS\n" +
                "        -- Nested procedure to process employee payroll\n" +
                "        PROCEDURE process_employee_payroll(\n" +
                "            p_emp_id IN NUMBER\n" +
                "        ) AS\n" +
                "        BEGIN\n" +
                "            NULL;\n" +
                "        END process_employee_payroll;\n" +
                "    BEGIN\n" +
                "        NULL;\n" +
                "    END process_department_payroll;\n" +
                "    \n" +
                "BEGIN\n" +
                "    -- Main procedure logic\n" +
                "    COMMIT;\n" +
                "END;";
        
        boolean result = isPackageBody(sourceCode);
        System.out.println("Result: " + result);
    }
}