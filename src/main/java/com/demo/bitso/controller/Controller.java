package com.demo.bitso.controller;

import com.demo.bitso.transport.BitsoWebSocketClientEndpoint;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.net.URI;

public class Controller {

    @FXML
    public ListView bidsListView;

    @FXML
    public ListView asksListView;

    @FXML
    public void initialize() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            BitsoWebSocketClientEndpoint bitsoWebSocketClientEndpoint = new BitsoWebSocketClientEndpoint(bidsListView, asksListView);
            container.connectToServer(bitsoWebSocketClientEndpoint, new URI("wss://ws.bitso.com"));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
