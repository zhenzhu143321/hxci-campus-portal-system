const { test, expect } = require('@playwright/test');

test('basic page navigation', async ({ page }) => {
  // 导航到示例网站
  await page.goto('https://example.com');
  
  // 验证页面标题
  await expect(page).toHaveTitle(/Example Domain/);
  
  // 截取页面截图
  await page.screenshot({ path: 'example.png' });
  
  // 验证页面内容
  const heading = page.locator('h1');
  await expect(heading).toContainText('Example Domain');
  
  console.log('✅ 无头浏览器自动化测试成功！');
});