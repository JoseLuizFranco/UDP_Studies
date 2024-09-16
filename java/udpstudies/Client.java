package com.sliftio.udpstudies;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Client extends Application {

    private boolean autoSend = false;
    private int sendInterval = 1;
    private String clientId;
    private int sequenceNumber = 0; // Added sequence number

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Cliente UDP");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        clientId = "Client " + (int) (Math.random() * 1000);

        TextField manualInput = new TextField();
        manualInput.setPromptText("Digite o valor");

        Button sendButton = new Button("Enviar Manualmente");
        sendButton.setOnAction(e -> sendManualData(manualInput.getText()));

        Button startAutoButton = new Button("Iniciar Envio Automático");
        startAutoButton.setOnAction(e -> startAutoSend());

        Button stopAutoButton = new Button("Parar Envio Automático");
        stopAutoButton.setOnAction(e -> stopAutoSend());

        HBox intervalBox = new HBox(5);
        Label intervalLabel = new Label("Intervalo (s):");
        Spinner<Integer> intervalSpinner = new Spinner<>(1, 10, 1);
        intervalSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            sendInterval = newValue;
        });
        intervalBox.getChildren().addAll(intervalLabel, intervalSpinner);

        root.getChildren().addAll(manualInput, sendButton, startAutoButton, stopAutoButton, intervalBox);

        Scene scene = new Scene(root, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void sendManualData(String value) {
        try {
            DatagramSocket socket = new DatagramSocket();
            String message = clientId + ":" + sequenceNumber + ":" + value;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("127.0.0.1"), 5005);
            socket.send(packet);
            socket.close();
            System.out.println("Enviado: " + message);
            sequenceNumber++; // Increment sequence number after sending
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAutoSend() {
        autoSend = true;
        new Thread(() -> {
            while (autoSend) {
                try {
                    sendManualData(String.valueOf(new Random().nextInt(100)));
                    Thread.sleep(sendInterval * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void stopAutoSend() {
        autoSend = false;
    }
}
