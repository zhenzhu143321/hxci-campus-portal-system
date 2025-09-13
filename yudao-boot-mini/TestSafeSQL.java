import java.util.regex.Pattern;

public class TestSafeSQL {
    private static final Pattern SAFE_CONDITION_PATTERN = Pattern.compile(
        "^[a-zA-Z_][a-zA-Z0-9_\\.]*\\s*(=|<|>|<=|>=|<>|!=|\\s+IN\\s+|\\s+LIKE\\s+|\\s+BETWEEN\\s+|\\s+IS\\s+(?:NOT\\s+)?NULL)\\s*[a-zA-Z0-9_\\.'\"\\s,\\(\\)]+$"
    );
    
    private static boolean isValidCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }
        
        // 去掉首尾空格
        String trimmed = condition.trim();
        
        // 禁止危险关键字
        String upperCondition = trimmed.toUpperCase();
        if (upperCondition.contains("DELETE") || upperCondition.contains("UPDATE") || 
            upperCondition.contains("INSERT") || upperCondition.contains("CREATE") ||
            upperCondition.contains("ALTER") || upperCondition.contains("EXEC") ||
            upperCondition.contains("UNION") || upperCondition.contains("--") ||
            upperCondition.contains("/*") || upperCondition.contains("*/") ||
            upperCondition.contains("DROP")) {
            return false;
        }
        
        // 验证基本格式：列名 操作符 值
        return SAFE_CONDITION_PATTERN.matcher(trimmed).matches();
    }
    
    public static void main(String[] args) {
        String condition = "deleted = 0";
        boolean isValid = isValidCondition(condition);
        System.out.println("Testing condition: '" + condition + "'");
        System.out.println("Is valid: " + isValid);
        System.out.println("Using .matches(): " + SAFE_CONDITION_PATTERN.matcher(condition).matches());
        System.out.println("Using .find(): " + SAFE_CONDITION_PATTERN.matcher(condition).find());
    }
}
