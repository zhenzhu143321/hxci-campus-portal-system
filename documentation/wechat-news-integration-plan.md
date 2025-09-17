# 微信公众号新闻集成方案

> 📅 创建时间：2025-09-14
> 🔗 示例链接：https://mp.weixin.qq.com/s/z9oRJII1Iccuf_sl5EPIww
> 📌 集成目标：从微信公众号获取校园新闻内容

## 📋 技术方案分析

### 方案一：后端爬虫方案（推荐）

#### 实现思路
1. **后端定时爬取**
   - 使用Java爬虫库（Jsoup）定期爬取公众号文章
   - 解析HTML提取标题、摘要、封面图、发布时间
   - 存储到数据库中

2. **数据处理流程**
   ```
   定时任务 → 爬取公众号 → 解析HTML → 存储数据库 → API提供给前端
   ```

3. **技术栈**
   - Jsoup：HTML解析
   - Spring Schedule：定时任务
   - MySQL：数据存储

#### 示例代码
```java
@Service
public class WechatNewsService {

    @Scheduled(cron = "0 0 */2 * * ?") // 每2小时执行一次
    public void fetchWechatNews() {
        try {
            // 爬取微信文章
            Document doc = Jsoup.connect("https://mp.weixin.qq.com/s/xxx")
                .userAgent("Mozilla/5.0")
                .get();

            // 解析标题
            String title = doc.select("h1.rich_media_title").text();

            // 解析内容摘要
            String summary = doc.select("div.rich_media_content")
                .text()
                .substring(0, 100) + "...";

            // 解析封面图
            String coverImage = doc.select("meta[property=og:image]")
                .attr("content");

            // 保存到数据库
            NewsItem news = new NewsItem();
            news.setTitle(title);
            news.setSummary(summary);
            news.setImage(coverImage);
            news.setSource("哈信息官方公众号");
            news.setUrl(articleUrl);
            news.setPublishTime(new Date());

            newsRepository.save(news);

        } catch (Exception e) {
            log.error("爬取微信文章失败", e);
        }
    }
}
```

### 方案二：微信开放平台API（正规但复杂）

#### 实现思路
1. 注册微信开放平台账号
2. 申请公众号授权
3. 使用官方API获取文章列表
4. 需要公众号管理员配合

#### API调用示例
```java
// 获取素材列表
String url = "https://api.weixin.qq.com/cgi-bin/material/batchget_material";
Map<String, Object> params = new HashMap<>();
params.put("type", "news");
params.put("offset", 0);
params.put("count", 20);
```

### 方案三：RSS订阅（如果公众号支持）

#### 实现思路
1. 使用第三方服务（如WeRSS）生成RSS源
2. 后端解析RSS XML
3. 定期更新数据

### 方案四：手动维护（最简单）

#### 实现思路
1. 管理员手动复制公众号文章信息
2. 通过后台管理系统录入
3. 存储到数据库

---

## 🚀 推荐实施方案

### 第一阶段：静态数据展示（立即可用）

修改默认新闻数据，使用真实的公众号文章：

```typescript
// api/news.ts
getDefaultNews(): NewsItem[] {
  return [
    {
      id: 1,
      title: '哈尔滨信息工程学院2024年秋季学期开学典礼',
      publishTime: '2024-09-01',
      image: '/images/news/opening-ceremony.jpg', // 本地存储的图片
      source: '哈信息官方公众号',
      url: 'https://mp.weixin.qq.com/s/xxx', // 实际公众号链接
      summary: '金秋九月，我校隆重举行2024年秋季学期开学典礼...',
      category: '学校要闻'
    },
    {
      id: 2,
      title: '我校学子在全国大学生程序设计竞赛中获佳绩',
      publishTime: '2024-08-25',
      image: '/images/news/competition.jpg',
      source: '哈信息官方公众号',
      url: 'https://mp.weixin.qq.com/s/yyy',
      summary: '近日，第XX届全国大学生程序设计竞赛落下帷幕...',
      category: '竞赛成果'
    },
    // 更多真实新闻...
  ]
}
```

### 第二阶段：半自动更新（短期方案）

1. **创建新闻管理脚本**
```javascript
// scripts/update-news.js
const axios = require('axios');
const cheerio = require('cheerio');

async function fetchWechatArticle(url) {
  const response = await axios.get(url);
  const $ = cheerio.load(response.data);

  return {
    title: $('h1.rich_media_title').text().trim(),
    summary: $('.rich_media_content').text().substring(0, 100),
    image: $('meta[property="og:image"]').attr('content'),
    url: url,
    publishTime: new Date().toISOString().split('T')[0]
  };
}

// 批量更新新闻
const articles = [
  'https://mp.weixin.qq.com/s/z9oRJII1Iccuf_sl5EPIww',
  // 更多文章链接
];

articles.forEach(async (url) => {
  const news = await fetchWechatArticle(url);
  console.log(JSON.stringify(news, null, 2));
});
```

2. **定期运行脚本更新数据**

### 第三阶段：自动化系统（长期方案）

1. **数据库表设计**
```sql
CREATE TABLE campus_news (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  summary TEXT,
  content TEXT,
  image_url VARCHAR(500),
  source_url VARCHAR(500),
  source_name VARCHAR(100) DEFAULT '哈信息官方公众号',
  category VARCHAR(50),
  publish_time DATETIME,
  view_count INT DEFAULT 0,
  is_pinned BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_publish_time (publish_time DESC)
);
```

2. **后端API实现**
```java
@RestController
@RequestMapping("/admin-api/campus/news")
public class CampusNewsController {

    @GetMapping("/list")
    public CommonResult<Page<NewsItem>> getNewsList(
        @RequestParam(defaultValue = "1") Integer pageNo,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        // 从数据库获取新闻
        Page<NewsItem> news = newsService.getNewsList(pageNo, pageSize);
        return CommonResult.success(news);
    }

    @GetMapping("/{id}")
    public CommonResult<NewsItem> getNewsDetail(@PathVariable Long id) {
        NewsItem news = newsService.getNewsById(id);
        return CommonResult.success(news);
    }
}
```

---

## 🎯 快速实施步骤

### 立即可做（5分钟）
1. 修改 `api/news.ts` 中的 `getDefaultNews()` 方法
2. 添加3-5条真实的公众号文章信息
3. 下载文章封面图存储到本地

### 今天可完成（2小时）
1. 创建Node.js爬虫脚本
2. 爬取最新的10篇公众号文章
3. 生成JSON数据文件
4. 更新前端默认数据

### 本周可完成（2天）
1. 设计数据库表
2. 实现后端API
3. 创建定时任务
4. 测试自动更新流程

---

## ⚠️ 注意事项

### 法律合规
- 爬取公开内容用于教育目的通常是允许的
- 需注明内容来源
- 不要过于频繁爬取（建议间隔2小时以上）
- 遵守robots.txt规则

### 技术考虑
- 微信文章可能有防爬机制
- 图片可能有防盗链
- 需要处理文章更新和删除
- 考虑缓存策略

### 用户体验
- 点击新闻可跳转到原文
- 显示文章来源
- 保持更新频率（每天至少更新一次）

---

## 📊 效果预期

### 短期效果
- 展示真实的校园新闻
- 内容来源可靠
- 用户体验提升

### 长期价值
- 自动化内容更新
- 减少维护成本
- 信息及时性保证

---

**📝 文档维护**
- 最后更新：2025-09-14
- 维护者：Claude Code AI Assistant
- 状态：方案设计完成