package cn.geelato.sample.auth.security;

import java.util.List;

/**
 * Demo 请求级主体持有者（ThreadLocal）。
 * <p>{@code cn.geelato.security.SecurityContext} 持有的 {@link cn.geelato.security.User} 只携带数据权限，
 * 不含功能权限码；故 demo 另用本持有者承载 {@code funcPermissions}/{@code roleCodes}（来自 introspect 声明），
 * 供 {@code PermissionAspect} 做功能/角色校验。
 * <p>由 {@code AuthFilter} 在请求开始时 set，请求结束时 clear。
 */
public final class CurrentUserHolder {

    private CurrentUserHolder() {
    }

    public record RequestPrincipal(String userId, String loginName,
                                   List<String> funcPermissions, List<String> roleCodes) {
    }

    private static final ThreadLocal<RequestPrincipal> HOLDER = new ThreadLocal<>();

    public static void set(RequestPrincipal principal) {
        HOLDER.set(principal);
    }

    public static RequestPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
