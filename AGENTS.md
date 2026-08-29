# AGENTS.md — SP-Complexity-Evaluator

SQL/存储过程复杂度评估 REST 服务，支持 Oracle/Gauss/Hive 方言。通过解析 AST 提取表、连接、子查询、循环嵌套等指标，按方言权重计算复杂度分数，支持批量 ZIP 处理与 Excel 导出。

## 先读再改

1. **环境与构建**：Java 17 + Spring Boot 3.2.6 + Maven。只用根目录的 `./mvnw` 执行所有命令，不要使用全局 `mvn`。依赖只以 `pom.xml` 为准（`pom.old` / `pom.xml.bak` 是残留文件，忽略）。
2. **代码结构**：
   - `src/main/java/com/sdchat/ce/sp/complexity/`
     - `controller/`: REST 接口（ComplexityEvaluationController）
     - `service/`: 业务逻辑编排（ComplexityEvaluationService）
     - `parser/`: SQL/存储过程解析器（SqlParser, StoredProcedureParser 及其方言实现）
     - `evaluator/`: 复杂度计算引擎（ComplexityEvaluator 及其方言实现）
     - `model/`: 数据模型（Request/Response DTOs）
   - `src/test/java/com/sdchat/ce/sp/complexity/`: 测试包路径与主代码一一对应。
3. **测试先行**：修改逻辑前必须先跑相关测试。完成 TDD 循环后按「完成标准与汇报」汇报。

---

## TDD 工作流（Red → Green → Refactor）

本仓库是 Java + Spring Boot 服务，测试框架是 JUnit 5 + Mockito + AssertJ（`spring-boot-starter-test`）。一次循环只锁定一个行为：先写会失败的测试（Red），再写最小实现让它通过（Green），最后在测试全绿的前提下重构（Refactor）。探索草稿不得直接合入，必须按本文件用 TDD 重写。

### 先读再改
1. 确认改动落在哪个包（`controller` / `service` / `parser` / `evaluator`）。
2. 只用 `./mvnw`（不要发明全局 `mvn` 或手动改依赖版本）。
3. 先跑与改动相关的最小测试；提交前再跑 `./mvnw clean package`。
4. 完成一个循环后按「完成标准与汇报」汇报，不要只说「做完了」。

### Never / Ask first / Always

**Never（不必请示，直接禁止）**
- 删除、注释、跳过已有测试：`@Disabled`、`@Ignore`（无 ticket）、注释掉 `@Test`、断言改成 `assertTrue(true)`/空测试
- 修改人类已有测试的断言来迁就实现
- 先提交无测试的业务行为，再「回头补」
- 写永真测试：无断言、只检查 `isNotNull()`、只 verify 调用次数不查参数与状态
- 用全量端到端测试覆盖本可单测完成的改动
- 提交半成品；把探索草稿、临时脚本、调试 `System.out.println` 留在主代码

**Ask first**
- 改人类已有测试（含断言、fixture）
- 新增运行时依赖、新的外部服务
- 为不可测代码做超出当前改动路径的重构
- 关闭/放宽任何静态检查

**Always**
- 改遗留路径前：先写特征测试，锁定当前可观察行为
- 新行为：先有会失败的行为断言，再写最少实现
- 难以测试时：先造接缝，再写测试（见「遗留代码与接缝」）
- 新增测试名描述行为。**注意本仓库现有测试用的是 `方法名_场景` 风格**（`evaluateStoredProcedure_WithNestedCalls`、`evaluateSqlStatement_Delete` …），没有一个以 `should` 开头。新测试**沿用现有风格**，不要引入第二套命名，更不要为了统一命名去重命名人类已有测试（那属于「只读」）。
- 现有测试因你的改动失败：修实现，不修测试（除非人类明确要求）

测试权限：

| 测试来源 | 权限 |
|---|---|
| 人类已有测试 | 只读 |
| 本任务新建测试 | 可改，直到该行为稳定 |
| 过时或环境偶发失败 | 只报告，不擅自跳过 |

### 工作流

**Red** — 写生产行为之前先写测试；测试必须能被收集且必须失败（断言失败，或缺失类/方法导致编译失败，二者都算合法 Red）。修改已有功能先写特征测试。一次只加一个行为的测试。

**Green** — 只写让当前失败测试通过的最少代码。禁止删掉/改掉失败测试、一次引入多个未验证变更、用更宽断言/吞异常/`verifyNoInteractions` 滥用换绿。

**Refactor** — 相关测试全绿后才重构；重构后立刻跑同一组测试；范围限于当前改动路径。

**探索 vs 实现** — 需求或方案不清可写草稿验证；草稿不得合并；方案确定后必须走 TDD 重写。

### 遗留代码与接缝

**特征测试** — 锁定现有行为，不是证明它正确。固定输入 SQL/存储过程 fixture + 输出比对。

**接缝（优先顺序，靠后的更差）**
1. 构造器注入（Spring 推荐），测试用 Mockito `@Mock` + `@InjectMocks`
2. 接口 + 假实现
3. 静态方法/单例改成可注入依赖
4. 最后才 `Mockito.mockStatic`，且只打在进程边界（外部服务、时钟、文件系统），禁止 mock 被测对象内部实现

只给即将修改的代码路径补测试，不要一次性「补全覆盖率」。

### 测试分层

| 层级 | 位置 | 测什么 |
|---|---|---|
| 单元（evaluator/parser） | `src/test/java/.../evaluator/`、`.../parser/` | 复杂度指标计算、方言差异、解析正确性 |
| 服务层 | `ComplexityEvaluationServiceTest` | 编排逻辑（Mockito mock 下层） |
| 控制器 | `ComplexityEvaluationControllerTest` | HTTP 契约（MockMvc / WebTestClient） |
| 上下文 | `ApplicationTests` | Spring 上下文能否加载 |

- evaluator/parser 的纯逻辑优先用真实对象 + 固定 fixture 测，不要用全量 mock 冒充契约。
- 不要为每个方言复制同样的测试逻辑——抽象出共享 fixture，按方言参数化。
- `script/test_from_file.sh` 是 curl 手动冒烟脚本，不是自动化测试，不要依赖它当门禁。

### Java Never 补遗
- 生产代码用 `printStackTrace()` 或空 `catch (Exception e) {}`
- 测试里改静态/全局状态却不还原
- 用 `Thread.sleep` 等待异步
- Lombok 仅用于减少样板（`@Data`/`@Slf4j` 等），不要用它隐藏真正该写的构造器/equals 逻辑

### 命令

```bash
# 单测（按测试类/方法过滤，方法名必须是真实存在的）
./mvnw test -Dtest=GaussComplexityEvaluatorTest
./mvnw test -Dtest=GaussComplexityEvaluatorTest#evaluateStoredProcedure_WithNestedCalls

# 全量测试
./mvnw test

# 提交前门禁（clean package 含编译 + 测试 + 打包）
./mvnw clean package
```

循环内只跑相关测试类；提交前再 `./mvnw clean package`。

> ⚠️ **本仓库没有任何 CI**（`.github/workflows/` 不存在）。以上门禁完全没有自动化兜底，必须本地跑完并在汇报里贴出实际命令与结果。
>
> 根目录残留 `pom.old` 与 `pom.xml.bak`，它们**不是**构建输入——只认 `pom.xml`，不要读它们、也不要照它们改依赖。

### 完成标准与汇报

提交或交还人类前，确认：
- [ ] 新行为有失败→通过的测试
- [ ] 修改的遗留路径有特征测试
- [ ] 未删除、跳过、改写人类已有测试
- [ ] 已跑 `./mvnw test`（或 `clean package`）
- [ ] 没有把探索草稿、调试输出、`*.class`/`target/` 变更带上

每个 TDD 循环汇报：1) 测试了什么行为（测试方法名）2) 最小实现改了哪些文件 3) 是否重构、边界 4) 实际命令与结果（通过/失败原因）。

### 质量判断（自我检查）
- 这条测试在实现写错时会失败吗？
- 我是否在测行为，而不是私有实现细节？
- 我是否用 @Disabled、更宽断言、空 catch 换绿？
- 命令是否来自本文件，而不是我编的？
