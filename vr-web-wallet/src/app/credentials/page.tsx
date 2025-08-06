'use client'

import React, { useEffect, useState } from 'react'
import Link from 'next/link'
import { VRButton } from '@/components/VRButton'
import { VRWalletAgent } from '@/lib/wallet-agent'

interface Credential {
  id: string
  type: string
  issuer: string
  issuedDate: string
  attributes: { [key: string]: any }
  credentialDefinitionId?: string
}

export default function CredentialsPage() {
  const [walletAgent] = useState(() => VRWalletAgent.getInstance())
  const [credentials, setCredentials] = useState<Credential[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadCredentials()
  }, [])

  const loadCredentials = async () => {
    try {
      const response = await fetch('/api/credentials')
      const data = await response.json()
      
      if (data.success) {
        // Transform unified credential store data to match expected format
        const transformedCredentials = data.credentials.map((cred: any) => ({
          id: cred.id,
          type: cred.originalFormat === 'anoncreds' ? 'AnonCreds Credential' : 'Digital Credential',
          issuer: cred.originalFormat === 'anoncreds' ? 'BCovrin Network' : 'Digital Issuer', 
          issuedDate: cred.createdAt || cred.timestamp,
          attributes: cred.attributes?.reduce((acc: any, attr: any) => {
            acc[attr.name] = attr.value
            return acc
          }, {}) || cred.credentialData || {},
          credentialDefinitionId: cred.credentialDefinitionId,
          state: cred.state,
          schemaId: cred.schemaId
        }))
        setCredentials(transformedCredentials)
      }
    } catch (error) {
      console.error('Failed to load credentials:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleRemoveCredential = async (credential: Credential) => {
    try {
      if (confirm(`Are you sure you want to remove the "${credential.type}" credential? This action cannot be undone.`)) {
        // Call DELETE API to remove credential from server
        const response = await fetch(`/api/credentials?id=${credential.id}`, {
          method: 'DELETE'
        })
        
        const data = await response.json()
        
        if (data.success) {
          // Remove from local state only after successful server deletion
          setCredentials(prev => prev.filter(c => c.id !== credential.id))
          console.log('Credential removed:', credential.id)
        } else {
          console.error('Failed to remove credential:', data.error)
          alert('Failed to remove credential: ' + data.error)
        }
      }
    } catch (error) {
      console.error('Failed to remove credential:', error)
      alert('Failed to remove credential. Please try again.')
    }
  }

  const getCredentialIcon = (type: string) => {
    if (type.toLowerCase().includes('gaming') || type.toLowerCase().includes('vr')) {
      return '🎮'
    } else if (type.toLowerCase().includes('education') || type.toLowerCase().includes('diploma')) {
      return '🎓'
    } else if (type.toLowerCase().includes('identity') || type.toLowerCase().includes('id')) {
      return '🆔'
    } else if (type.toLowerCase().includes('membership')) {
      return '👥'
    }
    return '📜'
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-vr-bg-primary flex items-center justify-center">
        <div className="text-center">
          <div className="vr-spinner mx-auto mb-vr-4"></div>
          <p className="vr-body text-tertiary">Loading credentials...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-vr-bg-primary">
      <div className="max-w-6xl mx-auto px-vr-6 py-vr-12">
        
        {/* Header */}
        <header className="mb-vr-12">
          <div className="flex items-center justify-between mb-vr-6">
            <Link href="/" className="vr-btn vr-btn-ghost vr-btn-sm">
              ← Back to Wallet
            </Link>
            <div className="vr-caption text-muted">
              {credentials.length} credential{credentials.length !== 1 ? 's' : ''}
            </div>
          </div>
          <h1 className="vr-title mb-vr-4">My Credentials</h1>
          <p className="vr-body-large text-tertiary">
            Your verified digital credentials and certificates
          </p>
        </header>

        {credentials.length === 0 ? (
          <div className="vr-card text-center">
            <div className="py-vr-12">
              <div className="text-6xl mb-vr-6">📜</div>
              <h2 className="vr-heading mb-vr-4">No Credentials Yet</h2>
              <p className="vr-body text-tertiary max-w-md mx-auto mb-vr-6">
                You haven't received any credentials yet. Credentials will appear here 
                once you accept them from issuers in your notifications.
              </p>
              <Link href="/notifications">
                <VRButton variant="primary">
                  Check Notifications
                </VRButton>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-vr-6">
            {credentials.map((credential) => (
              <div key={credential.id} className="vr-card">
                <div className="flex items-start space-x-6">
                  <div className="flex-shrink-0">
                    <div className="w-16 h-16 bg-accent bg-opacity-10 rounded-xl flex items-center justify-center">
                      <span className="text-2xl">{getCredentialIcon(credential.type)}</span>
                    </div>
                  </div>
                  
                  <div className="flex-grow">
                    <div className="flex items-start justify-between mb-vr-4">
                      <div>
                        <h3 className="vr-heading mb-vr-2">{credential.type}</h3>
                        <p className="vr-body text-tertiary mb-vr-1">
                          Issued by: {credential.issuer}
                        </p>
                        <p className="vr-caption text-muted">
                          {new Date(credential.issuedDate).toLocaleDateString()}
                        </p>
                      </div>
                      <div className="flex items-center space-x-3">
                        <div className="w-3 h-3 bg-success rounded-full"></div>
                        <VRButton 
                          variant="outline" 
                          size="sm"
                          onClick={() => handleRemoveCredential(credential)}
                        >
                          Remove
                        </VRButton>
                      </div>
                    </div>

                    <div className="vr-card bg-opacity-50">
                      <h4 className="vr-subtitle mb-vr-3">Attributes</h4>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-vr-3">
                        {Object.entries(credential.attributes).map(([key, value]) => (
                          <div key={key} className="flex justify-between items-center">
                            <span className="vr-body text-tertiary capitalize">{key}:</span>
                            <span className="vr-body text-secondary font-medium">{String(value)}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    {credential.credentialDefinitionId && (
                      <div className="mt-vr-3">
                        <h4 className="vr-caption font-medium text-tertiary mb-vr-1">Credential Definition ID</h4>
                        <p className="vr-mono text-xs text-muted break-all">
                          {credential.credentialDefinitionId}
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  )
}