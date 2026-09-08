package cn.geelato.sample.auth.security;

/** 权限/登录校验未通过，映射为 HTTP 403。 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
