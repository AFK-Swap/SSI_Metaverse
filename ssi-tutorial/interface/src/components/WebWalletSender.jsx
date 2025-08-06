"use client";
import React, { useState } from "react";
import {
  Card,
  CardHeader,
  CardBody,
  Typography,
  Button,
  Input,
  CardFooter,
} from "@material-tailwind/react";
import axios from "axios";

export function WebWalletSender() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    department: "",
    age: "",
  });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const sendCredentialToWebWallet = async () => {
    setLoading(true);
    setError("");
    setSuccess(false);

    try {
      // Get actual DID and schema from ACA-Py
      const didResponse = await axios.get('http://localhost:8021/wallet/did/public');
      const issuerDid = didResponse.data.result.did; // EQ6SUp3NCA6c4CHrnwKvRy
      
      // Get available schemas and credential definitions
      const schemasResponse = await axios.get('http://localhost:8021/schemas/created');
      const credDefsResponse = await axios.get('http://localhost:8021/credential-definitions/created');
      
      // Use the first available schema and credential definition
      const schemaId = schemasResponse.data.schema_ids[0]; // e.g., "EQ6SUp3NCA6c4CHrnwKvRy:2:Identity_Schema:1.0.1754408693670"
      const credDefId = credDefsResponse.data.credential_definition_ids[0]; // e.g., "EQ6SUp3NCA6c4CHrnwKvRy:3:CL:2885481:University-Certificate"
      
      // Send credential offer to web wallet
      const response = await axios.post('http://localhost:3001/api/notifications', {
        type: "credential-offer",
        title: "SwapPC Employee Credential",
        message: "New employee credential from SwapPC issued by ACA-Py",
        credentialData: {
          schemaId: schemaId,
          credentialDefinitionId: credDefId,
          credentialPreview: {
            attributes: [
              { name: "name", value: formData.name },
              { name: "email", value: formData.email },
              { name: "department", value: formData.department },
              { name: "issuer_did", value: issuerDid },
              { name: "age", value: formData.age }
            ]
          }
        }
      });

      console.log("Credential sent successfully:", response.data);
      console.log("Using DID:", issuerDid);
      console.log("Using Schema:", schemaId);
      console.log("Using CredDef:", credDefId);
      setSuccess(true);
      
      // Reset form
      setFormData({
        name: "",
        email: "",
        department: "",
        age: "",
      });
      
    } catch (error) {
      console.error("Error sending credential:", error);
      setError(error.response?.data?.message || error.message || "Failed to send credential");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    // Basic validation
    if (!formData.name || !formData.email || !formData.department || !formData.age) {
      setError("Please fill in all fields");
      return;
    }
    
    sendCredentialToWebWallet();
  };

  return (
    <div className="w-full px-32 py-16">
      <div className="text-center mb-12">
        <Typography variant="h2" color="blue-gray" className="mb-4">
          Send Credential to Web Wallet
        </Typography>
        <Typography variant="lead" color="gray">
          Send a credential offer directly to your VR Web Wallet with SwapPC as the issuer
        </Typography>
      </div>

      <Card className="w-full max-w-[48rem] mx-auto">
        <CardHeader
          shadow={false}
          floated={false}
          className="m-0 grid place-items-center px-4 py-8 text-center"
        >
          <div className="mb-4 h-20 p-6 text-white bg-gradient-to-tr from-blue-600 to-blue-400 rounded-full grid place-items-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.5}
              stroke="currentColor"
              className="w-10 h-10"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15 9h3.75M15 12h3.75M15 15h3.75M4.5 19.5h15a2.25 2.25 0 002.25-2.25V6.75A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25v10.5A2.25 2.25 0 004.5 19.5zm6-10.125a1.875 1.875 0 11-3.75 0 1.875 1.875 0 013.75 0z"
              />
            </svg>
          </div>
          <Typography variant="h4" color="blue-gray">
            SwapPC Employee Credential
          </Typography>
          <Typography color="gray" className="mt-1 font-normal">
            Enter employee details to issue credential
          </Typography>
        </CardHeader>

        <CardBody className="flex flex-col gap-4">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}
          
          {success && (
            <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
              Credential offer sent successfully to your web wallet! Check your notifications.
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <Input
                type="text"
                name="name"
                label="Full Name"
                value={formData.name}
                onChange={handleInputChange}
                required
                className="!border-t-blue-gray-200 focus:!border-t-gray-900"
                labelProps={{
                  className: "before:content-none after:content-none",
                }}
              />
            </div>

            <div className="mb-4">
              <Input
                type="email"
                name="email"
                label="Email Address"
                value={formData.email}
                onChange={handleInputChange}
                required
                className="!border-t-blue-gray-200 focus:!border-t-gray-900"
                labelProps={{
                  className: "before:content-none after:content-none",
                }}
              />
            </div>

            <div className="mb-4">
              <Input
                type="text"
                name="department"
                label="Department"
                value={formData.department}
                onChange={handleInputChange}
                required
                className="!border-t-blue-gray-200 focus:!border-t-gray-900"
                labelProps={{
                  className: "before:content-none after:content-none",
                }}
              />
            </div>

            <div className="mb-6">
              <Input
                type="number"
                name="age"
                label="Age"
                value={formData.age}
                onChange={handleInputChange}
                required
                min="18"
                max="120"
                className="!border-t-blue-gray-200 focus:!border-t-gray-900"
                labelProps={{
                  className: "before:content-none after:content-none",
                }}
              />
            </div>
          </form>
        </CardBody>

        <CardFooter className="pt-0">
          <Button
            onClick={handleSubmit}
            disabled={loading}
            className="w-full"
            loading={loading}
          >
            {loading ? "Sending..." : "Send Credential to Web Wallet"}
          </Button>
          
          <Typography variant="small" className="mt-6 flex justify-center">
            Issuer: SwapPC
            <br />
            Target: VR Web Wallet (localhost:3001)
          </Typography>
        </CardFooter>
      </Card>
    </div>
  );
}