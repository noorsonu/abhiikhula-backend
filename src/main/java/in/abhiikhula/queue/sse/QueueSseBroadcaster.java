package in.abhiikhula.queue.sse;

import in.abhiikhula.queue.dto.QueueStatusResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class QueueSseBroadcaster {

    // 30 minutes timeout for SSE streams before reconnecting
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L;

    // Map of shopId -> list of active client emitters
    private final Map<UUID, List<SseEmitter>> shopEmitters = new ConcurrentHashMap<>();

    /**
     * Subscribes a client to real-time queue updates for a specific shop.
     * Immediately pushes initial queue status upon connection.
     */
    public SseEmitter subscribe(UUID shopId, QueueStatusResponseDto initialStatus) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        shopEmitters.computeIfAbsent(shopId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(shopId, emitter));
        emitter.onTimeout(() -> removeEmitter(shopId, emitter));
        emitter.onError(e -> removeEmitter(shopId, emitter));

        // Send initial connect event
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(initialStatus, MediaType.APPLICATION_JSON));
            log.debug("New SSE client subscribed to shop: {}. Total listeners: {}", 
                    shopId, shopEmitters.get(shopId).size());
        } catch (IOException e) {
            log.warn("Failed to send initial SSE payload to client for shop: {}", shopId);
            removeEmitter(shopId, emitter);
        }

        return emitter;
    }

    /**
     * Broadcasts live queue update to all active clients listening to the shop.
     */
    public void broadcast(UUID shopId, QueueStatusResponseDto status) {
        List<SseEmitter> emitters = shopEmitters.get(shopId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        log.debug("Broadcasting queue update for shop {} to {} active listeners", shopId, emitters.size());

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("QUEUE_UPDATE")
                        .data(status, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                log.debug("Error sending SSE to client, marking as dead: {}", e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Pruned {} disconnected SSE listeners for shop {}", deadEmitters.size(), shopId);
        }
    }

    private void removeEmitter(UUID shopId, SseEmitter emitter) {
        List<SseEmitter> emitters = shopEmitters.get(shopId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                shopEmitters.remove(shopId);
            }
        }
        log.debug("SSE client disconnected from shop: {}", shopId);
    }
}

