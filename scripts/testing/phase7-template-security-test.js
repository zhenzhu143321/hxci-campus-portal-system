/**
 * Phase7模板安全性验证测试
 * 测试修复后的通知模板是否能通过SQL注入检测
 * 
 * 运行方式: node phase7-template-security-test.js
 */

const https = require('https');
const http = require('http');

// 测试配置
const TEST_CONFIG = {
    MAIN_API: 'http://localhost:48081',
    MOCK_API: 'http://localhost:48082',
    TEST_TIMEOUT: 10000
};

// 安全模板内容 - 与Phase7页面同步
const SAFE_TEMPLATES = {
    emergency: {
        title: '紧急通知：校园临时安排',
        content: `各位师生：\n\n因重要事务需要，今日下午将进行必要工作安排。\n\n时间安排：今日下午2点至4点\n影响范围：部分区域暂停服务\n\n请大家提前做好准备，合理安排时间。\n\n如有疑问请联系相关部门。\n\n谢谢配合！\n\n校务办公室\n${new Date().toLocaleDateString()}`,
        priority: 4,
        scope: 'SCHOOL_WIDE'
    },
    academic: {
        title: '教务通知：期末考试安排',
        content: `各位同学：\n\n期末考试相关安排通知如下：\n\n考试时间：12月中下旬\n考试地点：详见考试安排表\n\n重要提醒：\n• 请提前30分钟到达考场\n• 携带身份证件和准考证\n• 考试期间请保持安静\n• 诚信考试是基本要求\n\n祝大家考试顺利！\n\n教务处\n${new Date().toLocaleDateString()}`,
        priority: 2,
        scope: 'SCHOOL_WIDE'
    },
    meeting: {
        title: '会议通知：月度工作例会',
        content: `各位同事：\n\n定于本月26日上午9点召开月度工作例会。\n\n会议地点：会议室A301\n参会人员：全体教职工\n\n议程安排：\n1. 上月工作总结\n2. 本月计划部署\n3. 重点项目讨论\n4. 其他重要事项\n\n请各部门准备相关材料，准时参会。\n\n办公室\n${new Date().toLocaleDateString()}`,
        priority: 2,
        scope: 'DEPARTMENT'
    }
};

// 工具函数
class TemplateSecurityTester {
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
            employeeId: 'PRINCIPAL_001',
            name: 'Principal-Zhang',
            password: 'admin123'
        };

        console.log(`🔐 认证校长用户...`);
        
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

        console.log(`✅ 认证成功`);
        return token;
    }

    static async testTemplate(token, templateName, template) {
        console.log(`\n📋 测试模板: ${templateName} - ${template.title}`);
        console.log('-'.repeat(60));
        
        const notificationData = {
            title: template.title,
            content: template.content,
            level: template.priority,
            targetScope: template.scope,
            publisherName: 'Principal-Zhang',
            publisherId: 'PRINCIPAL_001'
        };
        
        console.log(`📝 模板内容预览:`);
        console.log(`   标题: ${template.title}`);
        console.log(`   内容长度: ${template.content.length}字符`);
        console.log(`   优先级: ${template.priority}`);
        console.log(`   范围: ${template.scope}`);
        
        try {
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
            
            if (response.statusCode !== 200) {
                throw new Error(`HTTP错误: ${response.statusCode}`);
            }

            const result = response.data;
            console.log(`📊 业务响应码: ${result.code}`);
            
            if (result.code === 0) {
                const notificationId = result.data?.notificationId;
                const status = result.data?.status;
                console.log(`✅ 模板${templateName}发布成功!`);
                console.log(`   通知ID: ${notificationId}`);
                console.log(`   状态: ${status}`);
                return { success: true, notificationId, status };
            } else {
                console.log(`❌ 模板${templateName}发布失败: ${result.msg}`);
                return { success: false, error: result.msg };
            }

        } catch (error) {
            console.log(`💥 模板${templateName}测试异常: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    static async runAllTemplateTests() {
        console.log('\n🛡️ Phase7模板安全性验证测试');
        console.log('='.repeat(70));
        console.log(`测试目标: 验证修复后的模板能通过SQL注入检测`);
        console.log(`测试时间: ${new Date().toLocaleString()}`);

        const results = [];

        try {
            // 1. 认证
            const token = await this.authenticate();

            // 2. 测试每个模板
            for (const [templateName, template] of Object.entries(SAFE_TEMPLATES)) {
                const result = await this.testTemplate(token, templateName, template);
                results.push({
                    templateName,
                    title: template.title,
                    ...result
                });
            }

            // 3. 汇总结果
            console.log('\n📊 模板测试结果汇总');
            console.log('='.repeat(70));

            const totalTemplates = results.length;
            const successfulTemplates = results.filter(r => r.success).length;
            const failedTemplates = totalTemplates - successfulTemplates;

            console.log(`📋 模板统计:`);
            console.log(`   总模板数: ${totalTemplates}`);
            console.log(`   成功模板: ${successfulTemplates} ✅`);
            console.log(`   失败模板: ${failedTemplates} ❌`);
            console.log(`   成功率: ${((successfulTemplates / totalTemplates) * 100).toFixed(1)}%`);

            console.log('\n📝 详细结果:');
            results.forEach((result, index) => {
                const status = result.success ? '✅' : '❌';
                console.log(`${index + 1}. ${status} ${result.templateName}: ${result.title}`);
                if (result.success) {
                    console.log(`   通知ID: ${result.notificationId}, 状态: ${result.status}`);
                } else {
                    console.log(`   错误: ${result.error}`);
                }
            });

            // 4. 安全性评估
            console.log('\n🔒 安全性评估:');
            if (failedTemplates === 0) {
                console.log('🎉 所有模板安全测试通过！');
                console.log('✅ SQL注入检测问题已完全解决');
                console.log('✅ 用户现在可以正常使用所有模板功能');
                console.log('✅ Phase7页面模板功能完全可用');
            } else {
                console.log('⚠️ 仍有模板存在安全问题:');
                const failedResults = results.filter(r => !r.success);
                failedResults.forEach(result => {
                    console.log(`• ${result.templateName}: ${result.error}`);
                });
            }

        } catch (error) {
            console.log(`\n💥 测试执行异常: ${error.message}`);
        }

        console.log(`\n📝 测试完成时间: ${new Date().toLocaleString()}`);
        return results;
    }
}

// 主执行函数
async function main() {
    try {
        await TemplateSecurityTester.runAllTemplateTests();
    } catch (error) {
        console.log('\n💥 程序执行异常:', error.message);
        process.exit(1);
    }
}

// 执行测试
if (require.main === module) {
    main().catch(console.error);
}