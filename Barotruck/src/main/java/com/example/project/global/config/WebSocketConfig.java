package com.example.project.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Flutter에서 연결할 엔드포인트: ws://서버주소:8080/ws-stomp
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*"); // 테스트를 위해 모든 오리진 허용
        // 보안상 특정 도메인만 허용해야 하지만, 개발 중에는 Flutter 앱이나 웹에서 자유롭게 접근할 수 있도록 모든 경로를 허용하는 설정입니다.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 받을 때(구독): /sub/chat/room/1
        registry.enableSimpleBroker("/sub");
        // 메시지를 보낼 때(발행): /pub/chat/message
        registry.setApplicationDestinationPrefixes("/pub");
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024); // 128KB로 확장
        registration.setSendBufferSizeLimit(512 * 1024); // 버퍼 크기 확장
        registration.setSendTimeLimit(20 * 1000); // 전송 시간 제한 20초
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                
                // 모든 STOMP 프레임 로그 찍기
                System.out.println("========= [STOMP Interceptor] =========");
                System.out.println("Command: " + accessor.getCommand());
                System.out.println("Destination: " + accessor.getDestination());
                System.out.println("Payload: " + new String((byte[]) message.getPayload()));
                System.out.println("=======================================");
                
                return message;
            }
        });
    }
}