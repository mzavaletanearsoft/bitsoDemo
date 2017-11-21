package com.demo.bitso.controller;

import com.demo.bitso.model.DiffOrderMessage;
import com.demo.bitso.model.Operation;
import com.demo.bitso.model.Payload;
import com.demo.bitso.transport.BitsoWebSocketClientEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Controller {

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private Map<Enum<Operation>, ConcurrentLinkedQueue<DiffOrderMessage>> diffOrdersMap = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String> diffOrders = new ConcurrentLinkedQueue();

    @FXML
    public ListView<DiffOrderMessage> bidsListView;

    @FXML
    public ListView<DiffOrderMessage> asksListView;

    @FXML
    public void initialize() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            BitsoWebSocketClientEndpoint bitsoWebSocketClientEndpoint = new BitsoWebSocketClientEndpoint(diffOrders);
            container.connectToServer(bitsoWebSocketClientEndpoint, new URI("wss://ws.bitso.com"));

            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersScheduler(Operation.BID, bidsListView, getDiffOrderMessageComparator().reversed()), 2000, 100, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersScheduler(Operation.ASK, asksListView, getDiffOrderMessageComparator()), 2000, 100, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersMappingScheduler(), 2000, 100, TimeUnit.MILLISECONDS);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Runnable createDiffOrdersMappingScheduler() {
        return () -> {
            String diffOrder = diffOrders.poll();
            if (diffOrder != null) {
                mapMessage(diffOrder);
            }
        };
    }

    private Runnable createDiffOrdersScheduler(Operation operation, ListView<DiffOrderMessage> listView, Comparator<DiffOrderMessage> orderMessageComparator) {
        return () -> {
            ConcurrentLinkedQueue<DiffOrderMessage> bidDiffOrderMessages = diffOrdersMap.getOrDefault(operation, new ConcurrentLinkedQueue<>());
            DiffOrderMessage diffOrderMessage = bidDiffOrderMessages.poll();
            if (diffOrderMessage != null) {
                Platform.runLater(processListView(listView, diffOrderMessage, orderMessageComparator));
            }
        };
    }

    private Runnable processListView(ListView<DiffOrderMessage> listView, DiffOrderMessage diffOrderMessage, Comparator<DiffOrderMessage> diffOrderMessageComparator) {
        return () -> {

            Collection<DiffOrderMessage> diffOrderMessages = new ArrayList<>();
            List<Payload> payload = diffOrderMessage.getPayload();

            if (payload.size() > 1) {
                payload.forEach(p -> {
                    DiffOrderMessage message = DiffOrderMessage.clone(diffOrderMessage);
                    ArrayList<Payload> payloads = new ArrayList<>();
                    payloads.add(p);
                    message.setPayload(payloads);
                    diffOrderMessages.add(message);
                });

                listView.getItems().addAll(diffOrderMessages);
            } else {
                listView.getItems().add(diffOrderMessage);
            }

            SortedList<DiffOrderMessage> sorted = listView.getItems()
                    .sorted(diffOrderMessageComparator);

            List<DiffOrderMessage> collect = sorted.stream()
                    .limit(Long.valueOf(System.getProperty("MAX_DISPLAYABLE_BIDS_AND_ASKS")))
                    .collect(Collectors.toList());

            listView.getItems().setAll(collect);
        };
    }

    private Comparator<DiffOrderMessage> getDiffOrderMessageComparator() {
        return (a, b) -> a.getFirstPayload().getRate() > b.getFirstPayload().getRate() ? 1 : 0;
    }

    public void killScheduledTasks() {
        scheduledExecutorService.shutdown();
    }

    private void mapMessage(String message) {
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

    private boolean isBuyMessage(JsonNode messageNode) {
        if (isValidDiffOrdersMessage(messageNode)) {
            int t = messageNode.path("payload").get(0).path("t").asInt();
            return t == 0;
        }
        return false;
    }

    public static JsonNode parseJsonMessage(String inputStream) throws IOException {
        return new ObjectMapper().readTree(inputStream);
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
}
