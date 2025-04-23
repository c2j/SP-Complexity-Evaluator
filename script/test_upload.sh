#!/bin/bash

# 设置服务器地址和端口
SERVER="http://localhost:8080"

# 测试上传SQL文件并评估复杂度
test_upload_sql_file() {
    local file_path=$1
    local dialect=${2:-"Oracle"}
    
    if [ ! -f "$file_path" ]; then
        echo "错误: 文件 '$file_path' 不存在"
        return 1
    fi
    
    echo -e "\n===== 测试上传SQL文件并评估复杂度: $(basename "$file_path") ====="
    curl -s -X POST "$SERVER/api/complexity/sql/upload" \
      -H "Content-Type: multipart/form-data" \
      -F "file=@$file_path" \
      -F "dialect=$dialect" | jq .
}

# 测试上传存储过程文件并评估复杂度
test_upload_stored_procedure_file() {
    local file_path=$1
    local name=$2
    local schema=${3:-"HR"}
    local dialect=${4:-"Oracle"}
    
    if [ ! -f "$file_path" ]; then
        echo "错误: 文件 '$file_path' 不存在"
        return 1
    fi
    
    # 如果未提供名称，则从文件名获取
    if [ -z "$name" ]; then
        name=$(basename "$file_path" .sql)
    fi
    
    echo -e "\n===== 测试上传存储过程文件并评估复杂度: $name (Schema: $schema) ====="
    curl -s -X POST "$SERVER/api/complexity/stored-procedure/upload" \
      -H "Content-Type: multipart/form-data" \
      -F "file=@$file_path" \
      -F "name=$name" \
      -F "schema=$schema" \
      -F "dialect=$dialect" | jq .
}

# 检查是否安装了jq
if ! command -v jq &> /dev/null; then
    echo "警告: 未安装jq工具，输出将不会格式化。建议安装jq以获得更好的输出格式。"
    # 定义一个空的jq函数，以便脚本可以继续运行
    jq() {
        cat
    }
fi

# 测试上传SQL文件
test_upload_sql_file "sql_samples/simple_query.sql"
test_upload_sql_file "sql_samples/complex_query.sql"

# 测试上传存储过程文件
test_upload_stored_procedure_file "sql_samples/simple_procedure.sql" "get_emp"
test_upload_stored_procedure_file "sql_samples/update_employee_salary.sql" "update_employee_salary" "HR"
test_upload_stored_procedure_file "sql_samples/analyze_sales_performance.sql" "analyze_sales_performance" "SALES"

echo -e "\n"
