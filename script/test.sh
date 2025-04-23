#!/bin/bash

# 设置服务器地址和端口
SERVER="http://localhost:8080"
DEFAULT_DIALECT="Oracle"

# 函数：从文件读取SQL内容并评估复杂度
evaluate_sql_from_file() {
    local file_path=$1
    local dialect=${2:-$DEFAULT_DIALECT}
    
    if [ ! -f "$file_path" ]; then
        echo "错误: 文件 '$file_path' 不存在"
        return 1
    fi
    
    # 读取SQL文件内容
    local sql_content=$(cat "$file_path")
    
    # 计算行数
    local line_count=$(echo "$sql_content" | wc -l)
    
    # 转义JSON特殊字符
    sql_content=$(echo "$sql_content" | tr '\n' ' ' | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')
    
    echo -e "\n===== 评估SQL语句复杂度: $(basename "$file_path") (行数: $line_count) ====="
    curl -s -X POST "$SERVER/api/complexity/sql" \
      -H "Content-Type: application/json" \
      -d "{\"sql\": \"$sql_content\", \"dialect\": \"$dialect\"}" | jq .
}

# 函数：从文件读取存储过程内容并评估复杂度
evaluate_procedure_from_file() {
    local file_path=$1
    local name=$2
    local schema=${3:-"HR"}
    local dialect=${4:-$DEFAULT_DIALECT}
    
    if [ ! -f "$file_path" ]; then
        echo "错误: 文件 '$file_path' 不存在"
        return 1
    fi
    
    # 如果未提供名称，则从文件名获取
    if [ -z "$name" ]; then
        name=$(basename "$file_path" .sql)
    fi
    
    # 读取存储过程文件内容
    local procedure_content=$(cat "$file_path")
    
    # 计算行数
    local line_count=$(echo "$procedure_content" | wc -l)
    
    # 转义JSON特殊字符
    procedure_content=$(echo "$procedure_content" | tr '\n' ' ' | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')
    
    echo -e "\n===== 评估存储过程复杂度: $name (Schema: $schema, 行数: $line_count) ====="
    curl -s -X POST "$SERVER/api/complexity/stored-procedure" \
      -H "Content-Type: application/json" \
      -d "{\"sourceCode\": \"$procedure_content\", \"name\": \"$name\", \"schema\": \"$schema\", \"dialect\": \"$dialect\"}" | jq .
}

# 检查是否安装了jq
if ! command -v jq &> /dev/null; then
    echo "警告: 未安装jq工具，输出将不会格式化。建议安装jq以获得更好的输出格式。"
    # 定义一个空的jq函数，以便脚本可以继续运行
    jq() {
        cat
    }
fi

# 测试简单SQL语句复杂度评估
echo -e "===== 测试简单SQL语句复杂度评估 ====="
curl -s -X POST "$SERVER/api/complexity/sql" \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM employees WHERE department_id = 10", "dialect": "Oracle"}' | jq .

# 从文件读取SQL语句并评估复杂度
evaluate_sql_from_file "sql_samples/complex_query.sql"

# 测试简单存储过程复杂度评估
echo -e "\n\n===== 测试简单存储过程复杂度评估 ====="
curl -s -X POST "$SERVER/api/complexity/stored-procedure" \
  -H "Content-Type: application/json" \
  -d '{"sourceCode": "CREATE OR REPLACE PROCEDURE get_emp AS BEGIN SELECT * FROM employees; END;", "name": "get_emp", "schema": "HR", "dialect": "Oracle"}' | jq .

# 从文件读取存储过程并评估复杂度
evaluate_procedure_from_file "sql_samples/update_employee_salary.sql" "update_employee_salary" "HR"

# 从文件读取复杂存储过程并评估复杂度
evaluate_procedure_from_file "sql_samples/analyze_sales_performance.sql" "analyze_sales_performance" "SALES"

echo -e "\n"
