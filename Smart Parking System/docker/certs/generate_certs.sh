#!/bin/bash
# =============================================================================
# Smart Parking System - Certificate Generation Script
# =============================================================================
# Generates a complete PKI hierarchy for mTLS:
#   1. Root CA (self-signed)
#   2. Mosquitto Broker certificate (signed by CA)
#   3. Client certificates for ESP32 devices (signed by CA)
#   4. Client certificates for Raspberry Pi gates (signed by CA)
#   5. Backend client certificate (signed by CA)
#
# Usage: ./generate_certs.sh [--clean]
#   --clean : Remove existing certs before generating
# =============================================================================

set -euo pipefail

# --- Configuration ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="${SCRIPT_DIR}"
CA_DIR="${CERT_DIR}/ca"
BROKER_DIR="${CERT_DIR}/broker"
CLIENTS_DIR="${CERT_DIR}/clients"

# Certificate parameters
CA_DAYS=3650          # 10 years
BROKER_DAYS=825       # ~2.25 years
CLIENT_DAYS=825       # ~2.25 years
RSA_BITS=4096
CLIENT_RSA_BITS=2048  # Smaller for constrained IoT devices

# CA Subject
CA_SUBJECT="/C=VN/ST=Hanoi/L=Hanoi/O=SmartParking/OU=IoT Security/CN=SmartParking-CA"

# Broker Subject (CN must match the hostname used in MQTT connection)
BROKER_CN="mosquitto"
BROKER_SUBJECT="/C=VN/ST=Hanoi/L=Hanoi/O=SmartParking/OU=Infrastructure/CN=${BROKER_CN}"

# --- Colors for output ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# --- Handle --clean flag ---
if [[ "${1:-}" == "--clean" ]]; then
    warn "Cleaning existing certificates..."
    rm -rf "${CA_DIR}" "${BROKER_DIR}" "${CLIENTS_DIR}"
    info "Cleaned. Regenerating..."
fi

# --- Check for openssl ---
command -v openssl >/dev/null 2>&1 || error "openssl is required but not installed."

# --- Create directories ---
info "Creating certificate directories..."
mkdir -p "${CA_DIR}"
mkdir -p "${BROKER_DIR}"
mkdir -p "${CLIENTS_DIR}/backend"

# =============================================================================
# 1. Generate Root CA
# =============================================================================
echo ""
echo "============================================="
echo "  Step 1: Root Certificate Authority (CA)"
echo "============================================="

if [[ -f "${CA_DIR}/ca.crt" && -f "${CA_DIR}/ca.key" ]]; then
    warn "CA certificate already exists. Skipping CA generation."
    warn "Use --clean flag to regenerate all certificates."
else
    info "Generating CA private key (RSA ${RSA_BITS})..."
    openssl genrsa -out "${CA_DIR}/ca.key" ${RSA_BITS} 2>/dev/null

    info "Generating CA self-signed certificate (valid ${CA_DAYS} days)..."
    openssl req -new -x509 \
        -key "${CA_DIR}/ca.key" \
        -out "${CA_DIR}/ca.crt" \
        -days ${CA_DAYS} \
        -subj "${CA_SUBJECT}" \
        -sha256

    # Restrict CA key permissions
    chmod 600 "${CA_DIR}/ca.key"
    chmod 644 "${CA_DIR}/ca.crt"

    ok "CA certificate generated: ${CA_DIR}/ca.crt"
fi

# =============================================================================
# 2. Generate Mosquitto Broker Certificate
# =============================================================================
echo ""
echo "============================================="
echo "  Step 2: Mosquitto Broker Certificate"
echo "============================================="

if [[ -f "${BROKER_DIR}/broker.crt" && -f "${BROKER_DIR}/broker.key" ]]; then
    warn "Broker certificate already exists. Skipping."
else
    info "Generating broker private key..."
    openssl genrsa -out "${BROKER_DIR}/broker.key" ${RSA_BITS} 2>/dev/null

    info "Creating broker CSR..."
    openssl req -new \
        -key "${BROKER_DIR}/broker.key" \
        -out "${BROKER_DIR}/broker.csr" \
        -subj "${BROKER_SUBJECT}"

    # Create SAN (Subject Alternative Names) extension for the broker
    cat > "${BROKER_DIR}/broker_ext.cnf" <<EOF
[v3_req]
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = mosquitto
DNS.2 = localhost
DNS.3 = smartparking-mosquitto
IP.1 = 127.0.0.1
EOF

    info "Signing broker certificate with CA..."
    openssl x509 -req \
        -in "${BROKER_DIR}/broker.csr" \
        -CA "${CA_DIR}/ca.crt" \
        -CAkey "${CA_DIR}/ca.key" \
        -CAcreateserial \
        -out "${BROKER_DIR}/broker.crt" \
        -days ${BROKER_DAYS} \
        -sha256 \
        -extensions v3_req \
        -extfile "${BROKER_DIR}/broker_ext.cnf"

    # Cleanup CSR and temp files
    rm -f "${BROKER_DIR}/broker.csr" "${BROKER_DIR}/broker_ext.cnf"

    chmod 600 "${BROKER_DIR}/broker.key"
    chmod 644 "${BROKER_DIR}/broker.crt"

    ok "Broker certificate generated: ${BROKER_DIR}/broker.crt"
fi

# =============================================================================
# 3. Function to Generate Client Certificates
# =============================================================================
generate_client_cert() {
    local client_name="$1"
    local client_dir="${CLIENTS_DIR}/${client_name}"

    if [[ -f "${client_dir}/${client_name}.crt" && -f "${client_dir}/${client_name}.key" ]]; then
        warn "  Client '${client_name}' already exists. Skipping."
        return
    fi

    mkdir -p "${client_dir}"

    local client_subject="/C=VN/ST=Hanoi/L=Hanoi/O=SmartParking/OU=IoT Devices/CN=${client_name}"

    # Generate client key (smaller for IoT devices)
    openssl genrsa -out "${client_dir}/${client_name}.key" ${CLIENT_RSA_BITS} 2>/dev/null

    # Create CSR
    openssl req -new \
        -key "${client_dir}/${client_name}.key" \
        -out "${client_dir}/${client_name}.csr" \
        -subj "${client_subject}"

    # Create client extension
    cat > "${client_dir}/client_ext.cnf" <<EOF
[v3_req]
basicConstraints = CA:FALSE
keyUsage = digitalSignature
extendedKeyUsage = clientAuth
EOF

    # Sign with CA
    openssl x509 -req \
        -in "${client_dir}/${client_name}.csr" \
        -CA "${CA_DIR}/ca.crt" \
        -CAkey "${CA_DIR}/ca.key" \
        -CAcreateserial \
        -out "${client_dir}/${client_name}.crt" \
        -days ${CLIENT_DAYS} \
        -sha256 \
        -extensions v3_req \
        -extfile "${client_dir}/client_ext.cnf"

    # Cleanup temp files
    rm -f "${client_dir}/${client_name}.csr" "${client_dir}/client_ext.cnf"

    # Copy CA cert into client directory for convenience
    cp "${CA_DIR}/ca.crt" "${client_dir}/ca.crt"

    chmod 600 "${client_dir}/${client_name}.key"
    chmod 644 "${client_dir}/${client_name}.crt"

    ok "  Generated: ${client_name}"
}

# =============================================================================
# 4. Generate ESP32 Client Certificates
# =============================================================================
echo ""
echo "============================================="
echo "  Step 3: ESP32 Client Certificates"
echo "============================================="

# Zone A: Slots A01-A10
info "Generating Zone A certificates (a01-a10)..."
for i in $(seq -w 1 10); do
    generate_client_cert "esp32_slot_a${i}"
done

# Zone B: Slots B01-B10
info "Generating Zone B certificates (b01-b10)..."
for i in $(seq -w 1 10); do
    generate_client_cert "esp32_slot_b${i}"
done

# =============================================================================
# 5. Generate Raspberry Pi Client Certificates
# =============================================================================
echo ""
echo "============================================="
echo "  Step 4: Raspberry Pi Client Certificates"
echo "============================================="

info "Generating Raspberry Pi gate certificates..."
generate_client_cert "rpi_gate1"
generate_client_cert "rpi_gate2"

# =============================================================================
# 6. Generate Backend Client Certificate
# =============================================================================
echo ""
echo "============================================="
echo "  Step 5: Backend Client Certificate"
echo "============================================="

info "Generating backend service certificate..."
generate_client_cert "backend"

# =============================================================================
# 7. Summary
# =============================================================================
echo ""
echo "============================================="
echo "  Certificate Generation Summary"
echo "============================================="
echo ""

# Count generated certificates
TOTAL_CERTS=$(find "${CERT_DIR}" -name "*.crt" -not -name "ca.crt" -o -name "ca.crt" -path "*/ca/*" | wc -l)
CLIENT_CERTS=$(find "${CLIENTS_DIR}" -maxdepth 2 -name "*.crt" -not -name "ca.crt" | wc -l)

echo -e "${GREEN}Certificate Directory:${NC} ${CERT_DIR}"
echo ""
echo -e "${GREEN}Generated Certificates:${NC}"
echo "  ├── CA:          ${CA_DIR}/ca.crt"
echo "  ├── Broker:      ${BROKER_DIR}/broker.crt"
echo "  ├── ESP32 Zone A: 10 client certificates"
echo "  ├── ESP32 Zone B: 10 client certificates"
echo "  ├── RPi Gates:   2 client certificates"
echo "  └── Backend:     1 client certificate"
echo ""
echo -e "${GREEN}Total:${NC} 1 CA + 1 Broker + ${CLIENT_CERTS} Client = ${TOTAL_CERTS} certificates"
echo ""

# Verify the CA cert
echo -e "${BLUE}CA Certificate Details:${NC}"
openssl x509 -in "${CA_DIR}/ca.crt" -noout -subject -dates 2>/dev/null
echo ""

# Verify broker cert chain
echo -e "${BLUE}Broker Certificate Verification:${NC}"
openssl verify -CAfile "${CA_DIR}/ca.crt" "${BROKER_DIR}/broker.crt" 2>/dev/null
echo ""

# Verify one client cert as sample
echo -e "${BLUE}Sample Client Verification (esp32_slot_a01):${NC}"
openssl verify -CAfile "${CA_DIR}/ca.crt" "${CLIENTS_DIR}/esp32_slot_a01/esp32_slot_a01.crt" 2>/dev/null
echo ""

echo -e "${GREEN}✅ All certificates generated successfully!${NC}"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANT:${NC}"
echo "  1. Keep ca.key SECURE - it can sign new certificates"
echo "  2. Never commit private keys (.key files) to version control"
echo "  3. Copy client cert bundles to respective IoT devices"
echo "  4. Certificates expire in ${CLIENT_DAYS} days - plan for renewal"
echo ""
echo "Directory structure:"
echo "  ${CERT_DIR}/"
echo "  ├── ca/"
echo "  │   ├── ca.key          (KEEP SECRET)"
echo "  │   └── ca.crt"
echo "  ├── broker/"
echo "  │   ├── broker.key      (KEEP SECRET)"
echo "  │   └── broker.crt"
echo "  └── clients/"
echo "      ├── esp32_slot_a01/"
echo "      │   ├── esp32_slot_a01.key"
echo "      │   ├── esp32_slot_a01.crt"
echo "      │   └── ca.crt"
echo "      ├── ... (20 ESP32 + 2 RPi + 1 Backend)"
echo "      └── backend/"
echo "          ├── backend.key"
echo "          ├── backend.crt"
echo "          └── ca.crt"
