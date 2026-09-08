package cn.geelato.sample.auth.permission;

import cn.geelato.sample.auth.config.DemoAuthProperties;
import cn.geelato.sample.auth.permission.dto.IntrospectionResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地内存缓存：
 * <ul>
 *   <li>introspect 结果（按 token，带 TTL，减少重复网络调用）。</li>
 *   <li>用户权限快照（按 userId）。</li>
 * </ul>
 * 权限变更通知到达时调用 {@link #invalidateUser}/{@link #invalidateAll} 失效。
 */
@Component
public class PermissionCache {

    private record IntrospectEntry(IntrospectionResult result, long expireAt) {
    }

    private final ConcurrentHashMap<String, IntrospectEntry> introspectCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserPermissionSnapshot> snapshotCache = new ConcurrentHashMap<>();
    private final DemoAuthProperties props;

    public PermissionCache(DemoAuthProperties props) {
        this.props = props;
    }

    // ---- introspect（按 token，TTL）----
    public IntrospectionResult getIntrospection(String token) {
        if (token == null) {
            return null;
        }
        IntrospectEntry e = introspectCache.get(token);
        if (e == null) {
            return null;
        }
        if (System.currentTimeMillis() > e.expireAt()) {
            introspectCache.remove(token);
            return null;
        }
        return e.result();
    }

    public void putIntrospection(String token, IntrospectionResult r) {
        if (token == null || r == null) {
            return;
        }
        long ttl = Math.max(1, props.getAuthCenter().getIntrospectCacheSeconds()) * 1000L;
        introspectCache.put(token, new IntrospectEntry(r, System.currentTimeMillis() + ttl));
    }

    // ---- 用户权限快照（按 userId）----
    public UserPermissionSnapshot getSnapshot(String userId) {
        return userId == null ? null : snapshotCache.get(userId);
    }

    public void putSnapshot(String userId, UserPermissionSnapshot s) {
        if (userId != null && s != null) {
            snapshotCache.put(userId, s);
        }
    }

    public void invalidateUser(String userId) {
        if (userId != null) {
            snapshotCache.remove(userId);
        }
    }

    /** 失效全部：快照 + introspect（功能权限声明也可能已变化）。 */
    public void invalidateAll() {
        snapshotCache.clear();
        introspectCache.clear();
    }
}
