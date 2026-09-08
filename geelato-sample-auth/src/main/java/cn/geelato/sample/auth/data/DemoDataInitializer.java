package cn.geelato.sample.auth.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时建表并灌入种子数据（用 primaryJdbcTemplate 直接执行 DDL/DML，
 * 避免动态数据源下 spring.sql.init 的不确定性）。
 * <p>H2 内存库每次启动为空，故直接 INSERT 即可，无需去重。
 *
 * <p><b>注意：</b>{@code demo_user.id} 需与认证中心 {@code platform_user.id} 对齐，
 * 这样 provisioning 才能按 userId 解析到该用户在认证中心配置的数据权限。
 * 请按你环境里的真实用户 id 调整下方种子（或登录后用真实 id 覆盖）。
 */
@Slf4j
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public DemoDataInitializer(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS demo_user ("
                + "id VARCHAR(64) PRIMARY KEY, login_name VARCHAR(128), name VARCHAR(128), "
                + "dept_id VARCHAR(64), bu_id VARCHAR(64), org_id VARCHAR(64), tenant_code VARCHAR(64))");

        jdbc.execute("CREATE TABLE IF NOT EXISTS demo_order ("
                + "id VARCHAR(64) PRIMARY KEY, title VARCHAR(128), "
                + "dept_id VARCHAR(64), bu_id VARCHAR(64), creator VARCHAR(64), tenant_code VARCHAR(64))");

        jdbc.execute("CREATE TABLE IF NOT EXISTS auth_perm_mirror ("
                + "id VARCHAR(64) PRIMARY KEY, user_id VARCHAR(64), perm_type VARCHAR(8), "
                + "entity VARCHAR(255), name VARCHAR(128), rule VARCHAR(1024), weight INT, "
                + "app_code VARCHAR(128), tenant_code VARCHAR(64), synced_at TIMESTAMP)");

        // 用户：id 为占位符，请按认证中心 platform_user.id 调整
        insertUser("u-alice", "alice", "Alice", "dept-01", "bu-01", "org-01", "geelato");
        insertUser("u-bob", "bob", "Bob", "dept-02", "bu-01", "org-01", "geelato");

        // 订单：跨部门/事业部/创建人，便于观察 &myDept / &myself / &all 的过滤差异
        insertOrder("o1", "订单-A（dept-01，alice 创建）", "dept-01", "bu-01", "u-alice", "geelato");
        insertOrder("o2", "订单-B（dept-02，bob 创建）", "dept-02", "bu-01", "u-bob", "geelato");
        insertOrder("o3", "订单-C（dept-01，bob 创建）", "dept-01", "bu-01", "u-bob", "geelato");
        insertOrder("o4", "订单-D（dept-03，alice 创建）", "dept-03", "bu-02", "u-alice", "geelato");

        log.info("demo 数据初始化完成：2 用户 / 4 订单（demo_user.id 需与认证中心 platform_user.id 对齐）");
    }

    private void insertUser(String id, String loginName, String name, String deptId, String buId, String orgId, String tenant) {
        jdbc.update("INSERT INTO demo_user(id,login_name,name,dept_id,bu_id,org_id,tenant_code) VALUES(?,?,?,?,?,?,?)",
                id, loginName, name, deptId, buId, orgId, tenant);
    }

    private void insertOrder(String id, String title, String deptId, String buId, String creator, String tenant) {
        jdbc.update("INSERT INTO demo_order(id,title,dept_id,bu_id,creator,tenant_code) VALUES(?,?,?,?,?,?)",
                id, title, deptId, buId, creator, tenant);
    }
}
