package cn.iocoder.yudao.server.framework.datapermission;

import cn.iocoder.yudao.framework.datapermission.core.rule.DataPermissionRule;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 用户数据权限规则
 * 实现垂直越权防护，确保用户只能访问自己的私有数据
 * 
 * @author Claude Code
 * @date 2025-09-07
 */
@Component
public class UserDataPermissionRule implements DataPermissionRule {
    
    /**
     * 用户ID列名
     */
    private static final String USER_COLUMN_NAME = "creator";
    
    /**
     * 需要应用此规则的表名，可通过配置文件灵活配置
     */
    @Value("#{'${yudao.data-permission.user-rule.tables:notification_info,todo_notification,notification_approval}'.split(',')}")
    private Set<String> tables;
    
    @Override
    public Set<String> getTableNames() {
        // 返回需要应用此规则的表名
        return tables;
    }
    
    @Override
    public Expression getExpression(String tableName, net.sf.jsqlparser.expression.Alias tableAlias) {
        // 获取当前登录用户
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            // 未登录，返回永假条件
            return new EqualsTo(
                new Column("1"),
                new LongValue(0)
            );
        }
        
        // 构建 creator = userId 的条件
        Column creatorColumn = MyBatisUtils.buildColumn(tableName, tableAlias, USER_COLUMN_NAME);
        
        // 使用用户ID作为数据权限过滤条件
        return new EqualsTo(
            creatorColumn,
            new LongValue(loginUser.getId())
        );
    }
}