#!/bin/bash
# Health check for all SSI Metaverse services

echo "🏥 SSI Metaverse Health Check"
echo "=============================="

# Function to check service
check_service() {
    local name=$1
    local url=$2
    local port=$3
    
    if curl -s --max-time 5 "$url" > /dev/null 2>&1; then
        echo "✅ $name - Running (Port $port)"
    else
        echo "❌ $name - Not responding (Port $port)"
    fi
}

# Check all services
check_service "Admin Interface" "http://localhost:3000" "3000"
check_service "VR Web Wallet" "http://localhost:3001" "3001" 
check_service "Issuer API" "http://localhost:4000" "4000"
check_service "Verifier API" "http://localhost:4002" "4002"
check_service "ACA-Py Faber Agent" "http://localhost:8020/status" "8020"
check_service "ACA-Py Admin" "http://localhost:8021/status" "8021"

# Check Minecraft server
if netstat -ln 2>/dev/null | grep -q ":25565 "; then
    echo "✅ Minecraft Server - Running (Port 25565)"
elif ss -ln 2>/dev/null | grep -q ":25565 "; then
    echo "✅ Minecraft Server - Running (Port 25565)"
else
    echo "❌ Minecraft Server - Not running (Port 25565)"
fi

echo ""
echo "📊 Process Status:"
echo "=================="

# Show relevant processes
ps aux | grep -E "(node|java|acapy)" | grep -v grep | while read line; do
    echo "🔄 $line"
done

echo ""
echo "🌐 Network Configuration:"
echo "========================"

if [ -f "ssi-tutorial/demo/acapy/.env" ]; then
    ISSUER_ENDPOINT=$(grep "ISSUER_AGENT_PUBLIC_ENDPOINT" ssi-tutorial/demo/acapy/.env | cut -d'=' -f2)
    VERIFIER_ENDPOINT=$(grep "VERIFIER_AGENT_PUBLIC_ENDPOINT" ssi-tutorial/demo/acapy/.env | cut -d'=' -f2)
    echo "📡 Issuer Endpoint: $ISSUER_ENDPOINT"
    echo "📡 Verifier Endpoint: $VERIFIER_ENDPOINT"
else
    echo "⚠️  Network not configured. Run ./scripts/setup-network.sh"
fi