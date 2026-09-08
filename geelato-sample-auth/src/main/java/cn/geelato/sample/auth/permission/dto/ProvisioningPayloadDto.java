package cn.geelato.sample.auth.permission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 认证中心 {@code GET /api/provisioning/user/{userId}} 响应载荷（与 {@code ProvisioningPayload} JSON 对齐）。
 * <p>外层为 {@code ApiResult}：{@code {msg, code, status, data}}，成功 {@code status="success"}。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProvisioningPayloadDto(
        String userId,
        String appCode,
        String tenantCode,
        Long version,
        List<PermissionRuntimeDto> dataPermissions,
        List<String> funcPermissions,
        List<String> roleCodes
) {

    /** 单条数据权限（与 {@code PermissionRuntime} 对齐）。 */
    public record PermissionRuntimeDto(
            String entity,
            String name,
            String rule,
            Integer weight,
            String type
    ) {
    }
}
