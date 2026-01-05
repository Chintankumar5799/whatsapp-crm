import React, { useEffect, useState } from 'react';
import {
    Container,
    Paper,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Button,
    TextField,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Box,
} from '@mui/material';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

interface PendingRequest {
    id: number;
    patientPhone: string;
    patientName: string;
    requestedDate: string;
    requestedStartTime: string;
    description?: string;
    addressId?: number;
}

const PendingRequests: React.FC = () => {
    const [requests, setRequests] = useState<PendingRequest[]>([]);
    const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
    const [selectedRequest, setSelectedRequest] = useState<PendingRequest | null>(null);
    const [rejectionMessage, setRejectionMessage] = useState('');

    // Address View State
    const [addressDetails, setAddressDetails] = useState<any>(null);
    const [addressDetailsOpen, setAddressDetailsOpen] = useState(false);

    // Get logged-in doctor from auth (same pattern as Dashboard)
    const storedUser = localStorage.getItem('user');
    const parsedUser = storedUser ? JSON.parse(storedUser) : null;
    const doctorId = parsedUser?.userId;

    useEffect(() => {
        // If no doctorId (not logged in as doctor), do nothing
        if (!doctorId) {
            console.warn('No doctorId found for pending requests');
            return;
        }

        loadPendingRequests();
        setupWebSocket();

        return () => {
            // Cleanup WebSocket if needed
        };
    }, [doctorId]);

    const loadPendingRequests = async () => {
        try {
            if (!doctorId) return;
            const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
            const response = await axios.get(`${apiUrl}/doctor/dashboard/pending-requests`, {
                params: { doctorId },
            });
            setRequests(response.data);
        } catch (error) {
            console.error('Failed to load pending requests', error);
        }
    };

    const setupWebSocket = () => {
        if (!doctorId) return;
        const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
        const wsUrl = 'http://localhost:8080';
        const socket = new SockJS(`${wsUrl}/ws`);
        const stompClient = new Client({
            webSocketFactory: () => socket as any,
            reconnectDelay: 5000,
        });

        stompClient.onConnect = () => {
            stompClient.subscribe(`/topic/doctor/${doctorId}/pending-requests`, () => {
                loadPendingRequests();
            });
        };

        stompClient.activate();
    };

    const handleConfirm = async (request: PendingRequest) => {
        try {
            const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
            await axios.post(
                `${apiUrl}/doctor/dashboard/requests/${request.id}/confirm`,
                { doctorId, durationMinutes: 30, remarks: '' }
            );
            loadPendingRequests();
        } catch (error: any) {
            if (error.response?.status === 409) {
                alert('Time slot conflict! Another appointment overlaps this time.');
            } else {
                alert('Failed to confirm request');
            }
        }
    };

    const handleReject = async () => {
        if (!selectedRequest) return;
        try {
            const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
            await axios.post(
                `${apiUrl}/doctor/dashboard/requests/${selectedRequest.id}/reject`,
                { doctorId, message: rejectionMessage }
            );
            setRejectDialogOpen(false);
            setRejectionMessage('');
            loadPendingRequests();
        } catch (error) {
            alert('Failed to reject request');
        }
    };

    const handleViewAddress = async (requestId: number, addressId: number) => {
        try {
            const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
            const response = await axios.get(`${apiUrl}/addresses/${addressId}`);
            setAddressDetails(response.data);
            setAddressDetailsOpen(true);
        } catch (e) {
            alert('Failed to fetch address details. Request has associated address ID: ' + addressId);
        }
    };

    return (
        <Container maxWidth="lg" sx={{ mt: 4 }}>
            <Paper sx={{ p: 3 }}>
                <Typography variant="h4" gutterBottom>
                    Pending Appointment Requests
                </Typography>
                <TableContainer>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Patient Phone</TableCell>
                                <TableCell>Patient Name</TableCell>
                                <TableCell>Date</TableCell>
                                <TableCell>Time</TableCell>
                                <TableCell>Description</TableCell>
                                <TableCell>Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {requests.map((request) => (
                                <TableRow key={request.id}>
                                    <TableCell>{request.patientPhone}</TableCell>
                                    <TableCell>{request.patientName}</TableCell>
                                    <TableCell>{request.requestedDate}</TableCell>
                                    <TableCell>{request.requestedStartTime}</TableCell>
                                    <TableCell>{request.description || '-'}</TableCell>
                                    <TableCell>
                                        {request.addressId && (
                                            <Button
                                                variant="outlined"
                                                size="small"
                                                onClick={() => handleViewAddress(request.id, request.addressId!)}
                                                sx={{ mr: 1 }}
                                            >
                                                Address
                                            </Button>
                                        )}
                                        <Button
                                            variant="contained"
                                            color="success"
                                            size="small"
                                            onClick={() => handleConfirm(request)}
                                            sx={{ mr: 1 }}
                                        >
                                            Confirm
                                        </Button>
                                        <Button
                                            variant="contained"
                                            color="error"
                                            size="small"
                                            onClick={() => {
                                                setSelectedRequest(request);
                                                setRejectDialogOpen(true);
                                            }}
                                        >
                                            Reject
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            {/* Reject Dialog */}
            <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)}>
                <DialogTitle>Reject Appointment</DialogTitle>
                <DialogContent>
                    <TextField
                        label="Rejection Message (Optional)"
                        multiline
                        rows={3}
                        value={rejectionMessage}
                        onChange={(e) => setRejectionMessage(e.target.value)}
                        fullWidth
                        sx={{ mt: 2 }}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
                    <Button onClick={handleReject} variant="contained" color="error">Reject</Button>
                </DialogActions>
            </Dialog>
            {/* Address Details Dialog */}
            <Dialog open={addressDetailsOpen} onClose={() => setAddressDetailsOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Patient Address</DialogTitle>
                <DialogContent>
                    {addressDetails ? (
                        <Box sx={{ mt: 1 }}>
                            <Typography variant="body1"><strong>Street:</strong> {addressDetails.addressLine1} {addressDetails.addressLine2}</Typography>
                            <Typography variant="body1"><strong>City:</strong> {addressDetails.city}</Typography>
                            <Typography variant="body1"><strong>State:</strong> {addressDetails.state}</Typography>
                            <Typography variant="body1"><strong>Zip:</strong> {addressDetails.postalCode}</Typography>
                            <Typography variant="body1"><strong>Country:</strong> {addressDetails.country}</Typography>
                        </Box>
                    ) : <Typography>Loading...</Typography>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setAddressDetailsOpen(false)}>Close</Button>
                </DialogActions>
            </Dialog>

        </Container>
    );
};

export default PendingRequests;
