package cn.geelato.sample.ormevent.event;

import cn.geelato.core.orm.event.AfterSaveEventListener;
import cn.geelato.core.orm.event.SaveEventContext;

/**
 * 场景 2：保存后异步旁路（AfterSave）。
 *
 * <p>演示「后置异步 + 执行结果回传」：主保存完成后打印审计日志。
 * 异步执行，异常只记日志、不影响主流程。
 *
 * <p>演示从上下文读取执行结果：{@link SaveEventContext#isSuccess()} / {@code getException()}。
 */
public class AuditLogAfterSaveListener implements AfterSaveEventListener {

    @Override
    public void beforeSave(SaveEventContext context) {
        // 本监听器只关心后置，before 留空
    }

    @Override
    public void afterSave(SaveEventContext context) {
        if (!"SampleProduct".equalsIgnoreCase(context.getCommand().getEntityName())) {
            return;
        }
        System.out.println("[AfterSave] 商品保存完成 eventId=" + context.getEventId()
                + ", success=" + context.isSuccess()
                + ", 耗时=" + (System.currentTimeMillis() - context.getStartTime()) + "ms");
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
