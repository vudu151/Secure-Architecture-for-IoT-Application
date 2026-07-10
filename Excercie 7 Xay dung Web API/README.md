# WebAPIDemo - IoT Sensor & Firmware Management API

[![.NET Version](https://img.shields.io/badge/.NET-10.0-blue.svg)](https://dotnet.microsoft.com/)
[![Database](https://img.shields.io/badge/Database-SQL_Server_2022-red.svg)](https://www.microsoft.com/en-us/sql-server/)
[![Containerized](https://img.shields.io/badge/Container-Docker_Compose-blue.svg)](https://www.docker.com/)
[![ORM](https://img.shields.io/badge/ORM-EF_Core_9-purple.svg)](https://learn.microsoft.com/en-us/ef/core/)

Dự án **Web API** được xây dựng trên nền tảng **ASP.NET Core (.NET 10)** dùng để thu thập dữ liệu cảm biến IoT (Nhiệt độ, Độ ẩm) và quản lý phiên bản ứng dụng (Firmware OTA update). Hệ thống sử dụng **Microsoft SQL Server 2022** chạy trên nền tảng **Docker** thay thế cho MongoDB.

---

## 📌 Mục lục
1. [Cấu trúc Thư mục](#-cấu-trúc-thư-mục)
2. [Yêu cầu Hệ thống](#-yêu-cầu-hệ-thống)
3. [Hướng dẫn Khởi chạy Nhanh](#-hướng-dẫn-khởi-chạy-nhanh)
4. [Danh sách API & Kiểm thử](#-danh-sách-api--kiểm-thử)
5. [Tính năng Bảo mật Tích hợp](#-tính-năng-bảo-mật-tích-hợp)
6. [Liên kết Hữu ích](#-liên-kết-hữu-ích)

---

## 📂 Cấu trúc Thư mục

```text
Excercie 7 - Xay dung Web API/
├── docker-compose.yml          # Triển khai SQL Server 2022 bằng Docker
├── start.sh                    # Script bash tự động hóa quy trình khởi động
├── BaoCao_BaiTap7_WebAPI.doc   # Tài liệu báo cáo chi tiết (đã thêm Mục lục & link Github)
└── WebAPIDemo/                 # Mã nguồn chính Web API (.NET 10)
    ├── Controllers/            # Xử lý các HTTP Request (SensorData, AppVersion)
    ├── Models/                 # Thực thể cơ sở dữ liệu & EF Core DbContext
    ├── Services/               # Lớp xử lý logic nghiệp vụ (Business Service Layer)
    ├── Program.cs              # Đăng ký dịch vụ, DI, Connection Retry & Pipelines
    └── appsettings.json        # Chuỗi kết nối Database SQL Server
```

---

## 💻 Yêu cầu Hệ thống
Trước khi khởi chạy dự án, hãy đảm bảo máy tính của bạn đã cài đặt các công cụ sau:
*   **.NET 10 SDK**
*   **Docker** & **Docker Compose**
*   Hệ điều hành Linux/macOS (để chạy script `.sh`) hoặc Windows (WSL2/Git Bash).

---

## 🚀 Hướng dẫn Khởi chạy Nhanh

### Cách 1: Khởi động tự động bằng Script (Khuyên dùng)
Dự án đã tích hợp sẵn script `start.sh` giúp tự động hóa toàn bộ quy trình cấu hình cơ sở dữ liệu và chạy ứng dụng.

Mở Terminal tại thư mục gốc của dự án và chạy:
```bash
chmod +x start.sh
./start.sh
```
*Script sẽ tự động khởi chạy SQL Server, đợi database chuyển sang trạng thái sẵn sàng kết nối (`healthy`), khôi phục NuGet packages và chạy Web API.*

### Cách 2: Khởi động thủ công từng bước

#### Bước 1: Khởi chạy cơ sở dữ liệu SQL Server
```bash
docker compose up -d
```

#### Bước 2: Chạy ứng dụng Web API
Di chuyển vào thư mục mã nguồn và khởi động dự án:
```bash
cd WebAPIDemo
dotnet restore
dotnet run --urls "http://0.0.0.0:5000"
```

---

## 🧪 Danh sách API & Kiểm thử

Sau khi ứng dụng khởi chạy thành công, bạn có thể thực hiện kiểm thử các API bằng lệnh `cURL` trên Terminal hoặc qua giao diện Swagger UI.

### 1. Nhóm API quản lý dữ liệu cảm biến (`SensorData`)

| Method | Endpoint | Mô tả nghiệp vụ |
| :--- | :--- | :--- |
| **GET** | `/api/SensorDatas` | Lấy danh sách toàn bộ log cảm biến |
| **GET** | `/api/SensorDatas/{id}` | Lấy chi tiết log cảm biến theo ID |
| **GET** | `/api/SensorDatas/device/{deviceId}` | Lấy danh sách dữ liệu theo mã thiết bị |
| **POST** | `/api/SensorDatas` | Thêm dữ liệu cảm biến mới |
| **PUT** | `/api/SensorDatas/{id}` | Cập nhật dữ liệu cảm biến theo ID |
| **DELETE** | `/api/SensorDatas/{id}` | Xóa dữ liệu cảm biến theo ID |

*   **Lệnh cURL test ghi nhận dữ liệu cảm biến (POST):**
    ```bash
    curl -X POST http://localhost:5000/api/SensorDatas \
      -H "Content-Type: application/json" \
      -d '{"deviceId":"DEVICE_001","temperature":29.5,"humidity":65.4}'
    ```
*   **Lệnh cURL test lấy log cảm biến (GET):**
    ```bash
    curl http://localhost:5000/api/SensorDatas
    ```

### 2. Nhóm API quản lý phiên bản phần mềm (`AppVersion`)

| Method | Endpoint | Mô tả nghiệp vụ |
| :--- | :--- | :--- |
| **GET** | `/api/AppVersion` | Lấy danh sách toàn bộ phiên bản đã đăng ký |
| **GET** | `/api/AppVersion/{id}` | Lấy thông tin phiên bản theo ID |
| **GET** | `/api/AppVersion/device/{deviceId}/latest` | Lấy phiên bản mới nhất của một thiết bị (Kiểm tra OTA) |
| **POST** | `/api/AppVersion` | Đăng ký phiên bản phần mềm mới cho thiết bị |
| **DELETE** | `/api/AppVersion/{id}` | Xóa phiên bản phần mềm theo ID |

*   **Lệnh cURL test đăng ký phiên bản firmware mới (POST):**
    ```bash
    curl -X POST http://localhost:5000/api/AppVersion \
      -H "Content-Type: application/json" \
      -d '{"deviceId":"DEVICE_001","version":2}'
    ```
*   **Lệnh cURL test kiểm tra phiên bản mới nhất (GET):**
    ```bash
    curl http://localhost:5000/api/AppVersion/device/DEVICE_001/latest
    ```

---

## 🔒 Tính năng Bảo mật Tích hợp

Dự án được thiết kế tuân thủ các nguyên tắc bảo mật ứng dụng IoT:
1.  **Chống SQL Injection:** Sử dụng hoàn toàn **Entity Framework Core 9 (LINQ)** giúp tự động tham số hóa mọi câu lệnh truy vấn, triệt tiêu khả năng chèn mã SQL độc hại từ client.
2.  **Bảo mật & Cô lập Database:** Cấu hình SQL Server chạy bên trong mạng ảo nội bộ của Docker container. Mật khẩu quản trị viên SA mạnh, ngăn chặn tấn công brute-force từ bên ngoài internet.
3.  **Tự động Phục hồi Kết nối (Resilience):** Cơ chế **Retry Logic** tích hợp trong `Program.cs` tự động thử lại 10 lần kết nối database khi khởi chạy đồng loạt, giúp hệ thống không bị crash khi database khởi động chậm hơn.
4.  **CORS Control:** API đăng ký dịch vụ CORS giúp giới hạn nguồn truy cập từ các domain dashboard được chỉ định khi triển khai thực tế.

---

## 🔗 Liên kết Hữu ích
*   **Tài liệu Swagger UI:** [http://localhost:5000/swagger](http://localhost:5000/swagger)
*   **Địa chỉ API Base:** [http://localhost:5000/api](http://localhost:5000/api)
*   **Đường dẫn mã nguồn Github:** [https://github.com/duvx/IoT-Sensor-WebAPI-Demo](https://github.com/duvx/IoT-Sensor-WebAPI-Demo)
