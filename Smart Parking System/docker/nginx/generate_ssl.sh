#!/bin/bash
# =============================================================================
# Smart Parking System - Nginx Self-Signed SSL Certificate Generator
# =============================================================================
# Generates a self-signed SSL certificate for development/testing.
# For production, use Let's Encrypt or a real CA.
#
# Usage: ./generate_ssl.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="${SCRIPT_DIR}/certs"

# Configuration (can be overridden via environment variables)
SSL_DOMAIN="${SSL_DOMAIN:-localhost}"
SSL_DAYS="${SSL_DAYS:-365}"
SSL_COUNTRY="${SSL_COUNTRY:-VN}"
SSL_STATE="${SSL_STATE:-Hanoi}"
SSL_CITY="${SSL_CITY:-Hanoi}"
SSL_ORG="${SSL_ORG:-SmartParking}"
SSL_UNIT="${SSL_UNIT:-Engineering}"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}[INFO]${NC} Generating self-signed SSL certificate for Nginx..."

# Create certs directory
mkdir -p "${CERT_DIR}"

# Check if certificate already exists
if [[ -f "${CERT_DIR}/server.crt" && -f "${CERT_DIR}/server.key" ]]; then
    echo -e "${YELLOW}[WARN]${NC} Certificate already exists at ${CERT_DIR}/"
    read -p "Overwrite? (y/N): " OVERWRITE
    if [[ "${OVERWRITE}" != "y" && "${OVERWRITE}" != "Y" ]]; then
        echo "Skipping certificate generation."
        exit 0
    fi
fi

# Generate private key and self-signed certificate with SAN
openssl req -x509 \
    -nodes \
    -days ${SSL_DAYS} \
    -newkey rsa:2048 \
    -keyout "${CERT_DIR}/server.key" \
    -out "${CERT_DIR}/server.crt" \
    -subj "/C=${SSL_COUNTRY}/ST=${SSL_STATE}/L=${SSL_CITY}/O=${SSL_ORG}/OU=${SSL_UNIT}/CN=${SSL_DOMAIN}" \
    -addext "subjectAltName=DNS:${SSL_DOMAIN},DNS:*.${SSL_DOMAIN},IP:127.0.0.1" \
    -addext "keyUsage=digitalSignature,keyEncipherment" \
    -addext "extendedKeyUsage=serverAuth" \
    2>/dev/null

# Set permissions
chmod 600 "${CERT_DIR}/server.key"
chmod 644 "${CERT_DIR}/server.crt"

echo ""
echo -e "${GREEN}✅ SSL certificate generated successfully!${NC}"
echo ""
echo "Files:"
echo "  Certificate: ${CERT_DIR}/server.crt"
echo "  Private Key: ${CERT_DIR}/server.key"
echo ""
echo "Certificate details:"
openssl x509 -in "${CERT_DIR}/server.crt" -noout -subject -dates -fingerprint
echo ""
echo -e "${YELLOW}⚠️  This is a self-signed certificate for development only.${NC}"
echo "  For production, use Let's Encrypt or a trusted CA."
