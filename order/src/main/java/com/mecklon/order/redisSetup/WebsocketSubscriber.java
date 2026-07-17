package com.mecklon.order.redisSetup;

import com.mecklon.order.dtos.WebSocketMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebsocketSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public void receive(WebSocketMessage message) {
        System.out.println("got the websocket message");
        System.out.println(message.getDestination());
        System.out.println("got the websocket message");

        messagingTemplate.convertAndSend(
                message.getDestination(),
                message
        );
    }

    @PostConstruct
    public void init() {
        System.out.println("Subscriber created");
    }
}
