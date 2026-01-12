package com.sdchat.ce.sp.complexity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdchat.ce.sp.complexity.controller.ComplexityEvaluationController.SqlEvaluationRequest;
import com.sdchat.ce.sp.complexity.controller.ComplexityEvaluationController.StoredProcedureEvaluationRequest;
import com.sdchat.ce.sp.complexity.model.ComplexityMetrics;
import com.sdchat.ce.sp.complexity.service.ComplexityEvaluationService;
import com.sdchat.ce.sp.complexity.parser.OracleStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.GaussStoredProcedureParser;
import com.sdchat.ce.sp.complexity.parser.HiveStoredProcedureParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplexityEvaluationController.class)
class ComplexityEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ComplexityEvaluationService service;
    
    @MockBean
    private OracleStoredProcedureParser oracleStoredProcedureParser;
    
    @MockBean
    private GaussStoredProcedureParser gaussStoredProcedureParser;
    
    @MockBean
    private HiveStoredProcedureParser hiveStoredProcedureParser;
    
    @Test
    void evaluateSql() throws Exception {
        // Prepare test data
        SqlEvaluationRequest request = new SqlEvaluationRequest();
        request.setSql("SELECT * FROM employees");
        request.setDialect("Oracle");
        
        ComplexityMetrics metrics = ComplexityMetrics.builder()
                .overallScore(10.5)
                .tableCount(1)
                .joinCount(0)
                .whereConditionCount(0)
                .build();
        
        // Mock service response
        when(service.evaluateSqlStatement(eq(request.getSql()), eq(request.getDialect())))
                .thenReturn(metrics);
        
        // Perform request and verify response
        mockMvc.perform(post("/api/complexity/sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(10.5))
                .andExpect(jsonPath("$.tableCount").value(1))
                .andExpect(jsonPath("$.joinCount").value(0))
                .andExpect(jsonPath("$.whereConditionCount").value(0));
    }
    
    @Test
    void evaluateStoredProcedure() throws Exception {
        // Prepare test data
        StoredProcedureEvaluationRequest request = new StoredProcedureEvaluationRequest();
        request.setSourceCode("CREATE OR REPLACE PROCEDURE test_proc AS BEGIN NULL; END;");
        request.setName("test_proc");
        request.setSchema("HR");
        request.setDialect("Oracle");
        
        ComplexityMetrics metrics = ComplexityMetrics.builder()
                .overallScore(5.0)
                .tableCount(0)
                .build();
        
        // Mock service response
        when(service.evaluateStoredProcedure(
                eq(request.getSourceCode()),
                eq(request.getName()),
                eq(request.getSchema()),
                eq(request.getDialect())))
                .thenReturn(metrics);
        
        // Perform request and verify response
        mockMvc.perform(post("/api/complexity/stored-procedure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(5.0))
                .andExpect(jsonPath("$.tableCount").value(0));
    }
    
    @Test
    void evaluateSqlWithInvalidRequest() throws Exception {
        // Prepare invalid request (missing SQL)
        SqlEvaluationRequest request = new SqlEvaluationRequest();
        request.setDialect("Oracle");
        
        // Perform request and verify response
        mockMvc.perform(post("/api/complexity/sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void evaluateSqlWithServiceException() throws Exception {
        // Prepare test data
        SqlEvaluationRequest request = new SqlEvaluationRequest();
        request.setSql("SELECT * FROM");
        request.setDialect("Oracle");
        
        // Mock service exception
        when(service.evaluateSqlStatement(anyString(), anyString()))
                .thenThrow(new Exception("Failed to parse SQL"));
        
        // Perform request and verify response
        mockMvc.perform(post("/api/complexity/sql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
