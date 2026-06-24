#!/bin/bash
# Script biên dịch chương trình Java MQTT

# Thư mục chứa code đã biên dịch
mkdir -p bin

# Đường dẫn tới javac của JBR JDK
JAVAC_PATH="/home/duvx/Downloads/idea-2026.1.2/jbr/bin/javac"

# Kiểm tra xem javac có tồn tại không
if [ ! -f "$JAVAC_PATH" ]; then
    echo "Lỗi: Không tìm thấy trình biên dịch javac tại $JAVAC_PATH"
    echo "Hãy cài đặt OpenJDK hoặc cấu hình lại đường dẫn JAVAC_PATH trong script này."
    exit 1
fi

echo "Đang biên dịch mã nguồn Java..."
"$JAVAC_PATH" -d bin -cp "lib/*" src/main/java/com/example/mqtt/*.java

if [ $? -eq 0 ]; then
    echo "Biên dịch thành công! Mã lớp nằm trong thư mục 'bin/'."
else
    echo "Lỗi: Biên dịch thất bại!"
    exit 1
fi
