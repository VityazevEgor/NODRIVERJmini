package com.vityazev_egor.Core;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Nullable;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ClientEndpoint
public class WebSocketClient {
    private Session session;
    private final CustomLogger logger = new CustomLogger(WebSocketClient.class.getName());
    private CompletableFuture<String> commandResult = null;

    public WebSocketClient(String url) throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, new URI(url));
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        logger.info("Connected to the server");
    }

    // TODO сделать тут что-то по типу общего пула сообщений и потом искать в нём нужный ответ по каким-то ключевым словам
    @OnMessage
    public void onMessage(String message) {
        logger.info("Received message: " + message);
        if (commandResult != null){
            commandResult.complete(message);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error occurred: " + throwable.getMessage());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.warning("Connection closed: " + closeReason);
    }

    public void sendCommand(String json) {
        //System.out.println(json);
        try{
            session.getAsyncRemote().sendText(json);
        }
        catch (Exception ex){
            logger.warning("Error in sendCommand method");
            ex.printStackTrace();
        }
    }

    public void sendCommand(String[] jsons){
        for (String json : jsons) {
            sendCommand(json);
        }
    }

    public void sendCommandAsync(String json){
        commandResult = new CompletableFuture<String>();
        sendCommand(json);
    }

    @Nullable
    public String waitResult(Integer timeOutSeconds){
        if (commandResult != null){
            try {
                String rawResult = commandResult.get(timeOutSeconds, TimeUnit.SECONDS);
                commandResult = null;
                return rawResult;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
    
}
