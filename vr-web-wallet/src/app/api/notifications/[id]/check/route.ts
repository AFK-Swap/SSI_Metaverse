import { NextRequest, NextResponse } from 'next/server';

declare global {
  var notificationStore: any[] | undefined;
  var unifiedCredentialStore: any[] | undefined;
  // Legacy stores for backward compatibility
  var credentialStore: any[] | undefined;
  var anonCredsStore: any[] | undefined;
}

// Helper function to ensure unified store is initialized and migrated
async function initializeUnifiedStore() {
  if (!globalThis.unifiedCredentialStore) {
    globalThis.unifiedCredentialStore = [];
  }
  
  // Migrate from legacy stores if they exist
  if (globalThis.credentialStore?.length > 0 || globalThis.anonCredsStore?.length > 0) {
    console.log('Migrating credentials to unified store for check...');
    
    // Migrate from credentialStore
    if (globalThis.credentialStore?.length > 0) {
      for (const cred of globalThis.credentialStore) {
        const existing = globalThis.unifiedCredentialStore.find(c => c.id === cred.id);
        if (!existing) {
          const normalized = {
            id: cred.id,
            originalFormat: 'legacy-simple',
            timestamp: cred.timestamp || new Date().toISOString(),
            status: cred.status || 'stored',
            credentialData: cred.credential,
            attributes: Object.keys(cred.credential || {}).map(key => ({
              name: key,
              value: cred.credential[key]
            })),
            ...cred
          };
          globalThis.unifiedCredentialStore.push(normalized);
        }
      }
      globalThis.credentialStore = [];
    }
    
    // Migrate from anonCredsStore
    if (globalThis.anonCredsStore?.length > 0) {
      for (const cred of globalThis.anonCredsStore) {
        const existing = globalThis.unifiedCredentialStore.find(c => c.id === cred.id);
        if (!existing) {
          const normalized = {
            ...cred,
            originalFormat: 'anoncreds',
            attributes: cred.credentialPreview?.attributes || []
          };
          globalThis.unifiedCredentialStore.push(normalized);
        }
      }
      globalThis.anonCredsStore = [];
    }
  }
}

function findMatchingCredentials(requestedAttributes: any) {
  if (!globalThis.unifiedCredentialStore) return [];
  
  const requiredAttributeNames = Object.values(requestedAttributes)
    .map((attr: any) => attr.name.toLowerCase());
  
  const matchingCredentials = [];
  
  for (const credential of globalThis.unifiedCredentialStore) {
    // Get credential attributes from normalized structure
    let credentialAttributes = [];
    
    if (credential.attributes && Array.isArray(credential.attributes)) {
      credentialAttributes = credential.attributes.map((attr: any) => attr.name?.toLowerCase()).filter(Boolean);
    } else if (credential.credentialPreview?.attributes) {
      credentialAttributes = credential.credentialPreview.attributes.map((attr: any) => attr.name?.toLowerCase()).filter(Boolean);
    } else if (credential.credentialData && typeof credential.credentialData === 'object') {
      credentialAttributes = Object.keys(credential.credentialData).map(key => key.toLowerCase());
    }
    
    const hasAllAttributes = requiredAttributeNames.every(required =>
      credentialAttributes.includes(required)
    );
    
    if (hasAllAttributes) {
      matchingCredentials.push(credential);
    }
  }
  
  return matchingCredentials;
}

function checkCredentialAvailability(requestedAttributes: any) {
  if (!globalThis.unifiedCredentialStore) {
    return {
      hasMatch: false,
      missingAttributes: Object.values(requestedAttributes).map((attr: any) => attr.name),
      availableAttributes: [],
      matchingCredentials: []
    };
  }
  
  const requiredAttributeNames = Object.values(requestedAttributes)
    .map((attr: any) => attr.name.toLowerCase());
  
  // Get all available attributes from all credentials
  const allAvailableAttributes = new Set();
  globalThis.unifiedCredentialStore.forEach(credential => {
    if (credential.attributes && Array.isArray(credential.attributes)) {
      credential.attributes.forEach((attr: any) => {
        if (attr.name) allAvailableAttributes.add(attr.name.toLowerCase());
      });
    } else if (credential.credentialPreview?.attributes) {
      credential.credentialPreview.attributes.forEach((attr: any) => {
        if (attr.name) allAvailableAttributes.add(attr.name.toLowerCase());
      });
    } else if (credential.credentialData && typeof credential.credentialData === 'object') {
      Object.keys(credential.credentialData).forEach(key => {
        allAvailableAttributes.add(key.toLowerCase());
      });
    }
  });
  
  const missingAttributes = requiredAttributeNames.filter(required => 
    !allAvailableAttributes.has(required)
  );
  
  const matchingCredentials = missingAttributes.length === 0 ? findMatchingCredentials(requestedAttributes) : [];
  
  return {
    hasMatch: missingAttributes.length === 0,
    missingAttributes: missingAttributes,
    availableAttributes: Array.from(allAvailableAttributes),
    matchingCredentials: matchingCredentials
  };
}

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const notificationId = params.id;
    
    // Initialize unified store and migrate legacy credentials
    await initializeUnifiedStore();
    
    if (!globalThis.notificationStore) {
      return NextResponse.json(
        { success: false, error: 'No notifications found' },
        { status: 404 }
      );
    }
    
    // Find the notification
    const notification = globalThis.notificationStore.find(
      (n: any) => n.id === notificationId
    );
    
    if (!notification || notification.type !== 'proof-request') {
      return NextResponse.json(
        { success: false, error: 'Proof request notification not found' },
        { status: 404 }
      );
    }
    
    const proofRequestData = notification.proofRequestData;
    const availability = checkCredentialAvailability(proofRequestData.requested_attributes);
    
    return NextResponse.json({
      success: true,
      ...availability,
      requestedAttributes: Object.values(proofRequestData.requested_attributes).map((attr: any) => attr.name)
    });
    
  } catch (error) {
    console.error('Error checking credential availability:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to check availability' },
      { status: 500 }
    );
  }
}