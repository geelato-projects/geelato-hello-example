package cn.geelato.sample.orm.spi;

import cn.geelato.core.meta.MetaManager;
import cn.geelato.core.meta.model.entity.EntityMeta;
import cn.geelato.core.mql.command.QueryCommand;
import cn.geelato.core.mql.filter.FilterGroup;
import cn.geelato.orm.query.MetaQuery;
import cn.geelato.orm.spi.FluentQueryFilterInjector;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 查询过滤注入 SPI 示例实现（{@link FluentQueryFilterInjector}）：模拟“多租户隔离”。
 *
 * <p>对所有“含 {@code tenantCode} 字段”的实体的 DSL 查询，自动附加 {@code tenant_code = 'demo-tenant'}。</p>
 *
 * <p>与 {@code disableInjectFilter()} / {@code isForceInject()} 的配合（见
 * {@code FluentQueryFilterRuntimeResolver.injectIfAvailable}）：</p>
 * <ul>
 *   <li>{@code isEnabled()}=false → 注入器完全不参与；</li>
 *   <li>查询调用 {@code disableInjectFilter()} 且 {@code isForceInject()}=false → 本次跳过注入；</li>
 *   <li>{@code isForceInject()}=true → 即使 {@code disableInjectFilter()} 也会强制注入。</li>
 * </ul>
 *
 * <p>这里把 {@code enabled} / {@code forceInject} 暴露为可运行期切换的字段，
 * 便于在 {@code /api/orm/spi/inject/*} 端点上演示“注入 / 跳过 / 强制注入”三种语义。</p>
 */
@Component
public class SampleTenantQueryFilterInjector implements FluentQueryFilterInjector {

    private static final String DEMO_TENANT = "demo-tenant";

    private volatile boolean enabled = true;
    private volatile boolean forceInject = false;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isForceInject() {
        return forceInject;
    }

    @Override
    public void inject(QueryCommand command, MetaQuery query) {
        String entityName = query == null ? null : query.resolveEntityName();
        if (entityName == null) {
            return;
        }
        EntityMeta meta = MetaManager.singleInstance().getByEntityName(entityName);
        if (meta == null) {
            return;
        }
        Set<String> fields = new HashSet<>(Arrays.asList(meta.getFieldNames()));
        if (!fields.contains("tenantCode")) {
            // 仅对带租户字段的实体注入，避免影响 sample_user 等无租户表
            return;
        }
        FilterGroup where = command.getWhere();
        if (where == null) {
            where = new FilterGroup();
            command.setWhere(where);
        }
        where.addFilter("tenantCode", DEMO_TENANT);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setForceInject(boolean forceInject) {
        this.forceInject = forceInject;
    }
}
