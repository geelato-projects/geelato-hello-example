package cn.geelato.sample.auth.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解（demo 自研，不依赖 Sa-Token）。
 * <p>由 {@link PermissionAspect} 拦截，校验当前主体的 roleCodes 是否命中任一所需角色。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** 所需角色编码（命中任一即通过）。 */
    String[] value();
}
