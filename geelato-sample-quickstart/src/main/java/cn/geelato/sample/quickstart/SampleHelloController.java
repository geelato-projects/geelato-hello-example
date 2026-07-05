package cn.geelato.sample.quickstart;

import cn.geelato.lang.api.ApiResult;
import cn.geelato.web.common.annotation.ApiRuntimeRestController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiRuntimeRestController("/api/sample")
public class SampleHelloController {

    private final JdbcTemplate primaryJdbcTemplate;

    public SampleHelloController(@Qualifier("primaryJdbcTemplate") JdbcTemplate primaryJdbcTemplate) {
        this.primaryJdbcTemplate = primaryJdbcTemplate;
    }

    @GetMapping("/ping")
    public ApiResult<Map<String, Object>> ping() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app", "geelato-sample-quickstart");
        payload.put("frameworkStarter", true);
        payload.put("database", primaryJdbcTemplate.queryForObject("select 'h2' ", String.class));
        return ApiResult.success(payload, "sample quickstart is ready");
    }
}
