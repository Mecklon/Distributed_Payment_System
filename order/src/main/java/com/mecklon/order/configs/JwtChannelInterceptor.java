package com.mecklon.order.configs;

import com.mecklon.core.security.JwtPrincipal;
import com.mecklon.core.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String bearer =
                    accessor.getFirstNativeHeader("Authorization");

            if (bearer != null && bearer.startsWith("Bearer ")) {

                String token = bearer.substring(7);

                if (jwtUtil.validateToken(token)) {

                    JwtPrincipal jwtPrincipal =
                            jwtUtil.extractUserDetails(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    jwtPrincipal,
                                    null,
                                    jwtPrincipal.getAuthorities());

                    accessor.setUser(authentication);
                    accessor.getSessionAttributes()
                            .put("auth", authentication);
                }
            }
        } else {

            Authentication authentication =
                    (Authentication) accessor
                            .getSessionAttributes()
                            .get("auth");

            if (authentication != null) {
                accessor.setUser(authentication);
            }
        }

        return message;
    }
}