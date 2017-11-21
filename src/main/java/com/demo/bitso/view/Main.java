package com.demo.bitso.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("bitsoTemplate.fxml"));
        primaryStage.setTitle("BitsoDemo");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }


    public static void main(String[] args) {
        System.setProperty("MAX_DISPLAYABLE_BIDS_AND_ASKS", "10");
        System.setProperty("CONSECUTIVE_UPTICKS", "3");
        System.setProperty("CONSECUTIVE_DOWNTICKS", "3");

        launch(args);
    }
}
