#!/usr/bin/env node

/**
 * Migration script: JSON Trust Registry → BCovrin Indy Ledger
 * 
 * This script helps migrate your existing JSON-based trusted DIDs
 * to the new blockchain-based trust registry.
 */

const fs = require('fs');
const path = require('path');

const API_BASE = 'http://localhost:4002/v2';
const JSON_FILE = path.join(__dirname, 'demo/acapy/data/trusted-dids.json');

async function migrateToLedger() {
  console.log('🚀 Migrating Trust Registry: JSON → BCovrin Ledger\n');
  
  try {
    // Step 1: Read existing JSON file
    console.log('1. 📖 Reading existing JSON trust registry...');
    if (!fs.existsSync(JSON_FILE)) {
      console.log('❌ JSON file not found:', JSON_FILE);
      console.log('   This might mean you\'re already using the ledger or the file is in a different location.');
      return;
    }
    
    const jsonData = JSON.parse(fs.readFileSync(JSON_FILE, 'utf8'));
    console.log(`✅ Found ${jsonData.length} trusted DIDs in JSON file:\n`);
    
    jsonData.forEach((did, index) => {
      console.log(`   ${index + 1}. ${did.name}`);
      console.log(`      DID: ${did.did}`);
      console.log(`      Added: ${new Date(did.addedDate).toLocaleDateString()}\n`);
    });
    
    // Step 2: Check current ledger status
    console.log('2. 🔍 Checking current ledger status...');
    const statusResponse = await fetch(`${API_BASE}/trust-registry/status`);
    const statusData = await statusResponse.json();
    
    if (!statusData.success) {
      console.log('❌ Cannot connect to ledger trust registry');
      console.log(`   Error: ${statusData.error}`);
      console.log('   Make sure your ssi-tutorial verifier is running on port 4002');
      return;
    }
    
    console.log('✅ Ledger trust registry is accessible');
    console.log(`   Current trusted DIDs on ledger: ${statusData.status.trusted_dids_count}`);
    console.log(`   ACA-Py connected: ${statusData.status.acapy_connectivity.connected}\n`);
    
    // Step 3: Migrate each DID
    console.log('3. 📤 Migrating DIDs to ledger...');
    let migrated = 0;
    let skipped = 0;
    let failed = 0;
    
    for (const didEntry of jsonData) {
      console.log(`   Migrating: ${didEntry.name} (${didEntry.did})...`);
      
      try {
        const response = await fetch(`${API_BASE}/trusted-dids`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            did: didEntry.did,
            name: didEntry.name
          })
        });
        
        const result = await response.json();
        
        if (result.success) {
          console.log(`   ✅ Migrated successfully`);
          migrated++;
        } else if (result.error && result.error.includes('already exists')) {
          console.log(`   ⏭️  Already exists on ledger`);
          skipped++;
        } else {
          console.log(`   ❌ Failed: ${result.error}`);
          failed++;
        }
      } catch (error) {
        console.log(`   ❌ Failed: ${error.message}`);
        failed++;
      }
      
      // Small delay to avoid overwhelming the API
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    
    console.log('\n4. 📊 Migration Summary:');
    console.log(`   ✅ Migrated: ${migrated}`);
    console.log(`   ⏭️  Skipped (already existed): ${skipped}`);
    console.log(`   ❌ Failed: ${failed}`);
    console.log(`   📝 Total processed: ${jsonData.length}\n`);
    
    // Step 4: Verify migration
    console.log('5. 🔍 Verifying migration...');
    const verifyResponse = await fetch(`${API_BASE}/trusted-dids`);
    const verifyData = await verifyResponse.json();
    
    if (verifyData.success) {
      console.log(`✅ Ledger now contains ${verifyData.count} trusted DIDs`);
      console.log(`   Source: ${verifyData.source || 'unknown'}\n`);
      
      // Step 5: Backup and archive JSON file
      if (migrated > 0) {
        console.log('6. 🗄️  Backing up JSON file...');
        const backupFile = JSON_FILE + '.backup.' + Date.now();
        fs.copyFileSync(JSON_FILE, backupFile);
        console.log(`✅ JSON file backed up to: ${backupFile}`);
        
        console.log('\n🎉 Migration completed successfully!');
        console.log('\n📋 Next steps:');
        console.log('1. Your trust registry is now decentralized on the BCovrin ledger');
        console.log('2. Use the admin interface at http://localhost:3000/admin');
        console.log('3. The JSON file has been backed up but is no longer used');
        console.log('4. All Minecraft verification will now use the blockchain trust registry');
        console.log('5. Changes are now persistent across server restarts and distributed');
      } else {
        console.log('\n✅ No migration needed - all DIDs already exist on ledger');
      }
    } else {
      console.log('❌ Failed to verify migration');
      console.log(`   Error: ${verifyData.error}`);
    }
    
  } catch (error) {
    console.error('❌ Migration failed:', error.message);
    console.error('\nTroubleshooting:');
    console.error('1. Ensure ssi-tutorial verifier is running: cd demo/acapy && npm start');
    console.error('2. Check ACA-Py agent is running with BCovrin ledger');
    console.error('3. Verify the JSON file path is correct');
  }
}

// Confirm before running
if (process.argv.includes('--confirm')) {
  migrateToLedger();
} else {
  console.log('🔄 BCovrin Ledger Migration Tool');
  console.log('\nThis will migrate your JSON-based trust registry to the blockchain ledger.');
  console.log('\nBefore running:');
  console.log('1. Make sure your ssi-tutorial verifier is running (npm start in demo/acapy)');
  console.log('2. Ensure ACA-Py is connected to BCovrin ledger');
  console.log('3. Back up your data if desired');
  console.log('\nTo proceed, run: node migrate-to-ledger.js --confirm');
}