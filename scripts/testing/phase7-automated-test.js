/**
 * Phase7 自动化功能测试脚本
 * 测试范围发布通知页面的核心功能
 * 
 * 运行方式: node phase7-automated-test.js
 */

const https = require('https');
const http = require('http');

// 测试配置
const TEST_CONFIG = {
    MAIN_API: 'http://localhost:48081',
    MOCK_API: 'http://localhost:48082',
    TEST_TIMEOUT: 10000,
    RETRY_COUNT: 3
};

// 测试数据
const TEST_ROLES = [
    { 
        roleCode: 'PRINCIPAL', 
        employeeId: 'PRINCIPAL_001', 
        name: 'Principal-Zhang',
        expectedScopes: ['SCHOOL_WIDE', 'DEPARTMENT', 'CLASS', 'GRADE'],
        maxLevel: 1
    },
    { 
        roleCode: 'ACADEMIC_ADMIN', 
        employeeId: 'ACADEMIC_ADMIN_001', 
        name: 'Director-Li',
        expectedScopes: ['SCHOOL_WIDE', 'DEPARTMENT', 'GRADE'],
        maxLevel: 2
    },
    { 
        roleCode: 'TEACHER', 
        employeeId: 'TEACHER_001', 
        name: 'Teacher-Wang',
        expectedScopes: ['DEPARTMENT', 'CLASS'],
        maxLevel: 3
    }
];

// 测试通知模板
const TEST_NOTIFICATION = {
    title: `教师通知测试发布`,
    content: '这是测试通知内容，验证发布功能正常工作。',
    level: 3,
    targetScope: 'DEPARTMENT', // 使用驼峰命名，符合前端接口
    require_confirm: 0,
    pinned: 0
};

// 工具函数
class TestUtils {
    static makeRequest(url, options = {}) {
        return new Promise((resolve, reject) => {
            const protocol = url.startsWith('https:') ? https : http;
            const timeout = setTimeout(() => {
                reject(new Error(`请求超时: ${url}`));
            }, TEST_CONFIG.TEST_TIMEOUT);

            const req = protocol.request(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    ...options.headers
                },
                ...options
            }, (res) => {
                clearTimeout(timeout);
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => {
                    try {
                        const result = data ? JSON.parse(data) : {};
                        resolve({ statusCode: res.statusCode, data: result, headers: res.headers });
                    } catch (e) {
                        resolve({ statusCode: res.statusCode, data: data, headers: res.headers });
                    }
                });
            });

            req.on('error', (err) => {
                clearTimeout(timeout);
                reject(err);
            });

            if (options.body) {
                req.write(JSON.stringify(options.body));
            }
            req.end();
        });
    }

    static async authenticate(roleData) {
        const authData = {
            employeeId: roleData.employeeId,
            name: roleData.name,
            password: 'admin123'
        };

        console.log(`🔐 认证用户: ${roleData.name} (${roleData.roleCode})`);
        
        const response = await this.makeRequest(`${TEST_CONFIG.MOCK_API}/mock-school-api/auth/authenticate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: authData
        });

        if (response.statusCode !== 200) {
            throw new Error(`认证失败: ${response.statusCode} - ${JSON.stringify(response.data)}`);
        }

        const token = response.data.data?.accessToken || response.data.data?.token;
        if (!token) {
            throw new Error(`认证响应中没有token: ${JSON.stringify(response.data)}`);
        }

        console.log(`✅ 认证成功，获取Token: ${token.substring(0, 20)}...`);
        return token;
    }

    static async getAvailableScopes(token) {
        console.log(`📊 获取可用范围权限...`);
        
        const response = await this.makeRequest(`${TEST_CONFIG.MAIN_API}/admin-api/test/notification/api/available-scopes`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'tenant-id': '1'
            }
        });

        if (response.statusCode !== 200) {
            throw new Error(`获取范围权限失败: ${response.statusCode} - ${JSON.stringify(response.data)}`);
        }

        console.log(`✅ 获取范围权限成功`);
        return response.data.data || response.data;
    }

    static async publishNotification(token, notificationData) {
        console.log(`📝 发布通知: ${notificationData.title.substring(0, 30)}...`);
        
        const response = await this.makeRequest(`${TEST_CONFIG.MAIN_API}/admin-api/test/notification/api/publish-database`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'tenant-id': '1'
            },
            body: notificationData
        });

        if (response.statusCode !== 200) {
            throw new Error(`发布通知失败: ${response.statusCode} - ${JSON.stringify(response.data)}`);
        }

        console.log(`✅ 通知发布成功`);
        return response.data;
    }

    static validateScopePermissions(scopeData, expectedScopes, roleCode) {
        console.log(`🔍 验证${roleCode}角色权限...`);
        console.log(`   原始数据:`, JSON.stringify(scopeData, null, 2));
        
        // 提取范围名称 - 适配不同的数据结构
        let actualScopes = [];
        if (Array.isArray(scopeData.availableScopes)) {
            actualScopes = scopeData.availableScopes.map(scope => {
                if (typeof scope === 'string') return scope;
                if (scope.scope) return scope.scope;
                if (scope.name) return scope.name;
                if (scope.code) return scope.code;
                return String(scope);
            });
        } else if (scopeData.scopes) {
            actualScopes = scopeData.scopes;
        }
        
        console.log(`   预期范围: ${expectedScopes.join(', ')}`);
        console.log(`   实际范围: ${actualScopes.join(', ')}`);

        // 范围映射 - 处理中英文对照
        const scopeMapping = {
            'SCHOOL_WIDE': ['SCHOOL_WIDE', '全校范围', 'school_wide'],
            'DEPARTMENT': ['DEPARTMENT', '部门范围', 'department'],
            'CLASS': ['CLASS', '班级范围', 'class'],
            'GRADE': ['GRADE', '年级范围', 'grade']
        };

        const normalizedActual = actualScopes.map(scope => {
            for (const [key, values] of Object.entries(scopeMapping)) {
                if (values.includes(scope)) return key;
            }
            return scope;
        });

        const missing = expectedScopes.filter(scope => !normalizedActual.includes(scope));
        const extra = normalizedActual.filter(scope => !expectedScopes.includes(scope));

        if (missing.length > 0 || extra.length > 0) {
            console.log(`   标准化后实际范围: ${normalizedActual.join(', ')}`);
            throw new Error(`权限验证失败 - 缺失: [${missing.join(', ')}], 多余: [${extra.join(', ')}]`);
        }

        console.log(`✅ ${roleCode}权限验证通过`);
    }
}

// 测试执行器
class Phase7Tester {
    constructor() {
        this.results = {
            total: 0,
            passed: 0,
            failed: 0,
            errors: []
        };
    }

    async runTest(testName, testFunction) {
        this.results.total++;
        console.log(`\n🧪 执行测试: ${testName}`);
        console.log('='.repeat(60));

        try {
            await testFunction();
            console.log(`✅ 测试通过: ${testName}`);
            this.results.passed++;
        } catch (error) {
            console.log(`❌ 测试失败: ${testName}`);
            console.log(`   错误: ${error.message}`);
            this.results.failed++;
            this.results.errors.push({ test: testName, error: error.message });
        }
    }

    async testBasicConnectivity() {
        console.log('📡 测试基础连接性...');
        
        // 测试主服务健康检查
        const mainHealth = await TestUtils.makeRequest(`${TEST_CONFIG.MAIN_API}/admin-api/test/notification/api/health`);
        if (mainHealth.statusCode !== 200) {
            throw new Error(`主服务连接失败: ${mainHealth.statusCode}`);
        }
        console.log('✅ 主通知服务连接正常');

        // 测试Mock API健康检查
        const mockHealth = await TestUtils.makeRequest(`${TEST_CONFIG.MOCK_API}/mock-school-api/auth/health`);
        if (mockHealth.statusCode !== 200) {
            throw new Error(`Mock API连接失败: ${mockHealth.statusCode}`);
        }
        console.log('✅ Mock School API连接正常');
    }

    async testRoleAuthentication() {
        console.log('🔐 测试角色认证功能...');
        
        for (const role of TEST_ROLES) {
            const token = await TestUtils.authenticate(role);
            if (!token || token.length < 10) {
                throw new Error(`${role.roleCode}角色认证返回无效token`);
            }
        }
        
        console.log('✅ 所有角色认证测试通过');
    }

    async testScopePermissions() {
        console.log('📊 测试范围权限获取...');
        
        for (const role of TEST_ROLES) {
            const token = await TestUtils.authenticate(role);
            const scopeData = await TestUtils.getAvailableScopes(token);
            
            // 验证返回的数据结构
            if (!scopeData.availableScopes || !Array.isArray(scopeData.availableScopes)) {
                throw new Error(`${role.roleCode}角色范围权限数据格式错误`);
            }
            
            // 验证权限范围
            TestUtils.validateScopePermissions(
                scopeData,
                role.expectedScopes, 
                role.roleCode
            );
        }
        
        console.log('✅ 所有角色范围权限测试通过');
    }

    async testNotificationPublish() {
        console.log('📝 测试通知发布功能...');
        
        // 只测试教师角色(权限适中，避免过多数据库写入)
        const teacherRole = TEST_ROLES.find(r => r.roleCode === 'TEACHER');
        const token = await TestUtils.authenticate(teacherRole);
        
        // 发布测试通知 - 使用教师角色可发布的范围
        const testNotification = {
            ...TEST_NOTIFICATION,
            title: `${TEST_NOTIFICATION.title} - ${teacherRole.roleCode}`,
            targetScope: 'DEPARTMENT'  // 教师可以发布到部门范围
        };
        
        const result = await TestUtils.publishNotification(token, testNotification);
        
        // 验证发布结果
        if (!result.success && !result.data && result.code !== 0) {
            throw new Error(`通知发布失败: ${JSON.stringify(result)}`);
        }
        
        // 检查是否成功发布（通过notificationId判断）
        const notificationId = result.data?.notificationId;
        if (!notificationId) {
            throw new Error(`通知发布成功但未返回ID: ${JSON.stringify(result)}`);
        }
        
        console.log(`✅ 通知发布成功，ID: ${notificationId}`);
        
        console.log('✅ 通知发布功能测试通过');
    }

    async testErrorHandling() {
        console.log('⚠️ 测试错误处理...');
        
        // 测试无效token - 应该返回401
        try {
            await TestUtils.getAvailableScopes('invalid_token');
            throw new Error('应该返回401错误');
        } catch (error) {
            if (!error.message.includes('401') && !error.message.includes('获取范围权限失败')) {
                throw error;
            }
            console.log('✅ 无效token错误处理正确');
        }
        
        // 测试无效认证 - 应该返回认证失败
        try {
            await TestUtils.authenticate({
                employeeId: 'INVALID_001',
                name: 'Invalid-User',
                roleCode: 'INVALID'
            });
            throw new Error('应该返回认证失败');
        } catch (error) {
            if (error.message.includes('认证响应中没有token')) {
                // 这是预期的错误，因为401响应没有token
                console.log('✅ 无效认证错误处理正确');
            } else {
                throw error;
            }
        }
        
        console.log('✅ 错误处理测试通过');
    }

    async runAllTests() {
        console.log('\n🚀 开始执行Phase7自动化测试');
        console.log('='.repeat(80));
        console.log(`测试目标: Phase7 范围发布通知页面`);
        console.log(`主服务: ${TEST_CONFIG.MAIN_API}`);
        console.log(`Mock API: ${TEST_CONFIG.MOCK_API}`);
        console.log(`测试时间: ${new Date().toLocaleString()}`);

        await this.runTest('基础连接性测试', () => this.testBasicConnectivity());
        await this.runTest('角色认证测试', () => this.testRoleAuthentication());
        await this.runTest('范围权限测试', () => this.testScopePermissions());
        await this.runTest('通知发布测试', () => this.testNotificationPublish());
        await this.runTest('错误处理测试', () => this.testErrorHandling());

        this.printResults();
    }

    printResults() {
        console.log('\n📊 测试结果汇总');
        console.log('='.repeat(80));
        console.log(`总测试数: ${this.results.total}`);
        console.log(`通过: ${this.results.passed} ✅`);
        console.log(`失败: ${this.results.failed} ❌`);
        console.log(`成功率: ${((this.results.passed / this.results.total) * 100).toFixed(1)}%`);

        if (this.results.failed > 0) {
            console.log('\n❌ 失败测试详情:');
            this.results.errors.forEach((error, index) => {
                console.log(`${index + 1}. ${error.test}: ${error.error}`);
            });
        }

        console.log('\n🎯 测试总结:');
        if (this.results.failed === 0) {
            console.log('🎉 所有测试通过！Phase7页面功能正常。');
        } else {
            console.log('⚠️  部分测试失败，请检查相关功能。');
        }
        
        console.log('\n📝 测试完成时间:', new Date().toLocaleString());
    }
}

// 主执行函数
async function main() {
    const tester = new Phase7Tester();
    
    try {
        await tester.runAllTests();
    } catch (error) {
        console.log('\n💥 测试执行异常:', error.message);
        process.exit(1);
    }
}

// 执行测试
if (require.main === module) {
    main().catch(console.error);
}

module.exports = { Phase7Tester, TestUtils };