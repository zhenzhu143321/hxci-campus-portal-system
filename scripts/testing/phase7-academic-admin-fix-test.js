/**
 * Phase7教务主任权限验证测试
 * 专门测试QA发现的priority/level字段映射问题修复
 * 
 * 运行方式: node phase7-academic-admin-fix-test.js
 */

const https = require('https');
const http = require('http');

// 测试配置
const TEST_CONFIG = {
    MAIN_API: 'http://localhost:48081',
    MOCK_API: 'http://localhost:48082',
    TEST_TIMEOUT: 10000
};

// 教务主任测试数据
const ACADEMIC_ADMIN_USER = {
    roleCode: 'ACADEMIC_ADMIN',
    employeeId: 'ACADEMIC_ADMIN_001', 
    name: 'Director-Li'
};

// 权限级别测试用例
const LEVEL_TEST_CASES = [
    {
        level: 1,
        title: '【紧急通知】测试1级通知发布',
        content: '测试教务主任发布1级紧急通知，应该触发审批流程。',
        targetScope: 'SCHOOL_WIDE',
        expectedResult: 'PENDING_APPROVAL'  // 应该需要审批
    },
    {
        level: 2,
        title: '【重要通知】测试2级通知发布',
        content: '测试教务主任发布2级重要通知，应该直接发布成功。',
        targetScope: 'DEPARTMENT',
        expectedResult: 'PUBLISHED'  // 应该直接发布
    },
    {
        level: 3,
        title: '【常规通知】测试3级通知发布',
        content: '测试教务主任发布3级常规通知，应该直接发布成功。',
        targetScope: 'GRADE',
        expectedResult: 'PUBLISHED'  // 应该直接发布
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

    static async authenticate() {
        const authData = {
            employeeId: ACADEMIC_ADMIN_USER.employeeId,
            name: ACADEMIC_ADMIN_USER.name,
            password: 'admin123'
        };

        console.log(`🔐 认证教务主任: ${ACADEMIC_ADMIN_USER.name}`);
        
        const response = await this.makeRequest(`${TEST_CONFIG.MOCK_API}/mock-school-api/auth/authenticate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: authData
        });

        if (response.statusCode !== 200) {
            throw new Error(`认证失败: ${response.statusCode}`);
        }

        const token = response.data.data?.accessToken;
        if (!token) {
            throw new Error(`认证响应中没有token`);
        }

        console.log(`✅ 认证成功，Token: ${token.substring(0, 20)}...`);
        return token;
    }

    static async publishNotificationWithLevel(token, testCase) {
        console.log(`📝 发布${testCase.level}级通知: ${testCase.title}`);
        
        const notificationData = {
            title: testCase.title,
            content: testCase.content,
            level: testCase.level,  // ✅ 使用修复后的level字段
            targetScope: testCase.targetScope,
            publisherName: ACADEMIC_ADMIN_USER.name,
            publisherId: ACADEMIC_ADMIN_USER.employeeId
        };
        
        console.log(`📋 发布数据: ${JSON.stringify(notificationData, null, 2)}`);
        
        const response = await this.makeRequest(`${TEST_CONFIG.MAIN_API}/admin-api/test/notification/api/publish-database`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'tenant-id': '1'
            },
            body: notificationData
        });

        console.log(`📥 API响应状态: ${response.statusCode}`);
        console.log(`📥 API响应数据: ${JSON.stringify(response.data, null, 2)}`);

        if (response.statusCode !== 200) {
            throw new Error(`发布失败: ${response.statusCode} - ${JSON.stringify(response.data)}`);
        }

        return response.data;
    }
}

// 主测试函数
async function runAcademicAdminTest() {
    console.log('\n🎯 Phase7教务主任权限修复验证测试');
    console.log('='.repeat(60));
    console.log(`测试目标: 验证priority/level字段修复后的教务主任发布功能`);
    console.log(`测试时间: ${new Date().toLocaleString()}`);

    let testResults = [];

    try {
        // 1. 认证获取token
        const token = await TestUtils.authenticate();

        // 2. 测试各个权限级别
        for (const testCase of LEVEL_TEST_CASES) {
            console.log(`\n📋 测试用例: ${testCase.level}级通知`);
            console.log('-'.repeat(50));

            try {
                const result = await TestUtils.publishNotificationWithLevel(token, testCase);

                // 分析结果
                let actualStatus = 'UNKNOWN';
                let notificationId = 'N/A';

                if (result.code === 0 && result.data) {
                    const data = result.data;
                    notificationId = data.notificationId || data.id || 'N/A';
                    
                    // 判断实际状态
                    if (data.status === 'PUBLISHED') {
                        actualStatus = 'PUBLISHED';
                    } else if (data.status === 'PENDING_APPROVAL' || data.approvalRequired) {
                        actualStatus = 'PENDING_APPROVAL';
                    } else {
                        actualStatus = data.status || 'UNKNOWN';
                    }
                } else {
                    actualStatus = 'FAILED';
                }

                // 验证结果
                const passed = actualStatus === testCase.expectedResult;
                const testResult = {
                    level: testCase.level,
                    title: testCase.title,
                    expected: testCase.expectedResult,
                    actual: actualStatus,
                    notificationId: notificationId,
                    passed: passed
                };

                testResults.push(testResult);

                if (passed) {
                    console.log(`✅ 测试通过: ${testCase.level}级通知状态正确 (${actualStatus})`);
                } else {
                    console.log(`❌ 测试失败: 期望${testCase.expectedResult}，实际${actualStatus}`);
                }

                console.log(`📬 通知ID: ${notificationId}`);

            } catch (error) {
                console.log(`❌ 测试异常: ${error.message}`);
                testResults.push({
                    level: testCase.level,
                    title: testCase.title,
                    expected: testCase.expectedResult,
                    actual: 'ERROR',
                    error: error.message,
                    passed: false
                });
            }
        }

        // 3. 汇总结果
        console.log('\n📊 测试结果汇总');
        console.log('='.repeat(60));

        const totalTests = testResults.length;
        const passedTests = testResults.filter(r => r.passed).length;
        const failedTests = totalTests - passedTests;

        console.log(`总测试数: ${totalTests}`);
        console.log(`通过数: ${passedTests} ✅`);
        console.log(`失败数: ${failedTests} ❌`);
        console.log(`成功率: ${((passedTests / totalTests) * 100).toFixed(1)}%`);

        console.log('\n📝 详细结果:');
        testResults.forEach((result, index) => {
            const status = result.passed ? '✅' : '❌';
            console.log(`${index + 1}. ${status} Level ${result.level}: ${result.expected} → ${result.actual}`);
            if (result.notificationId && result.notificationId !== 'N/A') {
                console.log(`   通知ID: ${result.notificationId}`);
            }
            if (result.error) {
                console.log(`   错误: ${result.error}`);
            }
        });

        // 4. 修复验证总结
        console.log('\n🎯 修复验证总结:');
        if (failedTests === 0) {
            console.log('🎉 所有测试通过！Priority/Level字段修复成功！');
            console.log('✅ 教务主任各级别通知发布功能正常');
            console.log('✅ 审批流程在1级通知中正确触发');
            console.log('✅ 前后端字段映射问题已解决');
        } else {
            console.log('⚠️ 仍有问题需要进一步排查:');
            const failedResults = testResults.filter(r => !r.passed);
            failedResults.forEach(result => {
                console.log(`• Level ${result.level}: 期望${result.expected}，实际${result.actual}`);
            });
        }

    } catch (error) {
        console.log(`\n💥 测试执行异常: ${error.message}`);
    }

    console.log(`\n📝 测试完成时间: ${new Date().toLocaleString()}`);
}

// 执行测试
if (require.main === module) {
    runAcademicAdminTest().catch(console.error);
}