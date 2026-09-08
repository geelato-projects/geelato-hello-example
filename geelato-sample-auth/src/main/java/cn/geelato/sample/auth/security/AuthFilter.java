package cn.geelato.sample.auth.security;

import cn.geelato.sample.auth.permission.AuthCenterClient;
import cn.geelato.sample.auth.permission.PermissionCache;
import cn.geelato.sample.auth.permission.ProvisioningService;
import cn.geelato.sample.auth.permission.UserPermissionSnapshot;
import cn.geelato.sample.auth.permission.dto.IntrospectionResult;
import cn.geelato.sample.auth.config.DemoAuthProperties;
import cn.geelato.security.SecurityContext;
import cn.geelato.security.Tenant;
import cn.geelato.security.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 请求鉴权过滤器（协议级，不依赖 Sa-Token）。
 * <ol>
 *   <li>取 {@code Authorization: Bearer <token>}（兼容 {@code ?access_token=}）；无 token → 匿名放行。</li>
 *   <li>有 token → 调认证中心 introspect（带 TTL 缓存）→ 非 active 返回 401。</li>
 *   <li>subject=loginName → 查本地 demo_user 取业务字段与 userId。</li>
 *   <li>按 userId 拉取数据权限快照（provisioning，缓存/镜像降级）。</li>
 *   <li>组装 {@link User}（数据权限 + 业务字段）置入 {@link SecurityContext}（供数据权限注入器），
 *       并把 funcPermissions/roleCodes 置入 {@link CurrentUserHolder}（供功能权限 AOP）。</li>
 * </ol>
 * 请求结束 finally 清理线程上下文。
 */
@Slf4j
@Component
public class AuthFilter extends OncePerRequestFilter {

    private final AuthCenterClient client;
    private final PermissionCache cache;
    private final ProvisioningService syncService;
    private final DemoUserRepository userRepository;
    private final DemoAuthProperties props;

    public AuthFilter(AuthCenterClient client, PermissionCache cache, ProvisioningService syncService,
                      DemoUserRepository userRepository, DemoAuthProperties props) {
        this.client = client;
        this.cache = cache;
        this.syncService = syncService;
        this.userRepository = userRepository;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        try {
            if (token != null) {
                IntrospectionResult ir = cache.getIntrospection(token);
                if (ir == null) {
                    ir = client.introspect(token);
                    if (ir != null && ir.active()) {
                        cache.putIntrospection(token, ir);
                    }
                }
                if (ir == null || !ir.active()) {
                    deny(response, "invalid or expired token");
                    return;
                }
                establishPrincipal(ir, token);
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
            CurrentUserHolder.clear();
        }
    }

    private void establishPrincipal(IntrospectionResult ir, String token) {
        String loginName = ir.subject();
        DemoUserInfo ui = loginName == null ? null : userRepository.findByLoginName(loginName);
        String userId = ui != null ? ui.id() : null;
        String tenant = ui != null ? ui.tenantCode() : null;
        String appCode = props.getAuthCenter().getAppCode();

        UserPermissionSnapshot snap = (userId != null)
                ? syncService.getOrLoad(userId, appCode, tenant, token)
                : UserPermissionSnapshot.empty();

        User user = new User();
        user.setUserId(userId);
        user.setLoginName(loginName);
        user.setUserName(ui != null && ui.name() != null ? ui.name() : loginName);
        if (ui != null) {
            user.setDeptId(ui.deptId());
            user.setBuId(ui.buId());
            user.setOrgId(ui.orgId());
            user.setTenantCode(ui.tenantCode());
        }
        user.setDataPermissions(snap.dataPermissions());

        SecurityContext.setCurrentUser(user);
        if (tenant != null && !tenant.isBlank()) {
            SecurityContext.setCurrentTenant(new Tenant(tenant));
        }
        List<String> func = ir.permissions() == null ? List.of() : ir.permissions();
        List<String> roles = ir.roles() == null ? List.of() : ir.roles();
        CurrentUserHolder.set(new CurrentUserHolder.RequestPrincipal(userId, loginName, func, roles));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null) {
            String h = header.trim();
            if (h.length() > 7 && h.substring(0, 7).equalsIgnoreCase("Bearer ")) {
                String t = h.substring(7).trim();
                if (!t.isEmpty()) {
                    return t;
                }
            }
        }
        String param = request.getParameter("access_token");
        return (param == null || param.isBlank()) ? null : param.trim();
    }

    private void deny(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":401,\"msg\":\"" + reason + "\"}");
    }
}
