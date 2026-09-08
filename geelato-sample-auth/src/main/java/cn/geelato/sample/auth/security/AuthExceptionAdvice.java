package cn.geelato.sample.auth.security;

import cn.geelato.lang.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** demo 鉴权异常 → HTTP 状态码映射。 */
@Slf4j
@RestControllerAdvice
public class AuthExceptionAdvice {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResult<Void>> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResult.fail(e.getMessage()));
    }
}
