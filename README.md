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

| 因素 | 权重 (Oracle) | 权重 (Gauss) | 描述 |
|--------|----------------|----------------|-------------|
| 表 | 1.0 | 1.0 | 查询中引用的每个表 |
| 连接 | 2.0 | 2.0 | 每个连接操作 |
| WHERE条件 | 1.5 | 1.0 | WHERE子句中的条件 |
| 子查询 | 3.0 | 3.0 | 嵌套的SELECT语句 |
| 聚合函数 | 1.0 | 1.5 | 如COUNT、SUM、AVG等函数 |
| CASE表达式 | 1.0 | 1.5 | CASE...WHEN...THEN语句 |
| 集合操作 | 2.0 | 2.0 | UNION、UNION ALL、INTERSECT、MINUS操作 |
| GROUP BY | 1.5 | 1.5 | GROUP BY子句 |
| ORDER BY | 1.0 | 1.0 | ORDER BY子句 |
| 查询深度 | - | - | 嵌套查询的深度（计算为1 + 子查询数量） |

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

| 因素 | 权重 (Oracle) | 权重 (Gauss) | 描述 |
|--------|----------------|----------------|-------------|
| 循环 | 2.5 | 2.0 | FOR、WHILE、LOOP语句 |
| 嵌套循环 | 1.5 | 3.0 | 每个嵌套级别的额外权重 |
| 自定义函数 | 2.0 | 1.5 | 用户定义函数的调用 |
| 高权重表 | 2.0 | 2.0 | 对标记为高权重的表的引用 |
| 嵌套过程 | 3.0 | 3.0 | 对其他存储过程的调用 |
| 高权重过程 | 2.5 | 2.5 | 对标记为高权重的过程的调用 |

存储过程的总体分数结合了：
1. 过程中所有SQL语句的复杂度分数总和
2. 循环和嵌套循环的额外复杂度
3. 自定义函数调用的额外复杂度
4. 高权重表引用的额外复杂度
5. 嵌套过程调用的额外复杂度
6. 高权重过程调用的额外复杂度

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
- 支持Oracle和Gauss SQL方言（可扩展到其他方言）
- 用于与其他系统集成的RESTful API
- 详细的复杂度指标
- 支持自定义函数列表以跟踪特定函数使用情况
- 支持高权重表列表以识别关键表访问
- 支持高权重过程列表以跟踪重要过程调用
- 通过ZIP上传批量处理SQL文件
- 支持JSON和Excel格式的结果导出

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
  "dialect": "Oracle"
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
  "dialect": "Oracle",
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
  "tableList": ["employees", "departments", "salary_history"]
}
```

### 上传SQL文件

```
POST /api/complexity/sql/upload
```

表单数据：
- `file`: SQL文件
- `dialect`: SQL方言（Oracle、Gauss）

### 上传存储过程文件

```
POST /api/complexity/stored-procedure/upload
```

表单数据：
- `file`: 包含存储过程的SQL文件
- `name`: 过程名称
- `schema`: 模式名称（可选，默认：HR）
- `dialect`: SQL方言（Oracle、Gauss）
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
- `dialect`: SQL方言（Oracle、Gauss）
- `customFunctionsFile`: 包含自定义函数名称的文件（可选）
- `highWeightTablesFile`: 包含高权重表名称的文件（可选）
- `highWeightProceduresFile`: 包含高权重过程名称的文件（可选）

### 批量上传（ZIP）

```
POST /api/complexity/batch/upload
```

表单数据：
- `file`: 包含SQL文件的ZIP文件
- `dialect`: SQL方言（Oracle、Gauss）
- `customFunctionsFile`: 包含自定义函数名称的文件（可选）
- `highWeightTablesFile`: 包含高权重表名称的文件（可选）
- `highWeightProceduresFile`: 包含高权重过程名称的文件（可选）
- `responseFormat`: 响应格式（json、excel）- 默认：json

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
./script/test_from_file.sh -z path/to/sql_files.zip -d Oracle
```

对于ZIP文件的Excel输出：

```bash
./script/test_from_file.sh -z path/to/sql_files.zip -e -d Oracle
```

运行`./script/test_from_file.sh --help`获取更多选项。

## 扩展到其他数据库方言

要添加对新数据库方言的支持：

1. 为该方言创建一个新的`SqlParser`实现
2. 为该方言创建一个新的`StoredProcedureParser`实现
3. 为该方言创建一个新的`ComplexityEvaluator`实现
4. 在`ComplexityEvaluationServiceImpl`中注册新的实现

## 许可证

本项目采用MIT许可证 - 详情请参阅LICENSE文件。
