package cn.geelato.sample.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 功能权限/角色校验 AOP（不依赖 Sa-Token）。
 * <p>读 {@link CurrentUserHolder} 中当前主体的 funcPermissions/roleCodes（来自 introspect 声明），
 * 对 {@link RequirePermission}/{@link RequireRole} 标注的方法做 contains 校验，未通过抛 {@link ForbiddenException}。
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(rp)")
    public Object aroundPermission(ProceedingJoinPoint pjp, RequirePermission rp) throws Throwable {
        CurrentUserHolder.RequestPrincipal principal = CurrentUserHolder.get();
        if (principal == null) {
            throw new ForbiddenException("login required");
        }
        if (!containsAny(principal.funcPermissions(), rp.value())) {
            log.warn("功能权限拒绝 loginName={} need={} have={}", principal.loginName(),
                    Arrays.toString(rp.value()), principal.funcPermissions());
            throw new ForbiddenException("permission denied: " + Arrays.toString(rp.value()));
        }
        return pjp.proceed();
    }

    @Around("@annotation(rr)")
    public Object aroundRole(ProceedingJoinPoint pjp, RequireRole rr) throws Throwable {
        CurrentUserHolder.RequestPrincipal principal = CurrentUserHolder.get();
        if (principal == null) {
            throw new ForbiddenException("login required");
        }
        if (!containsAny(principal.roleCodes(), rr.value())) {
            log.warn("角色拒绝 loginName={} need={} have={}", principal.loginName(),
                    Arrays.toString(rr.value()), principal.roleCodes());
            throw new ForbiddenException("role denied: " + Arrays.toString(rr.value()));
        }
        return pjp.proceed();
    }

    private boolean containsAny(Collection<String> owned, String[] required) {
        if (required == null || required.length == 0) {
            return true;
        }
        List<String> list = owned == null ? List.of() : owned.stream().map(String::trim).toList();
        for (String r : required) {
            if (r != null && list.contains(r.trim())) {
                return true;
            }
        }
        return false;
    }
}
