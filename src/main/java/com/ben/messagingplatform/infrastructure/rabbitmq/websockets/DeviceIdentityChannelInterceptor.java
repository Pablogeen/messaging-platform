package com.ben.messagingplatform.infrastructure.rabbitmq.websockets;


import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

    @Component
    public class DeviceIdentityChannelInterceptor implements ChannelInterceptor {

        private static final String DEVICE_ID_HEADER = "device-id";

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {

            StompHeaderAccessor accessor =
                    StompHeaderAccessor.wrap(message);

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                String deviceId =
                        accessor.getFirstNativeHeader(DEVICE_ID_HEADER);

                if (deviceId == null || deviceId.isBlank()) {
                    throw new MessagingException(
                            "Missing device-id header"
                    );
                }

                accessor.getSessionAttributes().put(
                        DEVICE_ID_HEADER,
                        deviceId
                );
            }

            return message;
        }
    }
