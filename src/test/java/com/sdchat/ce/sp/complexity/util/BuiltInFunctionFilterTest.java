package com.sdchat.ce.sp.complexity.util;

import com.sdchat.ce.sp.complexity.model.BuiltInFunction;
import com.sdchat.ce.sp.complexity.model.FunctionFilterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BuiltInFunctionFilterTest {

    @Autowired
    private BuiltInFunctionFilter builtInFunctionFilter;

    @Test
    void isBuiltinFunction_WithValidBuiltinFunction_ReturnsTrue() {
        assertTrue(builtInFunctionFilter.isBuiltinFunction("gs_index_advise"));
        assertTrue(builtInFunctionFilter.isBuiltinFunction("HLL_EMPTY"));
        assertTrue(builtInFunctionFilter.isBuiltinFunction("hash_array"));
    }

    @Test
    void isBuiltinFunction_WithCaseInsensitiveFunction_ReturnsTrue() {
        assertTrue(builtInFunctionFilter.isBuiltinFunction("GS_INDEX_ADVISE"));
        assertTrue(builtInFunctionFilter.isBuiltinFunction("Hll_Empty"));
        assertTrue(builtInFunctionFilter.isBuiltinFunction("HASH_ARRAY"));
    }

    @Test
    void isBuiltinFunction_WithUserDefinedFunction_ReturnsFalse() {
        assertFalse(builtInFunctionFilter.isBuiltinFunction("my_custom_proc"));
        assertFalse(builtInFunctionFilter.isBuiltinFunction("get_employee_details"));
        assertFalse(builtInFunctionFilter.isBuiltinFunction("process_data"));
    }

    @Test
    void isBuiltinFunction_WithNullFunction_ReturnsFalse() {
        assertFalse(builtInFunctionFilter.isBuiltinFunction(null));
    }

    @Test
    void filterFunctions_WithMixedFunctions_FiltersBuiltinFunctions() {
        List<String> functionNames = Arrays.asList(
                "gs_index_advise",
                "my_custom_proc",
                "hll_empty",
                "user_function_a",
                "hash_array"
        );

        FunctionFilterResult result = builtInFunctionFilter.filterFunctions(functionNames);

        assertNotNull(result);
        assertEquals(3, result.getFilteredCount());
        assertEquals(2, result.getRetainedCount());
        assertFalse(result.getFilteredFunctions().isEmpty());
        assertTrue(result.getCategoryBreakdown().containsKey("AI特性函数") ||
                   result.getCategoryBreakdown().containsKey("HashFunc函数"));
    }

    @Test
    void filterFunctions_WithOnlyUserFunctions_ReturnsEmptyFilterResult() {
        List<String> functionNames = Arrays.asList(
                "my_custom_proc",
                "user_function_a",
                "process_data"
        );

        FunctionFilterResult result = builtInFunctionFilter.filterFunctions(functionNames);

        assertNotNull(result);
        assertEquals(0, result.getFilteredCount());
        assertEquals(3, result.getRetainedCount());
        assertTrue(result.getFilteredFunctions().isEmpty());
        assertTrue(result.getCategoryBreakdown().isEmpty());
    }

    @Test
    void filterFunctions_WithOnlyBuiltinFunctions_FiltersAll() {
        List<String> functionNames = Arrays.asList(
                "gs_index_advise",
                "hll_empty",
                "hash_array"
        );

        FunctionFilterResult result = builtInFunctionFilter.filterFunctions(functionNames);

        assertNotNull(result);
        assertEquals(3, result.getFilteredCount());
        assertEquals(0, result.getRetainedCount());
        assertEquals(3, result.getFilteredFunctions().size());
    }

    @Test
    void filterFunctions_WithEmptyList_ReturnsEmptyResult() {
        FunctionFilterResult result = builtInFunctionFilter.filterFunctions(Arrays.asList());

        assertNotNull(result);
        assertEquals(0, result.getFilteredCount());
        assertEquals(0, result.getRetainedCount());
        assertTrue(result.getFilteredFunctions().isEmpty());
    }

    @Test
    void filterFunctions_WithNullList_ReturnsEmptyResult() {
        FunctionFilterResult result = builtInFunctionFilter.filterFunctions(null);

        assertNotNull(result);
        assertEquals(0, result.getFilteredCount());
        assertEquals(0, result.getRetainedCount());
    }

    @Test
    void getBuiltinFunction_WithValidFunction_ReturnsBuiltInFunction() {
        BuiltInFunction func = builtInFunctionFilter.getBuiltinFunction("gs_index_advise");

        assertNotNull(func);
        assertEquals("gs_index_advise", func.getName());
        assertNotNull(func.getCategory());
    }

    @Test
    void getBuiltinFunction_WithInvalidFunction_ReturnsNull() {
        BuiltInFunction func = builtInFunctionFilter.getBuiltinFunction("non_existent_function");

        assertNull(func);
    }

    @Test
    void getBuiltinFunctionCount_ReturnsPositiveCount() {
        int count = builtInFunctionFilter.getBuiltinFunctionCount();

        assertTrue(count > 0);
        assertTrue(count >= 1000);
    }

    @Test
    void isFilteringEnabled_ReturnsTrue() {
        assertTrue(builtInFunctionFilter.isFilteringEnabled());
    }
}
