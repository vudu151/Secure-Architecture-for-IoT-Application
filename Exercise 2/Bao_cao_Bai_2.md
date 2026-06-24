# BÁO CÁO BÀI TẬP 2: TRAO ĐỔI DỮ LIỆU QUA GIAO THỨC MQTT BẰNG JAVA

- **Môn học:** Kiến trúc An ninh cho Ứng dụng IoT (Secure Architecture for IoT Application)
- **Họ và tên:** [Điền họ tên của bạn vào đây]
- **Mã số sinh viên:** [Điền MSSV của bạn vào đây]
- **Lớp:** [Điền lớp vào đây]

---

## 1. Yêu cầu bài tập
- Viết chương trình bằng ngôn ngữ tự chọn (ở đây chọn Java) để:
  - Gửi (Publish) dữ liệu lên một MQTT Broker (sử dụng `broker.hivemq.com:1883`).
  - Đóng gói dữ liệu bằng JSON định dạng:
    ```json
    {
      "DeviceName": "my-mqtt-client",
      "temperature": 30,
      "humidity": 60
    }
    ```
  - Nhận (Subscribe) dữ liệu từ Broker.
  - Parse dữ liệu nhận được và hiển thị các trường dữ liệu tương ứng.
  - Sử dụng công cụ kiểm thử MQTT (ví dụ MQTTBox hoặc cửa sổ console) để kiểm chứng.
  - Nộp file báo cáo cùng toàn bộ file mã nguồn.

---

## 2. Thiết kế hệ thống và Định dạng dữ liệu
Hệ thống sử dụng mô hình truyền thông **Publish/Subscribe** qua Broker trung gian:
- **Broker:** `broker.hivemq.com` kết nối qua cổng TCP mặc định `1883`.
- **Topic truyền tin:** `hust/iot/sensor/data`
- **Định dạng dữ liệu:** Chuỗi JSON chứa 3 trường dữ liệu cảm biến:
  - `DeviceName` (String): Tên thiết bị gửi dữ liệu.
  - `temperature` (double): Nhiệt độ đo được (đơn vị °C).
  - `humidity` (double): Độ ẩm đo được (đơn vị %).

---

## 3. Các thành phần mã nguồn chính

### 3.1. Publisher (MqttPublisher.java)
Lớp thực hiện khởi tạo kết nối MQTT, đóng gói chuỗi JSON và đẩy thông điệp lên Broker.

```java
package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

public class MqttPublisher {
    private final String brokerUrl;
    private final String clientId;

    public MqttPublisher(String brokerUrl, String clientId) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
    }

    public void publishData(String topic, String deviceName, double temperature, double humidity) {
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            MqttClient client = new MqttClient(brokerUrl, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(10);
            
            client.connect(connOpts);

            JSONObject payload = new JSONObject();
            payload.put("DeviceName", deviceName);
            payload.put("temperature", temperature);
            payload.put("humidity", humidity);
            
            MqttMessage message = new MqttMessage(payload.toString(4).getBytes());
            message.setQos(1);
            
            client.publish(topic, message);
            client.disconnect();
        } catch (MqttException me) {
            System.err.println("Lỗi MQTT Publisher: " + me.getMessage());
        }
    }
}
```

### 3.2. Subscriber (MqttSubscriber.java)
Lớp lắng nghe kết nối bất đồng bộ từ Broker, bóc tách chuỗi JSON và in thông tin ra Console.

```java
package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;
import org.json.JSONException;

public class MqttSubscriber implements MqttCallback {
    private final String brokerUrl;
    private final String clientId;
    private MqttClient client;
    private boolean isRunning = false;

    public MqttSubscriber(String brokerUrl, String clientId) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
    }

    public synchronized void start(String topic) {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            client.setCallback(this);
            client.connect(connOpts);
            client.subscribe(topic);
            isRunning = true;
        } catch (MqttException me) {
            System.err.println("Lỗi Subscriber: " + me.getMessage());
        }
    }

    public synchronized void stop() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException me) {
            System.err.println("Lỗi dừng Subscriber: " + me.getMessage());
        } finally { isRunning = false; }
    }

    public boolean isRunning() { return isRunning; }

    @Override
    public void connectionLost(Throwable cause) {}

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            JSONObject json = new JSONObject(payload);
            String deviceName = json.optString("DeviceName", "Unknown");
            double temp = json.optDouble("temperature", Double.NaN);
            double hum = json.optDouble("humidity", Double.NaN);

            System.out.println("\n--- ĐÃ NHẬN GÓI TIN MQTT ---");
            System.out.println("Thiết bị: " + deviceName);
            System.out.println("Nhiệt độ: " + temp + " °C");
            System.out.println("Độ ẩm: " + hum + " %");
        } catch (JSONException e) {
            System.err.println("Lỗi định dạng JSON: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}
}
```

---

## 4. Kết quả chạy thử chương trình

### 4.1. Biên dịch chương trình
Sử dụng script biên dịch `build.sh` sử dụng Java SDK nội bộ:
```bash
./build.sh
```
*(Chèn ảnh chụp màn hình terminal lúc chạy build.sh thành công vào đây)*

### 4.2. Khởi chạy và tương tác truyền nhận dữ liệu
Khởi chạy qua script `run.sh`:
```bash
./run.sh
```
*(Chèn ảnh chụp màn hình terminal lúc nhập Broker URL / chạy menu chương trình và các bản tin nhận được vào đây)*

---

## 5. Kết luận
- Chương trình Java chạy ổn định và thực hiện đầy đủ các chức năng kết nối, đăng ký nhận (Subscribe), đóng gói JSON và xuất bản (Publish) thông điệp lên MQTT Broker.
- Việc sử dụng các Client ID phân biệt cho Publisher và Subscriber giúp đảm bảo kết nối song song diễn ra liên tục, không bị ngắt quãng do xung đột kết nối trùng lặp.
