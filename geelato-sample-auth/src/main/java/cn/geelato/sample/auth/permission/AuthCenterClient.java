package cn.geelato.sample.auth.permission;

import cn.geelato.sample.auth.config.DemoAuthProperties;
import cn.geelato.sample.auth.permission.dto.ApiResultWrapper;
import cn.geelato.sample.auth.permission.dto.IntrospectionResult;
import cn.geelato.sample.auth.permission.dto.ProvisioningPayloadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证中心 HTTP 客户端（远程调用，demo 不依赖 Sa-Token）：
 * <ul>
 *   <li>{@code introspect} — RFC7662 自省，校验令牌 + 取 permissions/roles。</li>
 *   <li>{@code syncUserPermissions} — 转发用户 Bearer 取数据权限载荷。</li>
 *   <li>{@code login} — 便捷代理，便于全程只与 demo 交互。</li>
 * </ul>
 */
@Slf4j
@Component
public class AuthCenterClient {

    private static final ParameterizedTypeReference<ApiResultWrapper<ProvisioningPayloadDto>> SYNC_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final DemoAuthProperties props;

    public AuthCenterClient(RestTemplate authCenterRestTemplate, DemoAuthProperties props) {
        this.restTemplate = authCenterRestTemplate;
        this.props = props;
    }

    /**
     * 调认证中心 introspect 校验令牌并取权限/角色声明。
     *
     * @return 自省结果；令牌无效返回 active=false 的结果，调用异常返回 null。
     */
    public IntrospectionResult introspect(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String url = props.getAuthCenter().getBaseUrl() + "/oauth2/introspect";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        form.add("client_id", props.getAuthCenter().getClientId());
        form.add("client_secret", props.getAuthCenter().getClientSecret());
        form.add("appCode", props.getAuthCenter().getAppCode());
        try {
            return restTemplate.postForObject(url, form, IntrospectionResult.class);
        } catch (Exception e) {
            log.warn("introspect 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 转发用户 Bearer 调 {@code /api/provisioning/user/{userId}} 取数据权限载荷。
     *
     * @return 载荷；非成功或调用异常返回 null（调用方可降级读本地镜像）。
     */
    public ProvisioningPayloadDto syncUserPermissions(String userId, String appCode, String tenantCode, String bearerToken) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(props.getAuthCenter().getBaseUrl() + "/api/provisioning/user/" + userId);
        if (appCode != null && !appCode.isBlank()) {
            b.queryParam("appCode", appCode);
        }
        if (tenantCode != null && !tenantCode.isBlank()) {
            b.queryParam("tenantCode", tenantCode);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        try {
            ResponseEntity<ApiResultWrapper<ProvisioningPayloadDto>> resp = restTemplate
                    .exchange(b.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), SYNC_TYPE);
            ApiResultWrapper<ProvisioningPayloadDto> body = resp.getBody();
            if (body != null && body.isSuccess()) {
                return body.data();
            }
            log.warn("provisioning 返回非成功: status={}", body == null ? "null" : body.status());
            return null;
        } catch (Exception e) {
            log.warn("provisioning 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /** 便捷登录代理：转发 username/password 到认证中心 {@code /api/auth/login}，返回其原始响应。 */
    public Map<String, Object> login(String username, String password) {
        String url = props.getAuthCenter().getBaseUrl() + "/api/auth/login?client_id="
                + props.getAuthCenter().getClientId();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username == null ? "" : username);
        body.put("password", password == null ? "" : password);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            return result;
        } catch (Exception e) {
            log.warn("login 代理调用失败: {}", e.getMessage());
            return null;
        }
    }
}
