#!/bin/bash
# Configure network IP addresses throughout the project

IP=$1
if [ -z "$IP" ]; then
    echo "Usage: $0 <your-ip-address>"
    echo "Example: $0 192.168.1.100"
    echo ""
    echo "You can find your IP with: ./scripts/find-local-ip.sh"
    exit 1
fi

echo "Configuring network with IP: $IP"

# Create .env from template if it doesn't exist
if [ ! -f "ssi-tutorial/demo/acapy/.env" ]; then
    echo "Creating .env from template..."
    cp ssi-tutorial/demo/acapy/.env.template ssi-tutorial/demo/acapy/.env
fi

# Update SSI tutorial config
echo "Updating SSI tutorial configuration..."
sed -i "s/YOUR-IP-ADDRESS/$IP/g" ssi-tutorial/demo/acapy/.env

# Create Minecraft config from template if it doesn't exist
if [ ! -f "minecraft-paper-ssi/plugins/SSIVerification/config.yml" ]; then
    echo "Creating Minecraft plugin config from template..."
    mkdir -p minecraft-paper-ssi/plugins/SSIVerification/
    cp minecraft-paper-ssi/plugins/SSIVerification/config.yml.template minecraft-paper-ssi/plugins/SSIVerification/config.yml
fi

# Update Minecraft plugin config  
echo "Updating Minecraft plugin configuration..."
sed -i "s/YOUR-IP-ADDRESS/$IP/g" minecraft-paper-ssi/plugins/SSIVerification/config.yml

# Update VR wallet configuration (multiple TypeScript files)
echo "Updating VR web wallet configuration..."
if [ -d "vr-web-wallet/src" ]; then
    find vr-web-wallet/src -name "*.ts" -o -name "*.js" -o -name "*.tsx" | xargs sed -i "s/localhost/$IP/g" 2>/dev/null || true
fi

# Update start scripts
echo "Updating startup scripts..."
if [ -f "minecraft-paper-ssi/start-with-ssi.sh" ]; then
    sed -i "s/localhost/$IP/g" minecraft-paper-ssi/start-with-ssi.sh
fi

echo "✅ Network configuration complete!"
echo ""
echo "Updated files:"
echo "  - ssi-tutorial/demo/acapy/.env"
echo "  - minecraft-paper-ssi/plugins/SSIVerification/config.yml"
echo "  - vr-web-wallet/src/* (TypeScript files)"
echo "  - minecraft-paper-ssi/start-with-ssi.sh"
echo ""
echo "Next steps:"
echo "  1. Install dependencies: npm install (in each Node.js directory)"
echo "  2. Build Minecraft plugin: cd minecraft-ssi-plugin && mvn clean package"
echo "  3. Start services: ./scripts/start-all.sh"