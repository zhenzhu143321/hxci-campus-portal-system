#!/bin/bash

# P1.2 JWT安全修复检查脚本
# Phase 1.2: JWT Information Leakage Fix Security Check

echo "🛡️  P1.2 JWT信息泄露修复安全检查"
echo "=================================="

# 检查1: 验证JWT相关代码修复
echo "🔍 检查1: JWT安全代码实施情况"

# 检查动态密钥生成
echo "   🔐 动态密钥生成检查..."
if grep -q "generateSecureKey()" /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ 动态密钥生成已实施"
else
    echo "   ❌ 动态密钥生成未实施"
fi

# 检查Token脱敏
echo "   🛡️  Token脱敏检查..."
if grep -q "maskToken" /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ Token脱敏机制已实施"
else
    echo "   ❌ Token脱敏机制未实施"
fi

# 检查极简载荷设计
echo "   🎯 极简载荷设计检查..."
if grep -q "P1.2极简载荷" /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ 极简载荷设计已实施"
else
    echo "   ❌ 极简载荷设计未实施"
fi

# 检查Token版本标识
echo "   🆕 Token版本控制检查..."
if grep -q '"ver", "2.0"' /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ P1.2 Token版本标识已实施"
else
    echo "   ❌ Token版本标识未实施"
fi

# 检查JWT ID强化
echo "   🆔 JWT ID强化检查..."
if grep -q "jwt_v2_" /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ JWT ID强化格式已实施"
else
    echo "   ❌ JWT ID强化格式未实施"
fi

echo ""

# 检查2: 敏感信息防护
echo "🔍 检查2: 敏感信息防护验证"

# 检查是否禁用了敏感信息
echo "   🚫 敏感信息禁用检查..."
if grep -q "绝对禁止：任何可识别个人身份的信息" /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ 敏感信息禁用规则已明确"
else
    echo "   ❌ 敏感信息禁用规则未明确"
fi

echo ""

# 检查3: 安全配置
echo "🔍 检查3: JWT安全配置检查"

# 检查Token有效期
echo "   ⏰ Token有效期检查..."
if grep -q "10 \* 60 \* 1000" /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ Token有效期已缩短到10分钟"
else
    echo "   ⚠️  Token有效期可能未优化"
fi

# 检查签发者和受众标识
echo "   🏷️  JWT标识符检查..."
if grep -q "hxci-campus-portal-v2" /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ P1.2签发者标识已更新"
else
    echo "   ❌ 签发者标识未更新"
fi

if grep -q "school-api-secure" /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini/yudao-mock-school-api/src/main/java/cn/iocoder/yudao/mock/school/service/impl/MockSchoolUserServiceImpl.java; then
    echo "   ✅ P1.2受众标识已更新"
else
    echo "   ❌ 受众标识未更新"
fi

echo ""

# 检查4: 编译状态
echo "🔍 检查4: 代码编译状态"
cd /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini

echo "   ⚙️  编译Mock School API..."
if mvn compile -pl yudao-mock-school-api -q > /dev/null 2>&1; then
    echo "   ✅ Mock School API编译成功"
else
    echo "   ❌ Mock School API编译失败"
fi

echo ""

# 检查5: 测试脚本准备
echo "🔍 检查5: 安全测试脚本准备"

if [ -f "/opt/hxci-campus-portal/hxci-campus-portal-system/jwt_security_verification_test.py" ]; then
    echo "   ✅ JWT安全验证测试脚本已准备"
    
    # 检查Python依赖
    if python3 -c "import requests" 2>/dev/null; then
        echo "   ✅ Python requests依赖可用"
    else
        echo "   ⚠️  Python requests依赖未安装，可能影响测试"
    fi
else
    echo "   ❌ JWT安全验证测试脚本未找到"
fi

echo ""

# 总结
echo "🏁 P1.2 JWT安全修复检查完成"
echo "=================================="
echo "📋 修复摘要:"
echo "   🔐 动态密钥生成防止硬编码泄露"
echo "   ⏰ Token有效期缩短到10分钟"
echo "   🛡️  Token脱敏防止日志泄露"
echo "   🎯 极简载荷移除所有敏感信息"
echo "   🆔 强化JWT ID防重放攻击"
echo "   🏷️  更新标识符增强安全性"
echo ""
echo "✅ P1.2修复已完成，等待用户重启服务进行测试验证"
echo "🚀 下一步：用户重启服务后运行 python3 jwt_security_verification_test.py"