import { NextRequest, NextResponse } from 'next/server';
import { loadCredentials, saveCredentials, addCredential, initializePersistentStorage } from '@/lib/persistent-storage';

declare global {
  var notificationStore: any[] | undefined;
  var unifiedCredentialStore: any[] | undefined;
  var verificationSessions: any[] | undefined;
  // Legacy stores for backward compatibility (will be migrated)
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
    console.log('Migrating credentials to unified store...');
    
    // Migrate from credentialStore
    if (globalThis.credentialStore?.length > 0) {
      for (const cred of globalThis.credentialStore) {
        const existing = globalThis.unifiedCredentialStore.find(c => c.id === cred.id);
        if (!existing) {
          // Normalize legacy credential format
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
            credentialPreview: {
              attributes: Object.keys(cred.credential || {}).map(key => ({
                name: key,
                value: cred.credential[key]
              }))
            },
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
          // AnonCreds are already in good format
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
  // Ensure unified store is initialized
  if (!globalThis.unifiedCredentialStore) {
    return [];
  }
  
  // Extract attribute names from the requested_attributes object
  const requiredAttributeNames = Object.values(requestedAttributes)
    .map((attr: any) => attr.name.toLowerCase());
  
  console.log('Looking for credentials in unified store with attributes:', requiredAttributeNames);
  console.log('Unified store contains', globalThis.unifiedCredentialStore.length, 'credentials');
  
  const matchingCredentials = [];
  
  // Find all credentials that have ALL required attributes
  for (const credential of globalThis.unifiedCredentialStore) {
    // Get credential attributes from normalized structure
    let credentialAttributes = [];
    
    if (credential.attributes && Array.isArray(credential.attributes)) {
      credentialAttributes = credential.attributes.map((attr: any) => attr.name?.toLowerCase()).filter(Boolean);
    } else if (credential.credentialPreview?.attributes) {
      credentialAttributes = credential.credentialPreview.attributes.map((attr: any) => attr.name?.toLowerCase()).filter(Boolean);
    } else if (credential.credentialData && typeof credential.credentialData === 'object') {
      credentialAttributes = Object.keys(credential.credentialData).map(key => key.toLowerCase());
    } else if (credential.credential && typeof credential.credential === 'object') {
      credentialAttributes = Object.keys(credential.credential).map(key => key.toLowerCase());
    }
    
    console.log(`Checking unified credential ${credential.id} (${credential.originalFormat}) with attributes:`, credentialAttributes);
    
    // Check if this credential has all required attributes
    const hasAllAttributes = requiredAttributeNames.every(required =>
      credentialAttributes.includes(required)
    );
    
    if (hasAllAttributes) {
      console.log('Found matching unified credential:', credential.id);
      matchingCredentials.push(credential);
    }
  }
  
  console.log(`Found ${matchingCredentials.length} matching credentials in unified store`);
  return matchingCredentials;
}

function findMatchingCredential(requestedAttributes: any) {
  const matchingCredentials = findMatchingCredentials(requestedAttributes);
  return matchingCredentials.length > 0 ? matchingCredentials[0] : null;
}

// Legacy function removed - now using unified findMatchingCredential function

async function sendProofToAcaPy(connectionId: string, credential: any, playerName: string) {
  try {
    console.log('Validating DID trust via ACA-Py for player:', playerName);
    
    // Extract issuer DID from credential
    const issuerDidAttribute = credential.credentialPreview?.attributes?.find((attr: any) => attr.name === 'issuer_did');
    const issuerDid = issuerDidAttribute?.value;
    
    if (!issuerDid) {
      console.error('No issuer_did found in credential');
      return false;
    }
    
    // Extract just the DID identifier (remove did:sov: prefix for ACA-Py)
    const didIdentifier = issuerDid.replace('did:sov:', '');
    console.log('Checking DID trust for:', didIdentifier);
    
    // Check if DID is trusted via ACA-Py trust registry
    const trustCheckResponse = await fetch('http://localhost:4002/v2/trusted-dids', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    if (!trustCheckResponse.ok) {
      console.error('Failed to check trusted DIDs');
      return false;
    }
    
    const trustData = await trustCheckResponse.json();
    const trustedDids = trustData.data || [];
    const isDIDTrusted = trustedDids.some((trustedDid: any) => trustedDid.did === didIdentifier);
    
    console.log(`DID ${didIdentifier} trusted:`, isDIDTrusted);
    console.log('Available trusted DIDs:', trustedDids.map((d: any) => d.did));
    
    if (isDIDTrusted) {
      console.log(`✅ DID trust validation PASSED for player ${playerName} - DID ${didIdentifier} is trusted`);
      
      // Update verification session with success
      await updateVerificationSession(connectionId, {
        status: 'verified',
        verified: true,
        didTrustValidation: true,
        trustedDid: didIdentifier,
        verificationMethod: 'acapy_trust_registry'
      });
      
      return true;
    } else {
      console.log(`❌ DID trust validation FAILED for player ${playerName} - DID ${didIdentifier} not in trusted list`);
      
      // Update verification session with failure
      await updateVerificationSession(connectionId, {
        status: 'failed',
        verified: false,
        didTrustValidation: false,
        untrustedDid: didIdentifier,
        message: 'DID not in trusted registry'
      });
      
      return false;
    }
    
  } catch (error) {
    console.error('Failed to validate DID trust via ACA-Py:', error);
    
    // Update verification session with error
    await updateVerificationSession(connectionId, {
      status: 'failed',
      verified: false,
      didTrustValidation: false,
      error: error instanceof Error ? error.message : 'Unknown error'
    });
    
    return false;
  }
}

async function sendConnectionlessProofToSSITutorial(proofExchangeId: string, credential: any, playerName: string) {
  try {
    console.log('Sending connectionless proof to ssi-tutorial for player:', playerName);
    
    // Build proof presentation using credential attributes
    const credentialAttributes = credential.credentialPreview?.attributes || [];
    
    // Create the requested_proof structure that ssi-tutorial expects
    const requestedProof = {
      revealed_attrs: {},
      self_attested_attrs: {},
      unrevealed_attrs: {},
      predicates: {}
    };
    
    // Map credential attributes to requested proof format
    credentialAttributes.forEach((attr: any, index: number) => {
      requestedProof.revealed_attrs[`attr_${index}_uuid`] = {
        sub_proof_index: 0,
        raw: attr.value,
        encoded: attr.value // For simplicity, using same value
      };
    });
    
    // Create proof object
    const proofData = {
      proof: {
        proofs: [
          {
            primary_proof: {},
            non_revoc_proof: null
          }
        ],
        aggregated_proof: {
          c_hash: "mock_hash",
          c_list: []
        }
      },
      requested_proof: requestedProof,
      identifiers: [
        {
          schema_id: credential.schemaId || 'BzCbsNYhMrjHiqZDTUASHg:2:employee_card:1.0',
          cred_def_id: credential.credentialDefinitionId || 'BzCbsNYhMrjHiqZDTUASHg:3:CL:456:TAG',
          rev_reg_id: null,
          timestamp: null
        }
      ]
    };
    
    console.log('Sending connectionless proof with data:', proofData);
    
    // Send proof to ssi-tutorial verifier
    const response = await fetch(`http://localhost:4002/v2/present-proof-2.0/records/${proofExchangeId}/send-presentation`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(proofData)
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error('Failed to send connectionless proof to ssi-tutorial:', errorText);
      return false;
    }
    
    const result = await response.json();
    console.log('Connectionless proof sent successfully:', result);
    
    return true;
    
  } catch (error) {
    console.error('Failed to send connectionless proof to ssi-tutorial:', error);
    return false;
  }
}

async function updateVerificationSession(sessionId: string, result: any) {
  try {
    // Find and update the verification session
    if (globalThis.verificationSessions) {
      const sessionIndex = globalThis.verificationSessions.findIndex((session: any) => 
        session.id === sessionId || session.connectionId === sessionId || session.verificationSessionId === sessionId
      );
      
      if (sessionIndex !== -1) {
        globalThis.verificationSessions[sessionIndex] = {
          ...globalThis.verificationSessions[sessionIndex],
          ...result,
          updatedAt: new Date().toISOString()
        };
        console.log('Updated verification session:', globalThis.verificationSessions[sessionIndex]);
      }
    }
  } catch (error) {
    console.error('Failed to update verification session:', error);
  }
}

function checkCredentialAvailability(requestedAttributes: any) {
  if (!globalThis.credentialStore) {
    return {
      hasMatch: false,
      missingAttributes: Object.values(requestedAttributes).map((attr: any) => attr.name),
      availableAttributes: []
    };
  }
  
  const requiredAttributeNames = Object.values(requestedAttributes)
    .map((attr: any) => attr.name.toLowerCase());
  
  // Get all available attributes from all credentials
  const allAvailableAttributes = new Set();
  globalThis.credentialStore.forEach(credential => {
    Object.keys(credential.credential || {}).forEach(key => {
      allAvailableAttributes.add(key.toLowerCase());
    });
  });
  
  const missingAttributes = requiredAttributeNames.filter(required => 
    !allAvailableAttributes.has(required)
  );
  
  return {
    hasMatch: missingAttributes.length === 0,
    missingAttributes: missingAttributes,
    availableAttributes: Array.from(allAvailableAttributes),
    matchingCredentials: missingAttributes.length === 0 ? findMatchingCredentials(requestedAttributes) : []
  };
}

export async function PATCH(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const { action, credentialId } = await request.json(); // 'accept' or 'decline', and optional credentialId
    const notificationId = params.id;
    
    // Initialize stores if they don't exist
    if (typeof globalThis !== 'undefined' && !globalThis.notificationStore) {
      globalThis.notificationStore = [];
    }
    
    // Initialize unified credential store and migrate legacy stores
    await initializeUnifiedStore();
    
    // Find the notification
    const notificationIndex = globalThis.notificationStore?.findIndex(
      (n: any) => n.id === notificationId
    ) ?? -1;
    
    if (notificationIndex === -1) {
      return NextResponse.json(
        { success: false, error: 'Notification not found' },
        { status: 404 }
      );
    }
    
    const notification = globalThis.notificationStore![notificationIndex];
    
    if (action === 'accept') {
      if (notification.type === 'credential-offer') {
        // Move credential offer to AnonCreds store (compatible format)
        const anonCredsCredential = {
          id: `cred_${Date.now()}`,
          state: 'done',
          connectionId: notification.credentialData?.connectionId,
          threadId: notification.credentialData?.threadId || `thread_${Date.now()}`,
          schemaId: notification.credentialData?.schemaId || 'BzCbsNYhMrjHiqZDTUASHg:2:employee_card:1.0',
          credentialDefinitionId: notification.credentialData?.credentialDefinitionId || 'BzCbsNYhMrjHiqZDTUASHg:3:CL:456:TAG',
          credentialPreview: notification.credentialData?.credentialPreview || {
            attributes: Object.entries(notification.credentialData || {}).map(([name, value]) => ({
              name,
              value: String(value)
            }))
          },
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          isRevoked: false
        };
        
        // Store in unified credential store (replaces both legacy stores)
        const unifiedCredential = {
          id: anonCredsCredential.id,
          originalFormat: 'anoncreds',
          timestamp: new Date().toISOString(),
          status: 'stored',
          // AnonCreds structure
          credentialPreview: anonCredsCredential.credentialPreview,
          // Normalized attributes array
          attributes: anonCredsCredential.credentialPreview?.attributes || [],
          // Legacy compatibility data
          credentialData: notification.credentialData?.credentialPreview?.attributes?.reduce((acc: any, attr: any) => {
            acc[attr.name] = attr.value;
            return acc;
          }, {}) || notification.credentialData || {},
          // Additional fields
          connectionId: anonCredsCredential.connectionId,
          state: anonCredsCredential.state,
          createdAt: anonCredsCredential.createdAt,
          updatedAt: anonCredsCredential.updatedAt,
          isRevoked: anonCredsCredential.isRevoked,
          rawMessage: notification.rawMessage
        };
        
        globalThis.unifiedCredentialStore?.push(unifiedCredential);
        console.log('Credential accepted and stored in unified store:', unifiedCredential);
        
      } else if (notification.type === 'proof-request') {
        // Handle proof request - find matching credential and send proof
        const proofRequestData = notification.proofRequestData;
        
        if (proofRequestData?.source === 'minecraft') {
          // This is a Minecraft verification request
          const sessionId = proofRequestData.verificationSessionId;
          
          // Find matching credential from stored credentials
          let matchingCredential;
          
          if (credentialId) {
            // Use specific credential selected by user from unified store
            matchingCredential = globalThis.unifiedCredentialStore?.find((cred: any) => cred.id === credentialId);
            console.log('Using user-selected unified credential:', credentialId);
          } else {
            // Use first matching credential from unified store
            matchingCredential = findMatchingCredential(proofRequestData.requested_attributes);
            console.log('Using first matching unified credential');
          }
          
          if (matchingCredential) {
            // Send proof to Minecraft verification endpoint
            try {
              const response = await fetch(`http://localhost:3001/api/minecraft/verify/${sessionId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                  action: 'share',
                  proof: matchingCredential
                })
              });
              
              if (response.ok) {
                const result = await response.json();
                console.log('Minecraft verification result:', result);
              }
            } catch (error) {
              console.error('Error sending proof to Minecraft verification:', error);
            }
          } else {
            console.log('No matching credential found for verification request');
          }
        } else if (proofRequestData?.source === 'acapy_minecraft') {
          // This is an ACA-Py Minecraft verification request - route through ACA-Py
          const verificationSessionId = proofRequestData.verificationSessionId;
          const playerName = proofRequestData.playerName;
          
          console.log('Processing ACA-Py Minecraft proof request for player:', playerName);
          
          // Find matching credential from AnonCreds store
          let matchingCredential;
          
          if (credentialId) {
            // Use specific credential selected by user from unified store
            matchingCredential = globalThis.unifiedCredentialStore?.find((cred: any) => cred.id === credentialId);
            console.log('Using user-selected unified credential:', credentialId);
          } else {
            // Find first matching credential from unified store  
            matchingCredential = findMatchingCredential(proofRequestData.requested_attributes);
            console.log('Using first matching unified credential');
          }
          
          if (matchingCredential) {
            // Send proof to ACA-Py for DID trust validation
            try {
              await sendProofToAcaPy(verificationSessionId, matchingCredential, playerName);
              console.log('Proof sent to ACA-Py for trust validation');
            } catch (error) {
              console.error('Error sending proof to ACA-Py:', error);
            }
          } else {
            console.log('No matching AnonCreds credential found for ACA-Py verification request');
            
            // Update verification session with missing credential
            await updateVerificationSession(verificationSessionId, {
              status: 'failed',
              verified: false,
              message: 'No matching credential found'
            });
          }
        } else if (proofRequestData?.source === 'web_minecraft') {
          // This is a web Minecraft verification request (Bifold-compatible) - validate with trust registry
          const verificationSessionId = proofRequestData.verificationSessionId;
          const playerName = proofRequestData.minecraftPlayer?.playerName;
          
          console.log('Processing web Minecraft proof request for player:', playerName);
          
          // Find matching credential from AnonCreds store
          let matchingCredential;
          
          if (credentialId) {
            // Use specific credential selected by user from unified store
            matchingCredential = globalThis.unifiedCredentialStore?.find((cred: any) => cred.id === credentialId);
            console.log('Using user-selected unified credential:', credentialId);
          } else {
            // Find first matching credential from unified store  
            matchingCredential = findMatchingCredential(proofRequestData.requested_attributes);
            console.log('Using first matching unified credential');
          }
          
          if (matchingCredential) {
            // Send proof with trust validation (like ACA-Py flow)
            try {
              await sendProofToAcaPy(verificationSessionId, matchingCredential, playerName);
              console.log('Web proof sent with trust validation');
            } catch (error) {
              console.error('Error sending web proof with trust validation:', error);
            }
          } else {
            console.log('No matching AnonCreds credential found for web verification request');
            
            // Update verification session with missing credential
            await updateVerificationSession(verificationSessionId, {
              status: 'failed',
              verified: false,
              message: 'No matching credential found'
            });
          }
        }
      }
      
      // Update notification status
      notification.status = 'accepted';
      notification.acceptedAt = new Date().toISOString();
      
      // Auto-close browser window if opened from Minecraft
      console.log('Notification processed - checking for auto-close');
      
    } else if (action === 'decline') {
      if (notification.type === 'proof-request' && notification.proofRequestData?.source === 'minecraft') {
        // Notify Minecraft that proof was declined
        const sessionId = notification.proofRequestData.verificationSessionId;
        
        try {
          await fetch(`http://localhost:3001/api/minecraft/verify/${sessionId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              action: 'decline'
            })
          });
        } catch (error) {
          console.error('Error notifying Minecraft of declined proof:', error);
        }
      }
      
      // Update notification status
      notification.status = 'declined';
      notification.declinedAt = new Date().toISOString();
    }
    
    return NextResponse.json({
      success: true,
      notification: notification,
      action: action
    });
    
  } catch (error) {
    console.error('Error processing notification action:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to process action' },
      { status: 500 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const notificationId = params.id;
    
    if (typeof globalThis !== 'undefined' && !globalThis.notificationStore) {
      globalThis.notificationStore = [];
    }
    
    // Remove notification from store
    const initialLength = globalThis.notificationStore?.length ?? 0;
    globalThis.notificationStore = globalThis.notificationStore?.filter(
      (n: any) => n.id !== notificationId
    ) ?? [];
    
    const removed = initialLength > (globalThis.notificationStore?.length ?? 0);
    
    return NextResponse.json({
      success: removed,
      message: removed ? 'Notification deleted' : 'Notification not found'
    });
    
  } catch (error) {
    console.error('Error deleting notification:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to delete notification' },
      { status: 500 }
    );
  }
}