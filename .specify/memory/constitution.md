<!--
Sync Impact Report:
- Version change: None → 1.0.0 (new constitution)
- Modified principles: None (new constitution)
- Added sections: All sections (new constitution)
- Removed sections: None (new constitution)
- Templates requiring updates:
  ✅ plan-template.md (Constitution Check section)
  ✅ spec-template.md (requirements section)
  ✅ tasks-template.md (task organization)
  ⚠ N/A (all templates appear compatible)
- Follow-up TODOs: None
-->

# SQL复杂度评估器 Constitution

## Core Principles

### I. API-First Design
所有功能必须通过RESTful API暴露。API是系统的唯一接口，确保清晰的数据契约和版本兼容性。Web界面和命令行工具都必须使用相同的API端点。

### II. 多方言SQL解析器
系统必须支持Oracle、Gauss和Hive数据库方言。每种方言需要独立的解析器实现，共享统一的复杂度评估框架。扩展新方言时不能影响现有方言功能。

### III. 准确性优先 (NON-NEGOTIABLE)
复杂度评估结果必须准确可靠。优先确保解析正确性，其次考虑性能。对于无法解析的SQL，系统必须记录错误并提供清晰的反馈，而不是返回错误的结果。

### IV. 容错性设计
系统必须能够处理部分解析失败。单个SQL语句或存储过程的错误不能中断整个批处理过程。所有失败的语句必须被记录并在结果中返回，便于用户分析和修复。

### V. 测试驱动开发
所有新功能必须先编写测试。测试覆盖率是代码质量的重要指标。每个数据库方言的SQL解析器都必须有对应的测试用例，包括边界情况和错误场景。

## 技术约束

### 性能要求
- 单个SQL语句评估响应时间 < 100ms
- 存储过程评估响应时间 < 1s
- ZIP文件批处理支持1000个SQL文件
- 系统必须支持并发处理多个评估请求

### 兼容性要求
- 支持Java 17+
- 必须向后兼容API v1
- 支持UTF-8和GBK文件编码
- Web界面必须支持现代浏览器

### 数据处理
- 支持JSON和Excel格式输出
- 临时文件必须自动清理
- 必须防止内存泄漏，特别是ThreadLocal使用
- 文件上传大小限制100MB

## 开发流程

### 代码质量
- 所有代码必须通过SonarQube质量门
- 代码覆盖率 > 80%
- 必须遵循Google Java Style Guide
- 所有公共API必须有JavaDoc

### 测试策略
- 单元测试：每个类和方法的独立测试
- 集成测试：API端点的完整流程测试
- 合约测试：确保API契约不被破坏
- 性能测试：关键路径的性能验证

### 发布管理
- 使用语义版本控制 (MAJOR.MINOR.PATCH)
- 每个发布必须包含完整的变更日志
- 数据库方言变更必须增加MINOR版本
- API不兼容变更必须增加MAJOR版本

## Governance

本宪法是项目开发的最高指导原则，所有其他实践和规范都必须遵守。

**修订流程**:
1. 修订提案必须详细说明变更理由和影响
2. 需要项目维护者团队过半数同意
3. 所有修订必须更新版本号
4. 修订后的宪法必须与所有模板文件保持一致

**合规检查**:
- 每个PR必须验证是否符合宪法原则
- 复杂度评估的核心算法变更需要双重审查
- 性能相关的变更必须包含基准测试结果
- 数据库方言支持变更需要提供完整的测试用例

**运行指导**: 使用`docs/development.md`文件指导具体的开发实践和编码标准。

**Version**: 1.0.0 | **Ratified**: 2024-11-28 | **Last Amended**: 2024-11-28