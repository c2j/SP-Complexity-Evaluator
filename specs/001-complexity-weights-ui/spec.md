# Feature Specification: Complexity Weights UI Enhancement

**Feature Branch**: `[001-complexity-weights-ui]`
**Created**: 2024-11-28
**Status**: Draft
**Input**: User description: "检查当前版本功能实现的完备性和准确性。实现优化功能：1、增加在页面上展示复杂度评估计算公式和各项权重；2、允许为本次计算修改权重，在最终输出excel中增加相关权重的内容"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Complexity Formulas and Weights (Priority: P1)

As a database developer, I want to see the complexity evaluation formulas and weight factors used by the system so that I can understand how SQL complexity scores are calculated and validate the methodology.

**Why this priority**: This is foundational for transparency and user trust - users need to understand how the system works before they can effectively modify weights or interpret results.

**Independent Test**: Can be fully tested by accessing the web interface and verifying that all complexity factors, their current weights, and the calculation formula are displayed correctly for each database dialect (Oracle, Gauss, Hive).

**Acceptance Scenarios**:

1. **Given** I am on the main evaluation page, **When** I look for complexity information, **Then** I see a dedicated section showing the complete evaluation formula
2. **Given** I select Oracle dialect, **When** I view the weights section, **Then** I see all Oracle-specific weights (Table: 1.0, Join: 2.0, WHERE Condition: 1.5, etc.)
3. **Given** I switch to Gauss dialect, **When** I view the weights, **Then** I see Gauss-specific weights displayed accurately
4. **Given** I switch to Hive dialect, **When** I view the weights, **Then** I see Hive-specific weights including LATERAL VIEW: 2.0, DISTRIBUTE BY: 1.5, etc.
5. **Given** I am viewing stored procedure complexity, **When** I look at the formula, **Then** I see additional factors like Loop: 2.5, Nested Loop: 1.5, Custom Function: 2.0, etc.

---

### User Story 2 - Modify Calculation Weights (Priority: P1)

As a database administrator, I want to adjust the weight factors for complexity calculation so that the evaluation better matches my organization's specific priorities and coding standards.

**Why this priority**: This is the core interactive functionality that makes the tool flexible and adaptable to different organizational needs.

**Independent Test**: Can be tested by modifying weights, running evaluations with original and modified weights, and verifying that results change according to the weight adjustments.

**Acceptance Scenarios**:

1. **Given** I am on the evaluation page with SQL input, **When** I click on weight modification controls, **Then** I can adjust individual weight factors using number inputs or sliders
2. **Given** I modify the Table weight from 1.0 to 2.0, **When** I evaluate a SQL with 3 tables, **Then** the table contribution to the score is 6.0 instead of 3.0
3. **Given** I modify multiple weights, **When** I run an evaluation, **Then** the final score reflects all my weight modifications accurately
4. **Given** I reset weights to default, **When** I run the same evaluation, **Then** I get the original system-calculated score
5. **Given** I enter invalid weight values (negative numbers, text), **When** I try to submit, **Then** the system validates and prevents submission with appropriate error messages

---

### User Story 3 - Export Weight Information to Excel (Priority: P2)

As a development team lead, I want the Excel export to include the weight configuration used for each evaluation so that I can document our complexity assessment methodology and maintain consistency across the team.

**Why this priority**: This enables documentation and standardization processes, which are important for team coordination and compliance requirements.

**Independent Test**: Can be fully tested by performing evaluations with custom weights, exporting to Excel, and verifying that the weight information appears correctly in the output file.

**Acceptance Scenarios**:

1. **Given** I perform an evaluation with default weights, **When** I export to Excel, **Then** I see a "Weights Configuration" worksheet with all default weights listed
2. **Given** I perform an evaluation with modified weights, **When** I export to Excel, **Then** the Weights Configuration sheet shows my custom weights and marks modified values
3. **Given** I export batch evaluation results, **When** I open the Excel file, **Then** each evaluation result includes the weight configuration used for that specific evaluation
4. **Given** I compare two evaluations with different weights, **When** I view the Excel, **Then** I can see which weights differed between the two evaluations

---

### User Story 4 - Compare Results with Different Weights (Priority: P3)

As a performance analyst, I want to compare the same SQL evaluation results using different weight configurations so that I can understand how various factors impact the overall complexity score.

**Why this priority**: This provides analytical capabilities for understanding the impact of different complexity factors on the overall assessment.

**Independent Test**: Can be tested by running the same SQL with different weight configurations and verifying that the comparison view correctly shows the differences.

**Acceptance Scenarios**:

1. **Given** I evaluate a SQL with default weights, **When** I modify weights and re-evaluate, **Then** I can see a side-by-side comparison of the two results
2. **Given** I have multiple evaluations of the same SQL, **When** I view the comparison, **Then** I can see which factors contributed most to the score differences
3. **Given** I want to revert to a previous weight configuration, **When** I select from saved configurations, **Then** the system applies the weights correctly

---

## Edge Cases

- What happens when users set all weights to zero? System should prevent this and require at least one weight to be positive.
- How does system handle very large weight values? Should have reasonable upper limits and validation.
- What happens when user modifies weights during an active evaluation? Should maintain session consistency.
- How does system handle weight modifications when evaluating ZIP files with multiple SQL files? All files should use the same weight configuration.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display the complete complexity evaluation formula for SQL statements showing how individual factors combine
- **FR-002**: System MUST display current weight factors for all complexity factors organized by database dialect (Oracle, Gauss, Hive)
- **FR-003**: System MUST display stored procedure complexity factors including loop, nested loop, custom function, high-weight table, and nested procedure weights
- **FR-004**: Users MUST be able to modify individual weight factors through the web interface
- **FR-005**: System MUST validate weight inputs to prevent invalid values (negative numbers, non-numeric input, all zeros)
- **FR-006**: System MUST recalculate complexity scores in real-time or on-demand when weights are modified
- **FR-007**: System MUST provide a reset function to restore default weights
- **FR-008**: Excel exports MUST include a worksheet showing the weight configuration used for evaluation
- **FR-009**: System MUST clearly indicate which weights were modified from default values in both the UI and Excel export
- **FR-010**: Users MUST be able to save and load custom weight configurations for repeated use
- **FR-011**: System MUST maintain weight configuration consistency across all SQL files in batch evaluations
- **FR-012**: System MUST support comparison of evaluation results using different weight configurations

### Key Entities

- **Weight Configuration**: Set of weight factors for all complexity factors, with dialect-specific variations, includes default and user-modified values
- **Evaluation Result**: Complexity assessment with associated weight configuration, showing both individual factor contributions and overall score
- **Weight Template**: Named set of custom weights that users can save and reuse across multiple evaluations
- **Comparison View**: Side-by-side display of evaluation results using different weight configurations

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can locate and view complexity formulas and weights within 10 seconds of accessing the main interface
- **SC-002**: Users can modify weights and see updated complexity results within 5 seconds of applying changes
- **SC-003**: 95% of weight modifications are validated and processed correctly without errors
- **SC-004**: Excel exports include complete and accurate weight configuration information 100% of the time
- **SC-005**: Users report improved understanding of complexity scoring methodology through post-feature surveys
- **SC-006**: Support tickets related to "understanding complexity scores" decrease by at least 40%