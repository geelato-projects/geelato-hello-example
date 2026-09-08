package cn.geelato.sample.auth.web;

import cn.geelato.lang.api.ApiResult;
import cn.geelato.orm.MetaFactory;
import cn.geelato.orm.query.Order;
import cn.geelato.sample.auth.permission.AuthCenterClient;
import cn.geelato.sample.auth.permission.DataPermissionFilterInjector;
import cn.geelato.sample.auth.security.CurrentUserHolder;
import cn.geelato.sample.auth.security.RequirePermission;
import cn.geelato.sample.auth.security.RequireRole;
import cn.geelato.security.SecurityContext;
import cn.geelato.security.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * demo 接口：演示「功能权限（注解 AOP）+ 数据权限（ORM 注入器）」两层控制。
 * <p>令牌校验在 {@code AuthFilter} 完成；功能权限声明来自 introspect，数据权限规则来自 provisioning。
 */
@RestController
@RequestMapping("/api/sample-auth")
public class SampleAuthController {

    private final AuthCenterClient authCenterClient;

    public SampleAuthController(AuthCenterClient authCenterClient) {
        this.authCenterClient = authCenterClient;
    }

    /** 公开：存活检查（无需 token）。 */
    @GetMapping("/public")
    public ApiResult<Map<String, Object>> ping() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app", "geelato-sample-auth");
        payload.put("ready", true);
        return ApiResult.success(payload, "sample-auth is ready");
    }

    /** 回显当前用户与权限声明（证明 introspect + 本地用户 + 数据权限快照全链路）。 */
    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        CurrentUserHolder.RequestPrincipal principal = CurrentUserHolder.get();
        User user = SecurityContext.getCurrentUser();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (principal == null) {
            payload.put("authenticated", false);
            return ApiResult.success(payload, "anonymous");
        }
        payload.put("authenticated", true);
        payload.put("userId", principal.userId());
        payload.put("loginName", principal.loginName());
        payload.put("funcPermissions", principal.funcPermissions());
        payload.put("roleCodes", principal.roleCodes());
        if (user != null) {
            payload.put("deptId", user.getDeptId());
            payload.put("buId", user.getBuId());
            payload.put("orgId", user.getOrgId());
            payload.put("tenantCode", user.getTenantCode());
            payload.put("dataPermissionCount", user.getDataPermissions() == null ? 0 : user.getDataPermissions().size());
        }
        return ApiResult.success(payload, "current user");
    }

    /**
     * 数据权限演示：查询 demo_order，注入器按当前用户的数据权限自动追加 where（如 dept_id=本部门）。
     * 返回过滤后的行 + 本次生效的 where 片段，便于观察。
     */
    @GetMapping("/orders")
    public ApiResult<Map<String, Object>> orders() {
        List<Map<String, Object>> list = MetaFactory.query(cn.geelato.sample.auth.entity.DemoOrderEntity.class)
                .order(Order.desc("id"))
                .list();
        String appliedRule = DataPermissionFilterInjector.pollLastApplied();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appliedRule", appliedRule);
        payload.put("count", list.size());
        payload.put("list", list);
        return ApiResult.success(payload, "orders with data permission");
    }

    /** 功能权限：需 order:export。 */
    @RequirePermission("order:export")
    @GetMapping("/orders/export")
    public ApiResult<Map<String, Object>> export() {
        return ApiResult.success(Map.of("action", "export"), "export allowed");
    }

    /** 功能权限：需 order:approve。 */
    @RequirePermission("order:approve")
    @PostMapping("/orders/approve")
    public ApiResult<Map<String, Object>> approve() {
        return ApiResult.success(Map.of("action", "approve"), "approve allowed");
    }

    /** 角色权限：需 admin。 */
    @RequireRole("admin")
    @GetMapping("/admin/dashboard")
    public ApiResult<Map<String, Object>> dashboard() {
        return ApiResult.success(Map.of("action", "admin-dashboard"), "admin allowed");
    }

    /** 便捷登录代理：转发到认证中心 /api/auth/login，返回 accessToken，便于全程只与 demo 交互。 */
    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        Map<String, Object> result = authCenterClient.login(body.get("username"), body.get("password"));
        if (result == null) {
            return ApiResult.fail("login failed (auth center unreachable or rejected)");
        }
        return ApiResult.success(result, "login proxied");
    }
}
