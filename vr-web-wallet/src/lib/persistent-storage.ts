import { promises as fs } from 'fs';
import path from 'path';

const STORAGE_DIR = path.join(process.cwd(), 'storage');
const CREDENTIALS_FILE = path.join(STORAGE_DIR, 'credentials.json');

// Ensure storage directory exists
async function ensureStorageDir() {
  try {
    await fs.mkdir(STORAGE_DIR, { recursive: true });
  } catch (error) {
    console.error('Error creating storage directory:', error);
  }
}

// Generic file operations
async function readJsonFile(filePath: string, defaultValue: any = []) {
  try {
    await ensureStorageDir();
    const data = await fs.readFile(filePath, 'utf8');
    return JSON.parse(data);
  } catch (error) {
    // File doesn't exist or is invalid, return default value
    console.log(`File ${filePath} not found, using default value`);
    return defaultValue;
  }
}

async function writeJsonFile(filePath: string, data: any) {
  try {
    await ensureStorageDir();
    await fs.writeFile(filePath, JSON.stringify(data, null, 2), 'utf8');
    console.log(`Data saved to ${filePath}`);
  } catch (error) {
    console.error(`Error writing to ${filePath}:`, error);
    throw error;
  }
}

// Credential storage operations
export async function loadCredentials(): Promise<any[]> {
  return await readJsonFile(CREDENTIALS_FILE, []);
}

export async function saveCredentials(credentials: any[]): Promise<void> {
  await writeJsonFile(CREDENTIALS_FILE, credentials);
}

export async function addCredential(credential: any): Promise<void> {
  const credentials = await loadCredentials();
  credentials.push(credential);
  await saveCredentials(credentials);
}

export async function removeCredential(credentialId: string): Promise<void> {
  const credentials = await loadCredentials();
  const filtered = credentials.filter(cred => cred.id !== credentialId);
  await saveCredentials(filtered);
}

// Notifications are kept in-memory only (temporary by nature)

// Initialize storage and migrate from in-memory data if exists
export async function initializePersistentStorage() {
  try {
    await ensureStorageDir();
    
    // Check if we have in-memory credentials to migrate
    if (typeof globalThis !== 'undefined') {
      if (globalThis.unifiedCredentialStore && globalThis.unifiedCredentialStore.length > 0) {
        const existingCredentials = await loadCredentials();
        if (existingCredentials.length === 0) {
          console.log('Migrating in-memory credentials to persistent storage...');
          await saveCredentials(globalThis.unifiedCredentialStore);
        }
      }
    }
    
    console.log('Persistent storage initialized for credentials only');
  } catch (error) {
    console.error('Error initializing persistent storage:', error);
  }
}