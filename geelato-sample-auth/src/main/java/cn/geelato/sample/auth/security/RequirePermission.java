package cn.geelato.sample.auth.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 功能权限校验注解（demo 自研，不依赖 Sa-Token）。
 * <p>由 {@link PermissionAspect} 拦截，校验当前主体（来自 introspect 声明）的 funcPermissions 是否命中任一所需权限码。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /** 所需功能权限码（命中任一即通过）。 */
    String[] value();
}
