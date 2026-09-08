package cn.geelato.sample.auth;

import cn.geelato.sample.auth.config.DemoAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 接入认证中心统一权限的 Demo（远程客户端）。
 * <p>不依赖 Sa-Token、不共享 Sa-Token 会话：令牌校验走认证中心标准 {@code /oauth2/introspect}，
 * 功能权限来自 introspect/userinfo 声明，数据权限来自 {@code /api/provisioning}，权限变更通过 Redis 通知失效。
 */
@SpringBootApplication
@EnableConfigurationProperties(DemoAuthProperties.class)
@EnableScheduling
@Slf4j
public class SampleAuthApplication {
    public static void main(String[] args) {
        log.info("Starting SampleAuthApplication (geelato-sample-auth)");
        SpringApplication.run(SampleAuthApplication.class, args);
    }
}
