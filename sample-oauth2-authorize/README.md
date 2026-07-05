# sample-oauth2-authorize

用于演示宿主应用通过 OAuth2 授权码模式跳转 `/oauth2/authorize`，回跳拿到 `code` 后调用后端接口 `POST /api/oauth2/login?code=...` 完成登录态落地。

## 运行

```bash
pnpm install
pnpm dev
```

启动后默认进入配置页：

- apiBase（必须）：宿主后端地址，例如 `http://localhost:8080`
- ssoAuthorizeUrl（推荐）：直接填写后端运行时 `.config` 中的 `ssoUrl`
  - 或者填写 authCenterOrigin + clientId 由示例自动拼接 `/oauth2/authorize`

确认后进入主页，点击“跳转授权登录”开始流程。

## 注意事项

- 认证中心必须允许本示例的 `redirect_uri`（页面配置页里会显示 computed 的 redirect_uri）。
- 若 `apiBase` 与示例前端非同源，后端需允许 CORS；否则浏览器会拦截接口调用。
