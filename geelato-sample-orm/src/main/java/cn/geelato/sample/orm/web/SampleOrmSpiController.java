package cn.geelato.sample.orm.web;

import cn.geelato.orm.MetaFactory;
import cn.geelato.orm.query.Filter;
import cn.geelato.orm.query.MetaInsert;
import cn.geelato.orm.query.Order;
import cn.geelato.sample.orm.entity.SampleProductEntity;
import cn.geelato.sample.orm.spi.SampleTenantQueryFilterInjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPI 扩展示例：演示两个 Fluent DSL 扩展点。
 * <ol>
 *   <li>默认字段填充 SPI（{@code FluentSaveFieldValueFiller}，实现见
 *       {@code spi.SampleAuditFieldValueFiller}）：保存前自动补齐 createAt/creator/tenantCode 等。</li>
 *   <li>查询过滤注入 SPI（{@code FluentQueryFilterInjector}，实现见
 *       {@code spi.SampleTenantQueryFilterInjector}）：查询时自动附加租户隔离条件，
 *       并可演示 {@code disableInjectFilter()} 跳过与 {@code isForceInject()} 强制注入。</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/orm/spi")
public class SampleOrmSpiController {

    @Autowired
    private SampleTenantQueryFilterInjector injector;

    // ============================== 默认字段填充 SPI ==============================

    /** 造数：插一个商品，不传 createAt/creator/tenantCode，由填充器补齐。 */
    @GetMapping("/fill/seed")
    public Map<String, Object> fillSeed() {
        String id = MetaFactory.insert(SampleProductEntity.class)
                .value("name", "填充演示商品")
                .value("price", 88.0)
                .value("stock", 8)
                .value("status", 1)
                .save();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("action", "fill/seed");
        r.put("id", id);
        return r;
    }

    /** 查看填充效果：createAt/creator/tenantCode 应非空。 */
    @GetMapping("/fill/show")
    public Map<String, Object> fillShow(@RequestParam String id) {
        Map<String, Object> row = MetaFactory.query(SampleProductEntity.class)
                .where(Filter.eq("id", id))
                .one();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("action", "fill/show");
        r.put("data", row);
        r.put("note", "createAt/creator/tenantCode 应非空（由 SampleAuditFieldValueFiller 自动补齐）");
        return r;
    }

    /** 更新演示：只改 name，updateAt/updater 由填充器补齐。 */
    @GetMapping("/fill/update")
    public Map<String, Object> fillUpdate(@RequestParam String id) {
        MetaFactory.update(SampleProductEntity.class)
                .where(Filter.eq("id", id))
                .value("name", "填充演示商品(改名)")
                .save();
        Map<String, Object> row = MetaFactory.query(SampleProductEntity.class)
                .where(Filter.eq("id", id))
                .one();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("action", "fill/update");
        r.put("data", row);
        r.put("note", "updateAt/updater 应非空（由 SampleAuditFieldValueFiller 自动补齐）");
        return r;
    }

    // ============================== 查询过滤注入 SPI ==============================

    /** 造数：3 个 demo-tenant 商品（租户由填充器补齐）+ 1 个 other-tenant 商品（显式租户，保留）。 */
    @GetMapping("/inject/seed")
    public Map<String, Object> injectSeed() {
        // 先清掉商品，便于观察（物理删除）
        MetaFactory.sql("delete from sample_adv_product").execute();
        product("注入商品A", 10.0, null);
        product("注入商品B", 20.0, null);
        product("注入商品C", 30.0, null);
        product("注入商品Other", 40.0, "other-tenant");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("action", "inject/seed");
        r.put("demoTenantInserted", 3);
        r.put("otherTenantInserted", 1);
        return r;
    }

    /** 当前注入器状态。 */
    @GetMapping("/inject/status")
    public Map<String, Object> injectStatus() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("enabled", injector.isEnabled());
        r.put("forceInject", injector.isForceInject());
        return r;
    }

    /** 切换注入器 isEnabled()。 */
    @GetMapping("/inject/enable")
    public Map<String, Object> injectEnable(@RequestParam boolean on) {
        injector.setEnabled(on);
        return injectStatus();
    }

    /** 切换注入器 isForceInject()。 */
    @GetMapping("/inject/force")
    public Map<String, Object> injectForce(@RequestParam boolean on) {
        injector.setForceInject(on);
        return injectStatus();
    }

    /**
     * 注入语义演示：
     * <ul>
     *   <li>skip=false（普通查询）：注入器启用时附加 tenant_code='demo-tenant'，仅返回 demo-tenant 行；</li>
     *   <li>skip=true（调用 disableInjectFilter()）：
     *     forceInject=false → 跳过注入（返回含 other-tenant 的全部行）；
     *     forceInject=true  → 仍强制注入（仅返回 demo-tenant 行）。</li>
     * </ul>
     */
    @GetMapping("/inject/query")
    public Map<String, Object> injectQuery(@RequestParam(required = false, defaultValue = "false") boolean skip) {
        List<Map<String, Object>> rows;
        if (skip) {
            rows = MetaFactory.query(SampleProductEntity.class)
                    .select(new String[]{"id", "name", "tenantCode"})
                    .disableInjectFilter()
                    .order(Order.asc("name"))
                    .list();
        } else {
            rows = MetaFactory.query(SampleProductEntity.class)
                    .select(new String[]{"id", "name", "tenantCode"})
                    .order(Order.asc("name"))
                    .list();
        }
        long demo = rows.stream()
                .filter(r -> "demo-tenant".equals(String.valueOf(r.get("tenantCode"))))
                .count();
        long other = rows.stream()
                .filter(r -> "other-tenant".equals(String.valueOf(r.get("tenantCode"))))
                .count();

        boolean injected;
        if (!injector.isEnabled()) {
            injected = false;
        } else if (skip) {
            injected = injector.isForceInject();
        } else {
            injected = true;
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("action", "inject/query");
        r.put("skip", skip);
        r.put("injectorEnabled", injector.isEnabled());
        r.put("forceInject", injector.isForceInject());
        r.put("injected", injected);
        r.put("total", rows.size());
        r.put("demoTenantCount", demo);
        r.put("otherTenantCount", other);
        r.put("list", rows);
        return r;
    }

    /** 插入一个商品；tenantCode 为 null 时不设置（由填充器补 demo-tenant），非 null 则显式保留。 */
    private void product(String name, double price, String tenantCode) {
        MetaInsert insert = MetaFactory.insert(SampleProductEntity.class)
                .value("name", name)
                .value("price", price)
                .value("stock", 1)
                .value("status", 1);
        if (tenantCode != null) {
            insert.value("tenantCode", tenantCode);
        }
        insert.save();
    }
}
