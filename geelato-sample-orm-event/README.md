# geelato-sample-orm-event

Geelato ORM 事件机制示例工程。基于最轻量的 `geelato-orm`（H2 内存库），演示 ORM 事件机制的各项能力，无需 MySQL/Redis/平台运行时，启动即可验证。

> 配套文档：[ORM 事件特性](https://docs.geelato.cn)（`geelato-community/website/official-docs/zh-cn/orm/event-features.md`）

## 演示的能力点

| 能力 | 监听器 / 入口 | 演示场景 |
|---|---|---|
| 前置同步校验（Before） | `PriceValidationBeforeSaveListener` | 保存前校验商品价格，违则抛异常阻断；演示 `isInsert()`/`isUpdate()` 区分 |
| 后置异步旁路（After） | `AuditLogAfterSaveListener` | 保存后打印审计日志；演示 `isSuccess()` 执行结果回传 |
| 事务感知后置（Transactional After） | `TransactionalEsSyncListener` | 仅事务提交后同步 ES；演示 `afterCommit`/`afterRollback` |
| 查询事件（Query） | `SlowQueryListener` | 查询后统计耗时与行数；演示 `QueryEventContext.rowCount` |
| 函数式 callback | `EventConfiguration` | 用 lambda 注册删除后清理，免写实现类 |
| 优先级（order） | `PriceValidationBeforeSaveListener.getOrder()` | 校验类监听器高优先级先执行 |
| 生命周期管理 | `EventConfiguration @PreDestroy` | 容器销毁时注销监听器，防泄漏 |
| 线程池配置 | `application.properties` | 注释展示了 `geelato.orm.event.*` 配置 |

## 快速开始

### 1. 前置条件

- JDK 17
- Maven（需先将 `geelato-orm` 1.0.0-SNAPSHOT 安装到本地仓库：在 `geelato-community/geelato-orm` 执行 `mvn install -DskipTests`）

### 2. 编译运行

```bash
cd geelato-hello-example/geelato-sample-orm-event
mvn spring-boot:run
```

或在聚合仓根目录：

```bash
cd geelato-hello-example
mvn -pl geelato-sample-orm-event spring-boot:run
```

启动后监听 `8083` 端口。

### 3. 触发事件并观察控制台

依次访问下列接口，观察应用控制台的日志输出：

| 接口 | 触发的事件 | 预期控制台输出 |
|---|---|---|
| `GET http://localhost:8083/api/event/insert` | BeforeSave 校验 → AfterSave 审计 → afterCommit ES 同步 | `[BeforeSave] 新增商品校验通过` / `[AfterSave] 商品保存完成...` / `[TransactionalAfterSave#afterCommit] 事务已提交，同步商品到 ES` |
| `GET http://localhost:8083/api/event/insertIllegal` | BeforeSave 校验拦截（价格为负） | 接口返回 `blocked:true`；保存被阻断 |
| `GET http://localhost:8083/api/event/update/{id}` | BeforeSave 更新校验 → AfterSave → afterCommit | `[BeforeSave] 更新商品校验通过` |
| `GET http://localhost:8083/api/event/query` | AfterQuery 慢查询统计 | `[AfterQuery] 查询完成...rows=...cost=...ms` |
| `GET http://localhost:8083/api/event/delete/{id}` | AfterDeleteCallback 函数式 callback | `[AfterDeleteCallback] 删除完成...affectedRows=1` |
| `GET http://localhost:8083/api/event/rollback` | 事务回滚 → afterRollback | `[TransactionalAfterSave#afterRollback] 事务已回滚，跳过 ES 同步` |

> `{id}` 用 `insert` 接口返回的 `saved.id` 值。

## 工程结构

```
geelato-sample-orm-event/
├── pom.xml                              # 依赖 geelato-orm + spring-boot-starter-web/jdbc + H2
└── src/main/
    ├── java/cn/geelato/sample/ormevent/
    │   ├── SampleOrmEventApplication.java       # 启动类
    │   ├── config/OrmDaoConfiguration.java      # 暴露 Dao Bean
    │   ├── entity/SampleProductEntity.java      # 示例实体（商品）
    │   ├── event/                               # 事件监听器与统一注册
    │   │   ├── PriceValidationBeforeSaveListener.java   # BeforeSave 校验
    │   │   ├── AuditLogAfterSaveListener.java           # AfterSave 审计
    │   │   ├── TransactionalEsSyncListener.java         # 事务感知 AfterSave
    │   │   ├── SlowQueryListener.java                   # AfterQuery 慢查询
    │   │   └── EventConfiguration.java                  # 统一注册 + 函数式 callback + 生命周期
    │   └── web/SampleEventController.java       # 触发事件的接口
    └── resources/
        ├── application.properties               # H2 + 事件线程池配置示例
        └── schema.sql                           # 建表
```

## 关键说明

- **写操作走 `Dao`**：为使事务感知 after（`afterCommit`/`afterRollback`）完整生效，本示例 Controller 的写操作走 `Dao.save/delete` 路径（而非 Fluent DSL `MetaFactory`）。
- **事务感知 after**：`TransactionalEsSyncListener` 的 `afterCommit` 仅在事务真正提交后触发；无事务时立即触发（视为已提交）。`afterRollback` 在事务回滚后触发。
- **Before 异常契约**：框架对 Before 监听器异常透传、不加全局开关。`PriceValidationBeforeSaveListener` 需要拦截就抛异常；若是旁路监听器（如审计），应在方法内 `try/catch` 吞掉异常，不阻断业务。
- **事件监听器注册**：`enabled()` 与 `supports()` 默认返回 `false`，必须覆写为 `true` 才会触发。
