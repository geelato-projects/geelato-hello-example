package cn.geelato.sample.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 读取 demo 本地用户表 {@code demo_user}。
 * <p>认证中心 introspect 返回的 subject 为 loginName，据此查本地用户取业务字段（deptId/buId/orgId）与 id。
 */
@Slf4j
@Component
public class DemoUserRepository {

    private final JdbcTemplate jdbc;

    public DemoUserRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DemoUserInfo findByLoginName(String loginName) {
        if (loginName == null || loginName.isBlank()) {
            return null;
        }
        try {
            return jdbc.query(
                    "SELECT id,login_name,name,dept_id,bu_id,org_id,tenant_code "
                            + "FROM demo_user WHERE login_name = ? LIMIT 1",
                    (rs, i) -> new DemoUserInfo(
                            rs.getString("id"),
                            rs.getString("login_name"),
                            rs.getString("name"),
                            rs.getString("dept_id"),
                            rs.getString("bu_id"),
                            rs.getString("org_id"),
                            rs.getString("tenant_code")),
                    loginName).stream().findFirst().orElse(null);
        } catch (Exception e) {
            log.warn("查询 demo_user 失败 loginName={}: {}", loginName, e.getMessage());
            return null;
        }
    }
}
