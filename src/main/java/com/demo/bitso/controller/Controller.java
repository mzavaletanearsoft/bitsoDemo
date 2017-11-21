package com.demo.bitso.controller;

import com.demo.bitso.model.DiffOrderMessage;
import com.demo.bitso.model.Operation;
import com.demo.bitso.model.Payload;
import com.demo.bitso.transport.BitsoWebSocketClientEndpoint;
import javafx.application.Platform;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Controller {

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private Map<Enum<Operation>, ConcurrentLinkedQueue<DiffOrderMessage>> diffOrdersMap = new ConcurrentHashMap<>();

    @FXML
    public ListView<DiffOrderMessage> bidsListView;

    @FXML
    public ListView<DiffOrderMessage> asksListView;

    @FXML
    public void initialize() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            BitsoWebSocketClientEndpoint bitsoWebSocketClientEndpoint = new BitsoWebSocketClientEndpoint(bidsListView, asksListView, diffOrdersMap);
            container.connectToServer(bitsoWebSocketClientEndpoint, new URI("wss://ws.bitso.com"));

            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersScheduler(Operation.BID, bidsListView), 2, 2, TimeUnit.SECONDS);
            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersScheduler(Operation.ASK, asksListView), 2, 2, TimeUnit.SECONDS);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Runnable createDiffOrdersScheduler(Operation operation, ListView<DiffOrderMessage> asksListView) {
        return () -> {
            ConcurrentLinkedQueue<DiffOrderMessage> bidDiffOrderMessages = diffOrdersMap.getOrDefault(operation, new ConcurrentLinkedQueue<>());
            DiffOrderMessage diffOrderMessage = bidDiffOrderMessages.poll();
            if (diffOrderMessage != null) {
                Platform.runLater(processListView(asksListView, diffOrderMessage, getDiffOrderMessageComparator()));
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
}
