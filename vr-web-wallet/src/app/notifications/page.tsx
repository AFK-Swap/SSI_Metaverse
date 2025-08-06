'use client'

import React, { useEffect, useState } from 'react'
import Link from 'next/link'
import { VRButton } from '@/components/VRButton'
import { VRCard } from '@/components/VRCard'
import { VRWalletAgent } from '@/lib/wallet-agent'

interface Notification {
  id: string
  type: 'credential-offer' | 'proof-request'
  title: string
  message: string
  timestamp: string
  credentialData?: any
  proofRequestData?: any
  rawMessage?: any
  status: 'pending' | 'accepted' | 'declined'
}

interface CredentialAvailability {
  hasMatch: boolean
  missingAttributes: string[]
  availableAttributes: string[]
  requestedAttributes: string[]
  matchingCredentials: any[]
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedProofRequest, setExpandedProofRequest] = useState<string | null>(null)
  const [credentialAvailability, setCredentialAvailability] = useState<{ [key: string]: CredentialAvailability }>({})
  const [selectedCredential, setSelectedCredential] = useState<{ [key: string]: string }>({})

  useEffect(() => {
    loadNotifications()
  }, [])

  const loadNotifications = async () => {
    try {
      const response = await fetch('/api/notifications')
      const data = await response.json()
      
      if (data.success) {
        setNotifications(data.notifications)
        // Check credential availability for proof requests
        await checkCredentialAvailability(data.notifications)
      }
    } catch (error) {
      console.error('Failed to load notifications:', error)
    } finally {
      setLoading(false)
    }
  }

  const checkCredentialAvailability = async (notifications: Notification[]) => {
    const availability: { [key: string]: CredentialAvailability } = {}
    
    for (const notification of notifications) {
      if (notification.type === 'proof-request' && notification.status === 'pending') {
        try {
          const response = await fetch(`/api/notifications/${notification.id}/check`)
          const data = await response.json()
          
          if (data.success) {
            availability[notification.id] = {
              hasMatch: data.hasMatch,
              missingAttributes: data.missingAttributes,
              availableAttributes: data.availableAttributes,
              requestedAttributes: data.requestedAttributes,
              matchingCredentials: data.matchingCredentials || []
            }
            
            // Set default selected credential to first one if multiple available
            if (data.matchingCredentials && data.matchingCredentials.length > 0) {
              setSelectedCredential(prev => ({
                ...prev,
                [notification.id]: data.matchingCredentials[0].id
              }))
            }
          }
        } catch (error) {
          console.error(`Failed to check availability for ${notification.id}:`, error)
        }
      }
    }
    
    setCredentialAvailability(availability)
  }

  const handleAccept = async (notification: Notification) => {
    try {
      // Include selected credential ID for proof requests
      const requestBody: any = { action: 'accept' }
      if (notification.type === 'proof-request' && selectedCredential[notification.id]) {
        requestBody.credentialId = selectedCredential[notification.id]
      }
      
      const response = await fetch(`/api/notifications/${notification.id}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      })
      
      if (response.ok) {
        // Update local state
        setNotifications(prev => 
          prev.map(n => n.id === notification.id ? { ...n, status: 'accepted' } : n)
        )
        
        if (notification.type === 'credential-offer') {
          // Auto-hide credential offers after 5 seconds
          setTimeout(() => {
            setNotifications(prev => prev.filter(n => n.id !== notification.id))
          }, 5000)
          console.log('Credential offer accepted and stored')
        } else if (notification.type === 'proof-request') {
          // For proof requests, send to ACA-Py and then remove
          console.log('Proof request accepted - sharing information')
          // Remove immediately for proof requests as they're handled
          setTimeout(() => {
            setNotifications(prev => prev.filter(n => n.id !== notification.id))
          }, 1000)
        }
      }
    } catch (error) {
      console.error('Failed to accept notification:', error)
    }
  }

  const handleReject = async (notification: Notification) => {
    try {
      const response = await fetch(`/api/notifications/${notification.id}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ action: 'decline' })
      })
      
      if (response.ok) {
        // Remove notification immediately when declined
        setNotifications(prev => prev.filter(n => n.id !== notification.id))
        console.log(`${notification.type === 'proof-request' ? 'Proof request' : 'Credential offer'} declined`)
      }
    } catch (error) {
      console.error('Failed to decline notification:', error)
    }
  }

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'credential-offer':
        return '📜'
      case 'proof-request':
        return '🔍'
      default:
        return '📋'
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'accepted':
        return 'text-success'
      case 'declined':
        return 'text-danger'
      default:
        return 'text-accent'
    }
  }

  const formatCredentialData = (credentialData: any) => {
    if (!credentialData || Object.keys(credentialData).length === 0) {
      return 'No credential details available'
    }
    
    return Object.entries(credentialData)
      .map(([key, value]) => `${key}: ${value}`)
      .join(', ')
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-vr-bg-primary flex items-center justify-center">
        <div className="text-center">
          <div className="vr-spinner mx-auto mb-vr-4"></div>
          <p className="vr-body text-tertiary">Loading notifications...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-vr-bg-primary">
      <div className="max-w-4xl mx-auto px-vr-6 py-vr-12">
        
        {/* Header */}
        <header className="mb-vr-12">
          <div className="flex items-center justify-between mb-vr-6">
            <Link href="/" className="vr-btn vr-btn-ghost vr-btn-sm">
              ← Back to Wallet
            </Link>
          </div>
          <h1 className="vr-title mb-vr-4">Notifications</h1>
          <p className="vr-body-large text-tertiary">
            Manage your credential offers and proof requests
          </p>
        </header>

        {/* Notifications List */}
        {notifications.length === 0 ? (
          <div className="vr-card text-center">
            <div className="py-vr-12">
              <div className="text-6xl mb-vr-6">🔔</div>
              <h2 className="vr-heading mb-vr-4">No Notifications</h2>
              <p className="vr-body text-tertiary max-w-md mx-auto">
                You don't have any pending credential offers or proof requests. 
                Notifications will appear here when issuers send you credentials.
              </p>
            </div>
          </div>
        ) : (
          <div className="space-y-vr-6">
            {notifications.map((notification) => (
              <div key={notification.id} className="vr-card">
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-4 flex-grow">
                    <div className="flex-shrink-0">
                      <div className="w-12 h-12 bg-accent bg-opacity-10 rounded-lg flex items-center justify-center">
                        <span className="text-xl">{getNotificationIcon(notification.type)}</span>
                      </div>
                    </div>
                    
                    <div className="flex-grow">
                      <div className="flex items-center space-x-3 mb-vr-2">
                        <h3 className="vr-subtitle">{notification.title}</h3>
                        <span className={`vr-caption-small px-2 py-1 rounded-full bg-opacity-10 ${getStatusColor(notification.status)}`}>
                          {notification.status}
                        </span>
                      </div>
                      
                      <p className="vr-body text-tertiary mb-vr-2">
                        {notification.message}
                      </p>
                      
                      {/* Credential Offer Details */}
                      {notification.type === 'credential-offer' && notification.credentialData?.credentialPreview?.attributes && (
                        <div className="vr-card bg-opacity-50 mb-vr-4">
                          <h4 className="vr-caption font-medium text-secondary mb-vr-2">Credential Details:</h4>
                          <div className="space-y-vr-1">
                            {notification.credentialData.credentialPreview.attributes.map((attr: any) => (
                              <div key={attr.name} className="flex justify-between text-sm">
                                <span className="text-tertiary capitalize">{attr.name}:</span>
                                <span className="text-secondary font-medium">{attr.value}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* Proof Request Details */}
                      {notification.type === 'proof-request' && notification.proofRequestData && (
                        <div className="mb-vr-4">
                          <button
                            onClick={() => setExpandedProofRequest(
                              expandedProofRequest === notification.id ? null : notification.id
                            )}
                            className="flex items-center space-x-2 text-accent hover:text-accent-hover transition-colors"
                          >
                            <span className="text-sm">
                              {expandedProofRequest === notification.id ? '▼' : '▶'}
                            </span>
                            <span className="vr-caption font-medium">View requested information</span>
                          </button>
                          
                          {expandedProofRequest === notification.id && (
                            <div className="mt-vr-3 vr-card bg-opacity-50">
                              <h4 className="vr-caption font-medium text-secondary mb-vr-2">Requested Information:</h4>
                              <div className="space-y-vr-2">
                                {notification.proofRequestData.requested_attributes && 
                                 Object.entries(notification.proofRequestData.requested_attributes).map(([key, attr]: [string, any]) => {
                                   const availability = credentialAvailability[notification.id]
                                   const isAvailable = availability?.availableAttributes.includes(attr.name.toLowerCase())
                                   
                                   return (
                                     <div key={key} className="flex items-center justify-between">
                                       <span className="text-tertiary capitalize">{attr.name}:</span>
                                       <span className={`px-2 py-1 rounded text-xs ${
                                         isAvailable 
                                           ? 'bg-success bg-opacity-10 text-success'
                                           : 'bg-danger bg-opacity-10 text-danger'
                                       }`}>
                                         {isAvailable ? 'Available' : 'Missing'}
                                       </span>
                                     </div>
                                   )
                                 })}
                              </div>
                              
                              {/* Show availability status */}
                              {credentialAvailability[notification.id] && (
                                <div className="mt-vr-3">
                                  {credentialAvailability[notification.id].hasMatch ? (
                                    <div className="space-y-3">
                                      <div className="p-3 bg-success bg-opacity-10 rounded border border-success border-opacity-20">
                                        <div className="flex items-start space-x-2">
                                          <span className="text-success text-sm">✅</span>
                                          <p className="text-xs text-success">
                                            All requested information is available in your wallet. You can share this information securely.
                                          </p>
                                        </div>
                                      </div>
                                      
                                      {/* Credential Selection */}
                                      {credentialAvailability[notification.id].matchingCredentials.length > 1 && (
                                        <div className="p-3 bg-accent bg-opacity-10 rounded border border-accent border-opacity-20">
                                          <h4 className="text-xs font-medium text-accent mb-2">Choose Credential to Share:</h4>
                                          <div className="space-y-2">
                                            {credentialAvailability[notification.id].matchingCredentials.map((credential: any) => (
                                              <label key={credential.id} className="flex items-start space-x-3 cursor-pointer">
                                                <input
                                                  type="radio"
                                                  name={`credential-${notification.id}`}
                                                  value={credential.id}
                                                  checked={selectedCredential[notification.id] === credential.id}
                                                  onChange={(e) => setSelectedCredential(prev => ({
                                                    ...prev,
                                                    [notification.id]: e.target.value
                                                  }))}
                                                  className="mt-1"
                                                />
                                                <div className="flex-grow">
                                                  <div className="text-xs font-medium text-secondary">
                                                    {credential.type || 'Digital Credential'}
                                                  </div>
                                                  <div className="text-xs text-tertiary">
                                                    {credential.credential?.name && `Name: ${credential.credential.name}`}
                                                    {credential.credential?.email && ` • Email: ${credential.credential.email}`}
                                                  </div>
                                                  <div className="text-xs text-muted">
                                                    {new Date(credential.timestamp).toLocaleDateString()}
                                                  </div>
                                                </div>
                                              </label>
                                            ))}
                                          </div>
                                        </div>
                                      )}
                                    </div>
                                  ) : (
                                    <div className="p-3 bg-danger bg-opacity-10 rounded border border-danger border-opacity-20">
                                      <div className="flex items-start space-x-2">
                                        <span className="text-danger text-sm">❌</span>
                                        <div className="text-xs text-danger">
                                          <p className="mb-2">You don't have the required information to fulfill this request.</p>
                                          <p className="font-medium">Missing: {credentialAvailability[notification.id].missingAttributes.join(', ')}</p>
                                        </div>
                                      </div>
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      )}
                      
                      <p className="vr-caption text-muted">
                        {new Date(notification.timestamp).toLocaleString()}
                      </p>
                    </div>
                  </div>

                  {notification.status === 'pending' && (
                    <div className="flex space-x-3 ml-4">
                      <VRButton 
                        variant="outline" 
                        size="sm"
                        onClick={() => handleReject(notification)}
                      >
                        {notification.type === 'proof-request' ? 'Cancel' : 'Reject'}
                      </VRButton>
                      
                      {/* Only show Accept/Share Info if credentials are available or it's not a proof request */}
                      {(notification.type !== 'proof-request' || 
                        (credentialAvailability[notification.id]?.hasMatch)) && (
                        <VRButton 
                          variant="primary" 
                          size="sm"
                          onClick={() => handleAccept(notification)}
                        >
                          {notification.type === 'proof-request' ? 'Share Info' : 'Accept'}
                        </VRButton>
                      )}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  )
}