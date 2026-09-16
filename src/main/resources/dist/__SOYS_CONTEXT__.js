// SOYS ContractInjector 自动契约文件（@soys 契约 v2）
// 伺服时把占位符 __SOYS_CONTEXT__ 替换为服务器环境原语 JSON：
//   { "scheme":"http|https", "host":"...", "port":8080,
//     "apiPrefix":"/api", "pluginsPrefix":"/plugins/MCER", "fullPrefix":"/api/plugins/MCER",
//     "pagePrefix":"/plugins/MCER", "webResourcePrefix":"web/plugins/MCER/page/" }
// 换服务器 / 改 api-prefix 后无需改动任何前端代码，重新请求即自动适配。
window.SOYS_CONTEXT = typeof __SOYS_CONTEXT__ !== 'undefined' ? __SOYS_CONTEXT__ : null;

// HTML 资源引用改写排除列表（前缀匹配）：
// /api 为 SOYS API 保留前缀（默认已排除）；/prod-api 为应用 API 前缀，必须显式声明，
// 避免 index.html 中的 API 请求被改写为插件资源路径。
window.SOYS_CONTEXT_EXCLUDES = ["/prod-api", "/api"];
