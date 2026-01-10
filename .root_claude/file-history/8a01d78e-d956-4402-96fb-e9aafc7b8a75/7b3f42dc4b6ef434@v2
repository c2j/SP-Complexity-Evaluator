# Quick Start Guide: Complexity Weights UI Enhancement

## Overview

This enhancement adds transparency and customization capabilities to the SQL complexity evaluator. Users can now view complexity evaluation formulas, modify weights interactively, and export weight information to Excel.

## New Features

### 1. View Complexity Formulas and Weights

**Purpose**: Provide transparency into how complexity scores are calculated.

**Access**: Main evaluation page → "Complexity Formula" section

**Features**:
- Display complete mathematical formula for each dialect (Oracle, Gauss, Hive)
- Show default weight factors for all complexity components
- Dialect-specific factor explanations
- Interactive tooltips with factor descriptions

**Usage**:
1. Navigate to the main evaluation page
2. Select your database dialect
3. View the "Complexity Formula" section below the input area
4. Click on any factor to see detailed explanation

### 2. Modify Calculation Weights

**Purpose**: Allow customization of complexity evaluation to match organizational priorities.

**Access**: Main evaluation page → "Weight Configuration" panel

**Features**:
- Real-time weight modification with sliders and number inputs
- Instant score recalculation
- Weight validation (prevent invalid inputs)
- Reset to default weights
- Save custom configurations as templates

**Usage**:
1. Enter your SQL or stored procedure
2. Expand the "Weight Configuration" panel
3. Adjust individual weights using:
   - Number inputs for precise values
   - Sliders for quick adjustments
4. View updated complexity score in real-time
5. Click "Reset" to restore defaults or "Save Template" to reuse

### 3. Export Weight Information to Excel

**Purpose**: Document weight configurations used in evaluations for team consistency.

**Access**: Export functionality → Enhanced Excel format

**Features**:
- Separate "Weights Configuration" worksheet
- Comparison of default vs. modified weights
- Factor contribution breakdowns
- Template metadata (name, creation date)

**Usage**:
1. Complete an evaluation with your preferred weights
2. Click "Export to Excel"
3. Open the generated Excel file
4. Review the "Weights Configuration" worksheet for complete weight details

### 4. Compare Results with Different Weights

**Purpose**: Analyze how weight changes impact complexity scores.

**Access**: Results page → "Compare" functionality

**Features**:
- Side-by-side comparison of evaluations
- Factor contribution differences
- Weight template management
- Historical comparison tracking

**Usage**:
1. Evaluate the same SQL with different weight configurations
2. Use the "Compare" feature to analyze differences
3. Identify which factors most impact the overall score
4. Save insights for team discussions

## API Integration

### New Endpoints

**Weight Management**:
- `GET /api/weights?dialect={dialect}` - Get current weights
- `PUT /api/weights` - Update weights
- `POST /api/weights/reset?dialect={dialect}` - Reset to defaults
- `GET /api/weights/formulas?dialect={dialect}` - Get complexity formulas

**Template Management**:
- `GET /api/weights/templates` - List saved templates
- `POST /api/weights/templates` - Save new template
- `GET /api/weights/templates/{id}` - Load template
- `DELETE /api/weights/templates/{id}` - Delete template

**Enhanced Evaluation**:
- `POST /api/complexity/sql` - Evaluate with optional weight configuration
- `POST /api/complexity/stored-procedure` - Evaluate procedures with weights

### Integration Examples

**Get Current Weights**:
```bash
curl -X GET "http://localhost:8080/api/weights?dialect=ORACLE" \
  -H "Accept: application/json"
```

**Update Weights**:
```bash
curl -X PUT "http://localhost:8080/api/weights" \
  -H "Content-Type: application/json" \
  -d '{
    "dialect": "ORACLE",
    "sqlWeights": {
      "tableCount": 1.5,
      "joinCount": 2.0,
      "whereConditionCount": 1.5
    },
    "procedureWeights": {
      "loopCount": 2.5,
      "customFunctionCount": 2.0
    }
  }'
```

**Evaluate with Custom Weights**:
```bash
curl -X POST "http://localhost:8080/api/complexity/sql" \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT * FROM employees WHERE department_id = 10",
    "dialect": "ORACLE",
    "weightConfigurationId": "custom-weight-id"
  }'
```

## Frontend Integration

### HTML Structure Updates

```html
<!-- Weight Configuration Panel -->
<div id="weight-config-panel" class="config-panel">
  <h3>Weight Configuration (Oracle)</h3>

  <div class="weight-category">
    <h4>SQL Factors</h4>
    <div class="weight-input">
      <label>Table Count</label>
      <input type="number" id="tableCount" value="1.0" step="0.1" min="0" max="100">
      <input type="range" id="tableCountSlider" value="1.0" step="0.1" min="0" max="100">
    </div>

    <div class="weight-input">
      <label>Join Count</label>
      <input type="number" id="joinCount" value="2.0" step="0.1" min="0" max="100">
      <input type="range" id="joinCountSlider" value="2.0" step="0.1" min="0" max="100">
    </div>
    <!-- ... more factors -->
  </div>

  <div class="weight-actions">
    <button id="resetWeights">Reset to Default</button>
    <button id="saveTemplate">Save as Template</button>
    <button id="recalculate">Recalculate</button>
  </div>
</div>

<!-- Complexity Formula Display -->
<div id="formula-display" class="formula-panel">
  <h3>Complexity Calculation Formula</h3>
  <div class="formula">
    overallScore = (tableCount × TABLE_WEIGHT) +
                 (joinCount × JOIN_WEIGHT) +
                 (whereConditionCount × WHERE_WEIGHT) +
                 (subqueryCount × SUBQUERY_WEIGHT) +
                 (aggregateFunctionCount × AGGREGATE_WEIGHT) +
                 (caseExpressionCount × CASE_WEIGHT) +
                 (setOperationCount × SET_WEIGHT) +
                 (groupByCount × GROUP_BY_WEIGHT) +
                 (orderByCount × ORDER_BY_WEIGHT)
  </div>
</div>
```

### JavaScript Integration

```javascript
// Weight management
class WeightManager {
  constructor() {
    this.weights = {};
    this.dialect = 'ORACLE';
    this.initializeWeights();
  }

  async loadWeights(dialect) {
    const response = await fetch(`/api/weights?dialect=${dialect}`);
    this.weights = await response.json();
    this.updateUI();
  }

  async updateWeights() {
    const response = await fetch('/api/weights', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(this.weights)
    });

    if (response.ok) {
      this.weights = await response.json();
      this.recalculateComplexity();
    }
  }

  bindEvents() {
    document.getElementById('tableCount').addEventListener('input', (e) => {
      this.weights.sqlWeights.tableCount = parseFloat(e.target.value);
      document.getElementById('tableCountSlider').value = e.target.value;
    });

    document.getElementById('resetWeights').addEventListener('click', () => {
      this.resetToDefault();
    });

    document.getElementById('saveTemplate').addEventListener('click', () => {
      this.saveAsTemplate();
    });
  }

  async recalculateComplexity() {
    // Trigger evaluation with current weights
    const sql = document.getElementById('sqlInput').value;
    const response = await fetch('/api/complexity/sql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sql: sql,
        dialect: this.dialect,
        weightConfigurationId: this.weights.id
      })
    });

    const result = await response.json();
    this.displayResults(result);
  }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
  const weightManager = new WeightManager();
  weightManager.loadWeights('ORACLE');
});
```

## Testing Guide

### Unit Testing

**Weight Management Tests**:
```java
@Test
public void testUpdateWeights() {
    WeightUpdateRequest request = new WeightUpdateRequest();
    request.setDialect("ORACLE");
    request.getSqlWeights().setTableCount(2.0);

    mockMvc.perform(put("/api/weights")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sqlWeights.tableCount").value(2.0));
}
```

**Excel Export Tests**:
```java
@Test
public void testExcelExportWithWeights() throws Exception {
    EvaluationResult result = createTestResult();
    result.setWeightConfiguration(createCustomWeights());

    byte[] excelData = excelExportService.exportWithWeights(result);

    Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelData));
    Sheet weightsSheet = workbook.getSheet("Weights Configuration");
    assertNotNull(weightsSheet);
    assertEquals("Custom Weights", weightsSheet.getRow(1).getCell(0).getStringCellValue());
}
```

### Integration Testing

**End-to-End Workflow**:
```java
@Test
public void testCompleteWorkflow() throws Exception {
    // 1. Load default weights
    mockMvc.perform(get("/api/weights?dialect=ORACLE"))
        .andExpect(status().isOk());

    // 2. Update weights
    WeightUpdateRequest update = createWeightUpdate();
    mockMvc.perform(put("/api/weights")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk());

    // 3. Evaluate with new weights
    SqlEvaluationRequest evaluation = new SqlEvaluationRequest();
    evaluation.setSql("SELECT * FROM table1 JOIN table2");
    evaluation.setDialect("ORACLE");

    MvcResult result = mockMvc.perform(post("/api/complexity/sql")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(evaluation)))
        .andReturn();

    // 4. Verify updated score reflects new weights
    EvaluationResult evalResult = objectMapper.readValue(
        result.getResponse().getContentAsString(),
        EvaluationResult.class
    );

    // Table count (2) × weight (1.0) + Join count (1) × weight (2.0) = 4.0
    assertEquals(4.0, evalResult.getOverallScore(), 0.01);
}
```

## Deployment Notes

### Configuration Updates

Add these properties to `application.properties`:
```properties
# Weight management settings
app.weights.max-value=100.0
app.weights.min-value=0.0
app.weights.session-timeout=24h
app.weights.max-sessions-per-user=5

# Excel export settings
app.export.excel.include-weights=true
app.export.excel.weights-sheet-name="Weights Configuration"
```

### Database Schema

No database changes required - weights are stored in session and file-based templates.

### Monitoring

Add metrics for weight management:
- Weight update request count and latency
- Template save/load operations
- Evaluation with custom weights vs default weights
- Excel export with weight information

## Troubleshooting

### Common Issues

**Weight Validation Errors**:
- Ensure all weights are non-negative numbers
- Check that at least one weight is positive
- Verify weight values don't exceed maximum (100.0)

**Session Timeout Issues**:
- Weights expire after 24 hours of inactivity
- Maximum 5 concurrent sessions per user
- Consider saving frequently used configurations as templates

**Excel Export Issues**:
- Ensure weight configuration is set before export
- Check that weights worksheet exists in export file
- Verify modified weights are properly highlighted

### Performance Considerations

- Weight updates are fast (<200ms response time)
- Excel export with weights adds minimal overhead (<100ms)
- Session storage uses minimal memory footprint
- Concurrent evaluations maintain weight isolation