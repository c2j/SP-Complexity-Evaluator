package com.sdchat.ce.sp.complexity.parser;

import com.sdchat.ce.sp.complexity.model.StoredProcedure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GaussPackageParsingTest {

    @Autowired
    private GaussStoredProcedureParser parser;

    @Test
    void testNestedProcedureCallsFileParsing() throws Exception {
        // Read the actual SQL file
        String sqlContent = Files.readString(Paths.get("sql_samples/gauss/nested_procedure_calls.sql"));
        
        // Check if it's identified as package body
        boolean isPackageBody = parser.isPackageBody(sqlContent);
        System.out.println("Is Package Body: " + isPackageBody);
        
        if (isPackageBody) {
            // Parse as package body
            List<StoredProcedure> procedures = parser.parsePackageBody(sqlContent, "nested_procedure_calls", "HR");
            
            System.out.println("=== Package Body Parsing Results ===");
            System.out.println("Number of procedures found: " + procedures.size());
            
            for (int i = 0; i < procedures.size(); i++) {
                StoredProcedure proc = procedures.get(i);
                System.out.println("\n--- Procedure " + (i + 1) + " ---");
                System.out.println("Name: " + proc.getName());
                System.out.println("Source Code Length: " + (proc.getSourceCode() != null ? proc.getSourceCode().length() : 0));
                System.out.println("SQL Statements Count: " + (proc.getSqlStatements() != null ? proc.getSqlStatements().size() : 0));
                
                // Check for transaction control statements in source code
                if (proc.getSourceCode() != null) {
                    String sourceCode = proc.getSourceCode().toUpperCase();
                    boolean hasCommit = sourceCode.contains("COMMIT");
                    boolean hasRollback = sourceCode.contains("ROLLBACK");
                    System.out.println("Has COMMIT: " + hasCommit);
                    System.out.println("Has ROLLBACK: " + hasRollback);
                    
                    // Show first 200 characters of source code
                    String preview = proc.getSourceCode().length() > 200 ? 
                                   proc.getSourceCode().substring(0, 200) + "..." : 
                                   proc.getSourceCode();
                    System.out.println("Source Code Preview: " + preview.replace("\n", "\\n"));
                }
            }
        } else {
            // Parse as single procedure
            StoredProcedure procedure = parser.parse(sqlContent, "process_monthly_payroll", "HR");
            System.out.println("=== Single Procedure Parsing Results ===");
            System.out.println("Name: " + procedure.getName());
            System.out.println("Source Code Length: " + (procedure.getSourceCode() != null ? procedure.getSourceCode().length() : 0));
            System.out.println("SQL Statements Count: " + (procedure.getSqlStatements() != null ? procedure.getSqlStatements().size() : 0));
            
            // Check for transaction control statements
            if (procedure.getSourceCode() != null) {
                String sourceCode = procedure.getSourceCode().toUpperCase();
                boolean hasCommit = sourceCode.contains("COMMIT");
                boolean hasRollback = sourceCode.contains("ROLLBACK");
                System.out.println("Has COMMIT: " + hasCommit);
                System.out.println("Has ROLLBACK: " + hasRollback);
            }
        }
    }
}