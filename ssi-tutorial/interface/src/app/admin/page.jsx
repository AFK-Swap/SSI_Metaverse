'use client';

import React, { useState, useEffect } from 'react';
import {
  Card,
  CardHeader,
  CardBody,
  Typography,
  Button,
  Input,
  Alert,
  IconButton,
} from "@material-tailwind/react";
import { TrashIcon, PlusIcon, CheckCircleIcon, XCircleIcon } from "@heroicons/react/24/outline";
import axios from 'axios';

const AdminTrustedDIDs = () => {
  const [trustedDIDs, setTrustedDIDs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);
  const [newDID, setNewDID] = useState({ did: '', name: '' });

  // API base URL for verifier
  const API_BASE = process.env.NEXT_PUBLIC_VERIFIER_API_URL || 'http://localhost:4002/v2';
  
  console.log('API_BASE:', API_BASE); // Debug logging

  // Fetch trusted DIDs
  const fetchTrustedDIDs = async () => {
    try {
      setLoading(true);
      setError(''); // Clear previous errors
      console.log('Fetching trusted DIDs from:', `${API_BASE}/trusted-dids`);
      
      const response = await axios.get(`${API_BASE}/trusted-dids`);
      console.log('API Response:', response.data);
      
      if (response.data.success) {
        setTrustedDIDs(response.data.data);
        console.log('Successfully loaded', response.data.data.length, 'trusted DIDs');
      } else {
        setError('Failed to fetch trusted DIDs');
      }
    } catch (err) {
      console.error('Fetch error details:', err.response?.data || err.message);
      if (err.response?.status === 0 || err.code === 'ERR_NETWORK') {
        setError('Cannot connect to verifier service. Is it running on port 4002?');
      } else {
        setError(`Error connecting to verifier service: ${err.message}`);
      }
    } finally {
      setLoading(false);
    }
  };

  // Add new trusted DID
  const addTrustedDID = async () => {
    if (!newDID.did || !newDID.name) {
      setError('Both DID and name are required');
      return;
    }

    try {
      const response = await axios.post(`${API_BASE}/trusted-dids`, {
        did: newDID.did.trim(),
        name: newDID.name.trim()
      });

      if (response.data.success) {
        setSuccess(`Successfully added: ${newDID.name}`);
        setNewDID({ did: '', name: '' });
        setShowAddForm(false);
        fetchTrustedDIDs(); // Refresh list
      } else {
        setError(response.data.error || 'Failed to add DID');
      }
    } catch (err) {
      if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else {
        setError('Error adding trusted DID');
      }
      console.error('Add error:', err);
    }
  };

  // Remove trusted DID
  const removeTrustedDID = async (did) => {
    if (!confirm(`Are you sure you want to remove this trusted DID: ${did}?`)) {
      return;
    }

    try {
      const encodedDID = encodeURIComponent(did);
      const response = await axios.delete(`${API_BASE}/trusted-dids/${encodedDID}`);

      if (response.data.success) {
        setSuccess(`Successfully removed DID: ${did}`);
        fetchTrustedDIDs(); // Refresh list
      } else {
        setError(response.data.error || 'Failed to remove DID');
      }
    } catch (err) {
      if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else {
        setError('Error removing trusted DID');
      }
      console.error('Remove error:', err);
    }
  };

  // Clear messages after 5 seconds
  useEffect(() => {
    if (error || success) {
      const timer = setTimeout(() => {
        setError('');
        setSuccess('');
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [error, success]);

  // Fetch data on component mount
  useEffect(() => {
    fetchTrustedDIDs();
  }, []);

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-6">
        <Typography variant="h3" color="blue-gray" className="mb-2">
          Trusted DID Management
        </Typography>
        <Typography color="gray" className="mb-4">
          Manage the list of trusted DIDs for SSI verification. Only credentials from these issuers will be accepted.
        </Typography>
      </div>

      {/* Success/Error Messages */}
      {error && (
        <Alert color="red" className="mb-4" icon={<XCircleIcon className="h-6 w-6" />}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert color="green" className="mb-4" icon={<CheckCircleIcon className="h-6 w-6" />}>
          {success}
        </Alert>
      )}

      {/* Add New DID Section */}
      <Card className="mb-6">
        <CardHeader floated={false} shadow={false} className="rounded-none">
          <div className="flex items-center justify-between">
            <Typography variant="h6" color="blue-gray">
              Add New Trusted DID
            </Typography>
            <Button
              size="sm"
              onClick={() => setShowAddForm(!showAddForm)}
              className="flex items-center gap-2"
            >
              <PlusIcon className="h-4 w-4" />
              {showAddForm ? 'Cancel' : 'Add DID'}
            </Button>
          </div>
        </CardHeader>
        {showAddForm && (
          <CardBody>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <Input
                label="DID"
                placeholder="e.g., Hfe4a7wUpqV1qEJxdqCTLr"
                value={newDID.did}
                onChange={(e) => setNewDID({ ...newDID, did: e.target.value })}
              />
              <Input
                label="Issuer Name"
                placeholder="e.g., University of Excellence"
                value={newDID.name}
                onChange={(e) => setNewDID({ ...newDID, name: e.target.value })}
              />
            </div>
            <Button onClick={addTrustedDID} className="w-full md:w-auto">
              Add Trusted DID
            </Button>
          </CardBody>
        )}
      </Card>

      {/* Trusted DIDs List */}
      <Card>
        <CardHeader floated={false} shadow={false} className="rounded-none">
          <div className="flex items-center justify-between">
            <Typography variant="h6" color="blue-gray">
              Current Trusted DIDs ({trustedDIDs.length})
            </Typography>
            <Button size="sm" variant="outlined" onClick={fetchTrustedDIDs}>
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardBody className="px-0">
          {loading ? (
            <div className="text-center py-8">
              <Typography color="gray">Loading trusted DIDs...</Typography>
            </div>
          ) : trustedDIDs.length === 0 ? (
            <div className="text-center py-8">
              <Typography color="gray">No trusted DIDs configured.</Typography>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px] table-auto">
                <thead>
                  <tr>
                    <th className="border-b border-blue-gray-50 py-3 px-6 text-left">
                      <Typography variant="small" className="text-[11px] font-medium uppercase text-blue-gray-400">
                        DID
                      </Typography>
                    </th>
                    <th className="border-b border-blue-gray-50 py-3 px-6 text-left">
                      <Typography variant="small" className="text-[11px] font-medium uppercase text-blue-gray-400">
                        Issuer Name
                      </Typography>
                    </th>
                    <th className="border-b border-blue-gray-50 py-3 px-6 text-left">
                      <Typography variant="small" className="text-[11px] font-medium uppercase text-blue-gray-400">
                        Added Date
                      </Typography>
                    </th>
                    <th className="border-b border-blue-gray-50 py-3 px-6 text-left">
                      <Typography variant="small" className="text-[11px] font-medium uppercase text-blue-gray-400">
                        Added By
                      </Typography>
                    </th>
                    <th className="border-b border-blue-gray-50 py-3 px-6 text-left">
                      <Typography variant="small" className="text-[11px] font-medium uppercase text-blue-gray-400">
                        Actions
                      </Typography>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {trustedDIDs.map((trustedDID, index) => (
                    <tr key={index}>
                      <td className="py-3 px-6 border-b border-blue-gray-50">
                        <Typography variant="small" className="text-xs font-medium text-blue-gray-600 font-mono">
                          {trustedDID.did}
                        </Typography>
                      </td>
                      <td className="py-3 px-6 border-b border-blue-gray-50">
                        <Typography variant="small" className="text-xs font-medium text-blue-gray-600">
                          {trustedDID.name}
                        </Typography>
                      </td>
                      <td className="py-3 px-6 border-b border-blue-gray-50">
                        <Typography variant="small" className="text-xs text-blue-gray-500">
                          {new Date(trustedDID.addedDate).toLocaleDateString()}
                        </Typography>
                      </td>
                      <td className="py-3 px-6 border-b border-blue-gray-50">
                        <Typography variant="small" className="text-xs text-blue-gray-500">
                          {trustedDID.addedBy}
                        </Typography>
                      </td>
                      <td className="py-3 px-6 border-b border-blue-gray-50">
                        <IconButton
                          variant="text"
                          color="red"
                          onClick={() => removeTrustedDID(trustedDID.did)}
                        >
                          <TrashIcon className="h-4 w-4" />
                        </IconButton>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardBody>
      </Card>

      {/* Info Section */}
      <Card className="mt-6">
        <CardBody>
          <Typography variant="h6" color="blue-gray" className="mb-2">
            How It Works
          </Typography>
          <Typography color="gray" className="text-sm">
            • Only credentials issued by DIDs in this trusted list will be accepted during verification<br/>
            • When a user presents a proof, the system checks the issuer_did attribute against this list<br/>
            • If the DID is not trusted, verification will fail with "Sorry, the DID is unauthorized"<br/>
            • Changes take effect immediately - no restart required<br/>
            • The default issuer DID is automatically included in the trusted list
          </Typography>
        </CardBody>
      </Card>
    </div>
  );
};

export default AdminTrustedDIDs;