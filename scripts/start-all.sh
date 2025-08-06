#!/bin/bash
# Master startup script for the complete SSI Metaverse system

echo "🌟 Starting SSI Metaverse System..."
echo "======================================"

# Check if network is configured
if [ ! -f "ssi-tutorial/demo/acapy/.env" ]; then
    echo "❌ Network not configured!"
    echo "Please run: ./scripts/setup-network.sh <your-ip-address>"
    echo "Find your IP with: ./scripts/find-local-ip.sh"
    exit 1
fi

# Start SSI tutorial system
echo "1️⃣ Starting SSI Tutorial System..."
./scripts/start-ssi-tutorial.sh

# Start VR web wallet
echo "2️⃣ Starting VR Web Wallet..."
./scripts/start-vr-wallet.sh

# Wait for services to stabilize
echo "⏳ Waiting for services to stabilize..."
sleep 15

# Build Minecraft plugin if needed
if [ ! -f "minecraft-ssi-plugin/target/minecraft-ssi-verification-1.0.0.jar" ]; then
    echo "3️⃣ Building Minecraft SSI Plugin..."
    cd minecraft-ssi-plugin
    mvn clean package -q
    cd ..
fi

# Copy plugin to Minecraft server if directory exists
if [ -d "minecraft-paper-ssi/plugins" ]; then
    echo "📋 Copying plugin to Minecraft server..."
    cp minecraft-ssi-plugin/target/minecraft-ssi-verification-1.0.0.jar minecraft-paper-ssi/plugins/
fi

# Start Minecraft server if available
if [ -f "minecraft-paper-ssi/start-with-ssi.sh" ]; then
    echo "4️⃣ Starting Minecraft Paper Server with SSI..."
    cd minecraft-paper-ssi
    ./start-with-ssi.sh &
    cd ..
else
    echo "⚠️  Minecraft server not found. Please set up Paper server manually."
fi

echo ""
echo "🎉 SSI Metaverse system startup complete!"
echo "======================================"
echo "📊 Services running:"
echo "  • Admin Interface: http://localhost:3000"
echo "  • VR Web Wallet: http://localhost:3001"
echo "  • ACA-Py Admin: http://localhost:8021"
echo "  • Minecraft Server: localhost:25565"
echo ""
echo "🎮 Connect to Minecraft and use '/verify web' to test SSI verification!"
echo ""
echo "📋 Use './scripts/health-check.sh' to monitor services"