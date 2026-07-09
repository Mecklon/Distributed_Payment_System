package com.mecklon.order.dtos;


import com.mecklon.order.dtos.types.WebSocketEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketMessage {
    private String destination;
    private WebSocketEventType eventType;
    private Object payload;
}