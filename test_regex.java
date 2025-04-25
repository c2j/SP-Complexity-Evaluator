import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test_regex {
    public static void main(String[] args) {
        // 测试SQL语句
        String sql = "SELECT salary, department_id, job_id, manager_id, count(1) cnt\n" +
                "INTO v_current_salary, v_department_id, v_job_id, v_manager_id\n" +
                "FROM employees\n" +
                "WHERE employee_id = p_employee_id\n" +
                "group by salary, department_id, job_id, manager_id\n" +
                "order by salary;";
        
        // 当前的正则表达式模式
        Pattern currentPattern = Pattern.compile(
                "\\b(SELECT|INSERT|UPDATE|DELETE|MERGE|COMMIT|ROLLBACK|CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE)\\b[\\s\\S]*?\\s*;",
                Pattern.CASE_INSENSITIVE
        );
        
        // 测试当前模式
        Matcher currentMatcher = currentPattern.matcher(sql);
        System.out.println("Current pattern matches: " + currentMatcher.find());
        if (currentMatcher.find(0)) {
            System.out.println("Matched text: " + currentMatcher.group());
        } else {
            System.out.println("No match found with current pattern.");
        }
        
        // 修改后的正则表达式模式（使用贪婪匹配）
        Pattern newPattern = Pattern.compile(
                "\\b(SELECT|INSERT|UPDATE|DELETE|MERGE|COMMIT|ROLLBACK|CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE)\\b[\\s\\S]*;",
                Pattern.CASE_INSENSITIVE
        );
        
        // 测试修改后的模式
        Matcher newMatcher = newPattern.matcher(sql);
        System.out.println("\nNew pattern matches: " + newMatcher.find());
        if (newMatcher.find(0)) {
            System.out.println("Matched text: " + newMatcher.group());
        } else {
            System.out.println("No match found with new pattern.");
        }
    }
}
