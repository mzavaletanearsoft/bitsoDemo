#bitsoDemo
##Playing with Bitso

|Feature|File name|Method name|
|-------|---------|-----------|
|Schedule the polling of trades over REST|[Controller.java](https://github.com/mzavaletanearsoft/bitsoDemo/blob/master/src/main/java/com/demo/bitso/controller/Controller.java)|createTradesTask|
|Request a book snapshot over REST|[Controller.java](https://github.com/mzavaletanearsoft/bitsoDemo/blob/master/src/main/java/com/demo/bitso/controller/Controller.java)|createOrderBookTask|
|Listen for diff-orders over websocket|[Controller.java](https://github.com/mzavaletanearsoft/bitsoDemo/blob/master/src/main/java/com/demo/bitso/controller/Controller.java)|initialize|
|Replay diff-orders|[Controller.java](https://github.com/mzavaletanearsoft/bitsoDemo/blob/master/src/main/java/com/demo/bitso/controller/Controller.java)|createDiffOrdersScheduler|
|Use config option X to request  recent trades|[main.java](https://github.com/mzavaletanearsoft/bitsoDemo/blob/master/src/main/java/com/demo/bitso/view/main.java) [Controller.java](https://github.com/mzavaletanearsoft/bitsoDemo/blob/master/src/main/java/com/demo/bitso/controller/Controller.java)|main \ createTradesTask|
|Use config option X to limit number of ASKs displayed in UI|[Controller.java](https://github.com/mzavaletanearsoft/bitsoDemo/blob/master/src/main/java/com/demo/bitso/controller/Controller.java)|processListView|
|The loop that causes the trading algorithm to reevaluate|[Controller.java](https://github.com/mzavaletanearsoft/bitsoDemo/blob/master/src/main/java/com/demo/bitso/controller/Controller.java)|addAndAnalyzeLastTrades|

