# 涓枃鏄剧ず娴嬭瘯鑴氭湰
# 鐢ㄤ簬楠岃瘉PowerShell UTF-8缂栫爜閰嶇疆鏄惁姝ｇ‘

Write-Host "馃嚚馃嚦 涓枃鏄剧ず娴嬭瘯寮€濮?.." -ForegroundColor Cyan

# 娴嬭瘯1: 鍩烘湰涓枃鏄剧ず
Write-Host "馃摑 娴嬭瘯1: 鍩烘湰涓枃瀛楃鏄剧ず" -ForegroundColor Yellow
Write-Host "鍝堝皵婊ㄤ俊鎭伐绋嬪闄㈡牎鍥棬鎴风郴缁? -ForegroundColor Green
Write-Host "瀛︾敓濮撳悕锛氬紶涓夈€佹潕鍥涖€佺帇浜? -ForegroundColor Blue
Write-Host "閫氱煡鍐呭锛氫粖鏃ヤ笅鍗?鐐瑰湪涓绘ゼ101鏁欏鍙紑瀛︾敓浼氳" -ForegroundColor Magenta

# 娴嬭瘯2: JSON鏍煎紡涓枃鏁版嵁
Write-Host "
馃摑 娴嬭瘯2: JSON鏍煎紡涓枃鏁版嵁" -ForegroundColor Yellow
$testJson = @{
    "鏍″悕" = "鍝堝皵婊ㄤ俊鎭伐绋嬪闄?
    "閫氱煡鏍囬" = "鏈熸湯鑰冭瘯瀹夋帓閫氱煡"
    "閫氱煡鍐呭" = "鍚勪綅鍚屽璇锋敞鎰忥紝鏈熸湯鑰冭瘯灏嗕簬涓嬪懆寮€濮嬶紝璇峰仛濂藉涔犲噯澶囥€?
    "鍙戝竷鑰? = "鏁欏姟澶?
} | ConvertTo-Json -Depth 3

Write-Host $testJson -ForegroundColor White

# 娴嬭瘯3: 鐗规畩涓枃瀛楃
Write-Host "
馃摑 娴嬭瘯3: 鐗规畩涓枃瀛楃鍜岀鍙? -ForegroundColor Yellow
Write-Host "鐗规畩瀛楃锛氣憼鈶♀憿鈶ｂ懁 銆侊紝銆傦紱锛氾紵锛? -ForegroundColor Cyan
Write-Host "璐у竵绗﹀彿锛氾骏 搴﹂噺鍗曚綅锛氣剝 鈩?銕?銕? -ForegroundColor Cyan

# 娴嬭瘯4: 缂栫爜淇℃伅鏄剧ず
Write-Host "
馃摑 娴嬭瘯4: 褰撳墠缂栫爜璁剧疆" -ForegroundColor Yellow
Write-Host "杈撳嚭缂栫爜: $($OutputEncoding.EncodingName)" -ForegroundColor Green
Write-Host "鎺у埗鍙拌緭鍑虹紪鐮? $([Console]::OutputEncoding.EncodingName)" -ForegroundColor Green
Write-Host "鎺у埗鍙拌緭鍏ョ紪鐮? $([Console]::InputEncoding.EncodingName)" -ForegroundColor Green

Write-Host "
鉁?涓枃鏄剧ず娴嬭瘯瀹屾垚锛? -ForegroundColor Green
Write-Host "濡傛灉浠ヤ笂涓枃瀛楃鏄剧ず姝ｅ父锛岃鏄嶶TF-8缂栫爜閰嶇疆鎴愬姛銆? -ForegroundColor Cyan
