package com.demo.bitso.controller;

import com.demo.bitso.model.*;
import com.demo.bitso.transport.BitsoWebSocketClientEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Controller {

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public static final String COINS = "COINS";
    public static final String LAST_PRICE = "LAST_PRICE";
    public static final String MONEY = "MONEY";
    public static final String STARTING_PRICE = "STARTING_PRICE";
    public static final String STARTING_TOTAL = "STARTING_TOTAL";

    private Map<Enum<Operation>, ConcurrentLinkedQueue<DiffOrderMessage>> diffOrdersMap = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String> diffOrders = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<TradesPayload> mockBuyAndSell = new ConcurrentLinkedQueue<>();
    private AtomicInteger lastTradeId = new AtomicInteger();

    private AtomicInteger uptickCount = new AtomicInteger(0);
    private AtomicInteger downtickCount = new AtomicInteger(0);

    private Integer restBookSequence;
    private Integer MAX_DISPLAYABLE_BIDS_AND_ASKS;
    private Integer CONSECUTIVE_UPTICKS;
    private Integer CONSECUTIVE_DOWNTICKS;

    private boolean firstTrades = true;

    private ConcurrentHashMap<String, BigDecimal> infoMap = new ConcurrentHashMap<>();

    @FXML
    public TextArea infoTextArea;

    @FXML
    public ListView<DiffOrderMessage> bidsListView;

    @FXML
    public ListView<DiffOrderMessage> asksListView;

    @FXML
    public ListView<TradesPayload> lastTradesListView;

    @FXML
    public void initialize() {
        try {
            MAX_DISPLAYABLE_BIDS_AND_ASKS = Integer.valueOf(System.getProperty("MAX_DISPLAYABLE_BIDS_AND_ASKS"));
            CONSECUTIVE_UPTICKS = Integer.valueOf(System.getProperty("CONSECUTIVE_UPTICKS"));
            CONSECUTIVE_DOWNTICKS = Integer.valueOf(System.getProperty("CONSECUTIVE_DOWNTICKS"));

            infoMap.put(COINS, BigDecimal.valueOf(100));
            infoMap.put(MONEY, BigDecimal.valueOf(0));

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            BitsoWebSocketClientEndpoint bitsoWebSocketClientEndpoint = new BitsoWebSocketClientEndpoint(diffOrders);
            container.connectToServer(bitsoWebSocketClientEndpoint, new URI("wss://ws.bitso.com"));

            final CountDownLatch latch = new CountDownLatch(1);
            scheduledExecutorService.schedule(createOrderBookTask(latch), 0, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleAtFixedRate(createTradesTask(), 0, 5, TimeUnit.SECONDS);

            latch.await();
            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersScheduler(Operation.BID, bidsListView, getDiffOrderMessageComparator().reversed()), 2000, 100, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersScheduler(Operation.ASK, asksListView, getDiffOrderMessageComparator()), 2000, 100, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersMappingScheduler(), 2000, 100, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleAtFixedRate(createDiffOrdersMappingScheduler(), 2000, 100, TimeUnit.MILLISECONDS);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Runnable createOrderBookTask(CountDownLatch latch) {
        return () -> {
            try {
                String url = "https://api.bitso.com/v3/order_book/?book=btc_mxn";
                InputStream inputStream = buildHttpURLConnection(url).getInputStream();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode responseNode = mapper.readTree(inputStream);

                restBookSequence = responseNode.path("payload").path("sequence").asInt();

                latch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private Runnable createTradesTask() {
        return () -> {
            try {
                TradesMessage tradesMessage = getTradesMessage();
                List<TradesPayload> trades = tradesMessage.getPayload();

                addAndAnalyzeLastTrades(trades);

                Platform.runLater(() -> lastTradesListView.getItems().setAll(trades.subList(0, MAX_DISPLAYABLE_BIDS_AND_ASKS)));

                updateInfoTextArea();

            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private void addAndAnalyzeLastTrades(List<TradesPayload> trades) {
        Deque<TradesPayload> lastTrades = new ArrayDeque<>();

        System.out.println("addAndAnalyzeLastTrades");
        trades.forEach(lastTrades::add);

        BigDecimal lastPrice = BigDecimal.valueOf(0);

        Integer currentTransactionId;
        TradesPayload currentTrade = lastTrades.pollLast();
        TradesPayload lastTrade = lastTrades.pollFirst();
        while (currentTrade != null) {
            currentTransactionId = currentTrade.getTid();
            if ((currentTransactionId - lastTradeId.intValue()) > 0) {

                BigDecimal price = BigDecimal.valueOf(Double.parseDouble(currentTrade.getPrice()));
                switch (price.compareTo(lastPrice)) {
                    case -1:
                        downtickCount.incrementAndGet();
                        uptickCount.set(0);
                        break;
                    case 0:
                        break;
                    case 1:
                        uptickCount.incrementAndGet();
                        downtickCount.set(0);
                        break;
                }

                if (!firstTrades) {
                    buyOrSell(currentTrade);
                }

                lastPrice = price;
                infoMap.put(LAST_PRICE, BigDecimal.valueOf(lastPrice.doubleValue()));
                lastTradeId.set(currentTransactionId);
            }

            currentTrade = lastTrades.pollLast();

        }

        if (firstTrades) {
            infoMap.putIfAbsent(STARTING_PRICE, BigDecimal.valueOf(lastPrice.doubleValue()));
            buyOrSell(lastTrade);
        }

        trades.addAll(mockBuyAndSell);
        trades.sort(Comparator.comparingInt(TradesPayload::getTid).reversed());

    }

    private void updateInfoTextArea() {
        Platform.runLater(() -> {
            StringJoiner sj = new StringJoiner("\n");
            BigDecimal coins = infoMap.getOrDefault(COINS, BigDecimal.valueOf(0));
            BigDecimal lastPrice = infoMap.getOrDefault(LAST_PRICE, BigDecimal.valueOf(0));
            BigDecimal money = infoMap.getOrDefault(MONEY, BigDecimal.valueOf(0));
            BigDecimal startingPrice = infoMap.getOrDefault(STARTING_PRICE, BigDecimal.valueOf(0));
            BigDecimal total = coins.multiply(lastPrice).add(money);

            infoMap.putIfAbsent(STARTING_TOTAL, coins.multiply(startingPrice));
            BigDecimal startingTotal = infoMap.get(STARTING_TOTAL);

            BigDecimal profit = total.subtract(startingTotal);

            sj.add("Coins: " + coins)
                    .add("Money: " + NumberFormat.getCurrencyInstance().format(money))
                    .add("Total: " + NumberFormat.getCurrencyInstance().format(total))
                    .add("Starting Total: " + NumberFormat.getCurrencyInstance().format(startingTotal))
                    .add("Profit: " + NumberFormat.getCurrencyInstance().format(profit))
                    .add("")
                    .add("Upticks: " + uptickCount.get())
                    .add("Downticks: " + downtickCount.get());

            infoTextArea.setText(sj.toString());
        });

    }

    private void buyOrSell(TradesPayload tradesPayload) {
        if (uptickCount.get() >= CONSECUTIVE_UPTICKS) {
            sellCoins(tradesPayload);
            firstTrades = false;
        }

        if (downtickCount.get() >= CONSECUTIVE_DOWNTICKS) {
            buyCoins(tradesPayload);
            firstTrades = false;
        }
    }

    private void buyCoins(TradesPayload tradesPayload) {
        executeTransaction(tradesPayload, "YOU BUY");
        infoMap.compute(COINS, (k, v) -> v.add(BigDecimal.valueOf(1)));
        infoMap.compute(MONEY, (k, v) -> v.subtract(BigDecimal.valueOf(Double.valueOf(tradesPayload.getPrice()))));
    }

    private void sellCoins(TradesPayload tradesPayload) {
        executeTransaction(tradesPayload, "YOU SELL");
        infoMap.compute(COINS, (k, v) -> v.subtract(BigDecimal.valueOf(1)));
        infoMap.compute(MONEY, (k, v) -> v.add(BigDecimal.valueOf(Double.valueOf(tradesPayload.getPrice()))));
    }

    private void executeTransaction(TradesPayload tradesPayload, String operation) {
        TradesPayload newTrade = TradesPayload.clone(tradesPayload);
        newTrade.setAmount("1.00000000");
        newTrade.setMaker_side(operation);

        mockBuyAndSell.offer(newTrade);
    }

    private TradesMessage getTradesMessage() throws IOException {
        String url = "https://api.bitso.com/v3/trades/?book=btc_mxn&limit=" + MAX_DISPLAYABLE_BIDS_AND_ASKS;
        InputStream inputStream = buildHttpURLConnection(url).getInputStream();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseNode = mapper.readTree(inputStream);
        return mapper.convertValue(responseNode, TradesMessage.class);
    }

    private HttpURLConnection buildHttpURLConnection(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        return con;
    }

    private Runnable createDiffOrdersMappingScheduler() {
        return () -> {
            String diffOrder = diffOrders.poll();
            if (diffOrder != null) {
                processDiffOrderMessage(diffOrder);
            }
        };
    }

    private Runnable createDiffOrdersScheduler(Operation operation, ListView<DiffOrderMessage> listView, Comparator<DiffOrderMessage> orderMessageComparator) {
        return () -> {
            ConcurrentLinkedQueue<DiffOrderMessage> bidDiffOrderMessages = diffOrdersMap.getOrDefault(operation, new ConcurrentLinkedQueue<>());
            DiffOrderMessage diffOrderMessage = bidDiffOrderMessages.poll();
            if (diffOrderMessage != null) {
                if (diffOrderMessage.getSequence() > restBookSequence) {
                    Platform.runLater(processListView(listView, diffOrderMessage, orderMessageComparator));
                }
            }
        };
    }

    private Runnable processListView(ListView<DiffOrderMessage> listView, DiffOrderMessage diffOrderMessage, Comparator<DiffOrderMessage> diffOrderMessageComparator) {
        return () -> {

            Collection<DiffOrderMessage> diffOrderMessages = new ArrayList<>();
            List<DiffOrderPayload> diffOrderPayload = diffOrderMessage.getPayload();

            if (diffOrderPayload.size() > 1) {
                diffOrderPayload.forEach(p -> {
                    DiffOrderMessage message = DiffOrderMessage.clone(diffOrderMessage);
                    ArrayList<DiffOrderPayload> diffOrderPayloads = new ArrayList<>();
                    diffOrderPayloads.add(p);
                    message.setPayload(diffOrderPayloads);
                    diffOrderMessages.add(message);
                });

                listView.getItems().addAll(diffOrderMessages);
            } else {
                listView.getItems().add(diffOrderMessage);
            }

            SortedList<DiffOrderMessage> sorted = listView.getItems()
                    .sorted(diffOrderMessageComparator);

            List<DiffOrderMessage> collect = sorted.stream()
                    .limit(MAX_DISPLAYABLE_BIDS_AND_ASKS)
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

    private void processDiffOrderMessage(String message) {
        try {
            JsonNode messageNode = parseJsonMessage(message);
            if (isValidDiffOrdersMessage(messageNode)) {
                DiffOrderMessage diffOrderMessage = parseDiffOrderMessage(messageNode);
                if (diffOrderMessage.isBuy()) {
                    diffOrdersMap.computeIfAbsent(Operation.ASK, k -> new ConcurrentLinkedQueue<>()).add(diffOrderMessage);
                } else {
                    diffOrdersMap.computeIfAbsent(Operation.BID, k -> new ConcurrentLinkedQueue<>()).add(diffOrderMessage);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DiffOrderMessage parseDiffOrderMessage(JsonNode messageNode) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(messageNode, DiffOrderMessage.class);
    }

    private static JsonNode parseJsonMessage(String inputStream) throws IOException {
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
