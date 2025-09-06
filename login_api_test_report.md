# 🔍 Login API 完整测试报告

## 📋 测试概述
- **API 端点**: `https://work.greathiit.com/api/user/loginWai`
- **测试时间**: 2025-09-06 05:56:30 - 05:56:55 GMT
- **测试方法**: POST 请求
- **测试工具**: curl
- **测试账号**: 学生账号、教师账号、无效账号

---

## 🎯 测试结果详情

### 1️⃣ 学生账号测试 (2023010105)

#### 📤 请求信息
```bash
curl -X POST https://work.greathiit.com/api/user/loginWai \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" \
  -H "Origin: https://work.greathiit.com" \
  -H "Referer: https://work.greathiit.com/" \
  -d '{
    "userNumber": "2023010105",
    "password": "888888",
    "autoLogin": true,
    "provider": "account"
  }' \
  --connect-timeout 30 \
  --max-time 60 \
  -v
```

#### 📥 完整响应数据
```json
{
  "code": 0,
  "data": {
    "id": "23230129050231",
    "companyId": "10000001",
    "officeId": "01",
    "no": "2023010105",
    "schoolName": "江北校区",
    "name": "顾春琳",
    "email": "2023010105@hrbiit.edu.cn",
    "phone": "15846029850",
    "mobile": null,
    "role": ["student"],
    "photo": null,
    "token": "557b76cd-ef17-4360-8a00-06a1b78b2656",
    "grade": "2023",
    "teacherStatus": null,
    "className": "软件23M01"
  },
  "message": "ok"
}
```

#### 🔗 完整curl详细输出
```
Note: Unnecessary use of -X or --request, POST is already inferred.
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 111.40.50.138:443...
* Connected to work.greathiit.com (111.40.50.138) port 443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
*  CAfile: /etc/ssl/certs/ca-certificates.crt
*  CApath: /etc/ssl/certs
* TLSv1.0 (OUT), TLS header, Certificate Status (22):
} [5 bytes data]
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
} [512 bytes data]
* TLSv1.2 (IN), TLS header, Certificate Status (22):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Server hello (2):
{ [122 bytes data]
* TLSv1.2 (IN), TLS header, Finished (20):
{ [5 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
{ [19 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Certificate (11):
{ [4591 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
{ [264 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Finished (20):
{ [52 bytes data]
* TLSv1.2 (OUT), TLS header, Finished (20):
} [5 bytes data]
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
} [1 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.3 (OUT), TLS handshake, Finished (20):
} [52 bytes data]
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* ALPN, server accepted to use h2
* Server certificate:
*  subject: CN=*.greathiit.com
*  start date: Feb 17 00:00:00 2025 GMT
*  expire date: Mar 20 23:59:59 2026 GMT
*  subjectAltName: host "work.greathiit.com" matched cert's "*.greathiit.com"
*  issuer: C=GB; ST=Greater Manchester; L=Salford; O=Sectigo Limited; CN=Sectigo RSA Domain Validation Secure Server CA
*  SSL certificate verify ok.
* Using HTTP2, server supports multiplexing
* Connection state changed (HTTP/2 confirmed)
* Copying HTTP/2 data in stream buffer to connection buffer after upgrade: len=0
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* Using Stream ID: 1 (easy handle 0x55b5927859f0)
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
> POST /api/user/loginWai HTTP/2
> Host: work.greathiit.com
> content-type: application/json
> accept: application/json
> user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36
> origin: https://work.greathiit.com
> referer: https://work.greathiit.com/
> content-length: 112
> 
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* We are completely uploaded and fine
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
{ [265 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
{ [265 bytes data]
* old SSL session ID is stale, removing
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* Connection state changed (MAX_CONCURRENT_STREAMS == 128)!
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
< HTTP/2 200 
< server: nginx
< date: Sat, 06 Sep 2025 05:56:30 GMT
< content-type: application/json
< vary: Accept-Encoding
< vary: Origin
< vary: Access-Control-Request-Method
< vary: Access-Control-Request-Headers
< access-control-allow-origin: https://work.greathiit.com
< access-control-expose-headers: *
< access-control-allow-credentials: true
< set-cookie: satoken=557b76cd-ef17-4360-8a00-06a1b78b2656; Max-Age=2592000; Expires=Mon, 6 Oct 2025 05:56:30 GMT; Path=/
< set-cookie: SESSION=YTBhZTU4NTEtZTYxYy00ZjQwLWE4YmQtZTFiMDlhOTNmNzA5; Max-Age=2592000; Expires=Mon, 6 Oct 2025 05:56:30 GMT; Path=/api; HttpOnly; SameSite=Lax
< cache-control: no-cache
< 
{ [372 bytes data]
100   484    0   372  100   112   1204    362 --:--:-- --:--:-- --:--:--  1566100   484    0   372  100   112   1204    362 --:--:-- --:--:-- --:--:--  1566
* Connection #0 to host work.greathiit.com left intact
```

---

### 2️⃣ 教师账号测试 (10031)

#### 📤 请求信息
```bash
curl -X POST https://work.greathiit.com/api/user/loginWai \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" \
  -H "Origin: https://work.greathiit.com" \
  -H "Referer: https://work.greathiit.com/" \
  -d '{
    "userNumber": "10031",
    "password": "888888",
    "autoLogin": true,
    "provider": "account"
  }' \
  --connect-timeout 30 \
  --max-time 60 \
  -v
```

#### 📥 完整响应数据
```json
{
  "code": 0,
  "data": {
    "id": "10031",
    "companyId": "10000001",
    "officeId": "90000022",
    "no": "10031",
    "schoolName": null,
    "name": "顾国欣",
    "email": "hxciguguoxin@126.com",
    "phone": "15945931099",
    "mobile": "15945931099",
    "role": ["teacher", "zaizhi", "listen_admin"],
    "photo": null,
    "token": "e3d31b31-d182-4829-b60c-d32b49217312",
    "grade": null,
    "teacherStatus": null,
    "className": null
  },
  "message": "ok"
}
```

#### 🔗 完整curl详细输出
```
Note: Unnecessary use of -X or --request, POST is already inferred.
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 111.40.50.138:443...
* Connected to work.greathiit.com (111.40.50.138) port 443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
*  CAfile: /etc/ssl/certs/ca-certificates.crt
*  CApath: /etc/ssl/certs
* TLSv1.0 (OUT), TLS header, Certificate Status (22):
} [5 bytes data]
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
} [512 bytes data]
* TLSv1.2 (IN), TLS header, Certificate Status (22):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Server hello (2):
{ [122 bytes data]
* TLSv1.2 (IN), TLS header, Finished (20):
{ [5 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
{ [19 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Certificate (11):
{ [4591 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
{ [264 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Finished (20):
{ [52 bytes data]
* TLSv1.2 (OUT), TLS header, Finished (20):
} [5 bytes data]
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
} [1 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.3 (OUT), TLS handshake, Finished (20):
} [52 bytes data]
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* ALPN, server accepted to use h2
* Server certificate:
*  subject: CN=*.greathiit.com
*  start date: Feb 17 00:00:00 2025 GMT
*  expire date: Mar 20 23:59:59 2026 GMT
*  subjectAltName: host "work.greathiit.com" matched cert's "*.greathiit.com"
*  issuer: C=GB; ST=Greater Manchester; L=Salford; O=Sectigo Limited; CN=Sectigo RSA Domain Validation Secure Server CA
*  SSL certificate verify ok.
* Using HTTP2, server supports multiplexing
* Connection state changed (HTTP/2 confirmed)
* Copying HTTP/2 data in stream buffer to connection buffer after upgrade: len=0
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* Using Stream ID: 1 (easy handle 0x558cc75b29f0)
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
> POST /api/user/loginWai HTTP/2
> Host: work.greathiit.com
> content-type: application/json
> accept: application/json
> user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36
> origin: https://work.greathiit.com
> referer: https://work.greathiit.com/
> content-length: 107
> 
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* We are completely uploaded and fine
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
{ [265 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
{ [265 bytes data]
* old SSL session ID is stale, removing
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* Connection state changed (MAX_CONCURRENT_STREAMS == 128)!
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
< HTTP/2 200 
< server: nginx
< date: Sat, 06 Sep 2025 05:56:42 GMT
< content-type: application/json
< vary: Accept-Encoding
< vary: Origin
< vary: Access-Control-Request-Method
< vary: Access-Control-Request-Headers
< access-control-allow-origin: https://work.greathiit.com
< access-control-expose-headers: *
< access-control-allow-credentials: true
< set-cookie: satoken=e3d31b31-d182-4829-b60c-d32b49217312; Max-Age=2592000; Expires=Mon, 6 Oct 2025 05:56:42 GMT; Path=/
< set-cookie: SESSION=NWU3Zjk2M2UtZDQ2Ni00Y2Y1LWFkNWItZDZjNzVlMDAwYWUw; Max-Age=2592000; Expires=Mon, 6 Oct 2025 05:56:42 GMT; Path=/api; HttpOnly; SameSite=Lax
< cache-control: no-cache
< 
{ [372 bytes data]
100   479    0   372  100   107    591    170 --:--:-- --:--:-- --:--:--   762
* Connection #0 to host work.greathiit.com left intact
```

---

### 3️⃣ 无效账号测试 (invalid_user)

#### 📤 请求信息
```bash
curl -X POST https://work.greathiit.com/api/user/loginWai \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" \
  -H "Origin: https://work.greathiit.com" \
  -H "Referer: https://work.greathiit.com/" \
  -d '{
    "userNumber": "invalid_user",
    "password": "wrong_password",
    "autoLogin": true,
    "provider": "account"
  }' \
  --connect-timeout 30 \
  --max-time 60 \
  -v
```

#### 📥 完整响应数据
```json
{
  "code": 40200,
  "data": null,
  "message": "密码错误"
}
```

#### 🔗 完整curl详细输出
```
Note: Unnecessary use of -X or --request, POST is already inferred.
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 111.40.50.138:443...
* Connected to work.greathiit.com (111.40.50.138) port 443 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
*  CAfile: /etc/ssl/certs/ca-certificates.crt
*  CApath: /etc/ssl/certs
* TLSv1.0 (OUT), TLS header, Certificate Status (22):
} [5 bytes data]
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
} [512 bytes data]
* TLSv1.2 (IN), TLS header, Certificate Status (22):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Server hello (2):
{ [122 bytes data]
* TLSv1.2 (IN), TLS header, Finished (20):
{ [5 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
{ [19 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Certificate (11):
{ [4591 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
{ [264 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Finished (20):
{ [52 bytes data]
* TLSv1.2 (OUT), TLS header, Finished (20):
} [5 bytes data]
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
} [1 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.3 (OUT), TLS handshake, Finished (20):
} [52 bytes data]
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* ALPN, server accepted to use h2
* Server certificate:
*  subject: CN=*.greathiit.com
*  start date: Feb 17 00:00:00 2025 GMT
*  expire date: Mar 20 23:59:59 2026 GMT
*  subjectAltName: host "work.greathiit.com" matched cert's "*.greathiit.com"
*  issuer: C=GB; ST=Greater Manchester; L=Salford; O=Sectigo Limited; CN=Sectigo RSA Domain Validation Secure Server CA
*  SSL certificate verify ok.
* Using HTTP2, server supports multiplexing
* Connection state changed (HTTP/2 confirmed)
* Copying HTTP/2 data in stream buffer to connection buffer after upgrade: len=0
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* Using Stream ID: 1 (easy handle 0x55f3001f09f0)
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
> POST /api/user/loginWai HTTP/2
> Host: work.greathiit.com
> content-type: application/json
> accept: application/json
> user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36
> origin: https://work.greathiit.com
> referer: https://work.greathiit.com/
> content-length: 122
> 
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
* We are completely uploaded and fine
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
{ [265 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
{ [265 bytes data]
* old SSL session ID is stale, removing
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* Connection state changed (MAX_CONCURRENT_STREAMS == 128)!
* TLSv1.2 (OUT), TLS header, Supplemental data (23):
} [5 bytes data]
100   122    0     0  100   122      0    545 --:--:-- --:--:-- --:--:--   544* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
* TLSv1.2 (IN), TLS header, Supplemental data (23):
{ [5 bytes data]
< HTTP/2 200 
< server: nginx
< date: Sat, 06 Sep 2025 05:56:55 GMT
< content-type: application/json
< vary: Accept-Encoding
< vary: Origin
< vary: Access-Control-Request-Method
< vary: Access-Control-Request-Headers
< access-control-allow-origin: https://work.greathiit.com
< access-control-expose-headers: *
< access-control-allow-credentials: true
< set-cookie: SESSION=OWI2OWM4YjAtMDIzMC00OTg4LWI2MTItZDI3MmQ2NzM3NDM2; Max-Age=2592000; Expires=Mon, 6 Oct 2025 05:56:55 GMT; Path=/api; HttpOnly; SameSite=Lax
< cache-control: no-cache
< 
{ [51 bytes data]
100   173    0    51  100   122    155    371 --:--:-- --:--:-- --:--:--   525
* Connection #0 to host work.greathiit.com left blank
```

---

## 📊 数据结构分析

### ✅ 成功响应字段结构 (code: 0)

| 字段 | 学生账号值 | 教师账号值 | 数据类型 | 说明 |
|------|------------|------------|----------|------|
| `code` | 0 | 0 | Number | 成功状态码 |
| `message` | "ok" | "ok" | String | 响应消息 |
| `data.id` | "23230129050231" | "10031" | String | 用户唯一标识 |
| `data.companyId` | "10000001" | "10000001" | String | 公司ID |
| `data.officeId` | "01" | "90000022" | String | 部门ID |
| `data.no` | "2023010105" | "10031" | String | 用户编号 |
| `data.schoolName` | "江北校区" | null | String/Null | 校区名称 |
| `data.name` | "顾春琳" | "顾国欣" | String | 用户姓名 |
| `data.email` | "2023010105@hrbiit.edu.cn" | "hxciguguoxin@126.com" | String | 邮箱地址 |
| `data.phone` | "15846029850" | "15945931099" | String | 联系电话 |
| `data.mobile` | null | "15945931099" | String/Null | 手机号码 |
| `data.role` | ["student"] | ["teacher", "zaizhi", "listen_admin"] | Array | 角色数组 |
| `data.photo` | null | null | Null | 头像信息 |
| `data.token` | "557b76cd-ef17-4360-8a00-06a1b78b2656" | "e3d31b31-d182-4829-b60c-d32b49217312" | String | UUID格式认证令牌 |
| `data.grade` | "2023" | null | String/Null | 年级信息 |
| `data.teacherStatus` | null | null | Null | 教师状态 |
| `data.className` | "软件23M01" | null | String/Null | 班级名称 |

### ❌ 错误响应字段结构 (code: 40200)

| 字段 | 值 | 数据类型 | 说明 |
|------|-----|----------|------|
| `code` | 40200 | Number | 错误状态码 |
| `data` | null | Null | 无数据返回 |
| `message` | "密码错误" | String | 中文错误描述 |

---

## 🔐 安全信息分析

### SSL/TLS 连接详情
- **服务器IP地址**: 111.40.50.138:443
- **SSL证书主题**: CN=*.greathiit.com
- **证书有效期**: Feb 17 00:00:00 2025 GMT - Mar 20 23:59:59 2026 GMT
- **证书颁发者**: C=GB; ST=Greater Manchester; L=Salford; O=Sectigo Limited; CN=Sectigo RSA Domain Validation Secure Server CA
- **SSL协议**: TLSv1.3 / TLS_AES_256_GCM_SHA384
- **ALPN协商**: HTTP/2
- **证书验证状态**: SSL certificate verify ok

### Cookie 设置详情

#### 成功登录时的Cookie设置：

**学生账号Cookie:**
1. **satoken**: `557b76cd-ef17-4360-8a00-06a1b78b2656`
   - Max-Age: 2592000秒 (30天)
   - Expires: Mon, 6 Oct 2025 05:56:30 GMT
   - Path: /

2. **SESSION**: `YTBhZTU4NTEtZTYxYy00ZjQwLWE4YmQtZTFiMDlhOTNmNzA5`
   - Max-Age: 2592000秒 (30天)
   - Expires: Mon, 6 Oct 2025 05:56:30 GMT
   - Path: /api
   - HttpOnly: true
   - SameSite: Lax

**教师账号Cookie:**
1. **satoken**: `e3d31b31-d182-4829-b60c-d32b49217312`
   - Max-Age: 2592000秒 (30天)
   - Expires: Mon, 6 Oct 2025 05:56:42 GMT
   - Path: /

2. **SESSION**: `NWU3Zjk2M2UtZDQ2Ni00Y2Y1LWFkNWItZDZjNzVlMDAwYWUw`
   - Max-Age: 2592000秒 (30天)
   - Expires: Mon, 6 Oct 2025 05:56:42 GMT
   - Path: /api
   - HttpOnly: true
   - SameSite: Lax

**失败登录时的Cookie设置:**
仅设置SESSION Cookie: `OWI2OWM4YjAtMDIzMC00OTg4LWI2MTItZDI3MmQ2NzM3NDM2`，无satoken Cookie

---

## 📈 性能指标详情

### 数据传输统计

| 测试场景 | 总字节数 | 接收字节 | 发送字节 | 平均下载速度 | 平均上传速度 | 总耗时 |
|----------|----------|----------|----------|--------------|--------------|--------|
| 学生登录 | 484 | 372 | 112 | 1204 字节/秒 | 362 字节/秒 | 1566ms |
| 教师登录 | 479 | 372 | 107 | 591 字节/秒 | 170 字节/秒 | 762ms |
| 错误登录 | 173 | 51 | 122 | 155 字节/秒 | 371 字节/秒 | 525ms |

### 连接建立详情
- **协议支持**: HTTP/2, HTTP/1.1 (ALPN协商)
- **TLS握手**: TLSv1.3完整握手过程
- **连接复用**: HTTP/2 Stream ID机制
- **最大并发流**: 128 (MAX_CONCURRENT_STREAMS)

---

## 🔍 HTTP 请求/响应头详情

### 通用请求头
```
POST /api/user/loginWai HTTP/2
Host: work.greathiit.com
content-type: application/json
accept: application/json
user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36
origin: https://work.greathiit.com
referer: https://work.greathiit.com/
content-length: [变化值: 112/107/122]
```

### 通用响应头
```
HTTP/2 200
server: nginx
date: [对应时间戳]
content-type: application/json
vary: Accept-Encoding
vary: Origin
vary: Access-Control-Request-Method
vary: Access-Control-Request-Headers
access-control-allow-origin: https://work.greathiit.com
access-control-expose-headers: *
access-control-allow-credentials: true
cache-control: no-cache
```

---

## 💡 关键发现与结论

### API 行为特征
1. **统一响应格式**: 所有响应都遵循 `{code, data, message}` 结构
2. **角色区分明确**: 学生和教师账号返回不同的数据字段
3. **认证令牌机制**: 成功登录返回UUID格式的token
4. **双重Cookie设置**: satoken用于认证，SESSION用于会话管理
5. **CORS配置完善**: 支持跨域请求，设置了适当的CORS头

### 安全性评估
1. **HTTPS强制**: 使用TLSv1.3加密传输
2. **Cookie安全**: SESSION设置了HttpOnly和SameSite属性
3. **令牌有效期**: 30天过期时间设置
4. **错误信息**: 提供适当的错误反馈，不暴露敏感信息

### 性能表现
1. **响应速度**: 教师登录最快(762ms)，学生登录较慢(1566ms)
2. **数据传输**: 成功登录返回372字节数据，失败仅51字节
3. **连接复用**: 支持HTTP/2多路复用，提高传输效率

---

## 📝 附录

### 测试环境信息
- **操作系统**: Linux 5.15.0-117-generic
- **curl版本**: 支持HTTP/2和TLSv1.3
- **CA证书路径**: /etc/ssl/certs/ca-certificates.crt
- **测试日期**: 2025年9月6日
- **GMT时区**: UTC+0

### 错误码说明
- **0**: 登录成功
- **40200**: 密码错误（可能也包括用户不存在等认证失败情况）

### 数据字段说明
- **id**: 用户在系统中的唯一标识符
- **companyId**: 公司/机构标识，所有用户都是"10000001"
- **officeId**: 部门标识，学生为"01"，教师为"90000022"
- **no**: 用户编号，与userNumber对应
- **role**: 用户角色数组，学生为["student"]，教师包含多个角色
- **token**: 用于后续API调用的认证令牌
- **grade**: 年级信息，仅学生有值
- **className**: 班级信息，仅学生有值

---

*报告生成时间: 2025-09-06*
*测试工具: curl with verbose output*
*报告格式: Markdown*