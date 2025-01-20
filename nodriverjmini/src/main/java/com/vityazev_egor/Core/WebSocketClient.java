package com.vityazev_egor.Core;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vityazev_egor.Core.WaitTask.TimeOutException;

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
    private final CommandsProcessor cmdProcessor = new CommandsProcessor();
    private List<AwaitedMessage> awaitedMessages = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    private class AwaitedMessage {
        private Integer id;
        private String message;
        private Boolean isAccepted = false;
    }

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
        cmdProcessor.parseIdFromCommand(message).ifPresentOrElse(
            messageId ->{
                logger.info("Amount of awaited messages = " + awaitedMessages.size());
                awaitedMessages.stream()
                        .filter(x -> x.getId().equals(messageId))
                        .findFirst()
                        .ifPresent(awaitedMessage -> {
                            awaitedMessage.setMessage(message);
                            awaitedMessage.setIsAccepted(true);
                        });
            }, 
            () -> logger.error("Could not patse id from this message: ")
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
    }

    public void sendCommand(String json) {
        sendAndWaitResult(2, json, 50);
    }

    public void sendCommand(String[] jsons){
        for (String json : jsons) {
            sendCommand(json);
        }
    }

    public void sendCommand(List<String> jsons){
        for (String json : jsons){
            sendCommand(json);
        }
    }

    public Optional<String> sendAndWaitResult(Integer timeOutSeconds, String json, Integer delayMilis){
        final AwaitedMessage awaitedMessage = new AwaitedMessage();
        try{
            Integer messageId = cmdProcessor.parseIdFromCommand(json).orElseThrow(() -> new Exception("Can't parse id of command"));
            logger.info("Sending message with id = " + messageId.toString());
            
            //регестрируем ожидание сообщения с определённым id
            awaitedMessage.setId(messageId);
            awaitedMessages.add(awaitedMessage);

            // ждём пока ответ будет получен
            var task = new WaitTask() {

                @Override
                public Boolean condition() {
                    return awaitedMessage.getIsAccepted();
                }
                
            };
            session.getAsyncRemote().sendText(json);

            var result = task.execute(timeOutSeconds, delayMilis);
            if (!result) throw new TimeOutException(String.format("Timeout during execution of task #%d", messageId));
            
            return Optional.of(awaitedMessage.getMessage());
        }catch (TimeOutException ex){
            logger.error(ex.getMessage());
            return Optional.empty();
        } 
        catch (Exception ex){
            logger.error("Error in sendAndWaitResult method", ex);
            return Optional.empty();
        }
        finally{
            awaitedMessages.remove(awaitedMessage);
        }
    }

    public Optional<String> sendAndWaitResult(Integer timeOutSeconds, String json){
        return sendAndWaitResult(timeOutSeconds, json, 50);
    }

    public void closeSession(){
        try {
            session.close();
        } catch (IOException e) {
            logger.error("Can't close session for some reason....", e);
            e.printStackTrace();
        }
    }
    
}
