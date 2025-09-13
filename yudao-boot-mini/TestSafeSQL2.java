public class TestSafeSQL2 {
    
    private static boolean isValidCondition(String condition) {
        String upperCondition = condition.toUpperCase();
        if (condition.contains(";") || 
            condition.contains("--") || 
            condition.contains("/*") || 
            condition.contains("*/") ||
            upperCondition.matches(".*\\bUNION\\b.*") ||
            upperCondition.matches(".*\\bSELECT\\b.*") ||
            upperCondition.matches(".*\\bINSERT\\b.*") ||
            upperCondition.matches(".*\\bUPDATE\\b.*") ||
            upperCondition.matches(".*\\bDELETE\\b.*") ||
            upperCondition.matches(".*\\bDROP\\b.*")) {
            return false;
        }
        
        return true; // 简化测试，只测单词边界
    }
    
    public static void main(String[] args) {
        String[] testCases = {
            "deleted = 0",
            "DELETE FROM table",
            "UPDATE table SET",
            "name LIKE '%test%'"
        };
        
        for (String testCase : testCases) {
            boolean isValid = isValidCondition(testCase);
            System.out.println("'" + testCase + "' -> " + isValid);
        }
    }
}
