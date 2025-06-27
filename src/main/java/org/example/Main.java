package org.example;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final String DEFAULT_BROKER = "tcp://localhost:1883";
    private static final String DEFAULT_TOPIC = "producers/mycar/data";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final String DEFAULT_FILE = "./mosaic.txt";
    private static final String DEFAULT_START = "2023-09-21 07:40:56";
    private static final String DEFAULT_END = "2023-09-21 07:59:00";

    private static NmeaMessageParser parser;

    public static void main(String[] args) throws IOException, InterruptedException {
        String filePath = args.length > 4 ? args[4] : DEFAULT_FILE;
        String startStr = args.length > 5 ? args[5] : DEFAULT_START;
        String endStr = args.length > 6 ? args[6] : DEFAULT_END;

        parser = new NmeaMessageParser(filePath, startStr, endStr);

        String broker = args.length > 0 ? args[0] : DEFAULT_BROKER;
        String username = args.length > 1 ? args[1] : DEFAULT_USERNAME;
        String password = args.length > 2 ? args[2] : DEFAULT_PASSWORD;
        String topic = args.length > 3 ? args[3] : DEFAULT_TOPIC;

        String hostPort = broker.replace("tcp://", "").replace("ssl://", "");
        String[] split = hostPort.split(":");
        String host = split[0];
        int port = (split.length > 1) ? Integer.parseInt(split[1])
                : (broker.startsWith("ssl://") ? 8883 : 1883);
        boolean useTls = broker.startsWith("ssl://");

        var builder = MqttClient.builder()
                .useMqttVersion3()
                .serverHost(host)
                .serverPort(port);

        if (useTls) {
            builder = builder.sslWithDefaultConfig();
        }

        Mqtt3AsyncClient client = builder.buildAsync();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown detected. Disconnecting...");
            client.disconnect()
                    .whenComplete((ack, ex) -> System.out.println("Disconnected. Bye!"));
        }));

        var connectBuilder = client.connectWith();
        if (!username.isEmpty()) {
            connectBuilder.simpleAuth()
                    .username(username)
                    .password(password.getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth();
        }

        try {
            Mqtt3ConnAck connAck = connectBuilder.send().join();
            System.out.println("Connected to broker: " + broker);
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            System.exit(1);
        }

        runSimulation(client, topic);
    }

    private static void runSimulation(Mqtt3AsyncClient client, String topic) throws InterruptedException {
        System.out.println("Starting simulation loop...");

        List<List<String>> perSecondMessages = parser.getMessagesBySecond();

        while (true) {
            for (int i = 0; i < perSecondMessages.size(); i++) {
                LocalDateTime ts = parser.getIntervalStart().plusSeconds(i);
                System.out.println("[" + ts + "]");

                List<String> messages = perSecondMessages.get(i);
                for (String msg : messages) {
                    String sensorId = extractSensorId(msg);
                    String fullTopic = topic + "/" + sensorId;
                    publishMessage(client, fullTopic, msg);
                }

                TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    private static String extractSensorId(String message) {
        int start = message.indexOf('[');
        int end = message.indexOf(']');
        if (start >= 0 && end > start) {
            return message.substring(start + 1, end).trim();
        }
        return "track";
    }

    private static void publishMessage(Mqtt3AsyncClient client, String topic, String payload) {
        client.publishWith()
                .topic(topic)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("Publish failed: " + ex.getMessage());
                    } else {
                        System.out.println("Published: " + payload);
                    }
                });
    }
}
