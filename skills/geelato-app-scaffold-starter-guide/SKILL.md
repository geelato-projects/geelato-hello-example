---
name: "geelato-app-scaffold-starter-guide"
description: "Guides creating Geelato business projects from `geelato-app-scaffold-starter`. Invoke when scaffolding a new Geelato project or setting up MQL CRUD, login, dict, org, user, or upload capabilities."
---

# Geelato App Scaffold Starter Guide

帮助基于 `cn.geelato:geelato-app-scaffold-starter` 从零搭建可运行的 Geelato 业务工程。本 Skill 提炼自 `app-scaffold-starter-project-guide.md`，聚焦“如何一次跑通 + 如何正确放业务代码”。

## 何时使用本 Skill

满足以下任一意图时优先调用本 Skill：

- 用户想“新建一个 Geelato 业务项目 / 脚手架项目”
- 用户想基于 `geelato-app-scaffold-starter` 起步一个 Spring Boot 工程
- 用户咨询“业务实体放哪里、建表脚本放哪里、如何用 MQL 做 CRUD”
- 用户需要复用脚手架内置的登录、字典、组织、用户、上传等能力
- 用户需要升级基于该 starter 的业务工程

## 核心边界（先于一切细节）

1. **业务归属**：业务实体、Controller、Service、业务 SQL 必须放在业务工程，**不要下沉到 starter**。
2. **升级入口**：业务工程通过升级 `geelato-framework-bom` 与 `geelato-app-scaffold-starter` 跟随框架升级，不要长期手工同步官方示例目录。
3. **决策次序**：单业务特性 → 业务工程；多业务复用 → 再评估下沉到 starter/runtime/framework。
4. **auto-init-tables 边界**：`geelato.app.scaffold.auto-init-tables=true` 仅适用于“首次建表”，不负责字段变更、类型变更、复杂迁移。

## 创建工程的两种方式

- **方式 A（推荐起步）**：直接复制 `geelato-app-scaffold` 作为起点，再改 `groupId/artifactId/version`、包名、`spring.application.name`、数据库配置。
- **方式 B（自主骨架）**：从空目录新建工程并引入 `geelato-app-scaffold-starter`。

## pom.xml 最小模板

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.acme</groupId>
    <artifactId>acme-order-center</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.0.0</spring-boot.version>
        <geelato.version>1.0.0-SNAPSHOT</geelato.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>cn.geelato</groupId>
                <artifactId>geelato-framework-bom</artifactId>
                <version>${geelato.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>cn.geelato</groupId>
            <artifactId>geelato-app-scaffold-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

## 启动类模板（继承 `BootApplication`）

```java
package com.acme.order;

import cn.geelato.web.platform.boot.BootApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"cn.geelato", "com.acme.order"})
@EnableConfigurationProperties
@EnableCaching
@EnableAsync(proxyTargetClass = true)
@Slf4j
public class AcmeOrderApplication extends BootApplication {
    public static void main(String[] args) {
        log.info("Starting AcmeOrderApplication");
        SpringApplication.run(AcmeOrderApplication.class, args);
    }
}
```

**关键点**：`scanBasePackages` 必须同时包含 `cn.geelato` 与业务包，缺一不可。

## application.properties 最小模板

```properties
spring.application.name=acme-order-center
server.port=8088

spring.datasource.primary.name=primary
spring.datasource.primary.jdbc-url=${GEELATO_PRIMARY_JDBCURL:jdbc:mysql://localhost:3306/acme_order_center?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowMultiQueries=true&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true}
spring.datasource.primary.username=${GEELATO_PRIMARY_JDBCUSER:acme_user}
spring.datasource.primary.password=${GEELATO_PRIMARY_JDBCPASSWORD:acme@psd}
spring.datasource.primary.driver-class-name=com.mysql.cj.jdbc.Driver

geelato.orm.dao-bean-name=dynamicDao
geelato.datasource.dynamic.enable-jta-transaction=false
geelato.datasource.dynamic.enable-seata-proxy=false

geelato.upload.root-directory=/upload
geelato.upload.convert-directory=/upload/convert
geelato.upload.config-directory=/upload/config

geelato.app.scaffold.enabled=true
geelato.app.scaffold.auto-init-tables=true
geelato.app.scaffold.strict=true
geelato.app.scaffold.capabilities=login,mql,organization,user,dictionary,upload

springdoc.api-docs.enabled=true
springdoc.api-docs.path=/v3/api-docs
geelato.app.scaffold.openapi-enabled=true
geelato.app.scaffold.openapi-expose-in-prod=false

geelato.meta.scan-package-names=cn.geelato,com.acme.order
geelato.graal.scan-package-names=cn.geelato,com.acme.order
```

**两个扫描配置最易遗漏**：`geelato.meta.scan-package-names` 与 `geelato.graal.scan-package-names` 必须把业务包加进去，否则 MQL 看不到业务实体。

## 数据库初始化

```sql
create database acme_order_center
  default character set utf8mb4
  default collate utf8mb4_unicode_ci;

create user 'acme_user'@'%' identified by 'acme@psd';
grant all privileges on acme_order_center.* to 'acme_user'@'%';
flush privileges;
```

第一次启动时，框架会初始化：

- `geelato-web-runtime` 自带的运行时基础 SQL
- `geelato-app-scaffold-starter` 与业务工程自己 `geelato/app/scaffold/init/*.sql`

脚手架默认会初始化一条 `platform_user`（登录名 `gl_user`，租户 `geelato`），仅用于首次验证登录链路。

## 启动与验证

### 0. 启动前本机环境准备（必做）

| 项 | 要求 |
| --- | --- |
| JDK | 17（`java -version`） |
| Maven | 3.8+（`mvn -v`） |
| MySQL | 8.x，本地 `localhost:3306` 可连通 |
| 数据库 | 已用第 3 节脚本创建 `acme_order_center` |
| 上传目录 | 手工建好并可写：`/upload`、`/upload/convert`、`/upload/config`（Windows 下用 `D:/upload` 等本地绝对路径替换） |
| 端口 | `8088` 未被占用（可在 `application.properties` 调整） |

Windows 示例（PowerShell）：

```powershell
New-Item -ItemType Directory -Force -Path D:\upload, D:\upload\convert, D:\upload\config
```

`application.properties` 同步改为：

```properties
geelato.upload.root-directory=D:/upload
geelato.upload.convert-directory=D:/upload/convert
geelato.upload.config-directory=D:/upload/config
```

### 1. 启动命令

```bash
mvn spring-boot:run
# 或
mvn clean package
java -jar target/acme-order-center-1.0.0-SNAPSHOT.jar
```

启动成功的标志（控制台）：

- `Started AcmeOrderApplication in X.XXX seconds`
- 出现 `Tomcat started on port(s): 8088 (http)`
- `Geelato app scaffold capabilities: login,mql,organization,user,dictionary,upload`

### 2. 第一步验证：starter 是否就绪

```bash
curl http://localhost:8088/api/scaffold/ready
```

期望响应（结构示例，字段以实际返回为准）：

```json
{
  "code": 200,
  "data": {
    "appName": "acme-order-center",
    "starter": "geelato-app-scaffold-starter",
    "capabilities": ["login","mql","organization","user","dictionary","upload"],
    "primary": { "ready": true }
  }
}
```

若 `primary.ready=false`，先解决 DB 连通问题，不要继续往下走。

### 3. 第二步验证：登录

**JWT 登录**：

```bash
curl -X POST http://localhost:8088/api/user/login \
  -H "Content-Type: application/json" \
  -d "{\"loginName\":\"gl_user\",\"password\":\"<默认密码>\",\"tenantCode\":\"geelato\"}"
```

期望响应：

```json
{
  "code": 200,
  "data": { "token": "eyJhbGciOi..." }
}
```

拿到 `token` 后，后续接口统一带 Header：

```
Authorization: JWTBearer eyJhbGciOi...
```

**OAuth2 登录**（如果启用）：

```bash
curl -X POST http://localhost:8088/api/oauth2/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"gl_user\",\"password\":\"<默认密码>\",\"tenantCode\":\"geelato\"}"
```

Header 改为 `Authorization: Bearer <accessToken>`。

### 4. 第三步验证：MQL 通用 CRUD

```bash
TOKEN="eyJhbGciOi..."

# 列表
curl -X POST http://localhost:8088/api/meta/list \
  -H "Authorization: JWTBearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platform_user":{"@fs":"id,loginName,name","@p":"1,10","@order":"createAt|-"}}'

# 按 ID 删除
curl -X POST http://localhost:8088/api/meta/delete/1/<id>

# 按条件删除
curl -X POST http://localhost:8088/api/meta/delete2/1 \
  -H "Authorization: JWTBearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"platform_user":{"loginName|eq":"gl_user"}}'
```

### 5. 第四步验证：内置能力

```bash
# 字典（通常无需鉴权）
curl http://localhost:8088/api/dictionary/<code>

# 字典分页
curl -X POST http://localhost:8088/api/dict/pageQuery \
  -H "Authorization: JWTBearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"@p":"1,10"}'

# 组织
curl -X POST http://localhost:8088/api/security/org/pageQuery \
  -H "Authorization: JWTBearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"@p":"1,10"}'

# 用户
curl -X POST http://localhost:8088/api/security/user/pageQuery \
  -H "Authorization: JWTBearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"@p":"1,10"}'
```

### 6. 第五步验证：业务实体 CRUD（端到端）

完成第 5 节"业务实体放置规范"（新建 `Customer` + `crm_customer.sql`）并重启后：

```bash
TOKEN="eyJhbGciOi..."

# 新增
curl -X POST http://localhost:8088/api/meta/save/1 \
  -H "Authorization: JWTBearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"crm_customer":{"name":"上海某客户","code":"CUST_001","contactName":"张三","contactPhone":"13800000000","enableStatus":1}}'

# 列表
curl -X POST http://localhost:8088/api/meta/list \
  -H "Authorization: JWTBearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"crm_customer":{"@fs":"id,name,code,contactName","@p":"1,10","@order":"createAt|-"}}'

# 更新（带 id 即为更新）
curl -X POST http://localhost:8088/api/meta/save/1 \
  -H "Authorization: JWTBearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"crm_customer":{"id":"<返回的id>","contactPhone":"13900000000"}}'

# 按 ID 删除
curl -X POST http://localhost:8088/api/meta/delete/1/<id>
```

返回 `code:200` 且 `data` 含主键或行数据，即视为跑通。

### 7. 可选验证：OpenAPI / Swagger

```bash
# OpenAPI JSON
curl http://localhost:8088/v3/api-docs | head -c 500

# Swagger UI
# 浏览器访问 http://localhost:8088/swagger-ui/index.html
```

prod 环境默认不暴露，参考 `geelato.app.scaffold.openapi-expose-in-prod=false`。

## 业务实体放置规范

- 实体类：`src/main/java/com/acme/order/entity/`
- 建表脚本：`src/main/resources/geelato/app/scaffold/init/<table_name>.sql`（**文件名必须与表名一致**）

实体示例：

```java
@Getter
@Setter
@Entity(name = "crm_customer")
@Title(title = "客户")
public class Customer extends BaseSortableEntity implements EntityEnableAble {
    @Title(title = "客户名称") private String name;
    @Title(title = "客户编码") private String code;
    @Title(title = "联系人") @Col(name = "contact_name") private String contactName;
    @Title(title = "联系电话") @Col(name = "contact_phone") private String contactPhone;
    @Title(title = "启用状态") @Col(name = "enable_status")
    private int enableStatus = ColumnDefault.ENABLE_STATUS_VALUE;
}
```

新增/修改实体后必须**重启应用**才能被 MQL 识别。

## MQL 通用 CRUD 入口

| 能力 | 接口 |
| --- | --- |
| 列表 | `POST /api/meta/list` |
| 多实体列表 | `POST /api/meta/multiList` |
| 新增/更新 | `POST /api/meta/save/{biz}` |
| 批量保存 | `POST /api/meta/batchSave` |
| 多实体保存 | `POST /api/meta/multiSave` |
| 按 ID 删除 | `POST /api/meta/delete/{biz}/{id}` |
| 按条件删除 | `POST /api/meta/delete2/{biz}` |

查询示例：

```json
{
  "crm_customer": {
    "@fs": "id,name,code,contactName,contactPhone,enableStatus,createAt",
    "@p": "1,10",
    "@order": "createAt|-"
  }
}
```

**何时自己写 Controller**：复杂事务编排、多实体聚合、特殊权限校验、非标准返回结构。其余单表增删查改直接用 `/api/meta/*` 即可。

## 脚手架内置能力速查

| 能力 | 入口 |
| --- | --- |
| JWT 登录 | `POST /api/user/login`，Header `Authorization: JWTBearer {token}` |
| OAuth2 登录 | `POST /api/oauth2/login`，刷新 `POST /api/oauth2/refreshToken` |
| 上传 | `POST /api/upload/file`（multipart，参数 `file`） |
| 下载文件 | `GET /api/resources/file` |
| 下载配置 | `GET /api/resources/json?fileName={hostname}_{locale}.config` |
| OSS（按需） | `/api/oss/*`（需配置 `geelato.oss.*`） |
| 字典 | `/api/dict/*`、`/api/dictItem/*`、`GET /api/dictionary/{code}` |
| 组织/用户/角色/权限 | `/api/security/{org,user,role,permission}/*` |
| ORM Fluent DSL（后端） | `MetaFactory.query("User").select(...).page(1,10).list()` |
| Swagger | `GET /v3/api-docs`、`/swagger-ui/index.html` |

## 升级流程

1. 升级 `geelato-framework-bom`
2. 升级 `geelato-app-scaffold-starter`
3. `mvn clean package` 重新编译
4. 重启并验证：`/api/scaffold/ready`、关键 MQL、字典/组织/用户/上传、本次改动接口

升级前提醒：

- 业务代码留在业务工程
- 不要把业务能力无脑下沉到 starter
- 仅当多业务工程都要复用时，才评估下沉

## 给 AI / 开发者的执行清单

接到"基于 starter 新建业务项目 / 跑通接口"类需求时，按以下顺序工作，**每一步都需可验证**：

1. 确认工程名 / 包名 / 数据库 / 端口
2. 选择方式 A（复制 `geelato-app-scaffold`）或方式 B（从零搭建）
3. 生成 `pom.xml`（含 BOM 引入与 starter 依赖）
4. 生成启动类（`scanBasePackages` 同时含 `cn.geelato` 与业务包）
5. 生成 `application.properties`（含 `geelato.meta.scan-package-names` / `geelato.graal.scan-package-names` 业务包；含数据源与 upload 目录）
6. 提供建库 SQL 模板，并提示手工建好 upload 目录
7. **执行启动命令**，等待 `Started AcmeOrderApplication` 日志
8. **验证 `/api/scaffold/ready`** 返回 `code:200` 且 `primary.ready=true`
9. **验证登录** `/api/user/login` 拿到 token
10. **验证 MQL** `/api/meta/list` 正常返回
11. **验证字典/组织/用户/上传** 至少一个内置能力
12. （可选）**新增业务实体端到端**：写实体 + SQL + 重启 + `/api/meta/save/1` + `/api/meta/list` + `/api/meta/delete/1/{id}`
13. 提示 auto-init-tables 不负责后续迁移
14. 提示升级路径：先 BOM，再 starter

**完成判定**：上述 8~11 全部 `code:200` 即视为"跑通、接口可访问"。**不要在未跑通前交付**。

## 故障排查速查表（端到端跑不通时按此排查）

| 现象 | 优先排查方向 | 修复 |
| --- | --- | --- |
| `/api/scaffold/ready` 返回 `primary.ready=false` | JDBC URL / 账号 / 数据库是否存在 | 确认 MySQL 已启动；`show databases` 能看到 `acme_order_center`；账号能从本机登录 |
| 启动报 `Communications link failure` | 端口 3306 / 防火墙 / SSL 参数 | 在 URL 里加 `&useSSL=false&allowPublicKeyRetrieval=true` |
| 启动报 `Access denied for user` | 账号密码 / grant | 重新执行 grant 脚本并 `flush privileges` |
| 登录返回 `account user not found` | `tenantCode` 缺失或租户不匹配 | 登录请求体带上 `"tenantCode":"geelato"`，或匹配实际租户编码 |
| 登录返回 `captcha required` | 启用登录验证码的能力 | 脚手架默认未强制校验；如出现，参考 captcha 配置或先关掉该能力 |
| 登录返回 `password error` | 默认密码策略 | 参考脚手架初始化脚本中 `gl_user` 的密码策略，或自行 reset |
| 业务实体 MQL 查不到 | `geelato.meta.scan-package-names` / `geelato.graal.scan-package-names` 漏配 | 把业务包加进两个配置并重启 |
| 业务实体 MQL 报 `Table not exist` | 建表脚本未执行 | 确认 SQL 路径在 `geelato/app/scaffold/init/` 且文件名 == `@Entity(name=)`；首次启动才会建 |
| 启动报 `ClassNotFoundException` / `NoSuchMethodError` | BOM 与 starter 版本不一致 | 升级 `geelato-framework-bom` 与 `geelato-app-scaffold-starter` 到同一版本 |
| 上传 401/403 | 未带 token，或 token 过期 | Header 改成 `Authorization: JWTBearer <token>`；重新登录取 token |
| 上传报 `Permission denied` / 路径不存在 | 上传目录没建或无写权限 | 按第 0 节建好 `/upload`、`/upload/convert`、`/upload/config` |
| Swagger UI 404 | openapi 未启用 / prod 默认关闭 | 确认 `geelato.app.scaffold.openapi-enabled=true`；dev 环境才能访问 |
| 接口返回乱码 | 后端编码问题 | 不影响 token / 主键字段，优先用主键 ID 关联；后端单独修编码 |
| `mvn package` 报 `repackage failed` | `spring-boot-maven-plugin` 未配置 mainClass | 在 `pom.xml` 的 plugin 中指定 `<mainClass>` |

## 反模式（不要做）

- `scanBasePackages` 只写业务包（框架控制器与自动装配扫不到）
- `scanBasePackages` 只写 `cn.geelato`（业务实体、Controller、Service 扫不到）
- `geelato.meta.scan-package-names` / `geelato.graal.scan-package-names` 漏配业务包
- 把业务实体或业务 SQL 写到 `geelato-app-scaffold-starter`
- 用 `auto-init-tables` 期望“重启应用就能改字段”
- 长期手工同步 `geelato-app-scaffold` 而不通过 BOM/starter 升级
