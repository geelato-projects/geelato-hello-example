package cn.geelato.sample.auth.permission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 认证中心 {@code POST /oauth2/introspect} 响应（与 {@code IntrospectionResponse} JSON 对齐）。
 * <p>注意：认证中心 Phase 1 后，响应携带扩展声明 {@code permissions} / {@code roles}。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IntrospectionResult(
        boolean active,
        String subject,
        String username,
        String scope,
        String clientId,
        List<String> permissions,
        List<String> roles,
        Long exp
) {
}
