const { chromium } = require('@playwright/test');

async function testHeadlessBrowser() {
  console.log('🚀 启动无头浏览器测试...');
  
  const browser = await chromium.launch({
    headless: true,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-extensions',
      '--disable-gpu',
      '--no-first-run',
      '--disable-web-security',
      '--disable-features=VizDisplayCompositor'
    ]
  });

  const page = await browser.newPage();
  
  try {
    console.log('📖 访问示例网站...');
    await page.goto('https://example.com', { waitUntil: 'networkidle' });
    
    console.log('📸 截取页面截图...');
    await page.screenshot({ path: 'test-screenshot.png', fullPage: true });
    
    const title = await page.title();
    console.log('✅ 页面标题:', title);
    
    const content = await page.textContent('body');
    console.log('✅ 页面内容长度:', content.length);
    
    console.log('🎉 无头浏览器测试成功！没有屏幕乱码问题！');
    
  } catch (error) {
    console.error('❌ 测试失败:', error.message);
  } finally {
    await browser.close();
  }
}

testHeadlessBrowser();