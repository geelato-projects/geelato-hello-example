package cn.geelato.sample.auth.permission;

import cn.geelato.core.mql.command.QueryCommand;
import cn.geelato.orm.query.MetaQuery;
import cn.geelato.orm.spi.FluentQueryFilterInjector;
import cn.geelato.sample.auth.config.DemoAuthProperties;
import cn.geelato.security.Permission;
import cn.geelato.security.PermissionRuleUtils;
import cn.geelato.security.SecurityContext;
import cn.geelato.security.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 数据权限注入器：参照 community {@code PlatformQueryFilterSupport}，在 ORM Fluent 查询执行前
 * 把当前用户对该实体的数据权限规则（替换 {@code #currentUser.xxx#} 后）追加为原始 where。
 * <p>关键路径：{@code MetaFactory.query(...)} → {@code QueryCommandAdapter} →
 * {@code FluentQueryFilterRuntimeResolver.injectIfAvailable} → 本注入器。
 *
 * <h3>demo 行为</h3>
 * <ul>
 *   <li>当前无登录用户（匿名）→ 不注入。</li>
 *   <li>该实体无任何数据权限配置 → 不注入（未配置=全可见，对比效果明显）。</li>
 *   <li>有规则 → 按 weight 取最大范围者，替换占位符后追加到 {@code originalWhere}（多条用 OR 合并，整体与原条件 AND）。</li>
 * </ul>
 */
@Slf4j
@Component
public class DataPermissionFilterInjector implements FluentQueryFilterInjector {

    /** 最近一次注入的 where 片段（同线程），供控制器观察「本次生效规则」。控制器取走后自动清理。 */
    private static final ThreadLocal<String> LAST_APPLIED = new ThreadLocal<>();

    private final DemoAuthProperties props;

    public DataPermissionFilterInjector(DemoAuthProperties props) {
        this.props = props;
    }

    public static String pollLastApplied() {
        String r = LAST_APPLIED.get();
        LAST_APPLIED.remove();
        return r;
    }

    @Override
    public boolean isEnabled() {
        return props.getPermission().isDataEnabled();
    }

    @Override
    public void inject(QueryCommand command, MetaQuery query) {
        LAST_APPLIED.remove();
        String entity = command == null ? null : command.getEntityName();
        if (entity == null || entity.isBlank()) {
            return;
        }
        User user = SecurityContext.getCurrentUser();
        if (user == null) {
            return;
        }
        List<Permission> perms = user.getDataPermissions();
        if (perms == null || perms.isEmpty()) {
            return;
        }
        // 命中该实体的数据权限（大小写不敏感），按 weight 取最大范围者
        Permission matched = perms.stream()
                .filter(p -> p != null && p.getRule() != null && !p.getRule().isBlank())
                .filter(p -> entity.equalsIgnoreCase(p.getEntity()))
                .max(Comparator.comparingInt(p -> p.getWeight() == null ? 0 : p.getWeight()))
                .orElse(null);
        if (matched == null) {
            return;
        }
        String resolved;
        try {
            resolved = PermissionRuleUtils.replaceRuleVariable(matched, user);
        } catch (Exception e) {
            log.warn("数据权限规则替换失败 entity={} rule={}: {}", entity, matched.getRule(), e.getMessage());
            return;
        }
        if (resolved == null || resolved.isBlank()) {
            return;
        }
        String existing = command.getOriginalWhere();
        String merged = (existing == null || existing.isBlank())
                ? resolved
                : "(" + existing + ") AND (" + resolved + ")";
        command.setOriginalWhere(merged);
        LAST_APPLIED.set(merged);
        log.info("数据权限注入 entity={} where={} loginName={}", entity, merged, user.getLoginName());
    }
}
