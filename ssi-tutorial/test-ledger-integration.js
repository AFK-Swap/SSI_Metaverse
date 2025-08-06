#!/usr/bin/env node

/**
 * Test script for the new BCovrin Ledger Trust Registry integration
 * 
 * This script tests the new ledger-based trust registry implementation
 * to ensure it works correctly with your existing setup.
 */

const API_BASE = 'http://localhost:4002/v2';

async function testTrustRegistryIntegration() {
  console.log('🔬 Testing BCovrin Ledger Trust Registry Integration\n');
  
  try {
    // Test 1: Check trust registry status
    console.log('1. 📊 Checking trust registry status...');
    const statusResponse = await fetch(`${API_BASE}/trust-registry/status`);
    const statusData = await statusResponse.json();
    
    if (statusData.success) {
      console.log('✅ Trust registry status:');
      console.log(`   Implementation: ${statusData.status.implementation}`);
      console.log(`   ACA-Py Admin: ${statusData.status.acapy_admin_url}`);
      console.log(`   ACA-Py Connected: ${statusData.status.acapy_connectivity.connected}`);
      console.log(`   Trusted DIDs: ${statusData.status.trusted_dids_count}`);
      console.log(`   Cache Age: ${Math.round(statusData.status.cache_age_ms / 1000)}s\n`);
    } else {
      console.log('❌ Failed to get trust registry status');
      console.log(`   Error: ${statusData.error}\n`);
    }
    
    // Test 2: List current trusted DIDs
    console.log('2. 📋 Listing current trusted DIDs...');
    const listResponse = await fetch(`${API_BASE}/trusted-dids`);
    const listData = await listResponse.json();
    
    if (listData.success) {
      console.log(`✅ Found ${listData.count} trusted DIDs (source: ${listData.source || 'file'}):`);
      listData.data.forEach((did, index) => {
        console.log(`   ${index + 1}. ${did.name}`);
        console.log(`      DID: ${did.did}`);
        console.log(`      Added: ${new Date(did.addedDate).toLocaleDateString()}`);
        console.log(`      By: ${did.addedBy}\n`);
      });
    } else {
      console.log('❌ Failed to list trusted DIDs');
      console.log(`   Error: ${listData.error}\n`);
    }
    
    // Test 3: Test adding a new trusted DID
    console.log('3. ➕ Testing add trusted DID...');
    const testDID = 'TestDID123456789012345678901234567890';
    const testName = 'Test University (Ledger Integration Test)';
    
    const addResponse = await fetch(`${API_BASE}/trusted-dids`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        did: testDID,
        name: testName
      })
    });
    
    const addData = await addResponse.json();
    if (addData.success) {
      console.log('✅ Successfully added test DID to ledger');
      console.log(`   DID: ${testDID}`);
      console.log(`   Name: ${testName}\n`);
      
      // Test 4: Verify the DID was added
      console.log('4. 🔍 Verifying DID was added...');
      const verifyResponse = await fetch(`${API_BASE}/trusted-dids`);
      const verifyData = await verifyResponse.json();
      
      const addedDID = verifyData.data.find(d => d.did === testDID);
      if (addedDID) {
        console.log('✅ Test DID found in trusted list');
        console.log(`   Verified name: ${addedDID.name}\n`);
        
        // Test 5: Test removing the DID
        console.log('5. 🗑️  Testing remove trusted DID...');
        const removeResponse = await fetch(`${API_BASE}/trusted-dids/${encodeURIComponent(testDID)}`, {
          method: 'DELETE'
        });
        
        const removeData = await removeResponse.json();
        if (removeData.success) {
          console.log('✅ Successfully removed test DID from ledger');
          console.log('🎉 All tests passed! Ledger integration is working correctly.\n');
        } else {
          console.log('⚠️  Failed to remove test DID (this is expected if ledger is read-only)');
          console.log(`   Error: ${removeData.error}\n`);
        }
      } else {
        console.log('⚠️  Test DID not found after adding (cache issue or ledger delay)');
      }
    } else {
      console.log('⚠️  Failed to add test DID (this is expected if ledger is read-only)');
      console.log(`   Error: ${addData.error}\n`);
    }
    
    console.log('🏁 Test completed!');
    console.log('\nTo use the new ledger-based trust registry:');
    console.log('1. Make sure your ACA-Py agent is running with BCovrin ledger');
    console.log('2. Use the admin interface at http://localhost:3000/admin');
    console.log('3. DIDs are now stored on the blockchain instead of JSON file');
    console.log('4. Changes take effect immediately across all instances');
    
  } catch (error) {
    console.error('❌ Test failed with error:', error.message);
    console.error('\nTroubleshooting:');
    console.error('1. Make sure the ssi-tutorial verifier is running on port 4002');
    console.error('2. Ensure ACA-Py agent is running and connected to BCovrin ledger');
    console.error('3. Check that LEDGER_URL=http://dev.greenlight.bcovrin.vonx.io is set');
  }
}

// Run the test
testTrustRegistryIntegration();