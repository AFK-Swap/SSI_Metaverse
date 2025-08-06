# 🌟 SSI Metaverse - Self-Sovereign Identity in Minecraft

Transform Minecraft into a decentralized identity verification metaverse using Hyperledger Aries and ACA-Py.

## 🎯 What This Project Does

- **In-Game SSI Verification**: Players verify their real-world credentials inside Minecraft
- **VR Web Wallet Integration**: Phone-shaped browser window appears in-game for credential sharing  
- **Trust Registry**: Validate DIDs against trusted issuer list
- **BCovrin Ledger**: Connect to real Hyperledger Indy network
- **ACA-Py Integration**: Full Aries CloudAgent Python framework

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Minecraft     │    │   VR Web        │    │   SSI Tutorial  │
│   Paper Server │◄───┤   Wallet        │◄───┤   System        │
│   + SSI Plugin  │    │   (:3001)       │    │   (:3000-4002)  │
│   (:25565)      │    └─────────────────┘    └─────────────────┘
└─────────────────┘              │                       │
                                 ▼                       ▼
                    ┌─────────────────────────────────────────┐
                    │        ACA-Py Agents                    │
                    │   Issuer (:8020) + Verifier (:8021)   │
                    │        BCovrin Ledger                  │
                    └─────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites Installation

**1. Install Required Software:**
```bash
# Node.js 18+
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Java 17+ (for Minecraft)
sudo apt install openjdk-17-jdk

# Python 3.8+ and ACA-Py (follow CrypticConsultancy guide)
sudo apt install python3 python3-pip
```

**2. Install ACA-Py Dependencies:**

Follow the comprehensive setup guide from [CrypticConsultancy SSI Tutorial](https://github.com/CrypticConsultancyLimited/ssi-tutorial/blob/credo-acapy/demo/acapy/README.md) for detailed ACA-Py installation and configuration.

**Prerequisites:**
- Docker (>= v24.0.1)
- Node.js (>= v16)
- Python (>= 3.12)
- Yarn (>= v1.22.22)
- Git
- Ngrok

**ACA-Py Agent Setup:**
```bash
# Clone OpenWallet's official ACA-Py repository in a separate directory
git clone -b 0.12.3 https://github.com/openwallet-foundation/acapy.git

# Navigate to demo folder
cd acapy/demo

# Update asyncpg version to avoid compatibility issues
sed -i 's/asyncpg.*/asyncpg==0.28.0/' requirements.txt
```

**Install Dependencies:**
```bash
# Install requirements
python3 -m pip install -r requirements.txt

# If using Python >= 3.12 and Debian Based Systems (like Ubuntu), avoid environment errors:
python3 -m pip install -r requirements.txt --break-system-packages
```

**Start the Agent (with Ngrok for public endpoint):**
```bash
# Start Ngrok for port 8020
ngrok http 8020

# Run demo agent. ACA-Py agent will automatically detect the ngrok url and use it as the public endpoint.
LEDGER_URL=http://dev.greenlight.bcovrin.vonx.io ./run_demo faber

# Or you can set manual endpoint yourself:
LEDGER_URL=http://dev.greenlight.bcovrin.vonx.io AGENT_ENDPOINT=https://{ngrok_url} ./run_demo faber
```

**Ngrok Setup (Required for Public Endpoints):**

Ngrok is required to create public endpoints for ACA-Py agents to communicate over the internet.

```bash
# Install ngrok (Ubuntu/Debian)
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update && sudo apt install ngrok

# Or download directly
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xvzf ngrok-v3-stable-linux-amd64.tgz
sudo cp ngrok /usr/local/bin

# Sign up at https://ngrok.com and get your auth token
ngrok authtoken YOUR_AUTH_TOKEN
```

**Troubleshooting:**
⚠️ If the above fails with a network error, try switching to mobile data or a different Wi-Fi.

### Project Setup

**1. Clone Repository:**
```bash
git clone git@github.com:AFK-Swap/SSI_Metaverse.git
cd SSI_Metaverse
```

**2. Install Dependencies:**
```bash
# SSI Tutorial dependencies
cd ssi-tutorial/demo/acapy
npm install
cd ../../../

# VR Web Wallet dependencies  
cd vr-web-wallet
npm install
cd ..
```

**3. Build Minecraft Plugin:**
```bash
cd minecraft-ssi-plugin
mvn clean compile package
# Plugin JAR will be in target/minecraft-ssi-verification-1.0.0.jar
cd ..
```

## ⚠️ CRITICAL: Network Configuration

**BEFORE STARTING - Configure Your IP Address:**

```bash
# 1. Find your local IP address
./scripts/find-local-ip.sh
# Example output: 192.168.1.100

# 2. Auto-configure all files with your IP
./scripts/setup-network.sh 192.168.1.100
```

**Files that get configured:**
- `ssi-tutorial/demo/acapy/.env` - ACA-Py agent endpoints
- `minecraft-paper-ssi/plugins/SSIVerification/config.yml` - Plugin configuration
- `vr-web-wallet/src/*` - Web wallet service endpoints
- `minecraft-paper-ssi/start-with-ssi.sh` - Startup script

## 🎮 Minecraft Paper Server Setup

### Download and Setup Minecraft Paper Server

**1. Create Server Directory:**
```bash
mkdir minecraft-server
cd minecraft-server
```

**2. Download Paper 1.20.4:**
```bash
# Download Paper server (version 1.20.4 - latest stable)
wget https://api.papermc.io/v2/projects/paper/versions/1.20.4/builds/497/downloads/paper-1.20.4-497.jar -O paper-server.jar

# Accept EULA
echo "eula=true" > eula.txt

# Configure server properties
cat > server.properties << EOF
server-port=25565
gamemode=creative
online-mode=false
enable-command-block=true
op-permission-level=4
max-players=20
motd=§6SSI Metaverse §r- Verify Your Identity!
spawn-protection=0
difficulty=peaceful
pvp=false
EOF
```

**3. Install SSI Plugin:**
```bash
# Create plugins directory
mkdir plugins

# Copy SSI verification plugin (after building it)
cp ../SSI_Metaverse/minecraft-ssi-plugin/target/minecraft-ssi-verification-1.0.0.jar plugins/

# Plugin configuration will be auto-generated on first run
```

**4. Start Minecraft Server:**
```bash
# Start server
java -Xmx2G -Xms1G -jar paper-server.jar nogui
```

### Alternative: Use Provided Minecraft Setup

If you prefer to use the existing minecraft-paper-ssi setup:

```bash
# Copy your Paper server JAR to the provided directory
cp /path/to/your/paper-server.jar minecraft-paper-ssi/

# The plugin and configurations are already set up
cd minecraft-paper-ssi
./start-with-ssi.sh
```

## 🚀 System Startup

### Start All Services:
```bash
# Start complete SSI system
./scripts/start-all.sh
```

### OR Start Services Individually:

```bash
# 1. Start SSI Tutorial System (Issuer + Verifier)
./scripts/start-ssi-tutorial.sh

# 2. Start VR Web Wallet
./scripts/start-vr-wallet.sh

# 3. Start Minecraft Server (if using provided setup)
cd minecraft-paper-ssi
./start-with-ssi.sh
```

## 🎮 How to Use

### In Minecraft:
1. **Connect**: `localhost:25565` (or your server IP)
2. **Verify Identity**: Type `/verify web`
3. **Phone Browser Opens**: In-game credential sharing interface
4. **Share Credentials**: Click "📤 Share Credential" button
5. **Get Verified**: Receive glowing effect + trusted status

### Admin Interface:
- **Access**: http://localhost:3000
- **Monitor**: View all verification activities
- **Manage**: Trust registry and credential definitions

## 📊 Service Ports

| Service | Port | URL | Purpose |
|---------|------|-----|---------|
| Admin Interface | 3000 | http://localhost:3000 | SSI system management |
| VR Web Wallet | 3001 | http://localhost:3001 | Credential wallet interface |
| Issuer API | 4000 | http://localhost:4000 | Credential issuance |
| Verifier API | 4002 | http://localhost:4002 | Verification service |
| ACA-Py Faber | 8020 | http://localhost:8020 | Issuer agent |
| ACA-Py Admin | 8021 | http://localhost:8021 | Agent administration |
| Minecraft Server | 25565 | localhost:25565 | Game server |

## 📋 Health Monitoring

Check if all services are running:
```bash
./scripts/health-check.sh
```

## 🔗 Based On

This project extends the excellent [SSI Tutorial by CrypticConsultancy](https://github.com/CrypticConsultancyLimited/ssi-tutorial/blob/credo-acapy/demo/acapy/README.md)

## 📚 Documentation

- [`SYSTEM_ARCHITECTURE.md`](SYSTEM_ARCHITECTURE.md) - Complete technical architecture
- [`SSI_PROTOCOL_FLOWS.md`](SSI_PROTOCOL_FLOWS.md) - Protocol flow documentation  
- [`FORMAL_PROTOCOL_SPECIFICATION.md`](FORMAL_PROTOCOL_SPECIFICATION.md) - Formal protocol specification

## 🐛 Troubleshooting

### Common Issues:

**1. "Connection refused" errors:**
- Check all services are running: `./scripts/health-check.sh`
- Verify IP addresses are correctly configured: `./scripts/setup-network.sh <your-ip>`
- Ensure all required ports are open

**2. Minecraft plugin not working:**
- Check Paper server version is 1.20.4
- Verify plugin is in `plugins/` directory  
- Check server logs for SSI plugin errors
- Ensure plugin configuration exists in `plugins/SSIVerification/config.yml`

**3. Web wallet not loading:**
- Check VR wallet is running on port 3001
- Verify network configuration is correct
- Check browser console for JavaScript errors

**4. ACA-Py agents not starting:**
- Follow the [CrypticConsultancy setup guide](https://github.com/CrypticConsultancyLimited/ssi-tutorial/blob/credo-acapy/demo/acapy/README.md)
- Check Python and pip installation
- Verify ACA-Py installation: `aca-py --version`

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Hyperledger Aries](https://www.hyperledger.org/use/aries) - SSI framework
- [CrypticConsultancy](https://github.com/CrypticConsultancyLimited/ssi-tutorial) - SSI tutorial foundation
- [PaperMC](https://papermc.io/) - High-performance Minecraft server
- [BCovrin](http://dev.greenlight.bcovrin.vonx.io/) - Indy ledger network