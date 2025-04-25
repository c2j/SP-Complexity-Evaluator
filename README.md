# 存储过程复杂度评估器

一个用于评估SQL语句和存储过程复杂度的服务，初始支持Oracle数据库方言，并可扩展到其他数据库方言。

## 概述

该服务基于各种指标分析SQL语句和存储过程，以确定其复杂度：

- 涉及的表数量
- 连接（Join）数量
- WHERE子句中的条件数量
- 子查询数量
- 聚合函数数量
- CASE表达式数量
- 集合操作（UNION、UNION ALL、INTERSECT、MINUS）数量
- 嵌套查询深度
- 循环和嵌套循环级别数量
- 自定义函数调用
- 高权重表引用
- 嵌套存储过程调用
- 以及更多...

该服务计算总体复杂度分数，可用于识别可能需要优化或重构的复杂查询。

## 复杂度计算方法

### SQL语句复杂度

SQL语句的复杂度基于以下因素计算，每个因素都有指定的权重：

| 因素 | 权重 (Oracle) | 权重 (Gauss) | 权重 (Hive) | 描述 |
|--------|----------------|----------------|----------------|-------------|
| 表 | 1.0 | 1.0 | 1.0 | 查询中引用的每个表 |
| 连接 | 2.0 | 2.0 | 2.0 | 每个连接操作 |
| WHERE条件 | 1.5 | 1.0 | 1.5 | WHERE子句中的条件 |
| 子查询 | 3.0 | 3.0 | 3.0 | 嵌套的SELECT语句 |
| 聚合函数 | 1.0 | 1.5 | 1.0 | 如COUNT、SUM、AVG等函数 |
| CASE表达式 | 1.0 | 1.5 | 1.5 | CASE...WHEN...THEN语句 |
| 集合操作 | 2.0 | 2.0 | 2.0 | UNION、UNION ALL、INTERSECT、MINUS操作 |
| GROUP BY | 1.5 | 1.5 | 1.5 | GROUP BY子句 |
| ORDER BY | 1.0 | 1.0 | 1.0 | ORDER BY子句 |
| LATERAL VIEW | - | - | 2.0 | Hive特有的LATERAL VIEW操作 |
| DISTRIBUTE BY | - | - | 1.5 | Hive特有的DISTRIBUTE BY子句 |
| CLUSTER BY | - | - | 1.5 | Hive特有的CLUSTER BY子句 |
| SORT BY | - | - | 1.0 | Hive特有的SORT BY子句 |
| PARTITION BY | - | - | 1.5 | Hive特有的PARTITION BY子句 |
| 窗口函数 | - | - | 2.5 | 窗口函数（OVER子句） |
| 查询深度 | - | - | - | 嵌套查询的深度（计算为1 + 子查询数量） |

对于SELECT语句，总体分数计算如下：
```
overallScore = (tableCount * TABLE_WEIGHT) +
               (joinCount * JOIN_WEIGHT) +
               (whereConditionCount * WHERE_CONDITION_WEIGHT) +
               (subqueryCount * SUBQUERY_WEIGHT) +
               (aggregateFunctionCount * AGGREGATE_FUNCTION_WEIGHT) +
               (caseExpressionCount * CASE_EXPRESSION_WEIGHT) +
               (setOperationCount * SET_OPERATION_WEIGHT) +
               (groupByCount * GROUP_BY_WEIGHT) +
               (orderByCount * ORDER_BY_WEIGHT)
```

对于非SELECT语句（INSERT、UPDATE、DELETE、MERGE），使用简化的计算方法，带有特定类型的乘数：
- INSERT: 1.0
- UPDATE: 1.2
- DELETE: 1.1
- MERGE: 1.5

对于动态SQL，复杂度基于语句长度和表数量进行估算：
```
baseScore = Math.log10(length) * 5
overallScore = baseScore * (1 + 0.1 * tableCount)
```

### 存储过程复杂度

对于存储过程，复杂度计算包括额外的因素：

| 因素 | 权重 (Oracle) | 权重 (Gauss) | 权重 (Hive) | 描述 |
|--------|----------------|----------------|----------------|-------------|
| 循环 | 2.5 | 2.0 | 2.5 | FOR、WHILE、LOOP语句 |
| 嵌套循环 | 1.5 | 3.0 | 1.5 | 每个嵌套级别的额外权重 |
| 自定义函数 | 2.0 | 1.5 | 2.0 | 用户定义函数的调用 |
| 高权重表 | 2.0 | 2.0 | 2.0 | 对标记为高权重的表的引用 |
| 嵌套过程 | 3.0 | 3.0 | 3.0 | 对其他存储过程的调用 |
| 高权重过程 | 2.5 | 2.5 | 2.5 | 对标记为高权重的过程的调用 |
| 游标 | 2.0 | 2.0 | 2.0 | 游标声明和操作 |

存储过程的总体分数结合了：
1. 过程中所有SQL语句的复杂度分数总和
2. 循环和嵌套循环的额外复杂度
3. 自定义函数调用的额外复杂度
4. 高权重表引用的额外复杂度
5. 嵌套过程调用的额外复杂度
6. 高权重过程调用的额外复杂度

此外，存储过程评估结果还包含一个DML语句（INSERT、UPDATE、DELETE、MERGE）数组，每个DML语句都包含其完整的复杂度指标，与SQL语句评估结果格式一致。

```
// 来自SQL语句的基础分数
double totalScore = 所有语句分数的总和

// 循环复杂度
double loopComplexity = loopCount * LOOP_WEIGHT
if (maxLoopNestingLevel > 1) {
    loopComplexity *= (1 + (maxLoopNestingLevel - 1) * NESTED_LOOP_WEIGHT)
}

// 自定义函数复杂度
double customFunctionComplexity = customFunctionCount * CUSTOM_FUNCTION_WEIGHT

// 初始总体分数
double overallScore = totalScore * (1 + 0.1 * statements.size()) + loopComplexity + customFunctionComplexity

// 添加高权重表复杂度
if (highWeightTableCount > 0) {
    overallScore *= (1 + (highWeightTableCount * 0.1 * HIGH_WEIGHT_TABLE_MULTIPLIER))
}

// 添加嵌套过程复杂度
overallScore += (nestedProcedureCount * NESTED_PROCEDURE_WEIGHT)

// 添加高权重过程复杂度
overallScore += (highWeightProcedureCount * HIGH_WEIGHT_PROCEDURE_MULTIPLIER)
```

### 自定义因素

系统支持用户提供的几个自定义因素：

1. **自定义函数**：在评估复杂度时应计算的用户定义函数列表
2. **高权重表**：引用时应给予额外权重的表列表
3. **高权重过程**：调用时应给予额外权重的存储过程列表

这些自定义因素允许根据特定的数据库环境和应用需求进行更定制化的复杂度分析。

## 功能特性

- 分析单个SQL语句
- 分析整个存储过程和包体
- 支持Oracle、Gauss和Hive SQL方言（可扩展到其他方言）
- 用于与其他系统集成的RESTful API
- 详细的复杂度指标
- 支持自定义函数列表以跟踪特定函数使用情况
- 支持高权重表列表以识别关键表访问
- 支持高权重过程列表以跟踪重要过程调用
- 通过ZIP上传批量处理SQL文件
- 支持JSON和Excel格式的结果导出（Excel格式包含高权重存储过程计数和列表）
- 存储过程评估结果包含DML语句（INSERT、UPDATE、DELETE、MERGE）数组，每个DML语句都包含完整的复杂度指标
- 增强的异常处理：当语句级别处理遇到异常时，系统会记录失败的语句并继续处理其他语句，而不是中断整个评估过程
- 评估结果包含失败语句列表，便于后续分析和修复

## 技术栈

- Java 17
- Spring Boot 3.4.4
- JSqlParser用于SQL解析
- Apache POI用于Excel导出
- JUnit 5用于测试

## API端点

### 评估SQL语句

```
POST /api/complexity/sql
```

请求体：
```json
{
  "sql": "SELECT * FROM employees WHERE department_id = 10",
  "dialect": "Oracle" // 支持的方言: "Oracle", "Gauss", "Hive"
}
```

响应：
```json
{
  "overallScore": 3.5,
  "tableCount": 1,
  "joinCount": 0,
  "whereConditionCount": 1,
  "subqueryCount": 0,
  "aggregateFunctionCount": 0,
  "caseExpressionCount": 0,
  "setOperationCount": 0,
  "queryDepth": 1,
  "lineCount": 1,
  "tableList": ["employees"]
}
```

### 评估存储过程

```
POST /api/complexity/stored-procedure
```

请求体：
```json
{
  "sourceCode": "CREATE OR REPLACE PROCEDURE get_employee_details(...) AS BEGIN ... END;",
  "name": "get_employee_details",
  "schema": "HR",
  "dialect": "Oracle", // 支持的方言: "Oracle", "Gauss", "Hive"
  "customFunctions": ["calculate_salary", "format_date"],
  "highWeightTables": ["employees", "departments", "salary_history"],
  "highWeightProcedures": ["update_employee", "process_payroll"]
}
```

响应：
```json
{
  "overallScore": 15.8,
  "tableCount": 3,
  "joinCount": 2,
  "whereConditionCount": 3,
  "subqueryCount": 1,
  "aggregateFunctionCount": 2,
  "caseExpressionCount": 1,
  "setOperationCount": 0,
  "queryDepth": 2,
  "loopCount": 2,
  "maxLoopNestingLevel": 1,
  "customFunctionCount": 3,
  "customFunctionList": ["calculate_salary", "format_date", "get_tax_rate"],
  "highWeightTableCount": 2,
  "highWeightTableList": ["employees", "salary_history"],
  "nestedProcedureCount": 2,
  "nestedProcedureList": ["get_department_info", "update_employee"],
  "highWeightProcedureCount": 1,
  "highWeightProcedureList": ["update_employee"],
  "procedureName": "get_employee_details",
  "lineCount": 25,
  "tableList": ["employees", "departments", "salary_history"],
  "hasExceptions": true,
  "failedStatements": ["SELECT * FROM invalid_table", "UPDATE employees SET invalid_column = 100"],
  "dmlStatements": [
    {
      "type": "UPDATE",
      "sql": "UPDATE employees SET salary = 5000 WHERE employee_id = 100;",
      "dialect": "Oracle",
      "overallScore": 8.6,
      "tableCount": 1,
      "tableList": ["employees"],
      "joinCount": 0,
      "whereConditionCount": 1,
      "subqueryCount": 0,
      "aggregateFunctionCount": 0,
      "caseExpressionCount": 0,
      "setOperationCount": 0,
      "groupByCount": 0,
      "orderByCount": 0,
      "queryDepth": 0,
      "loopCount": 0,
      "maxLoopNestingLevel": 0,
      "customFunctionCount": 0,
      "highWeightTableCount": 1,
      "highWeightTableList": ["employees"],
      "nestedProcedureCount": 0,
      "highWeightProcedureCount": 0,
      "cursorCount": 0,
      "cursorOperationCount": 0,
      "maxCursorNestingLevel": 0,
      "lineCount": 1
    },
    {
      "type": "DELETE",
      "sql": "DELETE FROM departments WHERE department_id = 10;",
      "dialect": "Oracle",
      "overallScore": 8.2,
      "tableCount": 1,
      "tableList": ["departments"],
      "joinCount": 0,
      "whereConditionCount": 1,
      "subqueryCount": 0,
      "aggregateFunctionCount": 0,
      "caseExpressionCount": 0,
      "setOperationCount": 0,
      "groupByCount": 0,
      "orderByCount": 0,
      "queryDepth": 0,
      "loopCount": 0,
      "maxLoopNestingLevel": 0,
      "customFunctionCount": 0,
      "highWeightTableCount": 0,
      "highWeightTableList": [],
      "nestedProcedureCount": 0,
      "highWeightProcedureCount": 0,
      "cursorCount": 0,
      "cursorOperationCount": 0,
      "maxCursorNestingLevel": 0,
      "lineCount": 1
    }
  ]
}
```

### 上传SQL文件

```
POST /api/complexity/sql/upload
```

表单数据：
- `file`: SQL文件
- `dialect`: SQL方言（Oracle、Gauss、Hive）

### 上传存储过程文件

```
POST /api/complexity/stored-procedure/upload
```

表单数据：
- `file`: 包含存储过程的SQL文件
- `name`: 过程名称
- `schema`: 模式名称（可选，默认：HR）
- `dialect`: SQL方言（Oracle、Gauss、Hive）
- `customFunctionsFile`: 包含自定义函数名称的文件（可选）
- `highWeightTablesFile`: 包含高权重表名称的文件（可选）
- `highWeightProceduresFile`: 包含高权重过程名称的文件（可选）

### 上传包体文件

```
POST /api/complexity/package-body/upload
```

表单数据：
- `file`: 包含包体的SQL文件
- `name`: 包名称
- `schema`: 模式名称（可选，默认：HR）
- `dialect`: SQL方言（Oracle、Gauss、Hive）
- `customFunctionsFile`: 包含自定义函数名称的文件（可选）
- `highWeightTablesFile`: 包含高权重表名称的文件（可选）
- `highWeightProceduresFile`: 包含高权重过程名称的文件（可选）

### 批量上传（ZIP）

```
POST /api/complexity/batch/upload
```

表单数据：
- `file`: 包含SQL文件的ZIP文件
- `dialect`: SQL方言（Oracle、Gauss、Hive）
- `customFunctionsFile`: 包含自定义函数名称的文件（可选）
- `highWeightTablesFile`: 包含高权重表名称的文件（可选）
- `highWeightProceduresFile`: 包含高权重过程名称的文件（可选）
- `responseFormat`: 响应格式（json、excel）- 默认：json

#### Excel导出列说明

当选择Excel格式（`responseFormat=excel`）时，导出的Excel文件包含以下列：

| 列名 | 描述 |
|------|------|
| Package Name | 包名称（从文件名提取） |
| Procedure Name | 存储过程名称 |
| Line Count | 源代码行数 |
| Overall Score | 总体复杂度分数 |
| Loop Count | 循环数量 |
| Max Loop Nesting Level | 最大循环嵌套级别 |
| Custom Function Count | 自定义函数调用数量 |
| Custom Function List | 自定义函数列表（逗号分隔） |
| High Weight Table Count | 高权重表引用数量 |
| High Weight Table List | 高权重表列表（逗号分隔） |
| High Weight Procedure Count | 高权重存储过程调用数量 |
| High Weight Procedure List | 高权重存储过程列表（逗号分隔） |
| Subquery Count | 子查询数量 |
| Table Count | 涉及的表数量 |
| Table List | 涉及的表列表（逗号分隔） |
| Has Exceptions | 是否在处理过程中遇到异常 |
| Failed Statements | 解析或评估失败的SQL语句列表（逗号分隔） |

## 构建和运行

### 前提条件

- Java 17或更高版本
- Maven 3.6或更高版本

### 构建

```bash
./mvnw clean package
```

### 运行

```bash
./mvnw spring-boot:run
```

服务将在http://localhost:8080上可用

## 测试

```bash
./mvnw test
```

### 命令行测试

项目包含一个用于从命令行评估SQL文件的测试脚本：

```bash
./script/test_from_file.sh -s path/to/sql_file.sql -d Oracle
./script/test_from_file.sh -p path/to/procedure.sql -n procedure_name -d Oracle
./script/test_from_file.sh -p path/to/hive_script.sql -n script_name -d Hive
./script/test_from_file.sh -z path/to/sql_files.zip -d Oracle
```

对于ZIP文件的Excel输出：

```bash
./script/test_from_file.sh -z path/to/sql_files.zip -e -d Oracle
```

Excel输出将包含上述Excel导出列说明中列出的所有列，包括高权重存储过程计数和列表。

运行`./script/test_from_file.sh --help`获取更多选项。

## 扩展到其他数据库方言

要添加对新数据库方言的支持：

1. 为该方言创建一个新的`SqlParser`实现
2. 为该方言创建一个新的`StoredProcedureParser`实现
3. 为该方言创建一个新的`ComplexityEvaluator`实现
4. 在`ComplexityEvaluationServiceImpl`中注册新的实现

## 已知问题

以下是系统当前已知的一些限制和潜在问题：

### 解析和评估限制

1. **复杂SQL解析**：对于极其复杂的SQL语句（如包含多层嵌套子查询、复杂CTE或非标准语法），JSqlParser可能无法正确解析，导致评估失败或不准确。

2. **方言特定语法**：虽然系统支持Oracle、Gauss和Hive方言，但每种方言都有特定的扩展语法，可能未被完全支持。特别是较新版本的数据库引入的语法特性可能无法正确识别。

3. **动态SQL评估**：动态SQL（如使用字符串拼接或EXECUTE IMMEDIATE构建的SQL）的复杂度评估基于估算，可能与实际执行的SQL复杂度有显著差异。

4. **存储过程嵌套深度**：系统可能无法准确跟踪深度嵌套的存储过程调用，特别是当过程通过动态SQL调用其他过程时。

5. **类型定义识别**：在高权重表识别中，类似`v_all_acnt_info_base.acnt_id%TYPE`的类型定义可能被错误地识别为表引用。

### 文件处理问题

1. **大文件处理**：处理非常大的SQL文件或包含大量存储过程的ZIP文件时，可能会遇到内存限制或性能下降。

2. **字符编码**：虽然系统尝试使用多种编码（UTF-8、ISO-8859-1、GBK、GB2312）读取文件，但对于使用其他编码的文件可能会出现乱码或解析失败。

3. **临时文件清理**：在处理ZIP文件时，如果应用程序异常终止，可能无法正确清理临时文件。

4. **ZIP文件结构**：系统假设ZIP文件中的SQL文件直接位于根目录或子目录中，对于复杂的嵌套目录结构可能无法正确处理。

### 并发和性能问题

1. **ThreadLocal使用**：系统使用ThreadLocal存储失败的SQL语句，在高并发环境下可能导致内存泄漏，特别是在使用线程池的情况下。

2. **并发请求处理**：在处理多个并发的大型文件上传请求时，系统可能会遇到资源竞争和性能瓶颈。

3. **长时间运行的评估**：对于非常复杂的存储过程或大量SQL文件的批量评估，处理时间可能很长，可能导致HTTP请求超时。

### 用户界面和API限制

1. **错误反馈**：API错误响应可能不够详细，难以诊断具体问题。当评估失败时，仅返回HTTP 400状态码，没有详细的错误信息。

2. **Excel导出限制**：Excel导出可能无法处理包含大量数据的结果，特别是当列表字段（如tableList）包含大量项目时。

3. **Web界面功能**：Web界面提供基本功能，但缺乏高级特性，如结果过滤、排序或可视化图表。

4. **批量处理进度反馈**：在处理大型ZIP文件时，缺乏实时进度反馈，用户无法知道处理的当前状态。

### 安全考虑

1. **输入验证**：对上传文件的内容验证有限，可能允许上传恶意SQL代码或非SQL文件。

2. **资源限制**：缺乏对上传文件大小、处理时间和内存使用的严格限制，可能导致资源耗尽攻击。

3. **临时文件安全**：处理ZIP文件时创建的临时文件可能存在权限问题，特别是在多用户环境中。

## 未来优化计划 (TODO List)

以下是系统未来可能的优化和改进方向：

### 功能增强

1. **支持更多数据库方言**
   - 添加对PostgreSQL、MySQL、SQL Server和DB2等主流数据库的支持
   - 为每种方言实现特定的语法解析和复杂度评估规则

2. **增强SQL解析能力**
   - 升级或替换JSqlParser，以支持更复杂的SQL语法和最新的SQL标准
   - 添加对数据库特定扩展语法的支持，如Oracle的分析函数、PostgreSQL的JSON操作等
   - 改进对CTE（公共表表达式）和递归查询的复杂度评估

3. **扩展复杂度评估指标**
   - 添加执行计划复杂度评估，考虑索引使用、表扫描和排序操作等因素
   - 引入数据量因子，根据表大小调整复杂度评分
   - 添加对存储过程中异常处理复杂度的评估
   - 实现对触发器和视图的复杂度评估

4. **自定义评估规则**
   - 允许用户定义自己的复杂度评估规则和权重
   - 提供基于行业最佳实践的预设评估配置
   - 支持按项目或团队保存和共享评估配置

### 性能优化

1. **并行处理**
   - 实现多线程批量处理，加速大型ZIP文件的评估
   - 使用响应式编程模型（如Spring WebFlux）处理大量并发请求
   - 优化内存使用，减少大文件处理时的内存占用

2. **缓存机制**
   - 实现SQL解析结果缓存，避免重复解析相同的SQL语句
   - 添加结果缓存，加速对相同文件的重复评估
   - 使用分布式缓存支持集群部署

3. **异步处理**
   - 实现长时间运行任务的异步处理和结果通知机制
   - 添加任务队列和后台处理，避免HTTP请求超时
   - 提供WebSocket或Server-Sent Events接口，实时反馈处理进度

### 用户体验改进

1. **增强Web界面**
   - 重新设计用户界面，提供更直观的操作体验
   - 添加复杂度评估结果的可视化图表和热力图
   - 实现结果比较功能，对比不同版本的SQL或存储过程复杂度变化
   - 添加批处理进度指示器和实时反馈

2. **报告生成**
   - 支持生成详细的PDF报告，包含复杂度分析和优化建议
   - 添加历史趋势分析，跟踪代码复杂度随时间的变化
   - 实现自定义报告模板，满足不同团队的需求

3. **集成增强**
   - 提供命令行工具，便于集成到CI/CD流程
   - 开发IDE插件（如VS Code、IntelliJ IDEA），实现编辑器内复杂度评估
   - 添加与版本控制系统（如Git）的集成，自动评估提交的SQL变更

### 安全和稳定性

1. **安全增强**
   - 实现严格的输入验证和文件类型检查
   - 添加文件大小和处理时间限制，防止资源耗尽
   - 改进临时文件处理，确保安全删除和适当的访问权限
   - 实现API访问控制和认证机制

2. **错误处理和恢复**
   - 改进异常处理机制，提供更详细的错误信息
   - 实现处理中断后的恢复机制，避免重新处理整个文件
   - 添加系统健康监控和自动恢复功能

3. **测试和质量保证**
   - 增加单元测试和集成测试覆盖率
   - 添加性能基准测试，确保优化不会导致性能下降
   - 实现自动化测试流程，验证对各种SQL方言和复杂度级别的支持

### 部署和运维

1. **容器化和云原生支持**
   - 提供Docker容器和Kubernetes部署配置
   - 实现云原生特性，如自动扩展和健康检查
   - 支持作为无服务器函数部署（如AWS Lambda或Azure Functions）

2. **监控和日志**
   - 集成应用监控工具（如Prometheus和Grafana）
   - 改进日志记录，便于问题诊断和性能分析
   - 添加用户操作审计日志

3. **多租户支持**
   - 实现多租户架构，支持SaaS部署模式
   - 添加资源隔离和配额管理
   - 支持按租户的自定义配置和规则

## 许可证

本项目采用MIT许可证 - 详情请参阅LICENSE文件。
