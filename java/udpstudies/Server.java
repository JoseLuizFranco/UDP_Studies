package com.sliftio.udpstudies;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class Server extends Application {

    private Map<String, Integer> clientDataCount = new HashMap<>();
    private Map<String, Integer> clientExpectedSequence = new HashMap<>();
    private Map<String, Integer> clientLostPackets = new HashMap<>();
    private Map<String, XYChart.Series<String, Number>> clientBarSeriesMap = new LinkedHashMap<>();
    private Map<String, XYChart.Series<String, Number>> clientLostBarSeriesMap = new LinkedHashMap<>();
    private Map<String, XYChart.Series<Number, Number>> clientLineSeriesMap = new LinkedHashMap<>();
    private LineChart<Number, Number> lineChart;
    private BarChart<String, Number> receivedBarChart;
    private BarChart<String, Number> lostBarChart;
    private int packetIndex = 0;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Dashboard de Pacotes UDP");

        CategoryAxis xAxisReceived = new CategoryAxis();
        NumberAxis yAxisReceived = new NumberAxis();
        yAxisReceived.setLabel("Pacotes Recebidos");
        yAxisReceived.setTickUnit(1);
        yAxisReceived.setMinorTickCount(0);
        yAxisReceived.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxisReceived) {
            @Override
            public String toString(Number object) {
                return String.format("%d", object.intValue());
            }
        });

        receivedBarChart = new BarChart<>(xAxisReceived, yAxisReceived);
        receivedBarChart.setTitle("Pacotes Recebidos por Cliente");
        xAxisReceived.setLabel("Cliente");

        CategoryAxis xAxisLost = new CategoryAxis();
        NumberAxis yAxisLost = new NumberAxis();
        yAxisLost.setLabel("Pacotes Perdidos");
        yAxisLost.setTickUnit(1);
        yAxisLost.setMinorTickCount(0);
        yAxisLost.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxisLost) {
            @Override
            public String toString(Number object) {
                return String.format("%d", object.intValue());
            }
        });

        lostBarChart = new BarChart<>(xAxisLost, yAxisLost);
        lostBarChart.setTitle("Pacotes Perdidos por Cliente");
        xAxisLost.setLabel("Cliente");

        NumberAxis xAxisLine = new NumberAxis();
        NumberAxis yAxisLine = new NumberAxis();
        lineChart = new LineChart<>(xAxisLine, yAxisLine);
        lineChart.setTitle("Valores Recebidos dos Clientes");
        xAxisLine.setLabel("Índice do Pacote");
        yAxisLine.setLabel("Valor do Dado");

        VBox vbox = new VBox(receivedBarChart, lostBarChart, lineChart);
        Scene scene = new Scene(vbox, 800, 800);

        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(this::startUDPServer).start();
    }

    private void startUDPServer() {
        try (DatagramSocket socket = new DatagramSocket(5005)) {
            byte[] buffer = new byte[1024];
            System.out.println("Servidor UDP escutando na porta 5005...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Recebido: " + received);

                String[] parts = received.split(":");
                if (parts.length >= 3) {
                    String clientId = parts[0].trim();
                    try {
                        int sequenceNumber = Integer.parseInt(parts[1].trim());
                        int value = Integer.parseInt(parts[2].trim());
                        updateData(clientId, sequenceNumber, value);
                    } catch (NumberFormatException e) {
                        System.out.println("Erro ao decodificar o valor: " + received);
                    }
                } else {
                    System.out.println("Formato de mensagem inválido: " + received);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateData(String clientId, int sequenceNumber, int value) {
        int expectedSequence = clientExpectedSequence.getOrDefault(clientId, 0);
        int lostPackets = clientLostPackets.getOrDefault(clientId, 0);

        if (sequenceNumber > expectedSequence) {
            int missingPackets = sequenceNumber - expectedSequence;
            lostPackets += missingPackets;
            clientLostPackets.put(clientId, lostPackets);
            updateLostBarChart(clientId, lostPackets);
        }

        clientExpectedSequence.put(clientId, sequenceNumber + 1);

        clientDataCount.put(clientId, clientDataCount.getOrDefault(clientId, 0) + 1);

        Platform.runLater(() -> {
            XYChart.Series<String, Number> receivedBarSeries = clientBarSeriesMap.get(clientId);
            if (receivedBarSeries == null) {
                receivedBarSeries = new XYChart.Series<>();
                receivedBarSeries.setName(clientId);
                clientBarSeriesMap.put(clientId, receivedBarSeries);
                receivedBarChart.getData().add(receivedBarSeries);

                XYChart.Data<String, Number> data = new XYChart.Data<>(clientId, clientDataCount.get(clientId));
                receivedBarSeries.getData().add(data);

                addLabelToBar(data);
            } else {
                XYChart.Data<String, Number> data = receivedBarSeries.getData().get(0);
                data.setYValue(clientDataCount.get(clientId));
                addLabelToBar(data);
            }
        });

        Platform.runLater(() -> {
            XYChart.Series<Number, Number> lineSeries = clientLineSeriesMap.get(clientId);
            if (lineSeries == null) {
                lineSeries = new XYChart.Series<>();
                lineSeries.setName(clientId);
                clientLineSeriesMap.put(clientId, lineSeries);
                lineChart.getData().add(lineSeries);
            }

            lineSeries.getData().add(new XYChart.Data<>(packetIndex++, value));
        });
    }

    private void updateLostBarChart(String clientId, int lostPackets) {
        Platform.runLater(() -> {
            XYChart.Series<String, Number> lostBarSeries = clientLostBarSeriesMap.get(clientId);
            if (lostBarSeries == null) {
                lostBarSeries = new XYChart.Series<>();
                lostBarSeries.setName(clientId);
                clientLostBarSeriesMap.put(clientId, lostBarSeries);
                lostBarChart.getData().add(lostBarSeries);

                XYChart.Data<String, Number> data = new XYChart.Data<>(clientId, lostPackets);
                lostBarSeries.getData().add(data);

                addLabelToBar(data);
            } else {
                XYChart.Data<String, Number> data = lostBarSeries.getData().get(0);
                data.setYValue(lostPackets);
                addLabelToBar(data);
            }
        });
    }

    private void addLabelToBar(XYChart.Data<String, Number> data) {
        Node node = data.getNode();
        if (node != null) {
            StackPane stackPane = (StackPane) node;
            Label label = new Label(String.valueOf(data.getYValue().intValue()));
            label.setStyle("-fx-text-fill: white;");
            stackPane.getChildren().removeIf(n -> n instanceof Label);
            stackPane.getChildren().add(label);
            StackPane.setAlignment(label, Pos.TOP_CENTER);
        }
    }
}
