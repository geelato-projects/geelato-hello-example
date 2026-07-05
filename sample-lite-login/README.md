# sample-lite-login

用于演示第三方应用以 iframe + popup 两种方式集成 `lite-login`（`gl-lite-sso` 门面），并通过 `postMessage` 接收 `LOGIN_SUCCESS(accessToken, ...)`。

## 运行

```bash
pnpm install
pnpm dev
```

启动后默认进入配置页，填写：

- liteLoginBaseUrl：`http://host/lite-login`（宿主容纳模式）或 `http://host/login`（独立 gl-lite-sso）
- liteSsoOrigin：`http://host`

确认后进入演示页。

## 对接真实 lite-sso 的注意事项

- `gl-lite-sso` 会基于 `VITE_TRUSTED_ORIGINS` 控制可信来源域名；需要把本示例的 origin 加入白名单，否则可能收不到 `LOGIN_SUCCESS` 回传消息。
