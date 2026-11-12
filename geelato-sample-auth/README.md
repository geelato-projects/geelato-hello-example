# geelato-sample-auth —— 接入认证中心统一权限的 Demo（远程客户端）

一个**完全不依赖 Sa-Token、不共享 Sa-Token 会话/Redis 会话**的 Spring Boot 示例，作为独立认证中心（`geelato-auth-server`）的客户端，演示**功能权限 + 数据权限**两层控制。

## 它演示了什么

| 层 | 机制 | 来源 |
|---|---|---|
| 令牌校验 | 调认证中心 `POST /oauth2/introspect`（RFC7662，按 token 缓存） | 认证中心 |
| 功能权限 | 自研 `@RequirePermission` / `@RequireRole` AOP，读 introspect 的 `permissions`/`roles` 声明 | introspect 扩展声明（认证中心 Phase 1） |
| 数据权限 | `FluentQueryFilterInjector` 在 ORM 查询前追加 `where`（`#currentUser.xxx#` 占位符替换） | `GET /api/provisioning/user/{userId}`（Bearer） |
| 实时失效 | 订阅 Redis `geelato:permission:change` 失效本地缓存 | 认证中心推送 |
| 容灾降级 | 认证中心不可达时读本地 `auth_perm_mirror` 镜像 | 本地 |

> 关键：demo 只用 HTTP + 标准协议接入，**无需引入 Sa-Token**。

## 前置条件

1. **启动认证中心** `geelato-auth-server`（默认 `http://localhost:9000`，需含 Phase 1：introspect 携带 `permissions`/`roles`、`/api/provisioning/**` 走 Bearer）。
2. **Redis**：与认证中心同实例（demo 订阅 `geelato:permission:change`）。默认 `localhost:6379`。
3. **注册 OAuth 客户端**：`client_id=sample-auth`、`system_code=sample-auth`（= appCode）、配置 secret；把 secret 填到 `application.properties` 的 `demo.auth-center.client-secret`。
4. **创建权限点**（认证中心管理端，appCode=`sample-auth`）：
   - 功能权限：`order:export`、`order:approve`。
   - 数据权限：**实体名必须填 `DemoOrder`**（与 `DemoOrderEntity` 的 `@Entity name` 一致）。可用内置范围：
     - `&myDept`（`dept_id=#currentUser.deptId#`）
     - `&myself`（`creator=#currentUser.userId#`）
     - `&myBusiness`（`bu_id=#currentUser.buId#`）
     - `&all`（`1=1`）
5. **分配权限**给测试角色/用户。
6. **对齐 demo 用户 id**：`demo_user.id` 需等于该测试用户在认证中心 `platform_user.id`，这样 `provisioning` 才能按 userId 解析其数据权限。
   - 修改 `DemoDataInitializer` 中 `u-alice` / `u-bob` 的 id 为真实 `platform_user.id`（或登录后用真实 id 覆盖表数据）。

## 构建与运行

```bash
# 在 geelato-hello-example 根目录
mvn -pl geelato-sample-auth -am package -DskipTests
java -jar geelato-sample-auth/target/geelato-sample-auth.jar
# 或直接 IDE 运行 SampleAuthApplication，监听 8084
```

## 配置（application.properties，示例工程直接写死）

```properties
demo.auth-center.base-url=http://localhost:9000
demo.auth-center.client-id=sample-auth
demo.auth-center.client-secret=          # 填认证中心注册的 secret
demo.auth-center.app-code=sample-auth
demo.auth-center.introspect-cache-seconds=30
demo.permission.data.enabled=true
```

## 端点

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/sample-auth/public` | 无 | 存活检查 |
| POST | `/api/sample-auth/login` | 无 | 便捷登录代理（转发认证中心，返回 accessToken） |
| GET | `/api/sample-auth/me` | Bearer | 回显当前用户 + functionPermissions(code)/roleCodes |
| GET | `/api/sample-auth/orders` | Bearer | **数据权限**：查询 demo_order，注入器按规则过滤；返回行 + 本次生效 where |
| GET | `/api/sample-auth/orders/export` | Bearer + `order:export` | **功能权限** |
| POST | `/api/sample-auth/orders/approve` | Bearer + `order:approve` | **功能权限** |
| GET | `/api/sample-auth/admin/dashboard` | Bearer + `admin` 角色 | **角色权限** |

## 验证（curl 序列）

```bash
# 1) 登录取 accessToken（用 demo 的登录代理，或直接调认证中心 /api/auth/login）
curl -X POST http://localhost:8084/api/sample-auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"<密码>"}'
# → data.accessToken

# 2) 当前用户与权限声明
curl http://localhost:8084/api/sample-auth/me \
  -H 'Authorization: Bearer <accessToken>'

# 3) 数据权限：授 alice 的 DemoOrder=&myDept → 只看到 dept-01 的订单
curl http://localhost:8084/api/sample-auth/orders \
  -H 'Authorization: Bearer <accessToken>'
# → appliedRule 含 "dept_id='dept-01'"，list 仅 dept-01 订单

# 4) 功能权限：未授 order:export → 403；认证中心分配后 → 200
curl http://localhost:8084/api/sample-auth/orders/export \
  -H 'Authorization: Bearer <accessToken>'

# 5) 实时失效：在认证中心把 alice 的数据权限从 &myDept 改为 &all
#    → demo 收到 Redis 通知失效缓存 → 下次 /orders 返回全部订单（秒级）
```

## 说明 / 排错

- **实体名**：数据权限规则的 `entity` 必须填 `DemoOrder`（`@Entity name`），大小写不敏感匹配。
- **未配置数据权限**：该实体不追加过滤 = 全可见（便于对比「配置前/后」）。
- **demo_user 未对齐**：provisioning 取不到该用户数据权限 → `/orders` 全可见；按前置条件 6 对齐后生效。
- **introspect 无 permissions/roles**：确认认证中心为 standalone `geelato-auth-server`（含 `UserPermissionService` 实现，用于富化）。
- **认证中心不可达**：demo 降级读 `auth_perm_mirror` 镜像，功能权限声明则取最近一次 introspect 缓存（TTL 30s 内）。
