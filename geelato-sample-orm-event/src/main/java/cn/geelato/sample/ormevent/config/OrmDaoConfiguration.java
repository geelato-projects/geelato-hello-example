package cn.geelato.sample.ormevent.config;

import cn.geelato.core.orm.Dao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 暴露 geelato-orm 的 Dao Bean。
 *
 * <p>本示例只用裸 geelato-orm（不用 framework-starter），需手动暴露 Dao。
 * 事件机制随 geelato-orm 引入，由 OrmEventAutoConfiguration 自动装配线程池。
 */
@Configuration
public class OrmDaoConfiguration {

    @Bean
    public Dao primaryDao(JdbcTemplate jdbcTemplate) {
        return new Dao(jdbcTemplate);
    }
}
