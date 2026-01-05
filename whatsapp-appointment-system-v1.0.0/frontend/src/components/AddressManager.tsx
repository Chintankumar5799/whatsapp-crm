import React, { useState, useEffect } from 'react';
import {
    Box,
    Typography,
    Button,
    TextField,
    Card,
    CardContent,
    CardActions,
    Grid,
    Checkbox,
    FormControlLabel,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    IconButton
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import AddIcon from '@mui/icons-material/Add';
import axios from 'axios';

interface Address {
    id?: number;
    userId: number;
    addressLine1: string;
    addressLine2?: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
    isPrimary: boolean;
}

interface AddressManagerProps {
    userId: number;
    onClose?: () => void;
}

const AddressManager: React.FC<AddressManagerProps> = ({ userId, onClose }) => {
    const [addresses, setAddresses] = useState<Address[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // Dialog State
    const [openForm, setOpenForm] = useState(false);
    const [currentAddress, setCurrentAddress] = useState<Partial<Address>>({});
    const [isEditing, setIsEditing] = useState(false);

    useEffect(() => {
        if (userId) {
            loadAddresses();
        }
    }, [userId]);

    const loadAddresses = async () => {
        setLoading(true);
        try {
            const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
            const response = await axios.get(`${apiUrl}/addresses/user/${userId}`);
            setAddresses(response.data);
        } catch (err) {
            console.error('Failed to load addresses', err);
            setError('Failed to load addresses');
        } finally {
            setLoading(false);
        }
    };

    const handleAddNew = () => {
        setCurrentAddress({
            userId,
            country: 'India',
            isPrimary: addresses.length === 0 // Default to primary if first address
        });
        setIsEditing(false);
        setOpenForm(true);
    };

    const handleEdit = (addr: Address) => {
        setCurrentAddress(addr);
        setIsEditing(true);
        setOpenForm(true);
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm('Are you sure you want to delete this address?')) return;
        try {
            const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
            await axios.delete(`${apiUrl}/addresses/${id}`);
            loadAddresses();
        } catch (err) {
            alert('Failed to delete address');
        }
    };

    const handleSave = async () => {
        try {
            const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
            // Basic validation
            if (!currentAddress.addressLine1 || !currentAddress.city || !currentAddress.postalCode) {
                alert('Please fill required fields (Address Line 1, City, Zip)');
                return;
            }

            const payload = { ...currentAddress, userId };

            if (isEditing && currentAddress.id) {
                await axios.put(`${apiUrl}/addresses/${currentAddress.id}`, payload);
            } else {
                await axios.post(`${apiUrl}/addresses/user/${userId}`, payload);
            }
            setOpenForm(false);
            loadAddresses();
        } catch (err) {
            console.error('Failed to save address', err);
            alert('Failed to save address. Check console for details.');
        }
    };

    return (
        <Box sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">My Addresses</Typography>
                <Button startIcon={<AddIcon />} variant="contained" onClick={handleAddNew}>
                    Add New
                </Button>
            </Box>

            {loading ? (
                <Typography>Loading...</Typography>
            ) : addresses.length === 0 ? (
                <Typography color="textSecondary">No addresses found.</Typography>
            ) : (
                <Grid container spacing={2}>
                    {addresses.map((addr) => (
                        <Grid item xs={12} sm={6} key={addr.id}>
                            <Card variant="outlined" sx={{ position: 'relative', borderColor: addr.isPrimary ? 'primary.main' : 'grey.300' }}>
                                <CardContent>
                                    {addr.isPrimary && (
                                        <Typography variant="caption" sx={{ color: 'primary.main', fontWeight: 'bold' }}>
                                            PRIMARY
                                        </Typography>
                                    )}
                                    <Typography variant="body1" fontWeight="500">{addr.addressLine1}</Typography>
                                    {addr.addressLine2 && <Typography variant="body2">{addr.addressLine2}</Typography>}
                                    <Typography variant="body2">{addr.city}, {addr.state} - {addr.postalCode}</Typography>
                                    <Typography variant="body2">{addr.country}</Typography>
                                </CardContent>
                                <CardActions disableSpacing sx={{ justifyContent: 'flex-end' }}>
                                    <IconButton size="small" onClick={() => handleEdit(addr)}>
                                        <EditIcon fontSize="small" />
                                    </IconButton>
                                    <IconButton size="small" color="error" onClick={() => handleDelete(addr.id!)}>
                                        <DeleteIcon fontSize="small" />
                                    </IconButton>
                                </CardActions>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            )}

            {/* Add/Edit Dialog */}
            <Dialog open={openForm} onClose={() => setOpenForm(false)} maxWidth="sm" fullWidth>
                <DialogTitle>{isEditing ? 'Edit Address' : 'Add New Address'}</DialogTitle>
                <DialogContent>
                    <Box sx={{ mt: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
                        <TextField
                            label="Address Line 1"
                            value={currentAddress.addressLine1 || ''}
                            onChange={(e) => setCurrentAddress({ ...currentAddress, addressLine1: e.target.value })}
                            fullWidth
                            required
                        />
                        <TextField
                            label="Address Line 2"
                            value={currentAddress.addressLine2 || ''}
                            onChange={(e) => setCurrentAddress({ ...currentAddress, addressLine2: e.target.value })}
                            fullWidth
                        />
                        <Grid container spacing={2}>
                            <Grid item xs={6}>
                                <TextField
                                    label="City"
                                    value={currentAddress.city || ''}
                                    onChange={(e) => setCurrentAddress({ ...currentAddress, city: e.target.value })}
                                    fullWidth
                                    required
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <TextField
                                    label="State"
                                    value={currentAddress.state || ''}
                                    onChange={(e) => setCurrentAddress({ ...currentAddress, state: e.target.value })}
                                    fullWidth
                                    required
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <TextField
                                    label="Postal Code"
                                    value={currentAddress.postalCode || ''}
                                    onChange={(e) => setCurrentAddress({ ...currentAddress, postalCode: e.target.value })}
                                    fullWidth
                                    required
                                />
                            </Grid>
                            <Grid item xs={6}>
                                <TextField
                                    label="Country"
                                    value={currentAddress.country || 'India'}
                                    onChange={(e) => setCurrentAddress({ ...currentAddress, country: e.target.value })}
                                    fullWidth
                                    required
                                />
                            </Grid>
                        </Grid>
                        <FormControlLabel
                            control={
                                <Checkbox
                                    checked={currentAddress.isPrimary || false}
                                    onChange={(e) => setCurrentAddress({ ...currentAddress, isPrimary: e.target.checked })}
                                />
                            }
                            label="Set as Primary Address"
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpenForm(false)}>Cancel</Button>
                    <Button onClick={handleSave} variant="contained">Save</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AddressManager;
