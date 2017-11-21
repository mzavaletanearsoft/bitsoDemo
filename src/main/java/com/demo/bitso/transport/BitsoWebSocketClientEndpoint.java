package com.demo.bitso.transport;

import com.demo.bitso.model.DiffOrderMessage;
import com.demo.bitso.model.Operation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.scene.control.ListView;

import javax.websocket.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@ClientEndpoint
public class BitsoWebSocketClientEndpoint {

    private Session userSession = null;
    private ListView<DiffOrderMessage> bidsListView;
    private ListView<DiffOrderMessage> asksListView;
    private Map<Enum<Operation>, ConcurrentLinkedQueue<DiffOrderMessage>> diffOrdersMap;

    public BitsoWebSocketClientEndpoint(ListView<DiffOrderMessage> bidsListView, ListView<DiffOrderMessage> asksListView, Map<Enum<Operation>, ConcurrentLinkedQueue<DiffOrderMessage>> diffOrdersMap) {
        this.bidsListView = bidsListView;
        this.asksListView = asksListView;
        this.diffOrdersMap = diffOrdersMap;
    }

    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("action", "subscribe");
        objectNode.put("book", "btc_mxn");
        objectNode.put("type", "diff-orders");

        String message = null;
        try {
            message = mapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // send message to websocket
        sendMessage(message);
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        queueMessage(message);
    }

    private void queueMessage(String message) {
        try {
            JsonNode messageNode = parseJsonMessage(message);
            if (isValidDiffOrdersMessage(messageNode)) {
                DiffOrderMessage diffOrderMessage = getDiffOrderMessage(messageNode);
                if (isBuyMessage(messageNode)) {
                    diffOrdersMap.computeIfAbsent(Operation.ASK, k -> new ConcurrentLinkedQueue<>()).add(diffOrderMessage);
                } else {
                    diffOrdersMap.computeIfAbsent(Operation.BID, k -> new ConcurrentLinkedQueue<>()).add(diffOrderMessage);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DiffOrderMessage getDiffOrderMessage(JsonNode messageNode) {
        System.out.println(messageNode.toString());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(messageNode, DiffOrderMessage.class);
    }

    private boolean isValidDiffOrdersMessage(JsonNode messageNode) {

        String type = messageNode.path("type").asText();

        if ("ka".equals(type)) {
            return false;
        }

        if (messageNode.has("action")) {
            return false;
        }

        if (messageNode.path("payload").get(0).path("s").asText().equals("cancelled")) {
            return false;
        }

        return "diff-orders".equals(type);
    }

    private boolean isBuyMessage(JsonNode messageNode) {
        if (isValidDiffOrdersMessage(messageNode)) {
            int t = messageNode.path("payload").get(0).path("t").asInt();
            return t == 0;
        }
        return false;
    }

    @OnError
    public void processError(Throwable t) {
        t.printStackTrace();
    }

    public void sendMessage(String message) {
        try {
            this.userSession.getBasicRemote().sendText(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JsonNode parseJsonMessage(String inputStream) throws IOException {
        return new ObjectMapper().readTree(inputStream);
    }

}