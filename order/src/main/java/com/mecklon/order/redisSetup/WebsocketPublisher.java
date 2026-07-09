package com.mecklon.order.redisSetup;

import com.mecklon.order.dtos.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebsocketPublisher {

    private static final String CHANNEL = "websocket-events";

    private final RedisTemplate<String, Object> redisTemplate;



    public void publish(WebSocketMessage message) {

        redisTemplate.convertAndSend(
                CHANNEL,
                message
        );
    }
}