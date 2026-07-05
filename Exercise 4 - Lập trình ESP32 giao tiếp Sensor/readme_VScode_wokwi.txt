Using Visual Code Studio with Wokwi Simulator
1. Install Extensions for Visual Code Studio: PlatformIO, Wokwi Simulator
2. Create a project using PlatformIO 
3. Config compiler in the file platformio.ini

[env:esp32-c3-devkitm-1]
platform = espressif32
board = esp32-c3-devkitm-1
framework = arduino
debug_tool = esp-builtin

4. Config the circuit diagram in the file diagram.json
(Copy from Wokwi Simulator)
5. Write the code for the program
6. Compile the program with PlatformIO build
Kết quả tạo file thực thi firmware.bin trong thư mục build của project
7. Cấu hình nạp file firmware cho MCU trên Wokwi Simulator
File wokwi.toml
[wokwi]
version = 1
firmware = '.pio/build/esp32-c3-devkitm-1/firmware.bin'
elf = '.pio/build/esp32-c3-devkitm-1/firmware.elf'
8. Chạy mô phỏng Wokwi Simulator