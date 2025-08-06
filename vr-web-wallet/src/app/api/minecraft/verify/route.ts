import { NextRequest, NextResponse } from 'next/server';

// Global storage for verification sessions
declare global {
  var verificationSessions: any[] | undefined;
}

interface VerificationRequest {
  type: string;
  requester: {
    playerUUID: string;
    playerName: string;
  };
  requestedAttributes: string[];
  timestamp: string;
}

export async function POST(request: NextRequest) {
  try {
    const verificationRequest: VerificationRequest = await request.json();
    
    console.log('Received verification request from Minecraft:', verificationRequest);
    
    // Initialize verification sessions store
    if (!globalThis.verificationSessions) {
      globalThis.verificationSessions = [];
    }
    
    // Determine verification method and source
    const isAcaPyVerification = verificationRequest.type === 'acapy_web_verification';
    const isWebProofRequest = verificationRequest.type === 'web_proof_request';
    const trustValidation = verificationRequest.trustValidation || (isAcaPyVerification || isWebProofRequest ? 'acapy' : 'none');
    
    // Create verification session
    const verificationSession = {
      id: `verification-${Date.now()}`,
      ...verificationRequest,
      status: 'pending',
      createdAt: new Date().toISOString(),
      proofReceived: null,
      verificationResult: null,
      acaPyTrustValidation: isAcaPyVerification || isWebProofRequest
    };
    
    globalThis.verificationSessions.push(verificationSession);
    
    // Create proof request for the web wallet
    let proofRequest;
    
    if (isWebProofRequest && verificationRequest.proofRequestData) {
      // Use the proof request data sent directly from the plugin (Bifold-compatible)
      proofRequest = verificationRequest.proofRequestData;
    } else {
      // Build requested attributes object (legacy format)
      proofRequest = {
        name: isAcaPyVerification ? 'Minecraft ACA-Py Trust Verification' : `${verificationRequest.type} Verification Request`,
        version: '1.0',
        requested_attributes: {}
      };
      
      verificationRequest.requestedAttributes.forEach((attr, index) => {
        proofRequest.requested_attributes[`attr_${index}`] = {
          name: attr.toLowerCase().trim()
        };
      });
    }
    
    // Create notification for the web wallet
    const notification = {
      id: `notification-${Date.now()}`,
      type: 'proof-request',
      title: isWebProofRequest 
        ? `${verificationRequest.title || 'Minecraft Web Verification'}` 
        : isAcaPyVerification 
          ? `Minecraft Verification with ACA-Py Trust` 
          : `Minecraft Verification: ${verificationRequest.type}`,
      message: isWebProofRequest
        ? `${verificationRequest.message || verificationRequest.requester.playerName + ' requests proof via web wallet (Bifold-compatible)'}`
        : isAcaPyVerification
          ? `${verificationRequest.requester.playerName} requests proof with DID trust validation via ACA-Py`
          : `${verificationRequest.requester.playerName} requests proof of: ${verificationRequest.requestedAttributes.join(', ')}`,
      proofRequestData: {
        ...proofRequest,
        minecraftPlayer: verificationRequest.requester,
        verificationSessionId: isWebProofRequest ? verificationRequest.verificationSessionId : verificationSession.id,
        source: isWebProofRequest ? 'web_minecraft' : isAcaPyVerification ? 'acapy_minecraft' : 'minecraft',
        trustValidation: trustValidation,
        acapyVerifierUrl: verificationRequest.acapyVerifierUrl
      },
      timestamp: new Date().toISOString(),
      status: 'pending'
    };
    
    // Store notification
    if (!globalThis.notificationStore) {
      globalThis.notificationStore = [];
    }
    
    globalThis.notificationStore.push(notification);
    
    console.log(`Created ${isAcaPyVerification ? 'ACA-Py trust ' : ''}verification request for ${verificationRequest.requester.playerName}:`, notification);
    
    return NextResponse.json({
      success: true,
      message: isAcaPyVerification 
        ? 'Verification request created with ACA-Py trust validation'
        : 'Verification request created',
      verificationId: verificationSession.id,
      proofRequest: proofRequest,
      playerName: verificationRequest.requester.playerName,
      trustValidation: trustValidation
    });
    
  } catch (error) {
    console.error('Error processing Minecraft verification request:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to process verification request' },
      { status: 500 }
    );
  }
}

export async function GET() {
  // Return current verification sessions
  if (!globalThis.verificationSessions) {
    globalThis.verificationSessions = [];
  }
  
  return NextResponse.json({
    success: true,
    sessions: globalThis.verificationSessions
  });
}