package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;


import com.ben.messagingplatform.infrastructure.rabbitmq.security.JwtService;
import com.ben.messagingplatform.user.entity.User;
import com.ben.messagingplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

        if (accessor == null) {
            throw new MessageDeliveryException("Invalid STOMP message");
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorizationHeader =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorizationHeader == null ||
                    !authorizationHeader.startsWith("Bearer ")) {
                throw new MessageDeliveryException(
                        "Missing Bearer token in STOMP CONNECT frame");
            }

            String token = authorizationHeader.substring(7);

            try {
                if (!jwtService.isTokenValid(token)) {
                    throw new IllegalArgumentException("Invalid JWT");
                }

                Long userId = jwtService.extractUserId(token);

                User user = userRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("User not found"));

                var authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                user.getId().toString(),
                                null,
                                List.of()
                        );

                accessor.setUser(authentication);

            } catch (RuntimeException exception) {
                throw new MessageDeliveryException(
                        "Invalid or expired authentication token");
            }
        }

        return message;
    }
}
