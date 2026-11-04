package com.cms.demo.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(
                            ServerHttpRequest request,
                            WebSocketHandler wsHandler,
                            Map<String, Object> attributes) {

                        // 1) SecurityContext (filter chain)
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null && auth.isAuthenticated()
                                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
                            return auth;
                        }

                        // 2) HTTP session SPRING_SECURITY_CONTEXT (form login)
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            HttpServletRequest http = servletRequest.getServletRequest();

                            Principal requestUser = http.getUserPrincipal();
                            if (requestUser != null) {
                                return requestUser;
                            }

                            HttpSession session = http.getSession(false);
                            if (session != null) {
                                Object ctx = session.getAttribute("SPRING_SECURITY_CONTEXT");
                                if (ctx instanceof SecurityContext securityContext) {
                                    Authentication a = securityContext.getAuthentication();
                                    if (a != null && a.isAuthenticated()) {
                                        return a;
                                    }
                                }
                            }
                        }

                        return super.determineUser(request, wsHandler, attributes);
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    if (accessor.getUser() == null) {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null && auth.isAuthenticated()) {
                            accessor.setUser(auth);
                        }
                    }
                }
                return message;
            }
        });
    }
}