#!/bin/bash

# 设置服务器地址和端口
SERVER="http://localhost:8080"
DEFAULT_DIALECT="Oracle"

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项] [文件路径]"
    echo "选项:"
    echo "  -h, --help                显示帮助信息"
    echo "  -s, --sql <文件路径>       评估SQL语句复杂度"
    echo "  -p, --procedure <文件路径> 评估存储过程复杂度"
    echo "  -n, --name <名称>          存储过程名称 (默认从文件名获取)"
    echo "  -c, --schema <模式>        存储过程模式/所有者 (默认为HR)"
    echo "  -d, --dialect <方言>       SQL方言 (默认为Oracle)"
    echo "  -a, --all                 评估sql_samples目录中的所有示例"
    echo
    echo "示例:"
    echo "  $0 -s sql_samples/simple_query.sql"
    echo "  $0 -p sql_samples/update_employee_salary.sql -n update_salary -c PAYROLL"
    echo "  $0 -a"
}

# 评估SQL语句复杂度
evaluate_sql() {
    local file_path=$1
    local dialect=${2:-$DEFAULT_DIALECT}
    
    if [ ! -f "$file_path" ]; then
        echo "错误: 文件 '$file_path' 不存在"
        return 1
    fi
    
    # 读取SQL文件内容
    local sql_content=$(cat "$file_path")
    
    # 转义JSON特殊字符
    sql_content=$(echo "$sql_content" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | tr '\n' ' ')
    
    echo -e "\n===== 评估SQL语句复杂度: $(basename "$file_path") ====="
    curl -s -X POST "$SERVER/api/complexity/sql" \
      -H "Content-Type: application/json" \
      -d "{\"sql\": \"$sql_content\", \"dialect\": \"$dialect\"}" | jq .
}

# 评估存储过程复杂度
evaluate_procedure() {
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
    
    # 转义JSON特殊字符
    procedure_content=$(echo "$procedure_content" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | tr '\n' ' ')
    
    echo -e "\n===== 评估存储过程复杂度: $name (Schema: $schema) ====="
    curl -s -X POST "$SERVER/api/complexity/stored-procedure" \
      -H "Content-Type: application/json" \
      -d "{\"sourceCode\": \"$procedure_content\", \"name\": \"$name\", \"schema\": \"$schema\", \"dialect\": \"$dialect\"}" | jq .
}

# 评估所有示例
evaluate_all_samples() {
    echo -e "===== 评估所有示例 ====="
    
    # 评估SQL查询
    evaluate_sql "sql_samples/simple_query.sql"
    evaluate_sql "sql_samples/complex_query.sql"
    
    # 评估存储过程
    evaluate_procedure "sql_samples/simple_procedure.sql" "get_emp"
    evaluate_procedure "sql_samples/update_employee_salary.sql" "update_employee_salary"
    evaluate_procedure "sql_samples/analyze_sales_performance.sql" "analyze_sales_performance" "SALES"
}

# 检查是否安装了jq
if ! command -v jq &> /dev/null; then
    echo "警告: 未安装jq工具，输出将不会格式化。建议安装jq以获得更好的输出格式。"
    # 定义一个空的jq函数，以便脚本可以继续运行
    jq() {
        cat
    }
fi

# 如果没有参数，显示帮助信息
if [ $# -eq 0 ]; then
    show_help
    exit 0
fi

# 解析命令行参数
while [ $# -gt 0 ]; do
    case "$1" in
        -h|--help)
            show_help
            exit 0
            ;;
        -s|--sql)
            if [ -n "$2" ]; then
                evaluate_sql "$2" "$DEFAULT_DIALECT"
                shift 2
            else
                echo "错误: --sql 选项需要一个文件路径参数"
                exit 1
            fi
            ;;
        -p|--procedure)
            if [ -n "$2" ]; then
                file_path="$2"
                name=$(basename "$file_path" .sql)
                schema="HR"
                dialect="$DEFAULT_DIALECT"
                shift 2
                
                # 检查是否有其他选项
                while [ $# -gt 0 ]; do
                    case "$1" in
                        -n|--name)
                            if [ -n "$2" ]; then
                                name="$2"
                                shift 2
                            else
                                echo "错误: --name 选项需要一个参数"
                                exit 1
                            fi
                            ;;
                        -c|--schema)
                            if [ -n "$2" ]; then
                                schema="$2"
                                shift 2
                            else
                                echo "错误: --schema 选项需要一个参数"
                                exit 1
                            fi
                            ;;
                        -d|--dialect)
                            if [ -n "$2" ]; then
                                dialect="$2"
                                shift 2
                            else
                                echo "错误: --dialect 选项需要一个参数"
                                exit 1
                            fi
                            ;;
                        *)
                            break
                            ;;
                    esac
                done
                
                evaluate_procedure "$file_path" "$name" "$schema" "$dialect"
            else
                echo "错误: --procedure 选项需要一个文件路径参数"
                exit 1
            fi
            ;;
        -a|--all)
            evaluate_all_samples
            shift
            ;;
        *)
            echo "错误: 未知选项 $1"
            show_help
            exit 1
            ;;
    esac
done

exit 0
