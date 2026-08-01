package cn.geelato.sample.ormevent.event;

import cn.geelato.core.orm.event.AfterQueryEventListener;
import cn.geelato.core.orm.event.QueryEventContext;

/**
 * 场景 4：查询后慢查询统计（AfterQuery）。
 *
 * <p>演示「查询事件」：查询完成后统计耗时与返回行数，超阈值告警。
 * 适用于读审计、慢查询监控、缓存预热等场景。
 */
public class SlowQueryListener implements AfterQueryEventListener {

    /** 慢查询阈值（毫秒）。示例阈值设低，便于演示触发。 */
    private static final long SLOW_THRESHOLD_MS = 50;

    @Override
    public void beforeQuery(QueryEventContext context) {
    }

    @Override
    public void afterQuery(QueryEventContext context) {
        long cost = System.currentTimeMillis() - context.getStartTime();
        String entity = context.getEntityType() != null ? context.getEntityType().getSimpleName() : "raw";
        System.out.println("[AfterQuery] 查询完成 entity=" + entity
                + ", rows=" + context.getRowCount()
                + ", cost=" + cost + "ms"
                + (cost > SLOW_THRESHOLD_MS ? " (慢查询)" : "")
                + ", eventId=" + context.getEventId());
    }

    @Override
    public boolean supports(QueryEventContext context) {
        return true;
    }

    @Override
    public boolean enabled(QueryEventContext context) {
        return true;
    }
}
