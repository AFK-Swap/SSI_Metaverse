#!/bin/bash
# Start the SSI Tutorial system (based on CrypticConsultancy guide)

echo "🚀 Starting SSI Tutorial System..."

# Check if .env exists
if [ ! -f "ssi-tutorial/demo/acapy/.env" ]; then
    echo "❌ Error: .env file not found!"
    echo "Please run: ./scripts/setup-network.sh <your-ip-address>"
    exit 1
fi

# Navigate to SSI tutorial directory
cd ssi-tutorial/demo/acapy

# Start the SSI tutorial system
echo "Starting issuer and verifier agents..."
npm run start:dev &

# Wait for services to initialize
echo "Waiting for services to start..."
sleep 15

# Check if services are running
if curl -s http://localhost:8020/status > /dev/null 2>&1; then
    echo "✅ Issuer agent running on port 8020"
else
    echo "⚠️  Issuer agent may still be starting..."
fi

if curl -s http://localhost:8021/status > /dev/null 2>&1; then
    echo "✅ Admin interface accessible on port 8021"
else
    echo "⚠️  Admin interface may still be starting..."
fi

echo "🌐 Access admin interface at: http://localhost:3000"
echo "📊 ACA-Py admin at: http://localhost:8021"

# Return to project root
cd ../../..