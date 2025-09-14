#!/bin/bash

# 通知系统localStorage缓存清理工具函数
# 用于解决数据库重置后ID重用导致的前端缓存冲突问题
# 作者: Claude AI Assistant
# 日期: 2025-09-13

# 生成localStorage缓存清理HTML页面
generate_cache_clear_page() {
    local clear_page_path="/tmp/clear-notification-cache.html"

    cat > "$clear_page_path" << 'EOF'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>🧹 通知缓存清理页面</title>
    <style>
        body {
            font-family: 'Microsoft YaHei', Arial, sans-serif;
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 10px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
        }
        .container {
            text-align: center;
            background: rgba(255,255,255,0.1);
            padding: 30px;
            border-radius: 10px;
            backdrop-filter: blur(10px);
        }
        h1 { margin-bottom: 20px; font-size: 2em; }
        .status {
            font-size: 1.2em;
            margin: 20px 0;
            padding: 15px;
            border-radius: 5px;
            background: rgba(255,255,255,0.2);
        }
        .success { color: #4CAF50; font-weight: bold; }
        .link {
            display: inline-block;
            padding: 12px 25px;
            background: #4CAF50;
            color: white;
            text-decoration: none;
            border-radius: 25px;
            margin: 10px;
            transition: all 0.3s;
        }
        .link:hover {
            background: #45a049;
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
        }
        .details {
            margin-top: 20px;
            padding: 15px;
            background: rgba(0,0,0,0.2);
            border-radius: 5px;
            font-size: 0.9em;
            text-align: left;
        }
        .progress {
            width: 100%;
            height: 6px;
            background: rgba(255,255,255,0.3);
            border-radius: 3px;
            margin: 15px 0;
            overflow: hidden;
        }
        .progress-bar {
            height: 100%;
            background: #4CAF50;
            width: 0%;
            transition: width 0.5s ease;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🧹 通知缓存清理页面</h1>
        <div class="status" id="status">
            📡 正在检测localStorage缓存状态...
        </div>

        <div class="progress">
            <div class="progress-bar" id="progressBar"></div>
        </div>

        <div class="details" id="details" style="display: none;">
            <h3>🔍 缓存清理详情：</h3>
            <ul id="clearList"></ul>
        </div>

        <div id="actionButtons" style="display: none;">
            <button class="link" onclick="window.close()">🔙 关闭页面</button>
            <button class="link" onclick="location.reload()">🔄 重新检测</button>
            <button class="link" onclick="showManualInstructions()">📋 手动清理指南</button>
        </div>

        <div id="manualInstructions" style="display: none; margin-top: 20px;">
            <div class="details">
                <h3>📋 手动清理localStorage指南：</h3>
                <ol style="text-align: left; margin: 15px 0;">
                    <li><strong>打开浏览器开发者工具</strong><br>
                        按 F12 键或右键页面 → "检查" → "Console"</li>
                    <li><strong>执行清理命令</strong><br>
                        复制以下命令到Console并按Enter：<br>
                        <code style="background: rgba(0,0,0,0.3); padding: 5px; border-radius: 3px; display: block; margin: 5px 0;">
                        localStorage.clear(); location.reload();
                        </code>
                    </li>
                    <li><strong>验证清理结果</strong><br>
                        刷新通知页面，检查新通知是否显示为"未读"状态</li>
                </ol>
            </div>
        </div>
    </div>

    <script>
        // 缓存清理主函数
        function clearNotificationCache() {
            const statusEl = document.getElementById('status');
            const progressBar = document.getElementById('progressBar');
            const detailsEl = document.getElementById('details');
            const clearListEl = document.getElementById('clearList');
            const actionButtonsEl = document.getElementById('actionButtons');

            // 第一步：检测现有缓存
            progressBar.style.width = '20%';
            statusEl.innerHTML = '🔍 正在扫描localStorage缓存...';

            setTimeout(() => {
                const notificationKeys = Object.keys(localStorage)
                    .filter(key => key.startsWith('notification_read_status_'));

                progressBar.style.width = '40%';
                statusEl.innerHTML = `📋 发现 ${notificationKeys.length} 个通知缓存项`;

                // 第二步：显示发现的缓存
                setTimeout(() => {
                    progressBar.style.width = '60%';
                    statusEl.innerHTML = '🧹 正在清理通知缓存...';

                    // 显示清理详情
                    detailsEl.style.display = 'block';

                    if (notificationKeys.length > 0) {
                        notificationKeys.forEach(key => {
                            const value = localStorage.getItem(key);
                            const listItem = document.createElement('li');
                            listItem.innerHTML = `<strong>${key}</strong>: ${value}`;
                            clearListEl.appendChild(listItem);

                            // 清理该缓存项
                            localStorage.removeItem(key);
                        });
                    } else {
                        const listItem = document.createElement('li');
                        listItem.innerHTML = '未发现需要清理的通知缓存';
                        clearListEl.appendChild(listItem);
                    }

                    // 第三步：完成清理
                    setTimeout(() => {
                        progressBar.style.width = '100%';
                        statusEl.innerHTML = `<span class="success">✅ 缓存清理完成！已清理 ${notificationKeys.length} 个缓存项</span>`;

                        // 显示操作按钮
                        actionButtonsEl.style.display = 'block';

                        // 额外清理：清理可能的其他相关缓存
                        const otherKeys = Object.keys(localStorage)
                            .filter(key => key.includes('notification') || key.includes('read'));

                        if (otherKeys.length > 0) {
                            const extraItem = document.createElement('li');
                            extraItem.innerHTML = `<em>额外清理了 ${otherKeys.length} 个相关缓存项</em>`;
                            clearListEl.appendChild(extraItem);

                            otherKeys.forEach(key => localStorage.removeItem(key));
                        }

                        // 显示完成状态，不自动跳转（因为用户可能在本地使用）
                        statusEl.innerHTML = `<span class="success">✅ 缓存清理完成！请手动返回通知页面查看效果</span>`;

                    }, 1000);
                }, 1000);
            }, 1000);
        }

        // 显示手动清理指南
        function showManualInstructions() {
            const manualEl = document.getElementById('manualInstructions');
            manualEl.style.display = manualEl.style.display === 'none' ? 'block' : 'none';
        }

        // 页面加载完成后自动开始清理
        document.addEventListener('DOMContentLoaded', function() {
            clearNotificationCache();
        });

        // 添加键盘快捷键支持
        document.addEventListener('keydown', function(event) {
            if (event.key === 'Enter' || event.key === ' ') {
                // 回车或空格键重新检测缓存
                location.reload();
            } else if (event.key === 'Escape') {
                // ESC键关闭页面
                window.close();
            }
        });
    </script>
</body>
</html>
EOF

    # 同时复制到项目demo目录，便于HTTP访问
    local demo_page_path="/opt/hxci-campus-portal/hxci-campus-portal-system/demo/clear-notification-cache.html"
    cp "$clear_page_path" "$demo_page_path" 2>/dev/null

    echo "🧹 localStorage缓存清理页面已生成："
    echo "📂 临时位置: $clear_page_path"
    echo "📂 项目位置: $demo_page_path"
    echo "🌐 文件访问: file://$demo_page_path"
    echo "🌐 HTTP访问: http://localhost:8099/clear-notification-cache.html"
    echo "   (需要先启动HTTP服务: scripts/notifications/cache_clear_server.sh)"
}

# 显示缓存清理提示信息
show_cache_clear_tips() {
    local notification_level="$1"
    local notification_id="$2"

    echo ""
    echo "🎉 Level $notification_level 通知发布成功！"
    echo "✅ 通知ID: $notification_id"
    echo ""
    echo "======================================="
    echo "⚠️  【重要提示】数据库重置检测"
    echo "======================================="
    echo "如果您之前清空过数据库并重置了auto_increment，"
    echo "新通知可能会由于ID重用而在前端显示为已读状态。"
    echo ""
    echo "📋 解决方案 - 请选择以下任一方式清理localStorage缓存："
    echo ""
    echo "🔧 方法1：浏览器开发者工具Console执行"
    echo "   localStorage.clear(); location.reload();"
    echo ""
    echo "🔧 方法2：精确清理通知缓存"
    echo "   Object.keys(localStorage)"
    echo "     .filter(key => key.startsWith('notification_read_status_'))"
    echo "     .forEach(key => localStorage.removeItem(key));"
    echo "   location.reload();"
    echo ""
    echo "🌐 方法3：访问自动清理页面（推荐）"

    # 生成清理页面
    generate_cache_clear_page

    echo ""
    echo "💡 清理完成后，请刷新页面验证新通知显示为未读状态"
    echo "======================================="
}

# 显示简化的缓存清理提示
show_simple_cache_tips() {
    echo ""
    echo "💡 提示：如果通知显示为已读状态，请清理浏览器localStorage缓存"
    echo "🔗 快速清理页面: file:///tmp/clear-notification-cache.html"
    echo ""
}

# 检查是否需要显示缓存清理提示
check_and_show_cache_tips() {
    local notification_id="$1"
    local notification_level="$2"

    # 如果通知ID较小（1-10），很可能是数据库重置后的新数据
    if [ "$notification_id" -le 10 ]; then
        show_cache_clear_tips "$notification_level" "$notification_id"
    else
        show_simple_cache_tips
    fi
}

# 导出函数供其他脚本使用
export -f generate_cache_clear_page
export -f show_cache_clear_tips
export -f show_simple_cache_tips
export -f check_and_show_cache_tips