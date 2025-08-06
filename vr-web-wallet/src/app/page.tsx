'use client'

import React, { useEffect, useState } from 'react'
import Link from 'next/link'
import { VRButton } from '@/components/VRButton'
import { VRCard } from '@/components/VRCard'
import { StatusIndicator } from '@/components/StatusIndicator'
import { VRWalletAgent } from '@/lib/wallet-agent'
import type { WalletStats } from '@/lib/types'

export default function Dashboard() {
  const [walletAgent] = useState(() => VRWalletAgent.getInstance())
  const [stats, setStats] = useState<WalletStats>({
    connections: 0,
    credentials: 0,
    proofs: 0,
    isInitialized: false,
    isConnected: false
  })
  const [notificationCount, setNotificationCount] = useState(0)
  const [isInitializing, setIsInitializing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadWalletStats()
    loadNotificationCount()
    // Poll for updates every 5 seconds
    const interval = setInterval(() => {
      loadWalletStats()
      loadNotificationCount()
    }, 5000)
    return () => clearInterval(interval)
  }, [])

  const loadWalletStats = async () => {
    try {
      // Get credentials count from unified credentials API
      const credentialsResponse = await fetch('/api/credentials')
      const credentialsData = await credentialsResponse.json()
      const credentialsCount = credentialsData.success ? credentialsData.credentials.length : 0
      
      setStats({
        connections: 0, // Will implement later
        credentials: credentialsCount,
        proofs: 0, // Will implement later
        isInitialized: true, // Assume initialized if API is working
        isConnected: true
      })
    } catch (error) {
      console.error('Failed to load wallet stats:', error)
      setError(error instanceof Error ? error.message : 'Unknown error')
    }
  }

  const loadNotificationCount = async () => {
    try {
      const response = await fetch('/api/notifications')
      const data = await response.json()
      if (data.success) {
        // Count only pending notifications
        const pendingCount = data.notifications.filter((n: any) => n.status === 'pending').length
        setNotificationCount(pendingCount)
      }
    } catch (error) {
      console.error('Failed to load notification count:', error)
    }
  }

  const initializeWallet = async () => {
    setIsInitializing(true)
    setError(null)
    
    try {
      await walletAgent.initialize()
      await loadWalletStats()
    } catch (error) {
      console.error('Failed to initialize wallet:', error)
      setError(error instanceof Error ? error.message : 'Failed to initialize wallet')
    } finally {
      setIsInitializing(false)
    }
  }

  const getConnectionStatus = (): 'online' | 'offline' | 'pending' => {
    if (isInitializing) return 'pending'
    if (!stats.isInitialized) return 'offline'
    return stats.isConnected ? 'online' : 'offline'
  }

  const getStatusLabel = (): string => {
    if (isInitializing) return 'Initializing...'
    if (!stats.isInitialized) return 'Not initialized'
    return stats.isConnected ? 'Connected' : 'Ready'
  }

  return (
    <div className="min-h-screen bg-vr-bg-primary">
      <div className="max-w-6xl mx-auto px-vr-6 py-vr-12">
        
        {/* Header Section */}
        <header className="text-center mb-vr-12">
          <h1 className="vr-title mb-vr-6">VR Identity Wallet</h1>
          <p className="vr-body-large text-tertiary max-w-2xl mx-auto mb-vr-8">
            Secure, decentralized identity management for the metaverse
          </p>
          <StatusIndicator 
            status={getConnectionStatus()}
            label={getStatusLabel()}
          />
        </header>

        {/* Error State */}
        {error && (
          <div className="vr-card border-danger mb-vr-8">
            <div className="text-center">
              <h3 className="vr-subtitle text-danger mb-vr-4">Error</h3>
              <p className="vr-body mb-vr-6">{error}</p>
              <VRButton variant="danger" onClick={() => setError(null)}>
                Dismiss
              </VRButton>
            </div>
          </div>
        )}

        {/* Initialization State */}
        {!stats.isInitialized && !error && (
          <div className="vr-card vr-card-elevated text-center mb-vr-8">
            <div className="max-w-md mx-auto">
              <h2 className="vr-heading mb-vr-6">Initialize Wallet</h2>
              <p className="vr-body text-tertiary mb-vr-8">
                Set up your secure digital identity wallet to begin managing credentials and verifications.
              </p>
              <VRButton
                variant="primary"
                size="lg"
                onClick={initializeWallet}
                loading={isInitializing}
                className="w-full"
              >
                {isInitializing ? 'Initializing...' : 'Initialize Wallet'}
              </VRButton>
            </div>
          </div>
        )}

        {/* Main Content - Only show if initialized */}
        {stats.isInitialized && (
          <>
            {/* Navigation Tabs */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-vr-6 mb-vr-12 max-w-2xl mx-auto">
              <Link href="/notifications" className="block">
                <div className="vr-card vr-card-interactive text-center relative">
                  {notificationCount > 0 && (
                    <div className="absolute -top-2 -right-2 bg-accent text-primary w-6 h-6 rounded-full flex items-center justify-center text-sm font-bold">
                      {notificationCount}
                    </div>
                  )}
                  <div className="text-4xl mb-vr-4">🔔</div>
                  <h3 className="vr-subtitle mb-vr-2">Notifications</h3>
                  {notificationCount > 0 ? (
                    <div className="text-2xl font-bold text-accent mb-vr-2">{notificationCount}</div>
                  ) : null}
                  <p className="vr-caption text-tertiary">Credential offers & proof requests</p>
                </div>
              </Link>

              <Link href="/credentials" className="block">
                <div className="vr-card vr-card-interactive text-center">
                  <div className="text-4xl mb-vr-4">📜</div>
                  <h3 className="vr-subtitle mb-vr-2">Credentials</h3>
                  <div className="text-2xl font-bold text-accent mb-vr-2">{stats.credentials}</div>
                  <p className="vr-caption text-tertiary">Your stored credentials</p>
                </div>
              </Link>
            </div>

            <div className="vr-divider"></div>

            {/* Info Section */}
            <section className="text-center">
              <h2 className="vr-heading mb-vr-8">Secure Digital Identity</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-vr-8 max-w-4xl mx-auto">
                <div>
                  <div className="w-16 h-16 bg-accent bg-opacity-10 rounded-xl flex items-center justify-center mx-auto mb-vr-4">
                    <span className="text-2xl">🛡️</span>
                  </div>
                  <h3 className="vr-subtitle mb-vr-2">Zero-Knowledge</h3>
                  <p className="vr-body text-tertiary">
                    Prove information without revealing private data
                  </p>
                </div>

                <div>
                  <div className="w-16 h-16 bg-accent bg-opacity-10 rounded-xl flex items-center justify-center mx-auto mb-vr-4">
                    <span className="text-2xl">🔒</span>
                  </div>
                  <h3 className="vr-subtitle mb-vr-2">Self-Sovereign</h3>
                  <p className="vr-body text-tertiary">
                    You own and control your digital identity
                  </p>
                </div>

                <div>
                  <div className="w-16 h-16 bg-accent bg-opacity-10 rounded-xl flex items-center justify-center mx-auto mb-vr-4">
                    <span className="text-2xl">🌐</span>
                  </div>
                  <h3 className="vr-subtitle mb-vr-2">Interoperable</h3>
                  <p className="vr-body text-tertiary">
                    Compatible with global identity standards
                  </p>
                </div>
              </div>
            </section>
          </>
        )}

        {/* Footer */}
        <footer className="text-center mt-vr-16 pt-vr-8 border-t border-subtle">
          <div className="flex items-center justify-center space-x-6 vr-caption-small text-muted mb-vr-4">
            <span>Aries Framework</span>
            <span>•</span>
            <span>DIDComm Protocol</span>
            <span>•</span>
            <span>W3C Standards</span>
          </div>
          <p className="vr-caption-small text-muted">
            Secure • Private • Decentralized
          </p>
        </footer>
      </div>
    </div>
  )
}