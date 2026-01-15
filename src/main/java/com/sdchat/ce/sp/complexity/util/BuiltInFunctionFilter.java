package com.sdchat.ce.sp.complexity.util;

import com.sdchat.ce.sp.complexity.model.BuiltInFunction;
import com.sdchat.ce.sp.complexity.model.FunctionFilterResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for filtering GaussDB built-in functions from analysis results.
 * Loads the built-in function list from gaussdb_functions.json at startup.
 */
@Slf4j
@Component
public class BuiltInFunctionFilter {

    private Set<String> builtinFunctionNames;
    private Map<String, BuiltInFunction> builtinFunctionMap;
    private List<BuiltInFunction> allBuiltinFunctions;

    @PostConstruct
    public void init() {
        loadBuiltinFunctions();
    }

    private void loadBuiltinFunctions() {
        builtinFunctionNames = new HashSet<>();
        builtinFunctionMap = new HashMap<>();
        allBuiltinFunctions = new ArrayList<>();

        try {
            ClassPathResource resource = new ClassPathResource("gaussdb_functions.json");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            Map<String, Object> jsonData = mapper.readValue(resource.getInputStream(), Map.class);

            List<Map<String, Object>> functions = (List<Map<String, Object>>) jsonData.get("functions");

            if (functions != null) {
                for (Map<String, Object> funcData : functions) {
                    try {
                        String name = getStringValue(funcData, "name");
                        String category = getStringValue(funcData, "category");

                        if (name != null && !name.isEmpty()) {
                            BuiltInFunction func = BuiltInFunction.builder()
                                    .name(name)
                                    .category(category)
                                    .parameters(getStringValue(funcData, "parameters"))
                                    .description(getStringValue(funcData, "description"))
                                    .returnType(getStringValue(funcData, "return_type"))
                                    .build();

                            String lowerName = name.toLowerCase();
                            builtinFunctionNames.add(lowerName);
                            builtinFunctionMap.put(lowerName, func);
                            allBuiltinFunctions.add(func);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse built-in function entry: {}", e.getMessage());
                    }
                }

                log.info("Loaded {} built-in functions from gaussdb_functions.json", builtinFunctionNames.size());
            }
        } catch (IOException e) {
            log.warn("Failed to load gaussdb_functions.json: {}. Filtering will be disabled.", e.getMessage());
            builtinFunctionNames = Collections.emptySet();
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Checks if a function name is a built-in function (case-insensitive).
     *
     * @param functionName the function name to check
     * @return true if it's a built-in function, false otherwise
     */
    public boolean isBuiltinFunction(String functionName) {
        if (functionName == null || builtinFunctionNames == null) {
            return false;
        }
        return builtinFunctionNames.contains(functionName.toLowerCase());
    }

    /**
     * Filters a list of function names, separating built-in from user-defined functions.
     *
     * @param functionNames list of function names to filter
     * @return FunctionFilterResult containing filtered functions and statistics
     */
    public FunctionFilterResult filterFunctions(List<String> functionNames) {
        if (functionNames == null || functionNames.isEmpty()) {
            return FunctionFilterResult.builder()
                    .filteredFunctions(Collections.emptyList())
                    .filteredCount(0)
                    .retainedCount(0)
                    .categoryBreakdown(Collections.emptyMap())
                    .build();
        }

        List<BuiltInFunction> filtered = new ArrayList<>();
        Map<String, Integer> categoryBreakdown = new HashMap<>();

        for (String funcName : functionNames) {
            String lowerName = funcName.toLowerCase();
            if (builtinFunctionNames.contains(lowerName)) {
                BuiltInFunction builtin = builtinFunctionMap.get(lowerName);
                if (builtin != null) {
                    filtered.add(builtin);
                    String category = builtin.getCategory();
                    if (category != null) {
                        categoryBreakdown.merge(category, 1, Integer::sum);
                    }
                }
            }
        }

        int totalFunctions = functionNames.size();
        int filteredCount = filtered.size();
        int retainedCount = totalFunctions - filteredCount;

        return FunctionFilterResult.builder()
                .filteredFunctions(filtered)
                .filteredCount(filteredCount)
                .retainedCount(retainedCount)
                .categoryBreakdown(categoryBreakdown)
                .build();
    }

    /**
     * Gets the BuiltInFunction object for a given function name.
     *
     * @param functionName the function name
     * @return BuiltInFunction if found, null otherwise
     */
    public BuiltInFunction getBuiltinFunction(String functionName) {
        if (functionName == null) {
            return null;
        }
        return builtinFunctionMap.get(functionName.toLowerCase());
    }

    /**
     * Gets the total count of loaded built-in functions.
     *
     * @return the number of built-in functions
     */
    public int getBuiltinFunctionCount() {
        return builtinFunctionNames != null ? builtinFunctionNames.size() : 0;
    }

    /**
     * Checks if the built-in functions list was successfully loaded.
     *
     * @return true if built-in functions are available for filtering
     */
    public boolean isFilteringEnabled() {
        return builtinFunctionNames != null && !builtinFunctionNames.isEmpty();
    }
}
