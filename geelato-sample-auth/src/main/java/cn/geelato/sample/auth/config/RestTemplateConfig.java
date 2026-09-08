package cn.geelato.sample.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** HTTP 客户端：调用认证中心 introspect / provisioning / login。 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate authCenterRestTemplate() {
        return new RestTemplate();
    }
}
