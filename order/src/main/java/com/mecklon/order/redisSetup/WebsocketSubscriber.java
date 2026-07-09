package com.mecklon.order.redisSetup;

import com.mecklon.order.dtos.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebsocketSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public void receive(WebSocketMessage message) {

        messagingTemplate.convertAndSend(
                message.getDestination(),
                message
        );
    }
}
