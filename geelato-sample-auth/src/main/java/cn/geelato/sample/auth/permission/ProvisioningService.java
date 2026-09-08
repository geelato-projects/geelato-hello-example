package cn.geelato.sample.auth.permission;

import cn.geelato.sample.auth.config.DemoAuthProperties;
import cn.geelato.sample.auth.permission.dto.ProvisioningPayloadDto;
import cn.geelato.sample.auth.permission.dto.ProvisioningPayloadDto.PermissionRuntimeDto;
import cn.geelato.security.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户权限拉取/缓存/降级编排：
 * <ol>
 *   <li>缓存命中 → 直接返回。</li>
 *   <li>调认证中心 {@code /api/provisioning/user/{userId}}（转发用户 Bearer）→ 映射为 {@link Permission} → 写缓存 + 镜像。</li>
 *   <li>认证中心不可达 → 读本地镜像表降级。</li>
 * </ol>
 */
@Slf4j
@Service
public class ProvisioningService {

    private final AuthCenterClient client;
    private final PermissionCache cache;
    private final PermissionMirrorRepository mirror;
    private final DemoAuthProperties props;

    public ProvisioningService(AuthCenterClient client, PermissionCache cache,
                                 PermissionMirrorRepository mirror, DemoAuthProperties props) {
        this.client = client;
        this.cache = cache;
        this.mirror = mirror;
        this.props = props;
    }

    /**
     * 取某用户的权限快照。
     *
     * @param bearerToken 用户 access_token，转发给认证中心做 Bearer 鉴权。
     */
    public UserPermissionSnapshot getOrLoad(String userId, String appCode, String tenantCode, String bearerToken) {
        UserPermissionSnapshot cached = cache.getSnapshot(userId);
        if (cached != null) {
            return cached;
        }
        String effectiveApp = (appCode == null || appCode.isBlank()) ? props.getAuthCenter().getAppCode() : appCode;
        ProvisioningPayloadDto payload = client.syncUserPermissions(userId, effectiveApp, tenantCode, bearerToken);
        if (payload != null) {
            List<Permission> dps = mapDataPermissions(payload.dataPermissions());
            mirror.save(userId, dps,
                    payload.appCode() != null ? payload.appCode() : effectiveApp,
                    payload.tenantCode() != null ? payload.tenantCode() : tenantCode);
            UserPermissionSnapshot snap = new UserPermissionSnapshot(
                    orEmpty(payload.funcPermissions()), dps, orEmpty(payload.roleCodes()));
            cache.putSnapshot(userId, snap);
            return snap;
        }
        // 降级：读本地镜像
        log.warn("provisioning 不可达，降级读本地镜像 userId={}", userId);
        List<Permission> dps = mirror.loadDataPermissions(userId);
        UserPermissionSnapshot degraded = new UserPermissionSnapshot(List.of(), dps, List.of());
        cache.putSnapshot(userId, degraded);
        return degraded;
    }

    private List<Permission> mapDataPermissions(List<PermissionRuntimeDto> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<Permission> out = new ArrayList<>();
        for (PermissionRuntimeDto d : list) {
            if (d == null || d.rule() == null || d.rule().isBlank()) {
                continue;
            }
            Permission p = new Permission();
            p.setEntity(d.entity());
            p.setName(d.name());
            p.setRule(d.rule());
            p.setWeight(d.weight() == null ? 0 : d.weight());
            out.add(p);
        }
        return out;
    }

    private List<String> orEmpty(List<String> l) {
        return l == null ? List.of() : l;
    }

    public void invalidateUser(String userId) {
        cache.invalidateUser(userId);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }
}
