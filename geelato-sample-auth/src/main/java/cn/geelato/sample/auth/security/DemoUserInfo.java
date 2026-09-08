package cn.geelato.sample.auth.security;

/** demo 本地用户记录（业务字段：用于数据权限规则占位符替换）。id 需与认证中心 platform_user.id 对齐。 */
public record DemoUserInfo(
        String id,
        String loginName,
        String name,
        String deptId,
        String buId,
        String orgId,
        String tenantCode
) {
}
