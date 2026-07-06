package cn.geelato.app.glskbuild;

import cn.geelato.web.platform.boot.BootApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"cn.geelato", "cn.geelato.app.glskbuild"})
@EnableConfigurationProperties
@EnableCaching
@EnableAsync(proxyTargetClass = true)
@Slf4j
public class GlSkBuildApplication extends BootApplication {

    public static void main(String[] args) {
        log.info("Starting GlSkBuildApplication");
        SpringApplication.run(GlSkBuildApplication.class, args);
    }
}
