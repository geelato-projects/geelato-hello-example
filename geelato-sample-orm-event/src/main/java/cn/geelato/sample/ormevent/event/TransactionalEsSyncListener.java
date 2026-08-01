package cn.geelato.sample.ormevent.event;

import cn.geelato.core.orm.event.SaveEventContext;
import cn.geelato.core.orm.event.TransactionalAfterSaveEventListener;

/**
 * 场景 3：事务感知后置（TransactionalAfterSave）。
 *
 * <p>演示「提交后才执行」：模拟 ES 同步，解决普通 After「异步、事务外、可能读到回滚数据」的问题。
 * - {@link #afterCommit} 仅在事务真正提交后触发（无事务时立即触发，视为已提交）
 * - {@link #afterRollback} 在事务回滚后触发（避免向 ES 写入回滚的脏数据）
 *
 * <p>注意：事务感知 after 回调在 {@code Dao.save/delete/multiSave/multiDelete} 路径完整生效。
 * 本示例 Controller 走 {@code Dao} 路径以触发该能力。
 */
public class TransactionalEsSyncListener implements TransactionalAfterSaveEventListener {

    @Override
    public void beforeSave(SaveEventContext context) {
    }

    @Override
    public void afterSave(SaveEventContext context) {
        // 普通的异步 after（本监听器不关心，留空）。只走事务感知的 afterCommit/afterRollback。
    }

    @Override
    public void afterCommit(SaveEventContext context) {
        if (!"SampleProduct".equalsIgnoreCase(context.getCommand().getEntityName())) {
            return;
        }
        // 此时事务已提交，数据已落库，可安全同步到 ES（示例仅打印）
        System.out.println("[TransactionalAfterSave#afterCommit] 事务已提交，同步商品到 ES，eventId=" + context.getEventId());
    }

    @Override
    public void afterRollback(SaveEventContext context) {
        if (!"SampleProduct".equalsIgnoreCase(context.getCommand().getEntityName())) {
            return;
        }
        // 事务回滚，不向 ES 写入（避免脏数据）
        System.out.println("[TransactionalAfterSave#afterRollback] 事务已回滚，跳过 ES 同步，eventId=" + context.getEventId());
    }

    @Override
    public boolean supports(SaveEventContext context) {
        return context.getCommand() != null
                && "SampleProduct".equalsIgnoreCase(context.getCommand().getEntityName());
    }

    @Override
    public boolean enabled(SaveEventContext context) {
        return true;
    }
}
