import cn.iocoder.yudao.server.util.SafeSQLExecutor;
import java.util.Arrays;

/**
 * SQL注入修复测试验证程序
 */
public class TestSQLInjectionFix {
    
    public static void main(String[] args) {
        System.out.println("=== SQL注入修复测试验证 ===\n");
        
        // 测试1: WHERE/AND逻辑修复
        System.out.println("1. WHERE/AND逻辑测试:");
        testWhereAndLogic();
        
        // 测试2: ORDER BY安全验证
        System.out.println("\n2. ORDER BY安全验证测试:");
        testOrderBySafety();
        
        // 测试3: SQL安全性验证
        System.out.println("\n3. SQL安全性验证测试:");
        testSQLSecurity();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    private static void testWhereAndLogic() {
        // 测试无WHERE时添加WHERE
        String sql1 = "SELECT * FROM users";
        String result1 = SafeSQLExecutor.appendCondition(sql1, "deleted = 0");
        System.out.println("输入: " + sql1);
        System.out.println("输出: " + result1);
        System.out.println("期望: SELECT * FROM users WHERE (deleted = 0)");
        System.out.println("结果: " + (result1.equals("SELECT * FROM users WHERE (deleted = 0)") ? "✅ 通过" : "❌ 失败"));
        
        System.out.println();
        
        // 测试有WHERE时添加AND
        String sql2 = "SELECT * FROM users WHERE status = 1";
        String result2 = SafeSQLExecutor.appendCondition(sql2, "deleted = 0");
        System.out.println("输入: " + sql2);
        System.out.println("输出: " + result2);
        System.out.println("期望: SELECT * FROM users WHERE status = 1 AND (deleted = 0)");
        System.out.println("结果: " + (result2.equals("SELECT * FROM users WHERE status = 1 AND (deleted = 0)") ? "✅ 通过" : "❌ 失败"));
        
        System.out.println();
        
        // 测试有ORDER BY时正确插入
        String sql3 = "SELECT * FROM users ORDER BY id DESC";
        String result3 = SafeSQLExecutor.appendCondition(sql3, "deleted = 0");
        System.out.println("输入: " + sql3);
        System.out.println("输出: " + result3);
        System.out.println("期望: SELECT * FROM users WHERE (deleted = 0) ORDER BY id DESC");
        System.out.println("结果: " + (result3.equals("SELECT * FROM users WHERE (deleted = 0) ORDER BY id DESC") ? "✅ 通过" : "❌ 失败"));
    }
    
    private static void testOrderBySafety() {
        // 测试白名单验证
        String orderBy1 = "id desc, name asc";
        String result1 = SafeSQLExecutor.buildSafeOrderBy(
            orderBy1,
            Arrays.asList("id", "name", "create_time"),
            "id DESC"
        );
        System.out.println("输入: " + orderBy1);
        System.out.println("白名单: [id, name, create_time]");
        System.out.println("输出: " + result1);
        System.out.println("期望: ORDER BY id DESC, name ASC");
        System.out.println("结果: " + (result1.equals(" ORDER BY id DESC, name ASC") ? "✅ 通过" : "❌ 失败"));
        
        System.out.println();
        
        // 测试SQL注入防护
        String orderBy2 = "id; DROP TABLE users--";
        String result2 = SafeSQLExecutor.buildSafeOrderBy(
            orderBy2,
            Arrays.asList("id", "name"),
            "id DESC"
        );
        System.out.println("输入(恶意): " + orderBy2);
        System.out.println("输出: " + result2);
        System.out.println("期望: ORDER BY id DESC (使用默认)");
        System.out.println("结果: " + (result2.equals(" ORDER BY id DESC") ? "✅ 通过" : "❌ 失败"));
    }
    
    private static void testSQLSecurity() {
        // 测试允许正常DML
        boolean allow1 = SafeSQLExecutor.isSecureSQL("INSERT INTO users (name) VALUES ('test')");
        System.out.println("INSERT语句: " + (allow1 ? "✅ 允许" : "❌ 拒绝"));
        
        boolean allow2 = SafeSQLExecutor.isSecureSQL("UPDATE users SET name = 'test' WHERE id = 1");
        System.out.println("UPDATE语句(有WHERE): " + (allow2 ? "✅ 允许" : "❌ 拒绝"));
        
        boolean allow3 = SafeSQLExecutor.isSecureSQL("DELETE FROM users WHERE id = 1");
        System.out.println("DELETE语句(有WHERE): " + (allow3 ? "✅ 允许" : "❌ 拒绝"));
        
        System.out.println();
        
        // 测试阻止危险操作
        boolean block1 = SafeSQLExecutor.isSecureSQL("DROP TABLE users");
        System.out.println("DROP TABLE: " + (!block1 ? "✅ 阻止" : "❌ 未阻止"));
        
        boolean block2 = SafeSQLExecutor.isSecureSQL("DELETE FROM users");
        System.out.println("DELETE(无WHERE): " + (!block2 ? "✅ 阻止" : "❌ 未阻止"));
        
        boolean block3 = SafeSQLExecutor.isSecureSQL("UPDATE users SET status = 0");
        System.out.println("UPDATE(无WHERE): " + (!block3 ? "✅ 阻止" : "❌ 未阻止"));
    }
}