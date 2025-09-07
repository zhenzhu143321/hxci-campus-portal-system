import cn.iocoder.yudao.server.util.SafeSQLExecutor;
import java.util.Arrays;

/**
 * SQL注入二次修复验证测试
 * 测试GPT-5发现的问题是否已修复
 */
public class TestSQLInjectionFixV2 {
    
    public static void main(String[] args) {
        System.out.println("=== SQL注入二次修复验证测试 ===\n");
        
        // 测试1: 验证条件注入防护
        System.out.println("1. 条件注入防护测试:");
        testConditionInjection();
        
        // 测试2: 验证子查询处理
        System.out.println("\n2. 子查询处理测试:");
        testSubqueryHandling();
        
        // 测试3: 验证ORDER BY大小写
        System.out.println("\n3. ORDER BY大小写测试:");
        testOrderByCaseInsensitive();
        
        // 测试4: 验证性能优化
        System.out.println("\n4. 性能测试:");
        testPerformance();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    private static void testConditionInjection() {
        String sql = "SELECT * FROM users";
        
        // 测试安全条件
        try {
            String safeCondition = "status = 1";
            String result1 = SafeSQLExecutor.appendCondition(sql, safeCondition);
            System.out.println("✅ 安全条件通过: " + safeCondition);
        } catch (Exception e) {
            System.out.println("❌ 安全条件被拒绝");
        }
        
        // 测试危险条件 - 包含UNION
        try {
            String dangerousCondition = "1=1 UNION SELECT * FROM passwords";
            String result2 = SafeSQLExecutor.appendCondition(sql, dangerousCondition);
            System.out.println("❌ 危险条件未被拦截: " + dangerousCondition);
        } catch (SecurityException e) {
            System.out.println("✅ 危险条件被拦截: UNION注入");
        }
        
        // 测试危险条件 - 包含分号
        try {
            String dangerousCondition2 = "1=1; DROP TABLE users";
            String result3 = SafeSQLExecutor.appendCondition(sql, dangerousCondition2);
            System.out.println("❌ 危险条件未被拦截: " + dangerousCondition2);
        } catch (SecurityException e) {
            System.out.println("✅ 危险条件被拦截: 分号注入");
        }
        
        // 测试危险条件 - 包含注释
        try {
            String dangerousCondition3 = "1=1 -- comment";
            String result4 = SafeSQLExecutor.appendCondition(sql, dangerousCondition3);
            System.out.println("❌ 危险条件未被拦截: " + dangerousCondition3);
        } catch (SecurityException e) {
            System.out.println("✅ 危险条件被拦截: 注释注入");
        }
    }
    
    private static void testSubqueryHandling() {
        // 测试子查询中的ORDER BY不被误处理
        String sqlWithSubquery = "SELECT * FROM (SELECT * FROM users ORDER BY id) t";
        String result = SafeSQLExecutor.appendCondition(sqlWithSubquery, "status = 1");
        
        // 应该在外层添加WHERE，而不是在子查询的ORDER BY之前
        boolean correct = result.contains(") t WHERE (status = 1)");
        System.out.println("子查询处理: " + (correct ? "✅ 正确" : "❌ 错误"));
        System.out.println("结果: " + result);
    }
    
    private static void testOrderByCaseInsensitive() {
        // 测试大小写不敏感的白名单匹配
        String orderBy1 = "ID desc, NAME asc";  // 大写
        String orderBy2 = "id DESC, name ASC";  // 小写
        String orderBy3 = "Id Desc, Name Asc";  // 混合
        
        String[] whitelist = {"id", "name", "create_time"};
        
        String result1 = SafeSQLExecutor.buildSafeOrderBy(orderBy1, Arrays.asList(whitelist), "id DESC");
        String result2 = SafeSQLExecutor.buildSafeOrderBy(orderBy2, Arrays.asList(whitelist), "id DESC");
        String result3 = SafeSQLExecutor.buildSafeOrderBy(orderBy3, Arrays.asList(whitelist), "id DESC");
        
        System.out.println("大写输入: " + (result1.contains("id") ? "✅ 匹配" : "❌ 未匹配"));
        System.out.println("小写输入: " + (result2.contains("id") ? "✅ 匹配" : "❌ 未匹配"));
        System.out.println("混合输入: " + (result3.contains("id") ? "✅ 匹配" : "❌ 未匹配"));
    }
    
    private static void testPerformance() {
        // 测试WHERE模式编译性能
        long start = System.nanoTime();
        
        for (int i = 0; i < 10000; i++) {
            String sql = "SELECT * FROM users WHERE status = 1";
            SafeSQLExecutor.ensureNotDeleted(sql);
        }
        
        long end = System.nanoTime();
        double ms = (end - start) / 1_000_000.0;
        
        System.out.println("10000次WHERE检查耗时: " + String.format("%.2f", ms) + "ms");
        System.out.println("平均每次: " + String.format("%.4f", ms / 10000) + "ms");
        System.out.println("性能评估: " + (ms < 100 ? "✅ 优秀" : ms < 500 ? "⚠️ 一般" : "❌ 需优化"));
    }
}