package cn.geelato.sample.ormevent.event;

import cn.geelato.core.orm.event.DeleteEventManager;
import cn.geelato.core.orm.event.QueryEventManager;
import cn.geelato.core.orm.event.SaveEventManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

/**
 * 事件监听器统一注册入口。
 *
 * <p>演示推荐做法：
 * <ol>
 *   <li>每个监听器单独一个类；</li>
 *   <li>在统一配置类里实例化并注册（register*IfAbsent 防重复）；</li>
 *   <li>{@code @PreDestroy} 注销（生命周期管理，防热部署/上下文刷新累积泄漏）；</li>
 *   <li>轻量场景可用函数式 callback（lambda）注册。</li>
 * </ol>
 */
@Configuration
public class EventConfiguration {

    // 持有监听器引用，供 @PreDestroy 注销
    private final PriceValidationBeforeSaveListener beforeSaveListener = new PriceValidationBeforeSaveListener();
    private final AuditLogAfterSaveListener afterSaveListener = new AuditLogAfterSaveListener();
    private final TransactionalEsSyncListener transactionalListener = new TransactionalEsSyncListener();
    private final SlowQueryListener slowQueryListener = new SlowQueryListener();

    @PostConstruct
    public void registerListeners() {
        // 保存事件：before（校验，order=10 先执行）/ after（审计）/ 事务感知 after（ES 同步）
        SaveEventManager.registerBeforeIfAbsent(beforeSaveListener);
        SaveEventManager.registerAfterIfAbsent(afterSaveListener);
        SaveEventManager.registerAfterIfAbsent(transactionalListener);

        // 查询事件：慢查询统计
        QueryEventManager.registerAfterIfAbsent(slowQueryListener);

        // 场景 6：函数式 callback（lambda）演示——删除后清理，无需写实现类
        DeleteEventManager.registerAfterCallback(ctx ->
                System.out.println("[AfterDeleteCallback] 删除完成 entity="
                        + (ctx.getCommand() != null ? ctx.getCommand().getEntityName() : "null")
                        + ", affectedRows=" + ctx.getAffectedRows()
                        + ", eventId=" + ctx.getEventId()));

        System.out.println("[EventConfiguration] ORM 事件监听器已注册：beforeSave(校验) / afterSave(审计) / transactionalAfter(ES) / afterQuery(慢查询) / afterDeleteCallback(函数式)");
    }

    @PreDestroy
    public void unregisterListeners() {
        // 生命周期管理：容器销毁时注销，防累积泄漏
        SaveEventManager.unregisterBefore(beforeSaveListener);
        SaveEventManager.unregisterAfter(afterSaveListener);
        SaveEventManager.unregisterAfter(transactionalListener);
        QueryEventManager.unregisterAfter(slowQueryListener);
        System.out.println("[EventConfiguration] ORM 事件监听器已注销");
    }
}
