package cn.geelato.sample.orm.web;

import cn.geelato.orm.MetaFactory;
import cn.geelato.orm.page.PageResult;
import cn.geelato.orm.query.Filter;
import cn.geelato.orm.query.Order;
import cn.geelato.sample.orm.entity.SampleUserEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础 CRUD 示例：以 {@link SampleUserEntity} 为对象，演示 Fluent DSL（{@link MetaFactory}）
 * 的增、查（单条 / 列表 / 分页）、改、删；并保留一个“一气呵成”的冒烟测试接口。
 *
 * <p>语义上是 POST 的写操作这里统一用 GET，便于浏览器 / curl 直接验证。</p>
 * <p>主键 id 由框架在保存时自动生成（{@code UIDGenerator}），调用方无需手写。</p>
 */
@RestController
public class SampleOrmCrudController {

    /** 新增。 */
    @GetMapping("/api/orm/user/insert")
    public Map<String, Object> insert(@RequestParam(required = false) String name,
                                      @RequestParam(required = false) String email,
                                      @RequestParam(required = false, defaultValue = "1") Integer status) {
        String id = MetaFactory.insert(SampleUserEntity.class)
                .value("name", name)
                .value("email", email)
                .value("status", status)
                .save();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "insert");
        result.put("id", id);
        return result;
    }

    /** 列表查询。 */
    @GetMapping("/api/orm/user/list")
    public Map<String, Object> list() {
        List<Map<String, Object>> list = MetaFactory.query(SampleUserEntity.class)
                .order(Order.desc("id"))
                .list();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "list");
        result.put("count", list.size());
        result.put("list", list);
        return result;
    }

    /** 单条查询。 */
    @GetMapping("/api/orm/user/get")
    public Map<String, Object> get(@RequestParam String id) {
        Map<String, Object> one = MetaFactory.query(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .one();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "get");
        result.put("data", one);
        return result;
    }

    /** 分页查询：演示 select 投影 + order + page。 */
    @GetMapping("/api/orm/user/page")
    public Map<String, Object> page(@RequestParam(required = false, defaultValue = "1") int page,
                                    @RequestParam(required = false, defaultValue = "10") int size) {
        PageResult<Map<String, Object>> pg = MetaFactory.query(SampleUserEntity.class)
                .select(new String[]{"id", "name", "email", "status"})
                .order(Order.desc("id"))
                .page(page, size)
                .page();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "page");
        result.put("current", pg.getCurrent());
        result.put("size", pg.getSize());
        result.put("total", pg.getTotal());
        result.put("list", pg.getRecords());
        return result;
    }

    /** 更新：按 id 定位。 */
    @GetMapping("/api/orm/user/update")
    public Map<String, Object> update(@RequestParam String id,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) String email,
                                      @RequestParam(required = false) Integer status) {
        MetaFactory.update(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .value("name", name)
                .value("email", email)
                .value("status", status)
                .save();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "update");
        result.put("id", id);
        return result;
    }

    /** 删除：按 id 定位。 */
    @GetMapping("/api/orm/user/delete")
    public Map<String, Object> delete(@RequestParam String id) {
        // 说明：Fluent DSL 的 MetaFactory.delete() 是“逻辑删除”（update delStatus/deleteAt），
        // 需要实体带这些列（继承 BaseEntity）。本示例实体为精简的 IdEntity，故用原生 SQL 做物理删除。
        int affected = MetaFactory.sql("delete from sample_user where id = ?")
                .param(id)
                .execute();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "delete");
        result.put("id", id);
        result.put("affected", affected);
        return result;
    }

    /**
     * 一气呵成冒烟测试：insert → query → update → query → delete → count，
     * 用于一次性验证基础 DSL 链路是否正常。
     */
    @GetMapping("/api/orm/crud")
    public Map<String, Object> crud() {
        String id = MetaFactory.insert(SampleUserEntity.class)
                .value("name", "Alice")
                .value("email", "alice@example.com")
                .value("status", 1)
                .save();

        Map<String, Object> beforeUpdate = MetaFactory.query(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .one();

        MetaFactory.update(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .value("name", "Bob")
                .value("email", "bob@example.com")
                .save();

        Map<String, Object> afterUpdate = MetaFactory.query(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .one();

        int deleted = MetaFactory.sql("delete from sample_user where id = ?")
                .param(id)
                .execute();

        long remaining = MetaFactory.query(SampleUserEntity.class)
                .where(Filter.eq("id", id))
                .count();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("insertedId", id);
        payload.put("beforeUpdate", beforeUpdate);
        payload.put("afterUpdate", afterUpdate);
        payload.put("deleted", deleted);
        payload.put("remaining", remaining);
        return payload;
    }
}
