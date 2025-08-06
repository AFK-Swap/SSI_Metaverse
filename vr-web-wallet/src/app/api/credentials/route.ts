import { NextRequest, NextResponse } from 'next/server';
import { loadCredentials, saveCredentials, addCredential, removeCredential, initializePersistentStorage } from '@/lib/persistent-storage';

// Unified credential store - supports all credential formats
declare global {
  var unifiedCredentialStore: any[] | undefined;
  // Keep legacy references for backward compatibility (will be removed)
  var credentialStore: any[] | undefined;
  var anonCredsStore: any[] | undefined;
}

// Helper function to detect credential format and normalize
function normalizeCredential(credential: any) {
  const normalized = {
    id: credential.id || `cred-${Date.now()}`,
    originalFormat: 'unknown',
    timestamp: credential.timestamp || new Date().toISOString(),
    status: credential.status || 'stored',
    // Standard fields
    credentialData: null,
    credentialPreview: null,
    attributes: []
  };

  // Detect and normalize different credential formats
  if (credential.credentialData?.credentialPreview?.attributes) {
    // Format: Credential offer with nested structure
    normalized.originalFormat = 'credential-offer';
    normalized.credentialData = credential.credentialData;
    normalized.credentialPreview = credential.credentialData.credentialPreview;
    normalized.attributes = credential.credentialData.credentialPreview.attributes;
  } else if (credential.credentialPreview?.attributes) {
    // Format: Direct credentialPreview (AnonCreds style)
    normalized.originalFormat = 'anoncreds';
    normalized.credentialPreview = credential.credentialPreview;
    normalized.attributes = credential.credentialPreview.attributes;
  } else if (credential.credential && typeof credential.credential === 'object') {
    // Format: Simple key-value object
    normalized.originalFormat = 'simple';
    normalized.credentialData = credential.credential;
    normalized.attributes = Object.keys(credential.credential).map(key => ({
      name: key,
      value: credential.credential[key]
    }));
    // Create credentialPreview for compatibility
    normalized.credentialPreview = {
      attributes: normalized.attributes
    };
  } else if (credential.attributes && Array.isArray(credential.attributes)) {
    // Format: Direct attributes array
    normalized.originalFormat = 'attributes-array';
    normalized.attributes = credential.attributes;
    normalized.credentialPreview = {
      attributes: credential.attributes
    };
  }

  // Copy any additional fields
  Object.keys(credential).forEach(key => {
    if (!normalized.hasOwnProperty(key)) {
      normalized[key] = credential[key];
    }
  });

  return normalized;
}

export async function GET() {
  try {
    // Initialize persistent storage
    await initializePersistentStorage();
    
    // Load credentials from persistent storage
    const credentials = await loadCredentials();
    
    // Also load from in-memory store and migrate if needed
    if (typeof globalThis !== 'undefined') {
      await migrateLegacyCredentials();
      
      // If in-memory store has more recent data, sync it to persistent storage
      if (globalThis.unifiedCredentialStore && globalThis.unifiedCredentialStore.length > credentials.length) {
        await saveCredentials(globalThis.unifiedCredentialStore);
        return NextResponse.json({
          success: true,
          credentials: globalThis.unifiedCredentialStore,
          anonCredsCredentials: globalThis.unifiedCredentialStore.filter(cred => 
            cred.originalFormat === 'anoncreds' || cred.credentialPreview?.attributes) || []
        });
      }
    }
    
    // Sync persistent storage back to in-memory for other APIs that might need it
    if (typeof globalThis !== 'undefined') {
      globalThis.unifiedCredentialStore = credentials;
    }
    
    return NextResponse.json({
      success: true,
      credentials: credentials,
      anonCredsCredentials: credentials.filter(cred => 
        cred.originalFormat === 'anoncreds' || cred.credentialPreview?.attributes) || []
    });
  } catch (error) {
    console.error('Error loading credentials:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to load credentials' },
      { status: 500 }
    );
  }
}

// Migration function to move credentials from legacy stores
async function migrateLegacyCredentials() {
  if (!globalThis.unifiedCredentialStore) {
    globalThis.unifiedCredentialStore = [];
  }

  // Migrate from credentialStore
  if (globalThis.credentialStore?.length > 0) {
    console.log('Migrating credentials from legacy credentialStore...');
    for (const cred of globalThis.credentialStore) {
      const existing = globalThis.unifiedCredentialStore.find(c => c.id === cred.id);
      if (!existing) {
        const normalized = normalizeCredential(cred);
        globalThis.unifiedCredentialStore.push(normalized);
      }
    }
    // Clear legacy store after migration
    globalThis.credentialStore = [];
  }

  // Migrate from anonCredsStore  
  if (globalThis.anonCredsStore?.length > 0) {
    console.log('Migrating credentials from legacy anonCredsStore...');
    for (const cred of globalThis.anonCredsStore) {
      const existing = globalThis.unifiedCredentialStore.find(c => c.id === cred.id);
      if (!existing) {
        const normalized = normalizeCredential(cred);
        globalThis.unifiedCredentialStore.push(normalized);
      }
    }
    // Clear legacy store after migration
    globalThis.anonCredsStore = [];
  }
}

export async function POST(request: NextRequest) {
  try {
    const credential = await request.json();
    
    // Initialize persistent storage
    await initializePersistentStorage();
    
    // Normalize the credential
    const normalizedCredential = normalizeCredential(credential);
    
    // Add to persistent storage
    await addCredential(normalizedCredential);
    
    // Also add to in-memory store for immediate availability
    if (typeof globalThis !== 'undefined') {
      if (!globalThis.unifiedCredentialStore) {
        globalThis.unifiedCredentialStore = [];
      }
      globalThis.unifiedCredentialStore.push(normalizedCredential);
    }
    
    console.log('Stored unified credential to persistent storage:', normalizedCredential);
    
    return NextResponse.json({
      success: true,
      credential: normalizedCredential
    });
    
  } catch (error) {
    console.error('Error storing credential:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to store credential' },
      { status: 500 }
    );
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const credentialId = searchParams.get('id');
    
    if (!credentialId) {
      return NextResponse.json(
        { success: false, error: 'Credential ID is required' },
        { status: 400 }
      );
    }
    
    // Initialize persistent storage
    await initializePersistentStorage();
    
    // Remove from persistent storage
    await removeCredential(credentialId);
    
    // Also remove from in-memory store
    if (typeof globalThis !== 'undefined' && globalThis.unifiedCredentialStore) {
      globalThis.unifiedCredentialStore = globalThis.unifiedCredentialStore.filter(
        (cred: any) => cred.id !== credentialId
      );
    }
    
    console.log('Deleted credential from persistent storage:', credentialId);
    return NextResponse.json({
      success: true,
      message: 'Credential deleted successfully'
    });
    
  } catch (error) {
    console.error('Error deleting credential:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to delete credential' },
      { status: 500 }
    );
  }
}