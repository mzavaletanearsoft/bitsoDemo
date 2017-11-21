package com.demo.bitso.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.websocket.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

@ClientEndpoint
public class BitsoWebSocketClientEndpoint {

    private Session userSession = null;
    private ConcurrentLinkedQueue<String> diffOrders = new ConcurrentLinkedQueue<>();

    public BitsoWebSocketClientEndpoint(ConcurrentLinkedQueue<String> diffOrders) {
        this.diffOrders = diffOrders;
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
        diffOrders.add(message);
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

}