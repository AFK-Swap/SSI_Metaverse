import { NextRequest, NextResponse } from 'next/server';

// Shared global storage for proof requests
declare global {
  var proofRequestStore: any[] | undefined;
}

export async function GET() {
  // Initialize global store if it doesn't exist
  if (typeof globalThis !== 'undefined' && !globalThis.proofRequestStore) {
    globalThis.proofRequestStore = [];
  }
  
  return NextResponse.json({
    success: true,
    proofRequests: globalThis.proofRequestStore || []
  });
}

export async function POST(request: NextRequest) {
  try {
    const proofRequestData = await request.json();
    
    // Initialize global store if it doesn't exist
    if (typeof globalThis !== 'undefined' && !globalThis.proofRequestStore) {
      globalThis.proofRequestStore = [];
    }
    
    // Create proof request
    const proofRequest = {
      id: `proof-request-${Date.now()}`,
      ...proofRequestData,
      timestamp: new Date().toISOString(),
      status: 'created'
    };
    
    if (typeof globalThis !== 'undefined' && globalThis.proofRequestStore) {
      globalThis.proofRequestStore.push(proofRequest);
    }
    
    console.log('Created proof request:', proofRequest);
    
    // Also create a notification for demonstration
    // In real implementation, this would be sent to the target holder
    const notification = {
      id: `notification-${Date.now()}`,
      type: 'proof-request',
      title: `Proof Request: ${proofRequest.name}`,
      message: `You have received a proof request for: ${Object.values(proofRequest.requested_attributes).map((attr: any) => attr.name).join(', ')}`,
      proofRequestData: proofRequest,
      timestamp: new Date().toISOString(),
      status: 'pending'
    };
    
    // Store notification
    if (typeof globalThis !== 'undefined' && !globalThis.notificationStore) {
      globalThis.notificationStore = [];
    }
    
    if (typeof globalThis !== 'undefined' && globalThis.notificationStore) {
      globalThis.notificationStore.push(notification);
      console.log('Created proof request notification:', notification);
    }
    
    return NextResponse.json({
      success: true,
      proofRequest: proofRequest,
      notification: notification
    });
    
  } catch (error) {
    console.error('Error creating proof request:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to create proof request' },
      { status: 500 }
    );
  }
}