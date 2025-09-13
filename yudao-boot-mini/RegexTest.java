import java.util.regex.Pattern;

public class RegexTest {
    private static final Pattern SAFE_CONDITION_PATTERN = Pattern.compile(
        "^[a-zA-Z_][a-zA-Z0-9_\\.]*\\s*(=|<|>|<=|>=|<>|!=|\\s+IN\\s+|\\s+LIKE\\s+|\\s+BETWEEN\\s+|\\s+IS\\s+(?:NOT\\s+)?NULL)\\s*[a-zA-Z0-9_\\.'\"\\s,\\(\\)]+$"
    );
    
    public static void main(String[] args) {
        String condition = "deleted = 0";
        boolean matches = SAFE_CONDITION_PATTERN.matcher(condition).matches();
        System.out.println("Condition: '" + condition + "'");
        System.out.println("Matches: " + matches);
        
        // 调试信息
        if (!matches) {
            System.out.println("Pattern: " + SAFE_CONDITION_PATTERN.pattern());
        }
    }
}
