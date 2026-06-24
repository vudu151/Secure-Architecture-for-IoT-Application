#!/bin/bash
# ================================================
# Script khởi động dự án WebAPIDemo với SQL Server
# ================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"
WEBAPI_DIR="$PROJECT_DIR/WebAPIDemo"

echo "================================================"
echo "  WebAPIDemo - IoT Sensor Web API"
echo "  Database: SQL Server 2022 (Docker)"
echo "================================================"

# ---- Hàm chạy docker (thử không có sudo, nếu lỗi thì dùng sudo) ----
run_docker() {
    if docker "$@" 2>/dev/null; then
        return 0
    else
        echo "  (dùng sudo docker...)"
        sudo docker "$@"
    fi
}

run_docker_compose() {
    if docker compose "$@" 2>/dev/null; then
        return 0
    else
        echo "  (dùng sudo docker compose...)"
        sudo docker compose "$@"
    fi
}

# Bước 1: Khởi động SQL Server
echo ""
echo "[1/3] Khởi động SQL Server qua Docker..."
run_docker_compose -f "$COMPOSE_FILE" up -d

# Kiểm tra container đã chạy chưa
echo "      Chờ SQL Server sẵn sàng (tối đa 60 giây)..."
for i in $(seq 1 12); do
    STATUS=$(run_docker inspect --format='{{.State.Health.Status}}' sqlserver_webapidemo 2>/dev/null || echo "starting")
    if [ "$STATUS" = "healthy" ]; then
        echo "      ✅ SQL Server đã sẵn sàng!"
        break
    fi
    echo "      ... đang chờ ($((i*5))s) - status: $STATUS"
    sleep 5
done

# Bước 2: Restore packages
echo ""
echo "[2/3] Restore NuGet packages..."
cd "$WEBAPI_DIR"
dotnet restore

# Bước 3: Chạy project
echo ""
echo "[3/3] Khởi động Web API..."
echo ""
echo "  📌 Swagger UI: http://localhost:5000/swagger"
echo "  📌 API Base:   http://localhost:5000/api"
echo ""
echo "  Các API:"
echo "    GET/POST/PUT/DELETE  /api/SensorDatas"
echo "    GET/POST/DELETE      /api/AppVersion"
echo ""
echo "  Để dừng API: Ctrl+C"
echo "  Để dừng SQL Server: sudo docker compose -f docker-compose.yml down"
echo ""

dotnet run --project "$WEBAPI_DIR/WebAPIDemo.csproj" --urls "http://0.0.0.0:5000"
