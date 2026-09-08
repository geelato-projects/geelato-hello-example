package cn.geelato.sample.auth.permission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订阅认证中心权限变更通知（Redis 频道 {@code geelato:permission:change}）。
 * <p>收到通知后失效本地缓存：携带 userId 则失效该用户，否则失效全部。下次请求重新从认证中心拉取。
 */
@Slf4j
@Component
public class PermissionChangeListener implements MessageListener {

    public static final String CHANNEL = "geelato:permission:change";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProvisioningService syncService;
    private final RedisMessageListenerContainer container;

    public PermissionChangeListener(ProvisioningService syncService, RedisMessageListenerContainer container) {
        this.syncService = syncService;
        this.container = container;
    }

    @PostConstruct
    public void register() {
        container.addMessageListener(this, new ChannelTopic(CHANNEL));
        log.info("已订阅权限变更通知频道: {}", CHANNEL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(body);
            String userId = node.path("userId").asText(null);
            if (userId != null && !userId.isBlank()) {
                syncService.invalidateUser(userId.trim());
                log.info("权限变更通知：失效用户 [{}] 缓存", userId);
            } else {
                syncService.invalidateAll();
                log.info("权限变更通知（全局）：失效全部用户缓存");
            }
        } catch (Exception e) {
            log.warn("权限变更通知处理失败: {}", e.getMessage());
        }
    }
}
