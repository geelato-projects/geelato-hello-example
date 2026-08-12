# geelato-sample-orm

Geelato ORM（Fluent DSL `MetaFactory`）示例工程。基于最轻量的 `geelato-orm`（H2 内存库），演示从基础 CRUD 到高级查询/写入/事务，再到 SPI 扩展（默认字段填充、查询过滤注入）的各项能力，无需 MySQL/Redis/平台运行时，启动即可验证。

> 配套文档：[后端 Fluent DSL 指引](https://docs.geelato.cn)（`geelato-community/docs/orm/backend-fluent-dsl-guide.md`）
> 事件机制示例：见姊妹工程 `geelato-sample-orm-event`。

## 演示的能力点

### 基础 CRUD（`/api/orm/user/*`、`/api/orm/crud`）

| 能力 | 入口 | 说明 |
|---|---|---|
| 新增 | `MetaFactory.insert(...).save()` | 主键 id 由框架自动生成 |
| 列表 / 单条 | `.list()` / `.where(...).one()` | |
| 分页 | `.select(...).order(...).page(n,s).page()` | 返回 `PageResult` |
| 更新 / 删除 | `MetaFactory.update/delete` | |

### 高级方法（`/api/orm/adv/*`）

| 能力 | 端点 | 演示要点 |
|---|---|---|
| 分页+排序+多条件 | `/query/page` | `Filter.eq/ge` + `Order.desc` + `page()` |
| 多运算符 | `/query/filter` | `in`/`ge`/`like`（复杂 OR 见下文说明） |
| 多表 join | `/query/join` | `.as("p")` + `selectExpr` + `leftJoin(...on.eqField)` |
| 聚合 | `/query/aggregate` | `groupBy` + `havingSql` + `count(*)/avg` |
| 单列查询 | `/query/oneColumn` | `.select({field}).oneColumn(Class)` |
| exists / count | `/query/exists`、`/query/count` | |
| 结果包装(→DTO) | `/query/wrapper` | `.wrapperResult(row -> new ProductDto(...))` |
| 原生 SQL | `/query/native` | `MetaFactory.sql(...).param(...).list()/.queryForObject(Class)` |
| 存储过程 | `/query/procedure` | `MetaFactory.procedure(name).in(...).list()`（H2 `CREATE ALIAS`） |
| 事务批插 | `/write/batch` | `MetaInsert.batch(List)`，内部 `batchSave(transaction=true)` |
| 父子嵌套保存 | `/write/nested` | `.child("子实体", c -> c.value("fk", ValueRefs.parent("id")))` |
| 编程式事务 | `/tx/programmatic` | `TransactionHelper.begin/rollback`，演示回滚 |

### SPI 扩展（`/api/orm/spi/*`）

| 能力 | 实现 / 入口 | 演示要点 |
|---|---|---|
| 默认字段填充 | `SampleAuditFieldValueFiller`（`FluentSaveFieldValueFiller`） | 保存前自动补 `createAt/creator/tenantCode`（Insert）与 `updateAt/updater`（Update）；实体感知、不覆盖业务显式值 |
| 查询过滤注入 | `SampleTenantQueryFilterInjector`（`FluentQueryFilterInjector`） | 对带租户字段的实体查询自动附加 `tenant_code='demo-tenant'` |
| 强制注入 | `injector.isForceInject()` | 即使查询调用 `disableInjectFilter()` 也仍注入 |
| DSL 跳过注入器 | `MetaQuery.disableInjectFilter()` | 运行期对单条查询关掉注入（被 forceInject 覆盖） |

## 快速开始

### 1. 前置条件

- JDK 17
- Maven（需先将 `geelato-orm` 1.0.0-SNAPSHOT 安装到本地仓库：在 `geelato-community/geelato-orm` 执行 `mvn install -DskipTests`）

### 2. 编译运行

```bash
cd geelato-hello-example/geelato-sample-orm
mvn spring-boot:run
```

或在聚合仓根目录：

```bash
cd geelato-hello-example
mvn -pl geelato-sample-orm spring-boot:run
```

启动后监听 `8082` 端口。

### 3. 基础 CRUD

```bash
# 一气呵成冒烟测试
curl "http://localhost:8082/api/orm/crud"

# 拆分接口
curl "http://localhost:8082/api/orm/user/insert?name=Tom&email=tom@a.com&status=1"
curl "http://localhost:8082/api/orm/user/list"
curl "http://localhost:8082/api/orm/user/page?page=1&size=10"
curl "http://localhost:8082/api/orm/user/update?id=<id>&name=Tom2"
curl "http://localhost:8082/api/orm/user/delete?id=<id>"
```

### 4. 高级方法（先 seed 造数）

```bash
curl "http://localhost:8082/api/orm/adv/seed"
curl "http://localhost:8082/api/orm/adv/query/page"
curl "http://localhost:8082/api/orm/adv/query/filter"
curl "http://localhost:8082/api/orm/adv/query/join"
curl "http://localhost:8082/api/orm/adv/query/aggregate"
curl "http://localhost:8082/api/orm/adv/query/oneColumn"
curl "http://localhost:8082/api/orm/adv/query/wrapper"
curl "http://localhost:8082/api/orm/adv/query/native?minPrice=100"
curl "http://localhost:8082/api/orm/adv/query/procedure?minStock=5"
curl "http://localhost:8082/api/orm/adv/write/batch"
curl "http://localhost:8082/api/orm/adv/write/nested"
curl "http://localhost:8082/api/orm/adv/tx/programmatic"
curl "http://localhost:8082/api/orm/adv/clear"
```

### 5. SPI 扩展

```bash
# 默认字段填充：写入时不传 createAt/creator/tenantCode，查回看自动补齐
curl "http://localhost:8082/api/orm/spi/fill/seed"
curl "http://localhost:8082/api/orm/spi/fill/show?id=<id>"
curl "http://localhost:8082/api/orm/spi/fill/update?id=<id>"

# 查询过滤注入：3 条 demo-tenant + 1 条 other-tenant
curl "http://localhost:8082/api/orm/spi/inject/seed"

# 普通查询：注入器启用 → 仅返回 demo-tenant（3 条），other 不可见
curl "http://localhost:8082/api/orm/spi/inject/query?skip=false"

# skip=true：默认 forceInject=false → 跳过注入，返回全部 4 条
curl "http://localhost:8082/api/orm/spi/inject/query?skip=true"

# 打开强制注入后，skip=true 仍被注入 → 又只剩 3 条 demo-tenant
curl "http://localhost:8082/api/orm/spi/inject/force?on=true"
curl "http://localhost:8082/api/orm/spi/inject/query?skip=true"

# 还原：关闭强制注入 / 关闭注入器
curl "http://localhost:8082/api/orm/spi/inject/force?on=false"
curl "http://localhost:8082/api/orm/spi/inject/enable?on=false"
```

> 每次查询时控制台会打印 `Applying/Skip Fluent query filter injection ...` / `Applying/Skip Fluent save field value fill ...` 日志，可佐证 SPI 是否生效。

## 工程结构

```
geelato-sample-orm/
├── pom.xml                                   # 依赖 geelato-orm + spring-boot-starter-web/jdbc + H2
└── src/main/
    ├── java/cn/geelato/sample/orm/
    │   ├── SampleOrmApplication.java         # 启动类
    │   ├── config/
    │   │   ├── OrmDaoConfiguration.java      # 暴露 @Bean Dao primaryDao(JdbcTemplate)
    │   │   └── SampleH2FunctionInitializer.java  # 启动期创建 H2 表值函数 proc_product_stats
    │   ├── entity/
    │   │   ├── SampleUserEntity.java         # 基础 CRUD 实体（id/name/email/status）
    │   │   ├── SampleCategoryEntity.java     # 高级：分类（join 目标）
    │   │   ├── SampleProductEntity.java      # 高级：商品（含审计/租户字段）
    │   │   └── SampleProductImageEntity.java # 高级：商品图片（父子保存的子表）
    │   ├── dto/ProductDto.java               # wrapperResult 演示用 record
    │   ├── spi/
    │   │   ├── SampleAuditFieldValueFiller.java       # 默认字段填充 SPI
    │   │   └── SampleTenantQueryFilterInjector.java   # 查询过滤注入 SPI（含 force 开关）
    │   └── web/
    │       ├── SampleOrmCrudController.java          # 基础 CRUD
    │       ├── SampleOrmAdvancedController.java      # 高级方法
    │       └── SampleOrmSpiController.java           # SPI 扩展演示
    └── resources/
        ├── application.properties             # H2 + spring.sql.init.mode=always
        └── schema.sql                         # 建表
```

## 关键说明

- **两种写法都可用**：实体既可 `extends IdEntity`（复用主键），也可不继承、在字段上直接标 `@Id`；本示例统一继承 `IdEntity`。
- **主键自动生成**：`MetaFactory.insert(...).save()` 不传 id 时，框架用 `UIDGenerator` 自动生成并返回。
- **查询过滤注入默认启用**：本示例的 `SampleTenantQueryFilterInjector` 默认 `enabled=true`，因此所有 `SampleProduct` 的 DSL 查询都会自动按 `demo-tenant` 过滤；`/adv/seed` 写入的商品租户由填充器补齐为 `demo-tenant`，故可被查到。`disableInjectFilter()` 或 `inject/enable?on=false` 可关掉。
- **原生 SQL 不走注入器**：`MetaFactory.sql(...)` 是直通执行，不会自动附加租户过滤，业务方需自行保证安全与条件。
- **SPI 实体感知**：填充器与注入器都按 `EntityMeta.getFieldNames()` 判定，仅作用于实际拥有相应字段的实体，因此 `sample_user`、分类、图片等表完全不受影响。

## 行为边界与排障提示（实测）

- **删除是物理删除（原生 SQL）**：Fluent DSL 的 `MetaFactory.delete()` 实际是“逻辑删除”（`update ... set delStatus/deleteAt ...`），需要实体带这些列（继承 `BaseEntity`）。本示例实体为精简的 `IdEntity`，没有这些列，直接走 Fluent 删除会生成空 `set` 报错；故示例的删除统一用 `MetaFactory.sql("delete from ...").execute()` 做物理删除。如果你的实体继承了 `BaseEntity`，可正常使用 `MetaFactory.delete()` 的逻辑删除。
- **复杂 OR 不建议在 Fluent `where` 里写**：`where(Filter...)` 以 AND 为主，跨组 OR 语义不稳定（指南亦建议复杂跨组逻辑走 MQL/原生 SQL）。故 `/query/filter` 只演示 `in/ge/like`。
- **聚合 `groupBy` 用真实列名**：`groupBy(...)` 是原始 SQL 片段，不做“字段名→列名”翻译；带 `.as("p")` 时要写 `p.category_id` 这种真实列名。`select(...)` 才会自动翻译字段名。
- **`oneColumn` 与注入过滤**：示例中 `/query/oneColumn` 调用了 `disableInjectFilter()`，避开 oneColumn 与附加条件参数叠加的问题；本表数据均为 demo-tenant，结果不受影响。
- **存储过程走 H2 `CREATE ALIAS`**：`proc_product_stats` 在启动期由 `SampleH2FunctionInitializer` 整体下发（其 Java 源码含分号/双引号，放进 schema.sql 会被脚本切分器误切）。`MetaFactory.procedure(name).in(...).list()` 适用于“仅 IN 参数、返回单结果集”的过程。

## 仅文档说明（本工程不做可运行端点）

- **多数据源**：`MetaQuery.useDataSource("connectId")` / `@Entity(connectId=...)`，需配置动态数据源（本示例只有单数据源）。
- **字段加密**：`EncryptUtils` + `ColumnEncryptedEnum`，依赖平台加密类型配置（`geelato-web-platform`）。
- **软删除**：`BaseEntity.delStatus` + `DeleteStatusEnum`，依赖 `BaseEntity` 与 Dao 的软删处理。
- **事件机制**：Before/After/TransactionalAfter/Query 监听器，见姊妹工程 `geelato-sample-orm-event`。
- **ValueRefs 其它用法**：`.value("createAt", ValueRefs.fnNowDateTime())`、`.value("creator", ValueRefs.ctx("userId"))`（上下文/函数值引用）；本示例在父子保存中演示了 `ValueRefs.parent("id")`。
