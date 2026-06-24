package com.example.mqtt;

import java.util.Scanner;

public class MqttApp {

    private static final String DEFAULT_BROKER = "tcp://broker.hivemq.com:1883";
    private static final String DEFAULT_TOPIC = "hust/iot/sensor/data";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String brokerUrl = DEFAULT_BROKER;
        String topic = DEFAULT_TOPIC;

        System.out.println("=== CHƯƠNG TRÌNH GIAO TIẾP MQTT TRÊN JAVA ===");
        System.out.println("Cấu hình mặc định:");
        System.out.println("  - Broker URL : " + brokerUrl);
        System.out.println("  - Topic      : " + topic);
        System.out.println("--------------------------------------------");
        System.out.print("Bạn có muốn đổi cấu hình mặc định không? (y/n): ");
        String changeConfig = scanner.nextLine().trim();

        if (changeConfig.equalsIgnoreCase("y")) {
            System.out.print("Nhập Broker URL (ví dụ: tcp://broker.hivemq.com:1883): ");
            String inputBroker = scanner.nextLine().trim();
            if (!inputBroker.isEmpty()) {
                brokerUrl = inputBroker;
            }
            System.out.print("Nhập Topic (ví dụ: my/mqtt/topic): ");
            String inputTopic = scanner.nextLine().trim();
            if (!inputTopic.isEmpty()) {
                topic = inputTopic;
            }
        }

        // Initialize Publisher and Subscriber with distinct Client IDs
        MqttPublisher publisher = new MqttPublisher(brokerUrl, "client-publisher-hust");
        MqttSubscriber subscriber = new MqttSubscriber(brokerUrl, "client-subscriber-hust");

        boolean exit = false;
        while (!exit) {
            System.out.println("\n================= MENU CHỨC NĂNG =================");
            System.out.println("1. Bật Subscriber (Đăng ký nhận dữ liệu)");
            System.out.println("2. Tắt Subscriber");
            System.out.println("3. Gửi dữ liệu JSON (Publish sensor data)");
            System.out.println("4. Thoát chương trình");
            System.out.println("==================================================");
            System.out.print("Chọn chức năng (1-4): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    if (subscriber.isRunning()) {
                        System.out.println("[Menu] Subscriber đang hoạt động rồi!");
                    } else {
                        System.out.println("[Menu] Đang khởi chạy Subscriber...");
                        final String finalTopic = topic;
                        // Run subscription in a separate thread so it doesn't block the menu UI
                        new Thread(() -> subscriber.start(finalTopic)).start();
                    }
                    break;

                case "2":
                    if (!subscriber.isRunning()) {
                        System.out.println("[Menu] Subscriber hiện tại không chạy.");
                    } else {
                        System.out.println("[Menu] Đang tắt Subscriber...");
                        subscriber.stop();
                    }
                    break;

                case "3":
                    System.out.println("\n--- GỬI DỮ LIỆU SENSOR GIAO THỨC MQTT ---");
                    System.out.print("Nhập Tên thiết bị (mặc định: my-mqtt-client): ");
                    String devName = scanner.nextLine().trim();
                    if (devName.isEmpty()) {
                        devName = "my-mqtt-client";
                    }

                    double temp = 30.0;
                    System.out.print("Nhập Nhiệt độ (mặc định: 30.0): ");
                    String tempStr = scanner.nextLine().trim();
                    if (!tempStr.isEmpty()) {
                        try {
                            temp = Double.parseDouble(tempStr);
                        } catch (NumberFormatException e) {
                            System.out.println("Định dạng sai, sử dụng giá trị mặc định 30.0");
                        }
                    }

                    double hum = 60.0;
                    System.out.print("Nhập Độ ẩm (mặc định: 60.0): ");
                    String humStr = scanner.nextLine().trim();
                    if (!humStr.isEmpty()) {
                        try {
                            hum = Double.parseDouble(humStr);
                        } catch (NumberFormatException e) {
                            System.out.println("Định dạng sai, sử dụng giá trị mặc định 60.0");
                        }
                    }

                    final String pubDevName = devName;
                    final double pubTemp = temp;
                    final double pubHum = hum;
                    final String pubTopic = topic;

                    // Publish on a separate thread to keep menu responsive
                    new Thread(() -> publisher.publishData(pubTopic, pubDevName, pubTemp, pubHum)).start();
                    break;

                case "4":
                    System.out.println("[Menu] Đang giải phóng tài nguyên và thoát...");
                    if (subscriber.isRunning()) {
                        subscriber.stop();
                    }
                    exit = true;
                    break;

                default:
                    System.out.println("Lựa chọn không hợp lệ. Vui lòng nhập số từ 1 đến 4.");
            }
            
            // Brief pause to allow prints from threads to settle
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {}
        }
        
        System.out.println("Cảm ơn bạn đã sử dụng chương trình! Tạm biệt.");
        System.exit(0);
    }
}
