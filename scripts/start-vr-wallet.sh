#!/bin/bash
# Start the VR Web Wallet

echo "🚀 Starting VR Web Wallet..."

# Check if vr-web-wallet directory exists
if [ ! -d "vr-web-wallet" ]; then
    echo "❌ Error: vr-web-wallet directory not found!"
    exit 1
fi

# Navigate to VR wallet directory
cd vr-web-wallet

# Check if dependencies are installed
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Start the VR web wallet
echo "Starting VR Web Wallet on port 3001..."
npm run dev &

# Wait for service to initialize
echo "Waiting for VR wallet to start..."
sleep 10

# Check if service is running
if curl -s http://localhost:3001 > /dev/null 2>&1; then
    echo "✅ VR Web Wallet running on port 3001"
else
    echo "⚠️  VR Web Wallet may still be starting..."
fi

echo "🌐 Access VR Web Wallet at: http://localhost:3001"

# Return to project root
cd ..