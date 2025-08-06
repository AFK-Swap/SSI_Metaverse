import { NextRequest, NextResponse } from 'next/server';

declare global {
  var verificationSessions: any[] | undefined;
}

// Handle proof submission and verification
export async function POST(
  request: NextRequest,
  { params }: { params: { sessionId: string } }
) {
  try {
    const { proof, action } = await request.json();
    const sessionId = params.sessionId;
    
    if (!globalThis.verificationSessions) {
      globalThis.verificationSessions = [];
    }
    
    // Find verification session by ID or verificationSessionId
    const sessionIndex = globalThis.verificationSessions.findIndex(
      (session: any) => session.id === sessionId || session.verificationSessionId === sessionId
    );
    
    if (sessionIndex === -1) {
      return NextResponse.json(
        { success: false, error: 'Verification session not found' },
        { status: 404 }
      );
    }
    
    const session = globalThis.verificationSessions[sessionIndex];
    
    if (action === 'share') {
      // User shared proof from web wallet
      const verificationResult = await verifyProofAgainstRequirements(session, proof);
      
      // Update session with result
      session.status = verificationResult.isValid ? 'verified' : 'failed';
      session.proofReceived = proof;
      session.verificationResult = verificationResult;
      session.completedAt = new Date().toISOString();
      
      console.log(`Verification ${verificationResult.isValid ? 'SUCCESS' : 'FAILED'} for ${session.requester.playerName}`);
      
      return NextResponse.json({
        success: true,
        verified: verificationResult.isValid,
        message: verificationResult.message,
        details: verificationResult.details,
        playerName: session.requester.playerName
      });
      
    } else if (action === 'decline') {
      // User declined to share proof
      session.status = 'declined';
      session.completedAt = new Date().toISOString();
      
      return NextResponse.json({
        success: true,
        verified: false,
        message: 'Proof request declined by user',
        playerName: session.requester.playerName
      });
    }
    
    return NextResponse.json(
      { success: false, error: 'Invalid action' },
      { status: 400 }
    );
    
  } catch (error) {
    console.error('Error processing verification:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to process verification' },
      { status: 500 }
    );
  }
}

// Get verification session status
export async function GET(
  request: NextRequest,
  { params }: { params: { sessionId: string } }
) {
  try {
    const sessionId = params.sessionId;
    
    if (!globalThis.verificationSessions) {
      return NextResponse.json(
        { success: false, error: 'No verification sessions found' },
        { status: 404 }
      );
    }
    
    const session = globalThis.verificationSessions.find(
      (s: any) => s.id === sessionId || s.verificationSessionId === sessionId
    );
    
    if (!session) {
      return NextResponse.json(
        { success: false, error: 'Session not found' },
        { status: 404 }
      );
    }
    
    return NextResponse.json({
      success: true,
      session: session
    });
    
  } catch (error) {
    console.error('Error getting verification session:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to get session' },
      { status: 500 }
    );
  }
}

async function verifyProofAgainstRequirements(session: any, proof: any) {
  // Extract required attributes from session
  const requiredAttributes = session.requestedAttributes;
  
  // For demo: proof is the user's credential data
  const providedAttributes = proof.credential || proof;
  
  console.log('Verifying proof:', {
    required: requiredAttributes,
    provided: Object.keys(providedAttributes)
  });
  
  const verificationDetails = {
    requiredAttributes: requiredAttributes,
    providedAttributes: Object.keys(providedAttributes),
    matches: [] as any[],
    missing: [] as string[],
    extra: [] as string[]
  };
  
  let allMatched = true;
  
  // Check each required attribute
  for (const requiredAttr of requiredAttributes) {
    const attrKey = requiredAttr.toLowerCase();
    const found = Object.keys(providedAttributes).find(
      key => key.toLowerCase() === attrKey
    );
    
    if (found) {
      verificationDetails.matches.push({
        required: requiredAttr,
        provided: found,
        value: providedAttributes[found]
      });
    } else {
      verificationDetails.missing.push(requiredAttr);
      allMatched = false;
    }
  }
  
  // Check for extra attributes (informational)
  for (const providedKey of Object.keys(providedAttributes)) {
    const isRequired = requiredAttributes.some(
      (req: string) => req.toLowerCase() === providedKey.toLowerCase()
    );
    if (!isRequired) {
      verificationDetails.extra.push(providedKey);
    }
  }
  
  // Check DID trust validation like SSI tutorial system
  let didValidationPassed = true;
  let didValidationMessage = '';
  
  if (allMatched && verificationDetails.missing.length === 0) {
    // Check if issuer_did is present and trusted
    const issuerDidMatch = verificationDetails.matches.find(m => m.required.toLowerCase() === 'issuer_did');
    if (issuerDidMatch) {
      const issuerDid = issuerDidMatch.value;
      const trustedDid = 'Hfe4a7wUpqV1qEJxdqCTLr'; // Same trusted DID as SSI tutorial
      
      if (issuerDid !== trustedDid) {
        didValidationPassed = false;
        didValidationMessage = `Sorry, the DID is unauthorized: ${issuerDid}`;
      } else {
        didValidationMessage = `Verified by trusted issuer: ${issuerDid}`;
      }
    } else {
      didValidationPassed = false;
      didValidationMessage = 'No issuer DID found in credential';
    }
  }
  
  // Determine final verification result (must pass both attribute check AND DID validation)
  let message: string;
  let isValid = allMatched && verificationDetails.missing.length === 0 && didValidationPassed;
  
  if (!allMatched || verificationDetails.missing.length > 0) {
    message = `❌ Verification FAILED! Missing attributes: ${verificationDetails.missing.join(', ')}`;
  } else if (!didValidationPassed) {
    message = `❌ DID Validation FAILED! ${didValidationMessage}`;
  } else {
    message = `✅ Verification SUCCESS! ${didValidationMessage}`;
  }
  
  return {
    isValid,
    message,
    details: verificationDetails,
    didValidation: {
      passed: didValidationPassed,
      message: didValidationMessage
    },
    timestamp: new Date().toISOString()
  };
}