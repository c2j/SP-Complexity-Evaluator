package com.sdchat.ce.sp.complexity.controller;

import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.model.ComplexityMetricsCollection;
import com.sdchat.ce.sp.complexity.service.ComplexityEvaluationService;
import com.sdchat.ce.sp.complexity.parser.StoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.GaussStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.OracleStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.HiveStoredProcedureParser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.commons.io.FileUtils;
import java.util.Collections;
import com.sdchat.ce.sp.complexity.util.ExcelExportUtil;

/**
 * REST controller for complexity evaluation.
 */
@Slf4j
@RestController
@RequestMapping("/api/complexity")
@RequiredArgsConstructor
public class ComplexityEvaluationController {

    private final ComplexityEvaluationService complexityEvaluationService;
    private final OracleStoredProcedureParser oracleStoredProcedureParser;
    private final GaussStoredProcedureParser gaussStoredProcedureParser;
    private final HiveStoredProcedureParser hiveStoredProcedureParser;

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

            // Check if there were any failed statements
            if (metrics.isHasExceptions() && metrics.getFailedStatements() != null && !metrics.getFailedStatements().isEmpty()) {
                log.warn("SQL evaluation completed with {} failed statements", metrics.getFailedStatements().size());
            }

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
            // Check if there were any failed statements
            if (metrics.isHasExceptions() && metrics.getFailedStatements() != null && !metrics.getFailedStatements().isEmpty()) {
                log.warn("Stored procedure evaluation completed with {} failed statements", metrics.getFailedStatements().size());
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
    public ResponseEntity<?> evaluateStoredProcedureFromFile(
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

            // Check if the file is a package body or contains multiple procedures
            StoredProcedureParser parser = getStoredProcedureParser(dialect);
            boolean isPackageBody = parser.isPackageBody(sourceCode);
            
            log.debug("File package body detection result: {}", isPackageBody);

            if (isPackageBody) {
                // Process as package body
                ComplexityMetricsCollection metricsCollection;
                boolean hasFunctions = (customFunctions != null && !customFunctions.isEmpty()) || (customFunctionsFile != null && !customFunctionsFile.isEmpty());
                boolean hasTables = (highWeightTables != null && !highWeightTables.isEmpty()) || (highWeightTablesFile != null && !highWeightTablesFile.isEmpty());
                boolean hasProcedures = (highWeightProcedures != null && !highWeightProcedures.isEmpty()) || (highWeightProceduresFile != null && !highWeightProceduresFile.isEmpty());

                // Process custom functions
                List<String> functionList = new ArrayList<>();
                if (customFunctions != null && !customFunctions.isEmpty()) {
                    functionList.addAll(customFunctions);
                }
                if (customFunctionsFile != null && !customFunctionsFile.isEmpty()) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(customFunctionsFile.getInputStream(), StandardCharsets.UTF_8))) {
                        reader.lines()
                              .map(String::trim)
                              .filter(line -> !line.isEmpty())
                              .forEach(functionList::add);
                    }
                }

                // Process high-weight tables
                List<String> tableList = new ArrayList<>();
                if (highWeightTables != null && !highWeightTables.isEmpty()) {
                    tableList.addAll(highWeightTables);
                }
                if (highWeightTablesFile != null && !highWeightTablesFile.isEmpty()) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(highWeightTablesFile.getInputStream(), StandardCharsets.UTF_8))) {
                        reader.lines()
                              .map(String::trim)
                              .filter(line -> !line.isEmpty())
                              .forEach(tableList::add);
                    }
                }

                // Process high-weight procedures
                List<String> procedureList = new ArrayList<>();
                if (highWeightProcedures != null && !highWeightProcedures.isEmpty()) {
                    procedureList.addAll(highWeightProcedures);
                }
                if (highWeightProceduresFile != null && !highWeightProceduresFile.isEmpty()) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(highWeightProceduresFile.getInputStream(), StandardCharsets.UTF_8))) {
                        reader.lines()
                              .map(String::trim)
                              .filter(line -> !line.isEmpty())
                              .forEach(procedureList::add);
                    }
                }

                // Call the appropriate method based on which parameters are provided
                if (hasFunctions && hasTables && hasProcedures) {
                    metricsCollection = complexityEvaluationService.evaluatePackageBody(
                            sourceCode, name, schema, dialect, functionList, tableList, procedureList);
                } else if (hasFunctions && hasTables) {
                    metricsCollection = complexityEvaluationService.evaluatePackageBody(
                            sourceCode, name, schema, dialect, functionList, tableList);
                } else if (hasFunctions && hasProcedures) {
                    metricsCollection = complexityEvaluationService.evaluatePackageBody(
                            sourceCode, name, schema, dialect, functionList, null, procedureList);
                } else if (hasTables && hasProcedures) {
                    metricsCollection = complexityEvaluationService.evaluatePackageBody(
                            sourceCode, name, schema, dialect, null, tableList, procedureList);
                } else if (hasFunctions) {
                    metricsCollection = complexityEvaluationService.evaluatePackageBody(
                            sourceCode, name, schema, dialect, functionList);
                } else if (hasTables) {
                    metricsCollection = complexityEvaluationService.evaluatePackageBody(
                            sourceCode, name, schema, dialect, null, tableList);
                } else if (hasProcedures) {
                    metricsCollection = complexityEvaluationService.evaluatePackageBody(
                            sourceCode, name, schema, dialect, null, null, procedureList);
                } else {
                    metricsCollection = complexityEvaluationService.evaluatePackageBody(
                            sourceCode, name, schema, dialect);
                }

                // Check if there were any failed statements in any of the procedures
                boolean hasFailedStatements = metricsCollection.getProcedures().stream()
                        .anyMatch(m -> m.isHasExceptions() && m.getFailedStatements() != null && !m.getFailedStatements().isEmpty());

                if (hasFailedStatements) {
                    log.warn("Package body evaluation completed with failed statements in some procedures");
                }

                return ResponseEntity.ok(metricsCollection);
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
            // Check if there were any failed statements
            if (metrics.isHasExceptions() && metrics.getFailedStatements() != null && !metrics.getFailedStatements().isEmpty()) {
                log.warn("Stored procedure file evaluation completed with {} failed statements", metrics.getFailedStatements().size());
            }

            log.debug("Controller returning metrics: hintCount={}, procedureName={}", metrics.getHintCount(), metrics.getProcedureName());

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
    /**
     * Evaluate the complexity of a package body and all its procedures from a file.
     *
     * @param file The SQL file containing the package body
     * @param packageName The name of the package
     * @param schema The schema/owner of the package
     * @param dialect The SQL dialect
     * @return The complexity metrics collection
     */
    @PostMapping(value = "/package-body/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplexityMetricsCollection> evaluatePackageBodyFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String packageName,
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

            // Evaluate the package body
            ComplexityMetricsCollection metricsCollection;
            boolean hasFunctions = !functionList.isEmpty();
            boolean hasTables = !tableList.isEmpty();
            boolean hasProcedures = !procedureList.isEmpty();

            if (hasFunctions && hasTables && hasProcedures) {
                // All three: custom functions, high-weight tables, and high-weight procedures
                metricsCollection = complexityEvaluationService.evaluatePackageBody(
                        sourceCode,
                        packageName,
                        schema,
                        dialect,
                        functionList,
                        tableList,
                        procedureList
                );
            } else if (hasFunctions && hasTables) {
                // Both custom functions and high-weight tables
                metricsCollection = complexityEvaluationService.evaluatePackageBody(
                        sourceCode,
                        packageName,
                        schema,
                        dialect,
                        functionList,
                        tableList
                );
            } else if (hasFunctions && hasProcedures) {
                // Both custom functions and high-weight procedures
                metricsCollection = complexityEvaluationService.evaluatePackageBody(
                        sourceCode,
                        packageName,
                        schema,
                        dialect,
                        functionList,
                        null,
                        procedureList
                );
            } else if (hasTables && hasProcedures) {
                // Both high-weight tables and high-weight procedures
                metricsCollection = complexityEvaluationService.evaluatePackageBody(
                        sourceCode,
                        packageName,
                        schema,
                        dialect,
                        null,
                        tableList,
                        procedureList
                );
            } else if (hasFunctions) {
                // Only custom functions
                metricsCollection = complexityEvaluationService.evaluatePackageBody(
                        sourceCode,
                        packageName,
                        schema,
                        dialect,
                        functionList
                );
            } else if (hasTables) {
                // Only high-weight tables
                metricsCollection = complexityEvaluationService.evaluatePackageBody(
                        sourceCode,
                        packageName,
                        schema,
                        dialect,
                        null,
                        tableList
                );
            } else if (hasProcedures) {
                // Only high-weight procedures
                metricsCollection = complexityEvaluationService.evaluatePackageBody(
                        sourceCode,
                        packageName,
                        schema,
                        dialect,
                        null,
                        null,
                        procedureList
                );
            } else {
                // None of the optional parameters
                metricsCollection = complexityEvaluationService.evaluatePackageBody(
                        sourceCode,
                        packageName,
                        schema,
                        dialect
                );
            }
            // Check if there were any failed statements in any of the procedures
            boolean hasFailedStatements = metricsCollection.getProcedures().stream()
                    .anyMatch(m -> m.isHasExceptions() && m.getFailedStatements() != null && !m.getFailedStatements().isEmpty());

            if (hasFailedStatements) {
                log.warn("Package body file evaluation completed with failed statements in some procedures");
            }

            return ResponseEntity.ok(metricsCollection);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to evaluate package body complexity from file", e);
            return ResponseEntity.badRequest().build();
        }
    }

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

            // Check if there were any failed statements
            if (metrics.isHasExceptions() && metrics.getFailedStatements() != null && !metrics.getFailedStatements().isEmpty()) {
                log.warn("SQL file evaluation completed with {} failed statements", metrics.getFailedStatements().size());
            }

            return ResponseEntity.ok(metrics);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to evaluate SQL complexity from file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/batch/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> evaluateBatchFromZip(
            @RequestPart("file") MultipartFile zipFile,
            @RequestParam(value = "dialect", required = false, defaultValue = "Oracle") String dialect,
            @RequestParam(value = "customFunctions", required = false) List<String> customFunctions,
            @RequestPart(value = "customFunctionsFile", required = false) MultipartFile customFunctionsFile,
            @RequestParam(value = "highWeightTables", required = false) List<String> highWeightTables,
            @RequestPart(value = "highWeightTablesFile", required = false) MultipartFile highWeightTablesFile,
            @RequestParam(value = "highWeightProcedures", required = false) List<String> highWeightProcedures,
            @RequestPart(value = "highWeightProceduresFile", required = false) MultipartFile highWeightProceduresFile,
            @RequestParam(value = "responseFormat", required = false, defaultValue = "json") String responseFormat) {
        List<ComplexityMetrics> results = new ArrayList<>();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("sql_zip_");

            // Process custom functions
            List<String> functionList = new ArrayList<>();
            if (customFunctions != null && !customFunctions.isEmpty()) {
                functionList.addAll(customFunctions);
            }
            if (customFunctionsFile != null && !customFunctionsFile.isEmpty()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(customFunctionsFile.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines()
                          .map(String::trim)
                          .filter(line -> !line.isEmpty())
                          .forEach(functionList::add);
                }
            }

            // Process high-weight tables
            List<String> tableList = new ArrayList<>();
            if (highWeightTables != null && !highWeightTables.isEmpty()) {
                tableList.addAll(highWeightTables);
            }
            if (highWeightTablesFile != null && !highWeightTablesFile.isEmpty()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(highWeightTablesFile.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines()
                          .map(String::trim)
                          .filter(line -> !line.isEmpty())
                          .forEach(tableList::add);
                }
            }

            // Process high-weight procedures
            List<String> procedureList = new ArrayList<>();
            if (highWeightProcedures != null && !highWeightProcedures.isEmpty()) {
                procedureList.addAll(highWeightProcedures);
            }
            if (highWeightProceduresFile != null && !highWeightProceduresFile.isEmpty()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(highWeightProceduresFile.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines()
                          .map(String::trim)
                          .filter(line -> !line.isEmpty())
                          .forEach(procedureList::add);
                }
            }

            boolean hasFunctions = !functionList.isEmpty();
            boolean hasTables = !tableList.isEmpty();
            boolean hasProcedures = !procedureList.isEmpty();

            try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".sql")) {
                        Path filePath = tempDir.resolve(entry.getName());
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);

                        // Try different charsets if UTF-8 fails
                        String sql = null;
                        List<Charset> charsets = Arrays.asList(
                            StandardCharsets.UTF_8,
                            StandardCharsets.ISO_8859_1,
                            Charset.forName("GBK"),
                            Charset.forName("GB2312")
                        );

                        for (Charset charset : charsets) {
                            try {
                                sql = Files.readString(filePath, charset);
                                break;  // If successful, exit the loop
                            } catch (MalformedInputException e) {
                                // Try next charset
                                continue;
                            }
                        }

                        if (sql != null) {
                            try {
                                // Extract file name without extension to use as procedure name
                                String fileName = entry.getName();
                                String baseName = fileName.substring(fileName.lastIndexOf('/') + 1);
                                if (baseName.toLowerCase().endsWith(".sql")) {
                                    baseName = baseName.substring(0, baseName.length() - 4);
                                }

                                // Check if this is a stored procedure or package body
                                boolean isStoredProcedure = false;
                                boolean isPackageBody = false;

                                // Simple detection based on keywords
                                String upperSql = sql.toUpperCase();
                                if (upperSql.contains("CREATE") &&
                                    (upperSql.contains("PROCEDURE") || upperSql.contains("FUNCTION"))) {
                                    isStoredProcedure = true;
                                }

                                if (upperSql.contains("CREATE") && upperSql.contains("PACKAGE") &&
                                    upperSql.contains("BODY")) {
                                    isPackageBody = true;
                                }

                                ComplexityMetrics metrics;

                                if (isPackageBody) {
                                    // Handle package body - get all procedures and add them individually
                                    ComplexityMetricsCollection metricsCollection;

                                    if (hasFunctions && hasTables && hasProcedures) {
                                        metricsCollection = complexityEvaluationService.evaluatePackageBody(
                                                sql, baseName, "HR", dialect, functionList, tableList, procedureList);
                                    } else if (hasFunctions && hasTables) {
                                        metricsCollection = complexityEvaluationService.evaluatePackageBody(
                                                sql, baseName, "HR", dialect, functionList, tableList);
                                    } else if (hasFunctions && hasProcedures) {
                                        metricsCollection = complexityEvaluationService.evaluatePackageBody(
                                                sql, baseName, "HR", dialect, functionList, null, procedureList);
                                    } else if (hasTables && hasProcedures) {
                                        metricsCollection = complexityEvaluationService.evaluatePackageBody(
                                                sql, baseName, "HR", dialect, null, tableList, procedureList);
                                    } else if (hasFunctions) {
                                        metricsCollection = complexityEvaluationService.evaluatePackageBody(
                                                sql, baseName, "HR", dialect, functionList);
                                    } else if (hasTables) {
                                        metricsCollection = complexityEvaluationService.evaluatePackageBody(
                                                sql, baseName, "HR", dialect, null, tableList);
                                    } else if (hasProcedures) {
                                        metricsCollection = complexityEvaluationService.evaluatePackageBody(
                                                sql, baseName, "HR", dialect, null, null, procedureList);
                                    } else {
                                        metricsCollection = complexityEvaluationService.evaluatePackageBody(
                                                sql, baseName, "HR", dialect);
                                    }

                                    // Check if there were any failed statements in any of the procedures
                                    boolean hasFailedStatements = metricsCollection.getProcedures().stream()
                                            .anyMatch(m -> m.isHasExceptions() && m.getFailedStatements() != null && !m.getFailedStatements().isEmpty());

                                    if (hasFailedStatements) {
                                        log.warn("Package body evaluation for {} completed with failed statements in some procedures", entry.getName());
                                    }

                                    // Add each procedure from the package to results
                                    if (metricsCollection != null && metricsCollection.getProcedures() != null) {
                                        for (ComplexityMetrics procMetrics : metricsCollection.getProcedures()) {
                                            // Add file name to each procedure metrics
                                            ComplexityMetrics procMetricsWithFileName = ComplexityMetrics.builder()
                                                        .overallScore(procMetrics.getOverallScore())
                                                        .tableCount(procMetrics.getTableCount())
                                                        .tableList(procMetrics.getTableList())
                                                        .joinCount(procMetrics.getJoinCount())
                                                        .whereConditionCount(procMetrics.getWhereConditionCount())
                                                        .subqueryCount(procMetrics.getSubqueryCount())
                                                        .aggregateFunctionCount(procMetrics.getAggregateFunctionCount())
                                                        .caseExpressionCount(procMetrics.getCaseExpressionCount())
                                                        .setOperationCount(procMetrics.getSetOperationCount())
                                                        .queryDepth(procMetrics.getQueryDepth())
                                                        .loopCount(procMetrics.getLoopCount())
                                                        .maxLoopNestingLevel(procMetrics.getMaxLoopNestingLevel())
                                                        .customFunctionCount(procMetrics.getCustomFunctionCount())
                                                        .customFunctionList(procMetrics.getCustomFunctionList())
                                                        .highWeightTableCount(procMetrics.getHighWeightTableCount())
                                                        .highWeightTableList(procMetrics.getHighWeightTableList())
                                                        .nestedProcedureCount(procMetrics.getNestedProcedureCount())
                                                        .nestedProcedureList(procMetrics.getNestedProcedureList())
                                                        .highWeightProcedureCount(procMetrics.getHighWeightProcedureCount())
                                                        .highWeightProcedureList(procMetrics.getHighWeightProcedureList())
                                                        .procedureName(procMetrics.getProcedureName())
                                                        .lineCount(procMetrics.getLineCount())
                                                        .fileName(entry.getName())
                                                        .additionalMetrics(procMetrics.getAdditionalMetrics())
                                                        .failedStatements(procMetrics.getFailedStatements())
                                                        .hasExceptions(procMetrics.isHasExceptions())
                                                        .subtransactionCount(procMetrics.getSubtransactionCount())
                                                        .maxSubtransactionNestingLevel(procMetrics.getMaxSubtransactionNestingLevel())
                                                        .subtransactionDetails(procMetrics.getSubtransactionDetails())
                                                         .procedureCallCount(procMetrics.getProcedureCallCount())
                                                        .procedureCallDetails(procMetrics.getProcedureCallDetails())
                                                        .dmlStatements(procMetrics.getDmlStatements())
                                                        .hintCount(procMetrics.getHintCount())
                                                        .hintList(procMetrics.getHintList())
                                                        .validHintCount(procMetrics.getValidHintCount())
                                                        .invalidHintCount(procMetrics.getInvalidHintCount())
                                                        .invalidHintList(procMetrics.getInvalidHintList())
                                                        .build();
                                            results.add(procMetricsWithFileName);
                                        }
                                    }
                                } else if (isStoredProcedure) {
                                    // Handle stored procedure
                                    if (hasFunctions && hasTables && hasProcedures) {
                                        metrics = complexityEvaluationService.evaluateStoredProcedure(
                                                sql, baseName, "HR", dialect, functionList, tableList, procedureList);
                                    } else if (hasFunctions && hasTables) {
                                        metrics = complexityEvaluationService.evaluateStoredProcedure(
                                                sql, baseName, "HR", dialect, functionList, tableList);
                                    } else if (hasFunctions && hasProcedures) {
                                        metrics = complexityEvaluationService.evaluateStoredProcedure(
                                                sql, baseName, "HR", dialect, functionList, null, procedureList);
                                    } else if (hasTables && hasProcedures) {
                                        metrics = complexityEvaluationService.evaluateStoredProcedure(
                                                sql, baseName, "HR", dialect, null, tableList, procedureList);
                                    } else if (hasFunctions) {
                                        metrics = complexityEvaluationService.evaluateStoredProcedure(
                                                sql, baseName, "HR", dialect, functionList);
                                    } else if (hasTables) {
                                        metrics = complexityEvaluationService.evaluateStoredProcedure(
                                                sql, baseName, "HR", dialect, null, tableList);
                                    } else if (hasProcedures) {
                                        metrics = complexityEvaluationService.evaluateStoredProcedure(
                                                sql, baseName, "HR", dialect, null, null, procedureList);
                                    } else {
                                        metrics = complexityEvaluationService.evaluateStoredProcedure(
                                                sql, baseName, "HR", dialect);
                                    }

                                    // Check if there were any failed statements
                                    if (metrics.isHasExceptions() && metrics.getFailedStatements() != null && !metrics.getFailedStatements().isEmpty()) {
                                        log.warn("Stored procedure evaluation for {} completed with {} failed statements",
                                                entry.getName(), metrics.getFailedStatements().size());
                                    }

                                    // Add file name to metrics
                                    ComplexityMetrics metricsWithFileName = ComplexityMetrics.builder()
                                        .overallScore(metrics.getOverallScore())
                                        .tableCount(metrics.getTableCount())
                                        .tableList(metrics.getTableList())
                                        .joinCount(metrics.getJoinCount())
                                        .whereConditionCount(metrics.getWhereConditionCount())
                                        .subqueryCount(metrics.getSubqueryCount())
                                        .aggregateFunctionCount(metrics.getAggregateFunctionCount())
                                        .caseExpressionCount(metrics.getCaseExpressionCount())
                                        .setOperationCount(metrics.getSetOperationCount())
                                        .queryDepth(metrics.getQueryDepth())
                                        .loopCount(metrics.getLoopCount())
                                        .maxLoopNestingLevel(metrics.getMaxLoopNestingLevel())
                                        .customFunctionCount(metrics.getCustomFunctionCount())
                                        .customFunctionList(metrics.getCustomFunctionList())
                                        .highWeightTableCount(metrics.getHighWeightTableCount())
                                        .highWeightTableList(metrics.getHighWeightTableList())
                                        .nestedProcedureCount(metrics.getNestedProcedureCount())
                                        .nestedProcedureList(metrics.getNestedProcedureList())
                                        .highWeightProcedureCount(metrics.getHighWeightProcedureCount())
                                        .highWeightProcedureList(metrics.getHighWeightProcedureList())
                                        .procedureName(baseName)
                                        .lineCount(metrics.getLineCount())
                                        .fileName(entry.getName())
                                        .additionalMetrics(metrics.getAdditionalMetrics())
                                        .failedStatements(metrics.getFailedStatements())
                                        .hasExceptions(metrics.isHasExceptions())
                                        .subtransactionCount(metrics.getSubtransactionCount())
                                        .maxSubtransactionNestingLevel(metrics.getMaxSubtransactionNestingLevel())
                                        .subtransactionDetails(metrics.getSubtransactionDetails())
                                        .procedureCallCount(metrics.getProcedureCallCount())
                                        .procedureCallDetails(metrics.getProcedureCallDetails())
                                        .dmlStatements(metrics.getDmlStatements())
                                        .hintCount(metrics.getHintCount())
                                        .hintList(metrics.getHintList())
                                        .validHintCount(metrics.getValidHintCount())
                                        .invalidHintCount(metrics.getInvalidHintCount())
                                        .invalidHintList(metrics.getInvalidHintList())
                                        .build();
                                    results.add(metricsWithFileName);
                                }
                            } catch (Exception e) {
                                log.error("Failed to process file {}", entry.getName(), e);
                            }
                        } else {
                            log.error("Unable to read file {} with any supported charset", entry.getName());
                        }
                    }
                }
            }

            // Return response based on requested format
            if ("excel".equalsIgnoreCase(responseFormat)) {
                try {
                    // Convert to Excel
                    byte[] excelBytes = ExcelExportUtil.convertToExcel(results);

                    // Set up response headers
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    headers.setContentDispositionFormData("attachment", "complexity_metrics.xlsx");
                    headers.setContentLength(excelBytes.length);

                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(excelBytes);
                } catch (IOException e) {
                    log.error("Failed to generate Excel file", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to generate Excel file: " + e.getMessage());
                }
            } else {
                // Default to JSON
                return ResponseEntity.ok(results);
            }
        } catch (Exception e) {
            log.error("Failed to process ZIP file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        } finally {
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                } catch (IOException e) {
                    log.error("Failed to clean up temporary directory", e);
                }
            }
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

    /**
     * Get the stored procedure parser for the specified dialect.
     *
     * @param dialect The SQL dialect
     * @return The stored procedure parser
     * @throws IllegalArgumentException If no parser is available for the dialect
     */
    private StoredProcedureParser getStoredProcedureParser(String dialect) {
        switch (dialect.toLowerCase()) {
            case "oracle":
                return oracleStoredProcedureParser;
            case "gauss":
                return gaussStoredProcedureParser;
            case "hive":
                return hiveStoredProcedureParser;
            default:
                throw new IllegalArgumentException("No stored procedure parser available for dialect: " + dialect);
        }
    }
}
