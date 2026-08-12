package cn.geelato.sample.orm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 在 H2 中创建演示用表值函数（存储过程）{@code proc_product_stats(minStock)}。
 *
 * <p>该函数返回一个结果集：按 category_id 分组统计库存达标（stock &gt;= minStock）商品的
 * 数量(cnt)与均价(avg_price)。</p>
 *
 * <p>放在 schema.sql 里会因为 Java 源码内含分号/双引号被 Spring 脚本切分器误切，
 * 故这里在启动期用 {@link JdbcTemplate#execute(String)} 整体下发，由 H2 自行解析。
 * 函数体内的 SQL 在被 CALL 时才执行，因此建表先后顺序不影响。</p>
 */
@Component
public class SampleH2FunctionInitializer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        // CREATE ALIAS ... AS '<java source>'，源码用单引号包裹，内部双引号/分号对 H2 安全。
        String createAlias = "CREATE ALIAS IF NOT EXISTS proc_product_stats AS "
                + "'java.sql.ResultSet proc(java.sql.Connection conn, Integer minStock) throws java.sql.SQLException { "
                + "return conn.createStatement().executeQuery(\""
                + "select category_id, count(*) as cnt, coalesce(avg(price),0) as avg_price "
                + "from sample_adv_product where stock >= \" + minStock + \" group by category_id\"); "
                + "}'";
        jdbcTemplate.execute(createAlias);
    }
}
