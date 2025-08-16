-- MySQL数据库初始化脚本 - 完整版
-- 兼容yudao-boot-mini框架

-- 系统用户表（简化）
CREATE TABLE IF NOT EXISTS system_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    password VARCHAR(100),
    nickname VARCHAR(30),
    status TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试用户
INSERT IGNORE INTO system_users (id, username, password, nickname) VALUES 
(1, 'admin', '$2a$10$7JB720yubVSOfvVWvJRjQe1KkCaDOPY8B7vF28eSbuxj0RUvnwP8a', '管理员'),
(2, 'demo', '$2a$10$7JB720yubVSOfvVWvJRjQe1KkCaDOPY8B7vF28eSbuxj0RUvnwP8a', '演示用户');

-- 通知分类表
CREATE TABLE IF NOT EXISTS notification_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(50) NOT NULL,
  description VARCHAR(200),
  icon VARCHAR(100),
  color VARCHAR(10),
  parent_id BIGINT DEFAULT 0,
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  creator VARCHAR(64) DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT 0,
  INDEX idx_code (code),
  INDEX idx_parent_id (parent_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知分类表';

-- 通知信息表
CREATE TABLE IF NOT EXISTS notification_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  summary VARCHAR(500),
  level TINYINT NOT NULL DEFAULT 3,
  status TINYINT NOT NULL DEFAULT 1,
  category_id BIGINT NOT NULL,
  publisher_id BIGINT NOT NULL,
  publisher_name VARCHAR(50) NOT NULL,
  publisher_role VARCHAR(30),
  scheduled_time DATETIME,
  expired_time DATETIME,
  push_channels JSON,
  require_confirm TINYINT DEFAULT 0,
  pinned TINYINT DEFAULT 0,
  push_count INT DEFAULT 0,
  read_count INT DEFAULT 0,
  confirm_count INT DEFAULT 0,
  creator VARCHAR(64) DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT 0,
  INDEX idx_category_id (category_id),
  INDEX idx_level (level),
  INDEX idx_status (status),
  INDEX idx_publisher (publisher_id),
  INDEX idx_scheduled_time (scheduled_time),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知信息表';

-- 插入通知分类示例数据
INSERT IGNORE INTO notification_category (id, tenant_id, name, code, description, icon, color, parent_id, sort, creator, updater) VALUES
(1, 1, '系统通知', 'SYSTEM', '系统维护、更新等通知', 'ep:setting', '#909399', 0, 1, 'admin', 'admin'),
(2, 1, '教务通知', 'ACADEMIC', '课程、考试等教务相关通知', 'ep:reading', '#409EFF', 0, 2, 'admin', 'admin'),
(3, 1, '财务通知', 'FINANCE', '学费、奖学金等财务通知', 'ep:money', '#67C23A', 0, 3, 'admin', 'admin'),
(4, 1, '活动通知', 'ACTIVITY', '校园活动、社团活动通知', 'ep:trophy', '#E6A23C', 0, 4, 'admin', 'admin'),
(5, 1, '安全通知', 'SECURITY', '校园安全相关通知', 'ep:warning-filled', '#F56C6C', 0, 5, 'admin', 'admin');

-- 插入示例通知数据  
INSERT IGNORE INTO notification_info (id, tenant_id, title, content, summary, level, status, category_id, publisher_id, publisher_name, publisher_role, scheduled_time, expired_time, push_channels, require_confirm, pinned, push_count, read_count, confirm_count, creator, updater) VALUES
(1, 1, '【重要】智能通知系统正式上线', '尊敬的用户：\n\n我们的智能通知系统已正式上线！该系统支持四级通知分类、多渠道推送、审批流程等功能。\n\n主要特性：\n1. 四级通知分类：紧急、重要、常规、提醒\n2. 多渠道推送：APP、短信、邮件、微信、系统通知\n3. 完整审批流程：草稿→审批→发布→归档\n4. 统计分析：推送量、阅读量、确认率统计\n\n感谢您的使用！', '智能通知系统正式上线，支持多渠道推送和审批流程', 2, 4, 1, 1, '系统管理员', 'ADMIN', NULL, DATE_ADD(NOW(), INTERVAL 30 DAY), '[1,2,3,5]', 1, 1, 1000, 850, 720, 'admin', 'admin'),

(2, 1, '【紧急】系统维护通知', '各位用户：\n\n系统将于今晚23:00-01:00进行维护升级，期间服务可能中断。请提前保存工作内容。\n\n维护内容：\n- 数据库优化\n- 新功能上线\n- 安全补丁更新\n\n如有问题请联系技术支持。', '系统维护通知，今晚23:00-01:00', 1, 4, 1, 1, '技术部门', 'TECH', NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), '[1,2,3,4,5]', 1, 1, 500, 495, 480, 'admin', 'admin'),

(3, 1, '期末考试安排通知', '各位同学：\n\n2024年春季学期期末考试安排如下：\n\n考试时间：6月15日-6月25日\n报名时间：5月20日-5月30日\n\n注意事项：\n1. 携带学生证和身份证\n2. 提前30分钟到达考场\n3. 严禁作弊\n\n祝考试顺利！', '期末考试安排，6月15-25日', 2, 4, 2, 2, '教务处', 'TEACHER', NULL, DATE_ADD(NOW(), INTERVAL 15 DAY), '[1,5]', 1, 0, 800, 650, 520, 'admin', 'admin'),

(4, 1, '学费缴费提醒', '各位学生：\n\n本学期学费缴费截止日期为本月底，请尚未缴费的同学尽快缴费。\n\n缴费方式：\n1. 在线支付平台\n2. 银行转账\n3. 现金缴费（财务处）\n\n逾期将产生滞纳金。', '学费缴费提醒，本月底截止', 2, 4, 3, 3, '财务处', 'FINANCE', NULL, DATE_ADD(NOW(), INTERVAL 7 DAY), '[1,2,3]', 1, 0, 1200, 800, 600, 'admin', 'admin'),

(5, 1, '校园文化节活动预告', '亲爱的同学们：\n\n一年一度的校园文化节即将拉开帷幕！\n\n活动时间：5月1日-5月7日\n活动地点：学校广场\n\n精彩活动：\n- 文艺演出\n- 社团展示\n- 美食节\n- 游戏互动\n\n期待您的参与！', '校园文化节活动预告，5月1-7日', 3, 4, 4, 4, '学生会', 'STUDENT', NULL, DATE_ADD(NOW(), INTERVAL 20 DAY), '[1,4,5]', 0, 1, 600, 480, 0, 'admin', 'admin'),

(6, 1, '上课提醒：高等数学', '提醒：\n\n您的高等数学课程将于30分钟后开始。\n\n时间：14:30-16:10\n地点：教学楼A203\n任课教师：张教授\n\n请提前到达教室。', '高等数学课程提醒', 4, 4, 2, 5, '教务系统', 'SYSTEM', NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), '[1,5]', 0, 0, 150, 145, 0, 'system', 'system');

SELECT '数据库初始化完成' as Status;
SELECT COUNT(*) as Category_Count FROM notification_category;
SELECT COUNT(*) as Notification_Count FROM notification_info;