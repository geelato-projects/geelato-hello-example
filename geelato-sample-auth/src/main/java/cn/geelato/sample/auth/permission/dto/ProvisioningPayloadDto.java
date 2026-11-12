package cn.geelato.sample.auth.permission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 认证中心 {@code GET /api/provisioning/user/{userId}} 响应载荷（与 {@code ProvisioningPayload} JSON 对齐）。
 * <p>外层为 {@code ApiResult}：{@code {msg, code, status, data}}，成功 {@code status="success"}。
 * <p>五类权限统一为 {@link PermissionItemDto}（code/name/rule/object/seqNo）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProvisioningPayloadDto(
        String userId,
        String appCode,
        String tenantCode,
        Long version,
        List<PermissionItemDto> dataPermissions,
        List<PermissionItemDto> modelPermissions,
        List<PermissionItemDto> columnPermissions,
        List<PermissionItemDto> elementPermissions,
        List<PermissionItemDto> functionPermissions,
        List<MenuNodeDto> menus,
        List<String> roleCodes,
        List<String> deptRoleCodes
) {

    /** 菜单树节点（与认证中心 MenuTreeNode 对齐，接入方可直接渲染）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MenuNodeDto(
            String id,
            String parentId,
            String name,
            String path,
            String component,
            String icon,
            Integer sortNo,
            String permissionCode,
            String type,
            List<MenuNodeDto> children
    ) {
    }

    /** 单条权限项（五类同构：code/name/rule/object/seqNo）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PermissionItemDto(
            String code,
            String name,
            String rule,
            String object,
            Integer seqNo
    ) {
    }
}
