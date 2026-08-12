package cn.geelato.sample.orm.web;

import cn.geelato.core.orm.TransactionHelper;
import cn.geelato.orm.MetaFactory;
import cn.geelato.orm.page.PageResult;
import cn.geelato.orm.query.Filter;
import cn.geelato.orm.query.Order;
import cn.geelato.orm.value.ValueRefs;
import cn.geelato.sample.orm.dto.ProductDto;
import cn.geelato.sample.orm.entity.SampleCategoryEntity;
import cn.geelato.sample.orm.entity.SampleProductEntity;
import cn.geelato.sample.orm.entity.SampleProductImageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 高级方法演示：在基础 CRUD 之上，集中演示 Fluent DSL 的进阶能力。
 * <ul>
 *   <li>查询：分页+排序+多条件、多运算符(in/ge/like/简单or)、多表 join、聚合(groupBy/havingSql)、
 *       oneColumn、exists、count、wrapperResult(→DTO)、原生 SQL、存储过程。</li>
 *   <li>写入：事务批插、父子嵌套保存(ValueRefs.parent)。</li>
 *   <li>事务：编程式事务 + 主动回滚。</li>
 * </ul>
 *
 * <p>注意：本示例启用了「查询过滤注入 SPI」（见 {@code spi.SampleTenantQueryFilterInjector}），
 * 所有 SampleProduct 的 DSL 查询会自动附加 {@code tenant_code='demo-tenant'}；
 * /seed 写入的商品租户字段由「默认字段填充 SPI」自动补齐为 demo-tenant，故可被查到。</p>
 */
@RestController
public class SampleOrmAdvancedController {

    @Autowired
    private DataSource dataSource;

    // ============================== 造数 / 清理 ==============================

    /** 造数：插入分类与商品（可重复执行，会先清空）。 */
    @GetMapping("/api/orm/adv/seed")
    public Map<String, Object> seed() {
        clearAll();
        String cElectronic = category("电子");
        String cBook = category("图书");
        String cFood = category("食品");
        product(cElectronic, "手机", 2999.0, 50, 1);
        product(cElectronic, "耳机", 399.0, 200, 1);
        product(cElectronic, "下架平板", 1599.0, 0, 0);
        product(cBook, "Java指南", 89.0, 300, 1);
        product(cBook, "算法", 79.0, 120, 1);
        product(cFood, "巧克力", 39.9, 500, 1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "seed");
        result.put("categories", countCategories());
        result.put("products", MetaFactory.query(SampleProductEntity.class).where(Filter.isNotNull("id")).count());
        return result;
    }

    /** 清空三张演示表。 */
    @GetMapping("/api/orm/adv/clear")
    public Map<String, Object> clear() {
        int[] deleted = clearAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "clear");
        result.put("deletedImages", deleted[0]);
        result.put("deletedProducts", deleted[1]);
        result.put("deletedCategories", deleted[2]);
        return result;
    }

    // ============================== 查询类 ==============================

    /** 分页 + 排序 + 多条件：status=1 且 price>=50，按价格倒序，取第 1 页每页 5 条。 */
    @GetMapping("/api/orm/adv/query/page")
    public Map<String, Object> queryPage(@RequestParam(required = false, defaultValue = "1") int page,
                                         @RequestParam(required = false, defaultValue = "5") int size) {
        PageResult<Map<String, Object>> pg = MetaFactory.query(SampleProductEntity.class)
                .select(new String[]{"id", "name", "price", "stock", "status"})
                .where(Filter.eq("status", 1))
                .where(Filter.ge("price", 50.0))
                .order(Order.desc("price"))
                .page(page, size)
                .page();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/page");
        result.put("current", pg.getCurrent());
        result.put("size", pg.getSize());
        result.put("total", pg.getTotal());
        result.put("list", pg.getRecords());
        return result;
    }

    /** 多运算符演示：in / ge / like。 */
    @GetMapping("/api/orm/adv/query/filter")
    public Map<String, Object> queryFilter() {
        List<Map<String, Object>> byOps = MetaFactory.query(SampleProductEntity.class)
                .select(new String[]{"id", "name", "price", "stock", "status"})
                .where(
                        Filter.in("status", 1, 0),
                        Filter.ge("price", 50.0),
                        Filter.like("name", "手")
                )
                .order(Order.desc("price"))
                .list();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/filter");
        result.put("byOperators_in_ge_like", byOps);
        result.put("note", "复杂跨组 OR 逻辑建议走 MQL / 原生 SQL，详见 README");
        return result;
    }

    /** 多表 join：商品 left join 分类，取分类名。 */
    @GetMapping("/api/orm/adv/query/join")
    public Map<String, Object> queryJoin() {
        List<Map<String, Object>> rows = MetaFactory.query(SampleProductEntity.class)
                .as("p")
                .select(new String[]{"id", "name", "price", "stock"})
                .selectExpr("c.name", "categoryName")
                .leftJoin(SampleCategoryEntity.class, "c", on -> on.eqField("p.categoryId", "c.id"))
                .order(Order.desc("price"))
                .list();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/join");
        result.put("list", rows);
        return result;
    }

    /** 聚合：按分类分组，统计商品数与均价，having 过滤。 */
    @GetMapping("/api/orm/adv/query/aggregate")
    public Map<String, Object> queryAggregate() {
        // 说明：DSL 默认会把主实体字段放进 SELECT，故这里用 .select({"categoryId"}) 收窄主表字段，
        // 并把出现在 SELECT 里的非聚合列（p.categoryId、c.name）一并放进 GROUP BY，保证 SQL 合法。
        List<Map<String, Object>> rows = MetaFactory.query(SampleProductEntity.class)
                .as("p")
                .select(new String[]{"categoryId"})
                .selectExpr("c.name", "categoryName")
                .selectExpr("count(*)", "productCount")
                .selectExpr("coalesce(avg(p.price),0)", "avgPrice")
                .leftJoin(SampleCategoryEntity.class, "c", on -> on.eqField("p.categoryId", "c.id"))
                .groupBy("c.name", "p.category_id")
                .havingSql("count(*) >= 2")
                .list();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/aggregate");
        result.put("list", rows);
        return result;
    }

    /** 单列查询：只取商品名（关掉注入过滤，避免 oneColumn 与附加条件叠加）。 */
    @GetMapping("/api/orm/adv/query/oneColumn")
    public Map<String, Object> queryOneColumn() {
        List<String> names = MetaFactory.query(SampleProductEntity.class)
                .select(new String[]{"name"})
                .disableInjectFilter()
                .order(Order.asc("name"))
                .oneColumn(String.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/oneColumn");
        result.put("names", names);
        return result;
    }

    /** exists：是否存在名为“手机”的商品。 */
    @GetMapping("/api/orm/adv/query/exists")
    public Map<String, Object> queryExists(@RequestParam(required = false, defaultValue = "手机") String name) {
        boolean exists = MetaFactory.query(SampleProductEntity.class)
                .where(Filter.eq("name", name))
                .exists();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/exists");
        result.put("name", name);
        result.put("exists", exists);
        return result;
    }

    /** count：上架(status=1)商品数。 */
    @GetMapping("/api/orm/adv/query/count")
    public Map<String, Object> queryCount() {
        long count = MetaFactory.query(SampleProductEntity.class)
                .where(Filter.eq("status", 1))
                .count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/count");
        result.put("onSaleCount", count);
        return result;
    }

    /** wrapperResult：把行 Map 映射成 {@link ProductDto}。 */
    @GetMapping("/api/orm/adv/query/wrapper")
    public Map<String, Object> queryWrapper() {
        List<ProductDto> dtos = MetaFactory.query(SampleProductEntity.class)
                .as("p")
                .select(new String[]{"id", "name", "price", "stock"})
                .selectExpr("c.name", "categoryName")
                .leftJoin(SampleCategoryEntity.class, "c", on -> on.eqField("p.categoryId", "c.id"))
                .order(Order.desc("price"))
                .wrapperResult((Map<String, Object> row) -> new ProductDto(
                        asString(row.get("id")),
                        asString(row.get("name")),
                        asDouble(row.get("price")),
                        asInt(row.get("stock")),
                        asString(row.get("categoryName"))))
                .list();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/wrapper");
        result.put("list", dtos);
        return result;
    }

    /**
     * 原生 SQL 直通：使用真实表名/列名，绕过元数据与注入器（不会自动附加租户过滤）。
     * 演示 list() 与 queryForObject(Class)。
     */
    @GetMapping("/api/orm/adv/query/native")
    public Map<String, Object> queryNative(@RequestParam(required = false, defaultValue = "100") double minPrice) {
        List<Map<String, Object>> rows = MetaFactory.sql(
                        "select name, price from sample_adv_product where price >= ? order by price desc")
                .param(minPrice)
                .list();
        Long total = MetaFactory.sql("select count(*) from sample_adv_product where status = ?")
                .param(1)
                .queryForObject(Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/native");
        result.put("gtePrice", minPrice);
        result.put("list", rows);
        result.put("onSaleTotal", total);
        return result;
    }

    /**
     * 存储过程：调用 H2 CREATE ALIAS 建立的表值函数 proc_product_stats(minStock)，
     * 返回各分类下库存达标商品数与均价。
     */
    @GetMapping("/api/orm/adv/query/procedure")
    public Map<String, Object> queryProcedure(@RequestParam(required = false, defaultValue = "5") int minStock) {
        List<Map<String, Object>> rows = MetaFactory.procedure("proc_product_stats")
                .in("minStock", minStock)
                .list();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query/procedure");
        result.put("minStock", minStock);
        result.put("list", rows);
        return result;
    }

    // ============================== 写入类 ==============================

    /** 批量插入（事务）：MetaInsert.batch(List) 内部走 batchSave(transaction=true)。 */
    @GetMapping("/api/orm/adv/write/batch")
    public Map<String, Object> writeBatch() {
        String cId = category("批量分类");
        List<Map<String, Object>> payloads = List.of(
                mapOf("name", "批量商品A", "price", 10.0, "stock", 100, "categoryId", cId, "status", 1),
                mapOf("name", "批量商品B", "price", 20.0, "stock", 50, "categoryId", cId, "status", 1),
                mapOf("name", "批量商品C", "price", 30.0, "stock", 5, "categoryId", cId, "status", 1)
        );
        List<String> pks = MetaFactory.insert(SampleProductEntity.class).batch(payloads);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "write/batch");
        result.put("inserted", pks.size());
        result.put("ids", pks);
        return result;
    }

    /**
     * 父子嵌套保存：一次保存商品 + 两张图片，子表用 ValueRefs.parent("id") 引用父表生成的主键。
     */
    @GetMapping("/api/orm/adv/write/nested")
    public Map<String, Object> writeNested() {
        String cId = category("套餐分类");
        String parentId = MetaFactory.insert(SampleProductEntity.class)
                .value("name", "套餐商品")
                .value("price", 199.0)
                .value("stock", 10)
                .value("categoryId", cId)
                .value("status", 1)
                .child("SampleProductImage", c -> c
                        .value("productId", ValueRefs.parent("id"))
                        .value("url", "https://demo/a.png"))
                .child("SampleProductImage", c -> c
                        .value("productId", ValueRefs.parent("id"))
                        .value("url", "https://demo/b.png"))
                .save();
        long imgCount = MetaFactory.query(SampleProductImageEntity.class)
                .where(Filter.eq("productId", parentId))
                .count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "write/nested");
        result.put("parentId", parentId);
        result.put("childImageCount", imgCount);
        return result;
    }

    // ============================== 事务类 ==============================

    /**
     * 编程式事务：在同一事务内保存商品后主动抛异常 → 回滚；
     * 用回滚前后的商品总数一致来证明事务生效。
     */
    @GetMapping("/api/orm/adv/tx/programmatic")
    public Map<String, Object> txProgrammatic() {
        long before = MetaFactory.query(SampleProductEntity.class).where(Filter.isNotNull("id")).count();

        DataSourceTransactionManager tm = new DataSourceTransactionManager(dataSource);
        TransactionStatus status = TransactionHelper.beginTransaction(tm);
        String insertedId = null;
        boolean rolledBack = false;
        try {
            insertedId = MetaFactory.insert(SampleProductEntity.class)
                    .value("name", "事务回滚商品")
                    .value("price", 1.0)
                    .value("stock", 1)
                    .value("status", 1)
                    .save();
            // 故意抛异常，触发回滚
            throw new IllegalStateException("演示编程式事务回滚");
        } catch (RuntimeException ex) {
            if (!status.isCompleted()) {
                TransactionHelper.rollbackTransaction(tm, status);
            }
            rolledBack = true;
        }

        long after = MetaFactory.query(SampleProductEntity.class).where(Filter.isNotNull("id")).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "tx/programmatic");
        result.put("countBefore", before);
        result.put("countAfter", after);
        result.put("rolledBack", rolledBack);
        result.put("insertedIdBeforeRollback", insertedId);
        result.put("note", "countBefore == countAfter 即表示插入已被回滚");
        return result;
    }

    // ============================== 私有辅助 ==============================

    private String category(String name) {
        return MetaFactory.insert(SampleCategoryEntity.class).value("name", name).save();
    }

    private String product(String categoryId, String name, double price, int stock, int status) {
        return MetaFactory.insert(SampleProductEntity.class)
                .value("name", name)
                .value("price", price)
                .value("stock", stock)
                .value("categoryId", categoryId)
                .value("status", status)
                .save();
    }

    private long countCategories() {
        return MetaFactory.query(SampleCategoryEntity.class).where(Filter.isNotNull("id")).count();
    }

    private int[] clearAll() {
        // 物理删除（本示例实体为精简 IdEntity，无 delStatus 列，不走 Fluent 逻辑删除）
        int d1 = MetaFactory.sql("delete from sample_adv_product_image").execute();
        int d2 = MetaFactory.sql("delete from sample_adv_product").execute();
        int d3 = MetaFactory.sql("delete from sample_adv_category").execute();
        return new int[]{d1, d2, d3};
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2, String k3, Object v3,
                                             String k4, Object v4, String k5, Object v5) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        m.put(k4, v4);
        m.put(k5, v5);
        return m;
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Double asDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    private static Integer asInt(Object o) {
        return o instanceof Number n ? n.intValue() : null;
    }
}
