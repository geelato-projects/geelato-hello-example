package cn.geelato.sample.auth.permission;

import cn.geelato.security.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 本地权限镜像表 {@code auth_perm_mirror} 读写（认证中心不可达时降级读，保证业务不中断）。
 * <p>仅持久化数据权限（type=dp）用于降级；功能权限来自 introspect，不镜像。
 */
@Slf4j
@Component
public class PermissionMirrorRepository {

    private final JdbcTemplate jdbc;

    public PermissionMirrorRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 覆盖式写入某用户的数据权限镜像（先删后插）。 */
    public void save(String userId, List<Permission> dps, String appCode, String tenantCode) {
        try {
            jdbc.update("DELETE FROM auth_perm_mirror WHERE user_id = ?", userId);
            if (dps == null || dps.isEmpty()) {
                return;
            }
            for (Permission p : dps) {
                if (p == null || p.getRule() == null || p.getRule().isBlank()) {
                    continue;
                }
                jdbc.update(
                        "INSERT INTO auth_perm_mirror(id,user_id,perm_type,entity,name,rule,weight,app_code,tenant_code,synced_at) "
                                + "VALUES(?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                        UUID.randomUUID().toString(), userId, "dp", p.getEntity(), p.getName(), p.getRule(),
                        p.getWeight() == null ? 0 : p.getWeight(), appCode, tenantCode);
            }
        } catch (Exception e) {
            log.warn("镜像表写入失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /** 读取某用户的数据权限镜像（降级用）。 */
    public List<Permission> loadDataPermissions(String userId) {
        try {
            return jdbc.query(
                    "SELECT entity,name,rule,weight FROM auth_perm_mirror WHERE user_id = ? AND perm_type = 'dp'",
                    (rs, i) -> {
                        Permission p = new Permission();
                        p.setEntity(rs.getString("entity"));
                        p.setName(rs.getString("name"));
                        p.setRule(rs.getString("rule"));
                        int w = rs.getInt("weight");
                        p.setWeight(rs.wasNull() ? 0 : w);
                        return p;
                    }, userId);
        } catch (Exception e) {
            log.warn("镜像表读取失败 userId={}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
}
