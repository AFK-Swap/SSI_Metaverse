# SSI Verification System Documentation

## Active Plugin Configuration

**Plugin Name:** `SSIVerification` (from plugin.yml)  
**Main Class:** `com.ssi.verification.SimpleSSIPlugin` (actual implementation)  
**Version:** 1.0.0  
**JAR File:** `minecraft-ssi-verification-1.0.0.jar`  
**Location:** `/home/swap/SSI_Metaverse/minecraft-paper-ssi/plugins/`  
**Status:** ✅ **CURRENTLY ACTIVE AND WORKING**

## Command Workflows

### `/verify` (Mobile QR Code Mode) - **WORKING**

**Source:** `SimpleSSIPlugin.java:66-72` → `handleVerify(player)`

**Flow:**
1. **Command Processing:** `SimpleSSIPlugin.onCommand()` detects `/verify` → calls `handleVerify(player)`
2. **ACA-Py Integration:** `createVerification(player)` calls `http://localhost:4002/v2/create-invitation`
3. **QR Code Generation:** `giveQRMap(player, invitationUrl)` uses ZXing library
4. **Actual QR Code:** Creates Minecraft map item with rendered QR code using `MatrixToImageWriter`
5. **Connection Monitoring:** `monitorConnection(connectionId, player)` polls ACA-Py every 3 seconds
6. **Proof Request:** When connection is active, sends proof request via `http://localhost:4002/v2/send-proof-request`
7. **DID Validation:** `validateProofWithDIDCheck()` calls `http://localhost:4002/v2/validate-proof`

**Key Features:**
- ✅ **Generates actual QR codes** (not text links)
- ✅ **Direct ACA-Py integration** (ports 4002/8021)  
- ✅ **DID trust validation**
- ✅ **Map-based QR display in Minecraft**

### `/verify web` (Web Wallet API Mode) - **WORKING**

**Source:** `SimpleSSIPlugin.java:67-68` → `handleWebVerify(player)`

**Flow:**
1. **Command Processing:** `SimpleSSIPlugin.onCommand()` detects `/verify web` → calls `handleWebVerify(player)`
2. **Web Wallet Request:** `createWebVerification(player)` calls `http://localhost:3001/api/minecraft/verify`
3. **Request Format:** Same attributes as mobile version: `["name", "email", "department", "issuer_did", "age"]`
4. **Browser Integration:** Sends notification to web wallet at localhost:3001
5. **Status Monitoring:** `startWebVerificationMonitoring()` polls every 3 seconds
6. **Verification States:** Handles verified/failed/declined responses

**Requested Attributes (Both Modes):**
- `name` - User's full name
- `email` - Email address  
- `department` - Department/organization
- `issuer_did` - DID of credential issuer
- `age` - Age for verification

## Technical Architecture

### Active Implementation: `SimpleSSIPlugin.java`

**Key Components:**
- **ACA-Py Endpoints:** `localhost:4002` (ssi-tutorial verifier), `localhost:8021` (ACA-Py admin)
- **Web Wallet:** `localhost:3001` (VR web wallet)
- **QR Generation:** ZXing library with Minecraft map rendering
- **DID Validation:** Full SSI tutorial integration with trusted issuer checking

**Key Methods:**
- `handleVerify()` - Mobile QR code path
- `handleWebVerify()` - Web wallet path  
- `giveQRMap()` - QR code generation and map creation
- `monitorConnection()` - ACA-Py connection monitoring
- `validateProofWithDIDCheck()` - DID trust verification

### Plugin Classes Available

1. **SimpleSSIPlugin.java** - ✅ **Currently Active**
   - Direct ACA-Py integration with actual QR code generation
   - Creates Minecraft map items with QR codes
   - Complex DID verification workflow
   - Handles both mobile and web verification modes

2. **SSIVerificationPlugin.java** - Available but not active
   - Web API based verification 
   - Simpler HTTP-based communication
   - Shows URL text links instead of QR codes

## Integration Points

### ACA-Py (SSI Tutorial) Endpoints
- `POST /v2/create-invitation` - Creates connection invitation
- `GET /v2/connections?connectionId=X` - Connection status  
- `POST /v2/send-proof-request` - Requests credential proof
- `POST /v2/validate-proof` - Validates proof with DID trust

### Web Wallet Endpoints
- `POST /api/minecraft/verify` - Web verification requests
- `GET /api/minecraft/verify/{id}` - Status polling

## Verification Process Details

### Mobile QR Code Flow (`/verify`)
1. Player runs `/verify` command in Minecraft
2. Plugin creates ACA-Py connection invitation
3. QR code is generated using ZXing and rendered on Minecraft map
4. Player receives map item with QR code
5. Player scans QR with mobile SSI wallet
6. Plugin monitors connection status every 3 seconds
7. When wallet connects, plugin removes QR map and sends proof request
8. Plugin monitors proof status and validates DID trust
9. On success: Player gets glowing effect + verification status

### Web Wallet Flow (`/verify web`)
1. Player runs `/verify web` command in Minecraft
2. Plugin sends verification request to web wallet API
3. Web wallet shows notification in browser at localhost:3001
4. Player clicks "Share Info" in web wallet
5. Plugin monitors verification status every 3 seconds
6. On success: Player gets glowing effect + verification status

## Task Management & Monitoring

**Connection Monitoring:**
- 40 attempts maximum (2 minutes timeout)
- Checks every 3 seconds (60L ticks)
- Properly cancels tasks using `Bukkit.getScheduler().cancelTask(taskId[0])`

**Proof Monitoring:**
- 60 attempts maximum (3 minutes timeout)  
- Checks every 3 seconds (60L ticks)
- Handles states: `presentation-received`, `done`, `abandoned`, `request-rejected`

**Web Verification Monitoring:**
- 100 attempts maximum (5 minutes timeout)
- Checks every 3 seconds (60L ticks)
- Handles states: `verified`, `failed`, `declined`

## Current Status: ✅ FULLY WORKING

Both verification modes are operational:

- **Mobile Mode (`/verify`):** Generates actual QR codes on Minecraft maps for mobile wallet scanning
- **Web Mode (`/verify web`):** Integrates with browser-based web wallet at localhost:3001
- **Dual Architecture:** Supports both ACA-Py direct integration and web wallet API
- **DID Validation:** Proper trust verification for credential issuers
- **Task Management:** Proper cleanup of monitoring tasks to prevent repeated notifications
- **Attribute Consistency:** Both modes request the same 5 attributes

## Configuration Files

**Plugin Configuration:** `/plugins/SSIVerification/config.yml`
- ACA-Py admin URL: `http://localhost:8021`
- Credential definition ID: `AbH2V5oKsrPXbzbKKrpU3f:3:CL:2872881:University-Certificate`
- Required attributes: `department`, `age` (≥18)

**Commands:**
- `/verify` - Mobile QR code verification
- `/verify web` - Web wallet verification  
- `/ssiverify <player>` - Check verification status

**Permissions:**
- `ssi.verify` - Allow verification (default: true)
- `ssi.check` - Check other players (default: op)
- `ssi.admin` - Admin permissions (default: op)