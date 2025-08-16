/**
 * Phase7全角色通知发布功能测试
 * 测试所有角色的通知发布功能，排查问题
 * 
 * 运行方式: node phase7-all-roles-test.js
 */

const https = require('https');
const http = require('http');

// 测试配置
const TEST_CONFIG = {
    MAIN_API: 'http://localhost:48081',
    MOCK_API: 'http://localhost:48082',
    TEST_TIMEOUT: 15000
};

// 所有角色测试数据
const ALL_ROLES = [
    {
        roleCode: 'PRINCIPAL',
        employeeId: 'PRINCIPAL_001',
        name: 'Principal-Zhang',
        description: '校长',
        expectedScopes: ['SCHOOL_WIDE', 'DEPARTMENT', 'CLASS', 'GRADE'],
        testLevels: [1, 2, 3, 4]
    },
    {
        roleCode: 'ACADEMIC_ADMIN',
        employeeId: 'ACADEMIC_ADMIN_001', 
        name: 'Director-Li',
        description: '教务主任',
        expectedScopes: ['SCHOOL_WIDE', 'DEPARTMENT', 'GRADE'],
        testLevels: [1, 2, 3]
    },
    {
        roleCode: 'TEACHER',
        employeeId: 'TEACHER_001',
        name: 'Teacher-Wang',
        description: '教师',
        expectedScopes: ['DEPARTMENT', 'CLASS'],
        testLevels: [3, 4]
    },
    {
        roleCode: 'CLASS_TEACHER',
        employeeId: 'CLASS_TEACHER_001',
        name: 'ClassTeacher-Liu',
        description: '班主任',
        expectedScopes: ['CLASS', 'GRADE'],
        testLevels: [3, 4]
    },
    {
        roleCode: 'STUDENT',
        employeeId: 'STUDENT_001',
        name: 'Student-Zhang',
        description: '学生',
        expectedScopes: ['CLASS'],
        testLevels: [4]
    }
];

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

        console.log(`🔐 认证${roleData.description}: ${roleData.name} (${roleData.roleCode})`);
        
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

        const token = response.data.data?.accessToken;
        if (!token) {
            throw new Error(`认证响应中没有token: ${JSON.stringify(response.data)}`);
        }

        console.log(`✅ ${roleData.description}认证成功`);
        return token;
    }

    static async getAvailableScopes(token, roleDescription) {
        console.log(`📊 获取${roleDescription}可用范围权限...`);
        
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

        console.log(`✅ ${roleDescription}权限获取成功`);
        return response.data.data || response.data;
    }

    static async publishNotification(token, roleData, level, targetScope) {
        const title = `通知发布功能测试`;
        const content = `这是通知发布功能的测试内容。当前测试角色为${roleData.description}。`;
        
        console.log(`📝 ${roleData.description}发布${level}级通知到${targetScope}范围`);
        
        const notificationData = {
            title: title,
            content: content,
            level: level,
            targetScope: targetScope,
            publisherName: roleData.name,
            publisherId: roleData.employeeId
        };
        
        const response = await this.makeRequest(`${TEST_CONFIG.MAIN_API}/admin-api/test/notification/api/publish-database`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'tenant-id': '1'
            },
            body: notificationData
        });

        console.log(`📥 ${roleData.description} API响应: ${response.statusCode}`);
        
        if (response.statusCode !== 200) {
            throw new Error(`HTTP ${response.statusCode}: ${JSON.stringify(response.data)}`);
        }

        // 检查业务逻辑错误
        if (response.data.code !== 0) {
            throw new Error(`业务错误 code=${response.data.code}: ${response.data.msg}`);
        }

        return response.data;
    }

    static extractScopes(scopeData) {
        let actualScopes = [];
        if (Array.isArray(scopeData.availableScopes)) {
            actualScopes = scopeData.availableScopes.map(scope => {
                if (typeof scope === 'string') return scope;
                if (scope.code) return scope.code;
                if (scope.name) return scope.name;
                return String(scope);
            });
        }
        
        // 范围映射
        const scopeMapping = {
            'SCHOOL_WIDE': ['SCHOOL_WIDE', '全校范围'],
            'DEPARTMENT': ['DEPARTMENT', '部门范围'],
            'CLASS': ['CLASS', '班级范围'], 
            'GRADE': ['GRADE', '年级范围']
        };

        return actualScopes.map(scope => {
            for (const [key, values] of Object.entries(scopeMapping)) {
                if (values.includes(scope)) return key;
            }
            return scope;
        });
    }
}

// 主测试类
class AllRolesTester {
    constructor() {
        this.results = [];
        this.summary = {
            totalRoles: 0,
            successfulRoles: 0,
            failedRoles: 0,
            totalTests: 0,
            passedTests: 0,
            failedTests: 0
        };
    }

    async testSingleRole(roleData) {
        console.log(`\n🎭 测试角色: ${roleData.description} (${roleData.roleCode})`);
        console.log('='.repeat(70));

        const roleResult = {
            role: roleData,
            authSuccess: false,
            scopesMatch: false,
            publishTests: [],
            overall: false,
            errors: []
        };

        try {
            // 1. 认证测试
            const token = await TestUtils.authenticate(roleData);
            roleResult.authSuccess = true;

            // 2. 权限范围测试
            const scopeData = await TestUtils.getAvailableScopes(token, roleData.description);
            const actualScopes = TestUtils.extractScopes(scopeData);
            
            console.log(`   预期范围: [${roleData.expectedScopes.join(', ')}]`);
            console.log(`   实际范围: [${actualScopes.join(', ')}]`);

            const scopesMatch = roleData.expectedScopes.every(scope => actualScopes.includes(scope));
            roleResult.scopesMatch = scopesMatch;

            if (scopesMatch) {
                console.log(`✅ ${roleData.description}权限范围验证通过`);
            } else {
                console.log(`❌ ${roleData.description}权限范围不匹配`);
            }

            // 3. 发布测试 - 测试每个级别
            for (const level of roleData.testLevels) {
                const targetScope = actualScopes[0]; // 使用第一个可用范围
                
                if (!targetScope) {
                    throw new Error(`${roleData.description}没有可用的发布范围`);
                }

                try {
                    const publishResult = await TestUtils.publishNotification(token, roleData, level, targetScope);
                    
                    console.log(`📋 ${roleData.description} Level${level} API完整响应:`, JSON.stringify(publishResult, null, 2));
                    
                    const testResult = {
                        level: level,
                        targetScope: targetScope,
                        success: true,
                        notificationId: publishResult.data?.notificationId || publishResult.data?.id || 'N/A',
                        status: publishResult.data?.status || 'UNKNOWN',
                        approvalRequired: publishResult.data?.approvalRequired || false,
                        rawResponse: publishResult
                    };

                    roleResult.publishTests.push(testResult);
                    console.log(`✅ ${roleData.description} Level${level}发布成功 (ID: ${testResult.notificationId}, Status: ${testResult.status})`);
                    this.summary.passedTests++;

                } catch (error) {
                    const testResult = {
                        level: level,
                        targetScope: targetScope,
                        success: false,
                        error: error.message
                    };

                    roleResult.publishTests.push(testResult);
                    roleResult.errors.push(`Level${level}发布失败: ${error.message}`);
                    console.log(`❌ ${roleData.description} Level${level}发布失败: ${error.message}`);
                    this.summary.failedTests++;
                }

                this.summary.totalTests++;
            }

            // 判断整体成功
            const allPublishSuccess = roleResult.publishTests.every(test => test.success);
            roleResult.overall = roleResult.authSuccess && roleResult.scopesMatch && allPublishSuccess;

            if (roleResult.overall) {
                console.log(`🎉 ${roleData.description}所有测试通过`);
                this.summary.successfulRoles++;
            } else {
                console.log(`⚠️ ${roleData.description}部分测试失败`);
                this.summary.failedRoles++;
            }

        } catch (error) {
            roleResult.errors.push(`角色测试异常: ${error.message}`);
            console.log(`💥 ${roleData.description}测试异常: ${error.message}`);
            this.summary.failedRoles++;
        }

        this.summary.totalRoles++;
        this.results.push(roleResult);
        return roleResult;
    }

    async runAllTests() {
        console.log('\n🚀 Phase7全角色通知发布功能测试');
        console.log('='.repeat(80));
        console.log(`测试时间: ${new Date().toLocaleString()}`);
        console.log(`测试角色数: ${ALL_ROLES.length}`);

        // 依次测试每个角色
        for (const roleData of ALL_ROLES) {
            await this.testSingleRole(roleData);
        }

        this.printSummary();
    }

    printSummary() {
        console.log('\n📊 全角色测试结果汇总');
        console.log('='.repeat(80));

        // 总体统计
        console.log(`👥 角色统计:`);
        console.log(`   总角色数: ${this.summary.totalRoles}`);
        console.log(`   成功角色: ${this.summary.successfulRoles} ✅`);
        console.log(`   失败角色: ${this.summary.failedRoles} ❌`);
        console.log(`   角色成功率: ${((this.summary.successfulRoles / this.summary.totalRoles) * 100).toFixed(1)}%`);

        console.log(`\n📝 发布测试统计:`);
        console.log(`   总测试数: ${this.summary.totalTests}`);
        console.log(`   通过测试: ${this.summary.passedTests} ✅`);
        console.log(`   失败测试: ${this.summary.failedTests} ❌`);
        console.log(`   测试成功率: ${((this.summary.passedTests / this.summary.totalTests) * 100).toFixed(1)}%`);

        // 详细结果
        console.log('\n📋 详细测试结果:');
        this.results.forEach((result, index) => {
            const status = result.overall ? '✅' : '❌';
            const role = result.role;
            console.log(`\n${index + 1}. ${status} ${role.description} (${role.roleCode})`);
            console.log(`   认证: ${result.authSuccess ? '✅' : '❌'}`);
            console.log(`   权限: ${result.scopesMatch ? '✅' : '❌'}`);
            console.log(`   发布: ${result.publishTests.length}个测试`);
            
            result.publishTests.forEach(test => {
                const testStatus = test.success ? '✅' : '❌';
                if (test.success) {
                    console.log(`     ${testStatus} Level${test.level} → ${test.status} (ID: ${test.notificationId})`);
                } else {
                    console.log(`     ${testStatus} Level${test.level} → ${test.error}`);
                }
            });

            if (result.errors.length > 0) {
                console.log(`   错误:`);
                result.errors.forEach(error => {
                    console.log(`     • ${error}`);
                });
            }
        });

        // 问题分析
        console.log('\n🔍 问题分析:');
        const failedRoles = this.results.filter(r => !r.overall);
        if (failedRoles.length === 0) {
            console.log('🎉 所有角色测试通过！Phase7功能完全正常！');
        } else {
            console.log('⚠️ 发现以下问题:');
            failedRoles.forEach(result => {
                console.log(`\n🚨 ${result.role.description} (${result.role.roleCode}):`);
                if (!result.authSuccess) {
                    console.log('   • 认证失败');
                }
                if (!result.scopesMatch) {
                    console.log('   • 权限范围不匹配');
                }
                const failedPublish = result.publishTests.filter(t => !t.success);
                if (failedPublish.length > 0) {
                    console.log(`   • 发布测试失败: ${failedPublish.length}/${result.publishTests.length}`);
                }
                result.errors.forEach(error => {
                    console.log(`   • ${error}`);
                });
            });
        }

        console.log(`\n📝 测试完成时间: ${new Date().toLocaleString()}`);
    }
}

// 主执行函数
async function main() {
    const tester = new AllRolesTester();
    
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

module.exports = { AllRolesTester, TestUtils };