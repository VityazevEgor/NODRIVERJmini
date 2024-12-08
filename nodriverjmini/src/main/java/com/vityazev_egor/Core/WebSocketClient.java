package com.vityazev_egor.Core;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import lombok.Setter;

@ClientEndpoint
public class WebSocketClient {
    private Session session;
    private final CustomLogger logger = new CustomLogger(WebSocketClient.class.getName());
    private final CommandsProcessor cmdProcessor = new CommandsProcessor();
    private ArrayList<AwaitedMessage> awaitedMessages = new ArrayList<>();

    @Getter
    @Setter
    private class AwaitedMessage {
        private final Integer id;
        private String message;
        private Boolean isAccepted = false;

        public AwaitedMessage(Integer id){
            this.id = id;
        }
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

        Optional<Integer> messageId = cmdProcessor.parseIdFromCommand(message);
        if (messageId.isPresent()){
            logger.info("I parse id of message = " + messageId.get());
        }
        else{
            logger.error("I could not parse id from this message", null);
            return;
        }
        // теперь мы смотрим нету ли в списке ожидаемых сообщений такого id
        // если есть, то записываем в него сообщение
        System.out.println("Amount of awaited messages = " + awaitedMessages.size());
        AwaitedMessage awaitedMessage = awaitedMessages.stream()
                .filter(x -> x.getId().equals(messageId.get()))
                .findFirst()
                .orElse(null);
        if (awaitedMessage != null){
            awaitedMessage.setMessage(message);
            awaitedMessage.setIsAccepted(true);
        }

        String messageCut = message;
        if (messageCut.length() > 150){
            messageCut = messageCut.substring(0, 150);
        }
        logger.info("Received message with type content = " + messageCut);
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
            logger.info("I sent the command: \n" + json);
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

    public void sendCommand(List<String> jsons){
        for (String json : jsons){
            sendCommand(json);
        }
    }

    public Optional<String> sendAndWaitResult(Integer timeOutSeconds, String json){
        Optional<Integer> messageId = cmdProcessor.parseIdFromCommand(json);
        if (!messageId.isPresent()) return Optional.empty();
        
        //регестрируем ождиание сообщения с определённым id
        var awaitedMessage = new AwaitedMessage(messageId.get());
        awaitedMessages.add(awaitedMessage);

        // ждём пока ответ будет получен
        var task = new WaitTask() {

            @Override
            public Boolean condition() {
                return awaitedMessage.getIsAccepted();
            }
            
        };
        sendCommand(json);

        var result = task.execute(timeOutSeconds, 50);
        if (result){
            logger.info("I found response with id = " + messageId.get());
            awaitedMessages.remove(awaitedMessage); // удаляем ожидающее сообщение
            return Optional.of(awaitedMessage.getMessage());
        }
        else{
            logger.warning("I could not find response with id = " + messageId.get());
            awaitedMessages.remove(awaitedMessage); // удаляем ожидающее сообщение
            return Optional.empty();
        }
    }
    
}
