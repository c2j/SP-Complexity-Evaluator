#!/bin/bash

# 设置服务器地址和端口
SERVER="http://localhost:8080"
DEFAULT_DIALECT="Gauss"
# 支持的方言列表
SUPPORTED_DIALECTS=("Oracle" "Gauss" "Hive")

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项] [文件路径]"
    echo "选项:"
    echo "  -h, --help                显示帮助信息"
    echo "  -s, --sql <文件路径>       评估SQL语句复杂度"
    echo "  -p, --procedure <文件路径> 评估存储过程复杂度"
    echo "  -z, --zip <文件路径>       评估ZIP包中的SQL文件"
    echo "  -n, --name <名称>          存储过程名称 (默认从文件名获取)"
    echo "  -c, --schema <模式>        存储过程模式/所有者 (默认为HR)"
    echo "  -d, --dialect <方言>       SQL方言 (默认为Oracle, 支持: Oracle, Gauss, Hive)"
    echo "  -f, --functions <文件路径> 自定义函数列表文件"
    echo "  -t, --tables <文件路径>    高权重表列表文件"
    echo "  -r, --hwprocedures <文件路径> 高权重存储过程列表文件"
    echo "  -e, --excel               返回Excel格式 (仅适用于ZIP评估)"
    echo "  -a, --all                 评估sql_samples目录中的所有示例"
    echo
    echo "示例:"
    echo "  $0 -s sql_samples/simple_query.sql"
    echo "  $0 -p sql_samples/update_employee_salary.sql -n update_salary -c PAYROLL"
    echo "  $0 -p sql_samples/gauss/custom_functions_procedure.sql -d Gauss -f sql_samples/gauss/custom_functions.txt -t sql_samples/gauss/high_weight_tables.txt -r sql_samples/gauss/high_weight_procedures.txt"
    echo "  $0 -z sql_samples/test.zip -d Oracle -f sql_samples/gauss/custom_functions.txt -t sql_samples/gauss/high_weight_tables.txt -r sql_samples/gauss/high_weight_procedures.txt"
    echo "  $0 -z sql_samples/test.zip -e -d Oracle -f sql_samples/gauss/custom_functions.txt"
    echo "  $0 -a"
}

# 检查方言是否支持
check_dialect() {
    local dialect=$1
    local supported=false

    for d in "${SUPPORTED_DIALECTS[@]}"; do
        if [ "$d" = "$dialect" ]; then
            supported=true
            break
        fi
    done

    if [ "$supported" = false ]; then
        echo "错误: 不支持的SQL方言 '$dialect'. 支持的方言: ${SUPPORTED_DIALECTS[*]}"
        return 1
    fi

    return 0
}

# 评估SQL语句复杂度
evaluate_sql() {
    local file_path=$1
    local dialect=${2:-$DEFAULT_DIALECT}

    if [ ! -f "$file_path" ]; then
        echo "错误: 文件 '$file_path' 不存在"
        return 1
    fi

    # 检查方言是否支持
    check_dialect "$dialect" || return 1

    echo -e "\n===== 评估SQL语句复杂度: $file_path ====="

    # 使用curl的multipart/form-data直接上传文件
    curl -s -X POST "$SERVER/api/complexity/sql/upload" \
      -F "file=@$file_path" \
      -F "dialect=$dialect" \
      -H "Content-Type: multipart/form-data" | jq .
}

# 评估存储过程复杂度
evaluate_procedure() {
    local file_path=$1
    local name=$2
    local schema=${3:-"HR"}
    local dialect=${4:-$DEFAULT_DIALECT}
    local custom_functions_file=$5
    local high_weight_tables_file=$6
    local high_weight_procedures_file=$7

    if [ ! -f "$file_path" ]; then
        echo "错误: 文件 '$file_path' 不存在"
        return 1
    fi

    # 检查方言是否支持
    check_dialect "$dialect" || return 1

    # 如果未提供名称，则从文件名获取
    if [ -z "$name" ]; then
        name=$(basename "$file_path" .sql)
    fi

    # 使用文件上传API直接发送文件
    echo -e "\n===== 评估存储过程复杂度: $file_path $name (Schema: $schema) ====="

    # 准备自定义函数、高权重表和高权重存储过程参数
    local custom_functions_param=""
    local high_weight_tables_param=""
    local high_weight_procedures_param=""

    # 如果提供了自定义函数文件
    if [ -n "$custom_functions_file" ] && [ -f "$custom_functions_file" ]; then
        custom_functions_param="-F customFunctionsFile=@$custom_functions_file"
    fi

    # 如果提供了高权重表文件
    if [ -n "$high_weight_tables_file" ] && [ -f "$high_weight_tables_file" ]; then
        high_weight_tables_param="-F highWeightTablesFile=@$high_weight_tables_file"
    fi

    # 如果提供了高权重存储过程文件
    if [ -n "$high_weight_procedures_file" ] && [ -f "$high_weight_procedures_file" ]; then
        high_weight_procedures_param="-F highWeightProceduresFile=@$high_weight_procedures_file"
    fi

    # 使用curl的multipart/form-data直接上传文件
    echo "Calling API with file: $file_path"
    curl -s -X POST "$SERVER/api/complexity/stored-procedure/upload" \
      -F "file=@$file_path" \
      -F "name=$name" \
      -F "schema=$schema" \
      -F "dialect=$dialect" \
      $custom_functions_param \
      $high_weight_tables_param \
      $high_weight_procedures_param \
      -H "Content-Type: multipart/form-data"
}

# 评估所有示例
evaluate_all_samples() {
    echo -e "===== 评估所有示例 ====="

    # 评估SQL查询 (Oracle)
    evaluate_sql "sql_samples/oracle/simple_query.sql" "Oracle"
    evaluate_sql "sql_samples/oracle/complex_query.sql" "Oracle"

    # 评估存储过程 (Oracle)
    evaluate_procedure "sql_samples/oracle/simple_procedure.sql" "get_emp" "HR" "Oracle"
    evaluate_procedure "sql_samples/oracle/update_employee_salary.sql" "update_employee_salary" "HR" "Oracle"
    evaluate_procedure "sql_samples/oracle/analyze_sales_performance.sql" "analyze_sales_performance" "SALES" "Oracle"

    # 评估SQL查询 (Gauss)
    evaluate_sql "sql_samples/gauss/simple_query.sql" "Gauss"
    evaluate_sql "sql_samples/gauss/complex_query.sql" "Gauss"
    evaluate_sql "sql_samples/gauss/window_functions.sql" "Gauss"
    evaluate_sql "sql_samples/gauss/cte_query.sql" "Gauss"

    # 评估存储过程 (Gauss)
    evaluate_procedure "sql_samples/gauss/simple_procedure.sql" "get_emp" "HR" "Gauss"
    evaluate_procedure "sql_samples/gauss/complex_procedure.sql" "update_employee_salary" "HR" "Gauss"
    evaluate_procedure "sql_samples/gauss/nested_loops_procedure.sql" "analyze_department_performance" "SALES" "Gauss"
    evaluate_procedure "sql_samples/gauss/custom_functions_procedure.sql" "process_customer_orders" "SALES" "Gauss" "sql_samples/gauss/custom_functions.txt" "sql_samples/gauss/high_weight_tables.txt" "sql_samples/gauss/high_weight_procedures.txt"
    evaluate_procedure "sql_samples/gauss/nested_procedure_calls.sql" "process_monthly_payroll" "HR" "Gauss"

    evaluate_procedure "sql_samples/gauss/a.sql" "insert_data" "HR" "Gauss"

    # 评估SQL查询 (Hive)
    evaluate_sql "sql_samples/hive/simple_query.sql" "Hive"
    evaluate_sql "sql_samples/hive/join_query.sql" "Hive"
    evaluate_sql "sql_samples/hive/complex_query.sql" "Hive"
    evaluate_sql "sql_samples/hive/lateral_view_query.sql" "Hive"
    evaluate_sql "sql_samples/hive/subquery_union_query.sql" "Hive"

    # 评估Hive脚本 (模拟存储过程)
    evaluate_procedure "sql_samples/hive/data_processing_script.sql" "data_processing_script" "default" "Hive"
    evaluate_procedure "sql_samples/hive/custom_functions_script.sql" "custom_functions_script" "default" "Hive" "sql_samples/hive/custom_functions.txt" "sql_samples/hive/high_weight_tables.txt" "sql_samples/hive/high_weight_procedures.txt"
}

# 评估ZIP包中的SQL文件
evaluate_zip() {
    local zip_path=$1
    local dialect=${2:-$DEFAULT_DIALECT}
    local custom_functions_file=$3
    local high_weight_tables_file=$4
    local high_weight_procedures_file=$5
    local response_format=${6:-"json"}

    if [ ! -f "$zip_path" ]; then
        echo "错误: ZIP文件 '$zip_path' 不存在"
        return 1
    fi

    echo -e "\n===== 评估ZIP包中的SQL文件: $(basename "$zip_path") ====="

    # 准备自定义函数、高权重表和高权重存储过程参数
    local custom_functions_param=""
    local high_weight_tables_param=""
    local high_weight_procedures_param=""

    # 如果提供了自定义函数文件
    if [ -n "$custom_functions_file" ] && [ -f "$custom_functions_file" ]; then
        custom_functions_param="-F customFunctionsFile=@$custom_functions_file"
    fi

    # 如果提供了高权重表文件
    if [ -n "$high_weight_tables_file" ] && [ -f "$high_weight_tables_file" ]; then
        high_weight_tables_param="-F highWeightTablesFile=@$high_weight_tables_file"
    fi

    # 如果提供了高权重存储过程文件
    if [ -n "$high_weight_procedures_file" ] && [ -f "$high_weight_procedures_file" ]; then
        high_weight_procedures_param="-F highWeightProceduresFile=@$high_weight_procedures_file"
    fi

    # 使用curl的multipart/form-data直接上传文件
    if [ "$response_format" = "excel" ]; then
        echo "正在下载Excel文件..."
        curl -X POST "$SERVER/api/complexity/batch/upload" \
          -F "file=@$zip_path" \
          -F "dialect=$dialect" \
          -F "responseFormat=excel" \
          $custom_functions_param \
          $high_weight_tables_param \
          $high_weight_procedures_param \
          -H "Content-Type: multipart/form-data" \
          -o "complexity_metrics.xlsx"

        if [ $? -eq 0 ]; then
            echo "Excel文件已保存为 complexity_metrics.xlsx"
        else
            echo "下载Excel文件失败"
        fi
    else
        # 默认返回JSON
        curl -s -X POST "$SERVER/api/complexity/batch/upload" \
          -F "file=@$zip_path" \
          -F "dialect=$dialect" \
          -F "responseFormat=json" \
          $custom_functions_param \
          $high_weight_tables_param \
          $high_weight_procedures_param \
          -H "Content-Type: multipart/form-data" | jq .
    fi
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
                file_path="$2"
                dialect="$DEFAULT_DIALECT"
                shift 2

                # 检查是否有其他选项
                while [ $# -gt 0 ]; do
                    case "$1" in
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

                evaluate_sql "$file_path" "$dialect"
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
                custom_functions_file=""
                high_weight_tables_file=""
                high_weight_procedures_file=""
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
                        -f|--functions)
                            if [ -n "$2" ]; then
                                custom_functions_file="$2"
                                shift 2
                            else
                                echo "错误: --functions 选项需要一个文件路径参数"
                                exit 1
                            fi
                            ;;
                        -t|--tables)
                            if [ -n "$2" ]; then
                                high_weight_tables_file="$2"
                                shift 2
                            else
                                echo "错误: --tables 选项需要一个文件路径参数"
                                exit 1
                            fi
                            ;;
                        -r|--hwprocedures)
                            if [ -n "$2" ]; then
                                high_weight_procedures_file="$2"
                                shift 2
                            else
                                echo "错误: --procedures 选项需要一个文件路径参数"
                                exit 1
                            fi
                            ;;
                        *)
                            break
                            ;;
                    esac
                done

                evaluate_procedure "$file_path" "$name" "$schema" "$dialect" "$custom_functions_file" "$high_weight_tables_file" "$high_weight_procedures_file"
            else
                echo "错误: --procedure 选项需要一个文件路径参数"
                exit 1
            fi
            ;;
        -a|--all)
            evaluate_all_samples
            shift
            ;;
        -z|--zip)
            if [ -n "$2" ]; then
                zip_path="$2"
                dialect="$DEFAULT_DIALECT"
                custom_functions_file=""
                high_weight_tables_file=""
                high_weight_procedures_file=""
                response_format="json"
                shift 2

                # 检查是否有其他选项
                while [ $# -gt 0 ]; do
                    case "$1" in
                        -d|--dialect)
                            if [ -n "$2" ]; then
                                dialect="$2"
                                shift 2
                            else
                                echo "错误: --dialect 选项需要一个参数"
                                exit 1
                            fi
                            ;;
                        -f|--functions)
                            if [ -n "$2" ]; then
                                custom_functions_file="$2"
                                shift 2
                            else
                                echo "错误: --functions 选项需要一个文件路径参数"
                                exit 1
                            fi
                            ;;
                        -t|--tables)
                            if [ -n "$2" ]; then
                                high_weight_tables_file="$2"
                                shift 2
                            else
                                echo "错误: --tables 选项需要一个文件路径参数"
                                exit 1
                            fi
                            ;;
                        -r|--hwprocedures)
                            if [ -n "$2" ]; then
                                high_weight_procedures_file="$2"
                                shift 2
                            else
                                echo "错误: --procedures 选项需要一个文件路径参数"
                                exit 1
                            fi
                            ;;
                        -e|--excel)
                            response_format="excel"
                            shift
                            ;;
                        *)
                            break
                            ;;
                    esac
                done

                evaluate_zip "$zip_path" "$dialect" "$custom_functions_file" "$high_weight_tables_file" "$high_weight_procedures_file" "$response_format"
                exit 0
            else
                echo "错误: --zip 选项需要一个ZIP文件路径"
                exit 1
            fi
            ;;
        *)
            echo "错误: 未知选项 $1"
            show_help
            exit 1
            ;;
    esac
done

exit 0

# 统计java代码行数
# echo `find . -name \*.java -exec wc -l {} \; | cut -d "." -f 1` | awk '{sum=0; for (i=1; i<=NF; i++) sum+=$i; print sum}'