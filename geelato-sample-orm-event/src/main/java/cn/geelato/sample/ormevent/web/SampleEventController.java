package cn.geelato.sample.ormevent.web;

import cn.geelato.core.mql.filter.FilterGroup;
import cn.geelato.core.orm.Dao;
import cn.geelato.sample.ormevent.entity.SampleProductEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 事件机制演示 Controller。
 *
 * <p>提供接口触发 insert/update/delete/query，观察各事件监听器的控制台输出。
 * 写操作走 {@link Dao} 路径，使事务感知 after 回调（afterCommit/afterRollback）完整生效。
 *
 * <p>验证步骤：依次访问下列接口，观察控制台日志：
 * <ul>
 *   <li>{@code GET /api/event/insert} —— 触发 BeforeSave 校验 + AfterSave 审计 + 提交后 ES 同步</li>
 *   <li>{@code GET /api/event/update/{id}} —— 触发更新事件</li>
 *   <li>{@code GET /api/event/query} —— 触发查询事件（慢查询统计）</li>
 *   <li>{@code GET /api/event/delete/{id}} —— 触发删除事件 + 函数式 callback</li>
 *   <li>{@code GET /api/event/insertIllegal} —— 价格为负，BeforeSave 校验拦截（保存被阻断）</li>
 *   <li>{@code GET /api/event/rollback} —— 演示事务回滚，afterRollback 触发、跳过 ES 同步</li>
 * </ul>
 */
@RestController
public class SampleEventController {

    @Autowired
    @Qualifier("primaryDao")
    private Dao dao;

    /** 新增商品：触发 before/after/事务感知 after(commit)。 */
    @GetMapping("/api/event/insert")
    public Map<String, Object> insert() {
        SampleProductEntity p = newProduct("示例商品", 99.9, 100);
        Map<String, Object> saved = dao.save(p);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "insert");
        result.put("saved", saved);
        result.put("tip", "查看控制台：BeforeSave 校验 / AfterSave 审计 / TransactionalAfterSave#afterCommit ES 同步");
        return result;
    }

    /** 新增非法商品（价格为负）：BeforeSave 校验拦截，保存被阻断。 */
    @GetMapping("/api/event/insertIllegal")
    public Map<String, Object> insertIllegal() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            SampleProductEntity p = newProduct("非法商品", -10.0, 1);
            dao.save(p);
            result.put("blocked", false);
        } catch (IllegalArgumentException ex) {
            // 被 BeforeSave 监听器拦截
            result.put("blocked", true);
            result.put("reason", ex.getMessage());
            result.put("tip", "BeforeSave 监听器抛异常，保存被阻断（框架透传异常，不加全局开关）");
        }
        return result;
    }

    /** 更新商品：触发更新事件。 */
    @GetMapping("/api/event/update/{id}")
    public Map<String, Object> update(@PathVariable String id) {
        SampleProductEntity p = new SampleProductEntity();
        p.setId(id);
        p.setName("已更新商品");
        p.setPrice(199.0);
        p.setStock(50);
        Map<String, Object> saved = dao.save(p);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "update");
        result.put("updated", saved);
        return result;
    }

    /** 查询商品：触发查询事件（慢查询统计）。 */
    @GetMapping("/api/event/query")
    public Map<String, Object> query() {
        List<Map<String, Object>> list = dao.queryForMapList(SampleProductEntity.class, new FilterGroup());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "query");
        result.put("count", list == null ? 0 : list.size());
        result.put("tip", "查看控制台：AfterQuery 慢查询统计");
        return result;
    }

    /** 删除商品：触发删除事件 + 函数式 callback。 */
    @GetMapping("/api/event/delete/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        int n = dao.delete(SampleProductEntity.class, "id", id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "delete");
        result.put("affectedRows", n);
        result.put("tip", "查看控制台：AfterDeleteCallback 函数式 callback");
        return result;
    }

    /**
     * 事务回滚演示：在事务内保存后抛异常，事务回滚，
     * 触发 TransactionalAfterSave#afterRollback（跳过 ES 同步）。
     */
    @GetMapping("/api/event/rollback")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rollback() {
        SampleProductEntity p = newProduct("回滚商品", 50.0, 10);
        dao.save(p);
        // 保存成功但主动抛异常 → 事务回滚 → afterRollback 触发（而非 afterCommit）
        throw new IllegalStateException("演示事务回滚：保存后主动抛异常");
    }

    private SampleProductEntity newProduct(String name, double price, int stock) {
        SampleProductEntity p = new SampleProductEntity();
        p.setId(UUID.randomUUID().toString().replace("-", ""));
        p.setName(name);
        p.setPrice(price);
        p.setStock(stock);
        return p;
    }
}
