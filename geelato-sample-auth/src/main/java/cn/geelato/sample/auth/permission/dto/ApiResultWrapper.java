package cn.geelato.sample.auth.permission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code ApiResult} 外层包装的解析 DTO（字段：msg/code/status/data）。
 * <p>用于解包认证中心 {@code provisioning} 等返回的 {@code ApiResult<T>}。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiResultWrapper<T>(
        String msg,
        int code,
        String status,
        T data
) {
    /** 认证中心成功时 status="success"（RC200 code=20000）。 */
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status) || code == 20000;
    }
}
