package cn.geelato.sample.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo 接入认证中心的配置（前缀 {@code demo}）。
 * <p>示例工程用直接写死的值，不引入 ${ENV:default} 之类复杂变量。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "demo")
public class DemoAuthProperties {

    /** 认证中心相关配置。 */
    private AuthCenter authCenter = new AuthCenter();

    /** 权限相关配置。 */
    private Permission permission = new Permission();

    @Getter
    @Setter
    public static class AuthCenter {
        /** 认证中心基础地址。 */
        private String baseUrl = "http://localhost:9000";
        /** 本 demo 作为 OAuth 客户端的 client_id（在认证中心注册）。 */
        private String clientId = "sample-auth";
        /** 本 demo 的 client_secret。 */
        private String clientSecret = "";
        /** 子系统编码（= oauth_client.system_code，权限归属标识）。 */
        private String appCode = "sample-auth";
        /** introspect 结果按 token 缓存的秒数。 */
        private int introspectCacheSeconds = 30;
    }

    @Getter
    @Setter
    public static class Permission {
        /** 是否启用数据权限注入。 */
        private boolean dataEnabled = true;
    }
}
