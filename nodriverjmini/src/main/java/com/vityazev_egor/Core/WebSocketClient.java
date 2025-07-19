package com.vityazev_egor.Core;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.vityazev_egor.Core.LambdaWaitTask.TimeOutException;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ClientEndpoint
public class WebSocketClient {
    private Session session;
    private final CustomLogger logger = new CustomLogger(WebSocketClient.class.getName());
    private final ConcurrentHashMap<Integer, AwaitedMessage> awaitedMessages = new ConcurrentHashMap<>();

    @Getter
    @Setter
    @NoArgsConstructor
    private static class AwaitedMessage {
        private String message;
        private boolean isAccepted = false;
    }

    /**
     * Creates a new WebSocket client and connects to the specified URL.
     *
     * @param url The WebSocket URL to connect to
     * @throws Exception if the connection fails
     */
    public WebSocketClient(String url) throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(10*1024*1024);
        container.connectToServer(this, new URI(url));
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        logger.info("Connected to the server");
    }

    @OnMessage
    public void onMessage(String message) {
        CDPCommandBuilder.parseIdFromCommand(message).ifPresentOrElse(
            messageId ->{
                logger.info("Amount of awaited messages = " + awaitedMessages.size());
                AwaitedMessage awaitedMessage = awaitedMessages.get(messageId);
                if (awaitedMessage != null) {
                    awaitedMessage.setMessage(message);
                    awaitedMessage.setAccepted(true);
                }
            }, 
            () -> logger.error("Could not parse id from this message: ")
        );
        logger.info(String.format("Received message with content = %s", message.length() > 150 ? message.substring(0, 150) : message));
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Error occurred: " + throwable.getMessage());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.warning("Connection closed: " + closeReason);
        awaitedMessages.clear();
    }

    /**
     * Sends a command and waits for the result with default timeout settings.
     * Uses a 2-second timeout and 50ms delay between checks.
     *
     * @param json The JSON command to send
     */
    public void sendCommand(String json) {
        sendAndWaitResult(2, json, 50);
    }

    /**
     * Sends a command and waits for the response with custom timeout settings.
     *
     * @param timeOutSeconds Maximum time to wait for response in seconds
     * @param json The JSON command to send
     * @param delayMilis Delay between response checks in milliseconds
     * @return Optional containing the response message, or empty if timeout/error occurred
     */
    public Optional<String> sendAndWaitResult(Integer timeOutSeconds, String json, Integer delayMilis){
        Integer messageId = null;
        try{
            messageId = CDPCommandBuilder.parseIdFromCommand(json).orElseThrow(() -> new Exception("Can't parse id of command"));
            logger.info("Sending message with id = " + messageId.toString());
            
            final AwaitedMessage awaitedMessage = new AwaitedMessage();
            //регистрируем ожидание сообщения с определённым id
            awaitedMessages.put(messageId, awaitedMessage);

            // ждём пока ответ будет получен
            var task = new LambdaWaitTask(awaitedMessage::isAccepted);
            session.getAsyncRemote().sendText(json);

            var result = task.execute(timeOutSeconds, delayMilis);
            if (!result) throw new TimeOutException(String.format("Timeout during execution of task #%d", messageId));
            
            return Optional.of(awaitedMessage.getMessage());
        } catch (TimeOutException ex){
            logger.error(ex.getMessage());
            return Optional.empty();
        } catch (Exception ex){
            logger.error("Error in sendAndWaitResult method", ex);
            return Optional.empty();
        } finally {
            if (messageId != null) {
                awaitedMessages.remove(messageId);
            }
        }
    }

    /**
     * Sends a command and waits for the response with default delay.
     * Uses 50ms delay between response checks.
     *
     * @param timeOutSeconds Maximum time to wait for response in seconds
     * @param json The JSON command to send
     * @return Optional containing the response message, or empty if timeout/error occurred
     */
    public Optional<String> sendAndWaitResult(Integer timeOutSeconds, String json){
        return sendAndWaitResult(timeOutSeconds, json, 50);
    }

    /**
     * Closes the WebSocket session and cleans up resources.
     * Clears all awaited messages to prevent memory leaks.
     */
    public void closeSession(){
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            logger.error("Can't close session", e);
        } finally {
            awaitedMessages.clear();
        }
    }
    
}
