package com.sdchat.ce.sp.complexity.controller;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.service.ComplexityEvaluationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for complexity evaluation.
 */
@Slf4j
@RestController
@RequestMapping("/api/complexity")
@RequiredArgsConstructor
public class ComplexityEvaluationController {

    private final ComplexityEvaluationService complexityEvaluationService;

    /**
     * Evaluate the complexity of a SQL statement.
     *
     * @param request The SQL evaluation request
     * @return The complexity metrics
     */
    @PostMapping("/sql")
    public ResponseEntity<ComplexityMetrics> evaluateSql(@Valid @RequestBody SqlEvaluationRequest request) {
        try {
            // Ensure SQL is properly encoded as UTF-8
            String sql = new String(request.getSql().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            ComplexityMetrics metrics = complexityEvaluationService.evaluateSqlStatement(
                    sql,
                    request.getDialect()
            );
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Failed to evaluate SQL complexity", e);
            return ResponseEntity.badRequest().build();
        }
    }



    /**
     * Evaluate the complexity of a stored procedure.
     *
     * @param request The stored procedure evaluation request
     * @return The complexity metrics
     */
    @PostMapping("/stored-procedure")
    public ResponseEntity<ComplexityMetrics> evaluateStoredProcedure(@Valid @RequestBody StoredProcedureEvaluationRequest request) {
        try {
            // Ensure source code is properly encoded as UTF-8
            String sourceCode = new String(request.getSourceCode().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            ComplexityMetrics metrics;
            boolean hasCustomFunctions = request.getCustomFunctions() != null && !request.getCustomFunctions().isEmpty();
            boolean hasHighWeightTables = request.getHighWeightTables() != null && !request.getHighWeightTables().isEmpty();
            boolean hasHighWeightProcedures = request.getHighWeightProcedures() != null && !request.getHighWeightProcedures().isEmpty();

            // Call the appropriate method based on which parameters are provided
            if (hasCustomFunctions && hasHighWeightTables && hasHighWeightProcedures) {
                // All three: custom functions, high-weight tables, and high-weight procedures
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        request.getName(),
                        request.getSchema(),
                        request.getDialect(),
                        request.getCustomFunctions(),
                        request.getHighWeightTables(),
                        request.getHighWeightProcedures()
                );
            } else if (hasCustomFunctions && hasHighWeightTables) {
                // Both custom functions and high-weight tables
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        request.getName(),
                        request.getSchema(),
                        request.getDialect(),
                        request.getCustomFunctions(),
                        request.getHighWeightTables()
                );
            } else if (hasCustomFunctions && hasHighWeightProcedures) {
                // Both custom functions and high-weight procedures
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        request.getName(),
                        request.getSchema(),
                        request.getDialect(),
                        request.getCustomFunctions(),
                        null,
                        request.getHighWeightProcedures()
                );
            } else if (hasHighWeightTables && hasHighWeightProcedures) {
                // Both high-weight tables and high-weight procedures
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        request.getName(),
                        request.getSchema(),
                        request.getDialect(),
                        null,
                        request.getHighWeightTables(),
                        request.getHighWeightProcedures()
                );
            } else if (hasCustomFunctions) {
                // Only custom functions
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        request.getName(),
                        request.getSchema(),
                        request.getDialect(),
                        request.getCustomFunctions()
                );
            } else if (hasHighWeightTables) {
                // Only high-weight tables
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        request.getName(),
                        request.getSchema(),
                        request.getDialect(),
                        null,
                        request.getHighWeightTables()
                );
            } else if (hasHighWeightProcedures) {
                // Only high-weight procedures
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        request.getName(),
                        request.getSchema(),
                        request.getDialect(),
                        null,
                        null,
                        request.getHighWeightProcedures()
                );
            } else {
                // None of the optional parameters
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        request.getName(),
                        request.getSchema(),
                        request.getDialect()
                );
            }
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Failed to evaluate stored procedure complexity", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Evaluate the complexity of a stored procedure from a file.
     *
     * @param file The SQL file containing the stored procedure
     * @param name The name of the stored procedure
     * @param schema The schema/owner of the stored procedure
     * @param dialect The SQL dialect
     * @return The complexity metrics
     */
    @PostMapping(value = "/stored-procedure/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplexityMetrics> evaluateStoredProcedureFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "schema", required = false, defaultValue = "HR") String schema,
            @RequestParam(value = "dialect", required = false, defaultValue = "Oracle") String dialect,
            @RequestParam(value = "customFunctions", required = false) List<String> customFunctions,
            @RequestPart(value = "customFunctionsFile", required = false) MultipartFile customFunctionsFile,
            @RequestParam(value = "highWeightTables", required = false) List<String> highWeightTables,
            @RequestPart(value = "highWeightTablesFile", required = false) MultipartFile highWeightTablesFile,
            @RequestParam(value = "highWeightProcedures", required = false) List<String> highWeightProcedures,
            @RequestPart(value = "highWeightProceduresFile", required = false) MultipartFile highWeightProceduresFile) {
        try {
            // Read the file content
            String sourceCode;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                sourceCode = reader.lines().collect(Collectors.joining("\n"));
            }

            // Process custom functions
            List<String> functionList = new ArrayList<>();

            // Add functions from request parameter if provided
            if (customFunctions != null && !customFunctions.isEmpty()) {
                functionList.addAll(customFunctions);
            }

            // Add functions from file if provided
            if (customFunctionsFile != null && !customFunctionsFile.isEmpty()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(customFunctionsFile.getInputStream(), StandardCharsets.UTF_8))) {
                    // Each line in the file is a function name
                    reader.lines()
                          .map(String::trim)
                          .filter(line -> !line.isEmpty())
                          .forEach(functionList::add);
                }
            }

            // Process high-weight tables
            List<String> tableList = new ArrayList<>();

            // Add tables from request parameter if provided
            if (highWeightTables != null && !highWeightTables.isEmpty()) {
                tableList.addAll(highWeightTables);
            }

            // Add tables from file if provided
            if (highWeightTablesFile != null && !highWeightTablesFile.isEmpty()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(highWeightTablesFile.getInputStream(), StandardCharsets.UTF_8))) {
                    // Each line in the file is a table name
                    reader.lines()
                          .map(String::trim)
                          .filter(line -> !line.isEmpty())
                          .forEach(tableList::add);
                }
            }

            // Process high-weight procedures
            List<String> procedureList = new ArrayList<>();

            // Add procedures from request parameter if provided
            if (highWeightProcedures != null && !highWeightProcedures.isEmpty()) {
                procedureList.addAll(highWeightProcedures);
            }

            // Add procedures from file if provided
            if (highWeightProceduresFile != null && !highWeightProceduresFile.isEmpty()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(highWeightProceduresFile.getInputStream(), StandardCharsets.UTF_8))) {
                    // Each line in the file is a procedure name
                    reader.lines()
                          .map(String::trim)
                          .filter(line -> !line.isEmpty())
                          .forEach(procedureList::add);
                }
            }

            // Evaluate the stored procedure
            ComplexityMetrics metrics;
            boolean hasFunctions = !functionList.isEmpty();
            boolean hasTables = !tableList.isEmpty();
            boolean hasProcedures = !procedureList.isEmpty();

            if (hasFunctions && hasTables && hasProcedures) {
                // All three: custom functions, high-weight tables, and high-weight procedures
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        name,
                        schema,
                        dialect,
                        functionList,
                        tableList,
                        procedureList
                );
            } else if (hasFunctions && hasTables) {
                // Both custom functions and high-weight tables
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        name,
                        schema,
                        dialect,
                        functionList,
                        tableList
                );
            } else if (hasFunctions && hasProcedures) {
                // Both custom functions and high-weight procedures
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        name,
                        schema,
                        dialect,
                        functionList,
                        null,
                        procedureList
                );
            } else if (hasTables && hasProcedures) {
                // Both high-weight tables and high-weight procedures
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        name,
                        schema,
                        dialect,
                        null,
                        tableList,
                        procedureList
                );
            } else if (hasFunctions) {
                // Only custom functions
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        name,
                        schema,
                        dialect,
                        functionList
                );
            } else if (hasTables) {
                // Only high-weight tables
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        name,
                        schema,
                        dialect,
                        null,
                        tableList
                );
            } else if (hasProcedures) {
                // Only high-weight procedures
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        name,
                        schema,
                        dialect,
                        null,
                        null,
                        procedureList
                );
            } else {
                // None of the optional parameters
                metrics = complexityEvaluationService.evaluateStoredProcedure(
                        sourceCode,
                        name,
                        schema,
                        dialect
                );
            }
            return ResponseEntity.ok(metrics);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to evaluate stored procedure complexity from file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Evaluate the complexity of a SQL statement from a file.
     *
     * @param file The SQL file containing the SQL statement
     * @param dialect The SQL dialect
     * @return The complexity metrics
     */
    @PostMapping(value = "/sql/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplexityMetrics> evaluateSqlFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "dialect", required = false, defaultValue = "Oracle") String dialect) {
        try {
            // Read the file content
            String sql;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            // Evaluate the SQL statement
            ComplexityMetrics metrics = complexityEvaluationService.evaluateSqlStatement(
                    sql,
                    dialect
            );
            return ResponseEntity.ok(metrics);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to evaluate SQL complexity from file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Request object for SQL evaluation.
     */
    @Data
    public static class SqlEvaluationRequest {

        @NotBlank(message = "SQL statement is required")
        private String sql;

        @NotBlank(message = "Dialect is required")
        private String dialect = "Oracle"; // Default to Oracle
    }

    /**
     * Request object for stored procedure evaluation.
     */
    @Data
    public static class StoredProcedureEvaluationRequest {

        @NotBlank(message = "Source code is required")
        private String sourceCode;

        @NotBlank(message = "Name is required")
        private String name;

        private String schema;

        @NotBlank(message = "Dialect is required")
        private String dialect = "Oracle"; // Default to Oracle

        private List<String> customFunctions; // List of custom function names

        private List<String> highWeightTables; // List of high-weight table names

        private List<String> highWeightProcedures; // List of high-weight procedure names
    }
}
