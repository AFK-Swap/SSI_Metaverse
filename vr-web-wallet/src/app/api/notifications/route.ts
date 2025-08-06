import { NextRequest, NextResponse } from 'next/server';

// Shared global storage for notifications
declare global {
  var notificationStore: any[] | undefined;
}

// CORS headers
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};

export async function OPTIONS() {
  return new Response(null, { status: 200, headers: corsHeaders });
}

export async function GET() {
  // Initialize global store if it doesn't exist (in-memory only)
  if (typeof globalThis !== 'undefined' && !globalThis.notificationStore) {
    globalThis.notificationStore = [];
  }
  
  // Filter to only show pending notifications
  const pendingNotifications = (globalThis.notificationStore || []).filter(
    (notification: any) => notification.status === 'pending'
  );
  
  return NextResponse.json({
    success: true,
    notifications: pendingNotifications
  }, { headers: corsHeaders });
}

export async function POST(request: NextRequest) {
  try {
    const notification = await request.json();
    
    // Initialize global store if it doesn't exist (in-memory only)
    if (typeof globalThis !== 'undefined' && !globalThis.notificationStore) {
      globalThis.notificationStore = [];
    }
    
    // Add notification to in-memory store only
    const storedNotification = {
      id: notification.id || `notification-${Date.now()}`,
      ...notification,
      timestamp: new Date().toISOString(),
      status: 'pending'
    };
    
    if (typeof globalThis !== 'undefined' && globalThis.notificationStore) {
      globalThis.notificationStore.push(storedNotification);
    }
    
    console.log('Stored notification (in-memory only):', storedNotification);
    
    return NextResponse.json({
      success: true,
      notification: storedNotification
    }, { headers: corsHeaders });
    
  } catch (error) {
    console.error('Error storing notification:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to store notification' },
      { status: 500, headers: corsHeaders }
    );
  }
}