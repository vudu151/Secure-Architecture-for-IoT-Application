#!/bin/bash
# Script chạy chương trình Java MQTT

# Đường dẫn tới java của JBR JDK
JAVA_PATH="/home/duvx/Downloads/idea-2026.1.2/jbr/bin/java"

# Kiểm tra xem java có tồn tại không
if [ ! -f "$JAVA_PATH" ]; then
    echo "Lỗi: Không tìm thấy java tại $JAVA_PATH"
    echo "Hãy cấu hình lại đường dẫn JAVA_PATH trong script này."
    exit 1
fi

# Chạy chương trình
"$JAVA_PATH" -cp "bin:lib/*" com.example.mqtt.MqttApp
