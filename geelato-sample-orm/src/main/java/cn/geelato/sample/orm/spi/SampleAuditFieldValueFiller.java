package cn.geelato.sample.orm.spi;

import cn.geelato.core.meta.model.entity.EntityMeta;
import cn.geelato.core.mql.command.CommandType;
import cn.geelato.orm.spi.FluentSaveFieldValueFillContext;
import cn.geelato.orm.spi.FluentSaveFieldValueFiller;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 默认字段填充 SPI 示例实现（{@link FluentSaveFieldValueFiller}）。
 *
 * <p>本轻量示例未引入 {@code geelato-web-platform}（平台的默认填充器不存在），
 * 这里自定义一个 {@code @Component} 实现，演示如何在保存前自动补齐审计/租户字段。</p>
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li><b>实体感知</b>：通过 {@link EntityMeta#getFieldNames()} 判定，仅填充实体真实拥有的字段，
 *       因此不带这些字段的实体（如 sample_user / 分类 / 图片）完全不受影响。</li>
 *   <li><b>不覆盖业务值</b>：仅当当前值为 {@code null}（或空串）时才填充——
 *       业务侧显式传入的值（如“其它租户”）会被保留。</li>
 *   <li>与查询过滤注入 SPI 不同，<b>保存字段填充允许多个填充器并存</b>，resolver 会遍历所有启用者。</li>
 * </ul>
 */
@Component
public class SampleAuditFieldValueFiller implements FluentSaveFieldValueFiller {

    private static final String SYSTEM_USER = "system";
    private static final String DEMO_TENANT = "demo-tenant";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void fill(FluentSaveFieldValueFillContext context) {
        EntityMeta meta = context.getEntityMeta();
        Set<String> fields = meta == null ? new HashSet<>()
                : new HashSet<>(Arrays.asList(meta.getFieldNames()));
        Map<String, Object> target = context.getTargetValueMap();

        if (context.getCommandType() == CommandType.Insert) {
            fillIfAbsent(fields, target, "createAt", new Date());
            fillIfAbsent(fields, target, "creator", SYSTEM_USER);
            fillIfAbsent(fields, target, "tenantCode", DEMO_TENANT);
        } else if (context.getCommandType() == CommandType.Update) {
            fillIfAbsent(fields, target, "updateAt", new Date());
            fillIfAbsent(fields, target, "updater", SYSTEM_USER);
        }
    }

    /** 仅当实体拥有该字段、且当前值为 null/空串时才填充（不覆盖业务显式传入的值）。 */
    private void fillIfAbsent(Set<String> fields, Map<String, Object> target, String field, Object value) {
        if (!fields.contains(field)) {
            return;
        }
        Object current = target.get(field);
        if (current == null || (current instanceof String s && s.isEmpty())) {
            target.put(field, value);
        }
    }
}
