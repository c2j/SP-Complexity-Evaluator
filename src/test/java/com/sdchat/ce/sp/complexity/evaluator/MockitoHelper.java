package com.sdchat.ce.sp.complexity.evaluator;

import org.mockito.stubbing.OngoingStubbing;

import java.util.List;

/**
 * Helper class to solve Mockito generic type issues in tests.
 */
public class MockitoHelper {
    
    /**
     * Safely cast a OngoingStubbing to work with List return types.
     * 
     * @param <T> The type of items in the list
     * @param stubbing The Mockito stubbing to modify
     * @param returnValue The list value to return
     * @return The same stubbing for method chaining
     */
    public static <T> OngoingStubbing<List<T>> thenReturnList(OngoingStubbing<List<T>> stubbing, List<T> returnValue) {
        return stubbing.thenReturn(returnValue);
    }
}