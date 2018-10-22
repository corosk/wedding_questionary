package com.entertainment.handler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.entertainment.handler.SocketHandler;

/**
 * Webソケットの設定
 *
 * @author t_furuya
 */
@Configuration
@EnableWebSocket
public class SocketConfig implements WebSocketConfigurer {
    /**
     * ソケットハンドラ
     */
    @Autowired
    private SocketHandler handler;

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/socket");
    }
}
