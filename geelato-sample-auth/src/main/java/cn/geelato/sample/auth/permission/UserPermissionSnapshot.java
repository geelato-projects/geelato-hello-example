package cn.geelato.sample.auth.permission;

import cn.geelato.security.Permission;

import java.util.List;

/**
 * 某用户的生效权限快照（来自认证中心 provisioning，缓存于本地）。
 * <p>{@code dataPermissions} 用于数据权限注入；{@code funcPermissions}/{@code roleCodes} 为冗余备份，
 * 功能权限主源为 introspect 声明。
 */
public record UserPermissionSnapshot(
        List<String> funcPermissions,
        List<Permission> dataPermissions,
        List<String> roleCodes
) {
    public static UserPermissionSnapshot empty() {
        return new UserPermissionSnapshot(List.of(), List.of(), List.of());
    }
}
