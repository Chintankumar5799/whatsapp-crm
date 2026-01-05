import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Box,
  AppBar,
  Toolbar,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Stepper,
  Step,
  StepLabel,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  CircularProgress
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import axios from 'axios';
import AddressManager from '../components/AddressManager';

interface Doctor {
  id: number;
  name: string;
  specialization: string;
  qualification: string;
  phone: string;
}

interface Slot {
  id: number;
  startTime: string;
  endTime: string;
  isAvailable: boolean;
}

interface Address {
  id: number;
  addressLine1: string;
  city: string;
  isPrimary: boolean;
}

interface Booking {
  id: number;
  bookingDate: string;
  startTime: string;
  doctorName: string;
  status: string;
  paymentStatus?: string;
  paymentLink?: string;
  totalAmount?: number;
}

const steps = ['Select Doctor', 'Select Date & Time', 'Details & Address', 'Review & Submit'];

const PatientDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(false);
  const [specializationFilter, setSpecializationFilter] = useState<string>('');

  // Booking Wizard State
  const [wizardOpen, setWizardOpen] = useState(false);
  const [activeStep, setActiveStep] = useState(0);

  const storedUser = localStorage.getItem('user');
  const user = storedUser ? JSON.parse(storedUser) : {};
  const patientId = user?.userId || user?.id;

  const [bookingData, setBookingData] = useState({
    doctorId: null as number | null,
    doctorName: '',
    date: new Date() as Date | null,
    slotTime: null as string | null,
    addressId: null as number | null,
    description: '',
    patientName: user.name || user.username || '',
    patientPhone: user.phone || ''
  });

  const [availableSlots, setAvailableSlots] = useState<Slot[]>([]);
  const [userAddresses, setUserAddresses] = useState<Address[]>([]);
  const [addressManagerOpen, setAddressManagerOpen] = useState(false);

  useEffect(() => {
    if (!patientId) {
      navigate('/login');
      return;
    }
    loadDoctors();
    loadMyBookings();
    loadAddresses();
  }, [patientId, navigate]);

  const loadDoctors = async () => {
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const res = await axios.get(`${apiUrl}/patient/bookings/doctors`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setDoctors(res.data);
    } catch (err) {
      console.error("Failed to load doctors", err);
    }
  };

  const loadMyBookings = async () => {
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const res = await axios.get(`${apiUrl}/patient/bookings/list`, {
        params: { patientId },
        headers: { Authorization: `Bearer ${token}` }
      });
      setBookings(res.data);
    } catch (err) {
      console.error("Failed to load bookings", err);
    }
  };

  const loadAddresses = async () => {
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const res = await axios.get(`${apiUrl}/addresses/user/${patientId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setUserAddresses(res.data);
      if (!bookingData.addressId) {
        const primary = res.data.find((a: any) => a.isPrimary);
        if (primary) setBookingData(prev => ({ ...prev, addressId: primary.id }));
      }
    } catch (err) {
      console.error("Failed to load addresses", err);
    }
  };

  const activeBooking = bookings.find(b =>
    ['PENDING', 'CONFIRMED', 'ACCEPTED', 'PAID'].includes(b.status) && b.status !== 'COMPLETED'
  );

  const handleStartBooking = (doctor: Doctor) => {
    setBookingData(prev => ({
      ...prev,
      doctorId: doctor.id,
      doctorName: doctor.name,
    }));
    setActiveStep(1); // Skip step 0 (Select Doctor) as we selected from table
    setWizardOpen(true);
  };

  const handleDateChange = async (date: Date | null) => {
    setBookingData(prev => ({ ...prev, date, slotTime: null }));
    if (date && bookingData.doctorId) {
      try {
        const token = localStorage.getItem('token');
        const dateStr = date.toISOString().split('T')[0];
        const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
        const res = await axios.get(`${apiUrl}/patient/bookings/slots/available`, {
          params: { doctorId: bookingData.doctorId, date: dateStr },
          headers: { Authorization: `Bearer ${token}` }
        });
        setAvailableSlots(res.data);
      } catch (err) {
        console.error(err);
      }
    }
  };

  const handleSubmitRequest = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const doc = doctors.find(d => d.id === bookingData.doctorId);

      const payload = {
        doctorPhone: doc?.phone || '',
        patientPhone: bookingData.patientPhone,
        patientName: bookingData.patientName,
        requestedDate: bookingData.date?.toISOString().split('T')[0],
        requestedStartTime: bookingData.slotTime,
        description: bookingData.description,
        addressId: bookingData.addressId
      };

      await axios.post(`${apiUrl}/patient/bookings/request`, payload, {
        headers: { Authorization: `Bearer ${token}` }
      });
      alert('Request submitted successfully!');
      setWizardOpen(false);
      loadMyBookings();
    } catch (err: any) {
      console.error("Booking failed", err);
      const errorMessage = err.response?.data?.message || err.response?.data?.error || err.message || 'Failed to submit booking request';
      alert(`Error: ${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const handleAddressManagerClose = () => {
    setAddressManagerOpen(false);
    loadAddresses(); // Refresh addresses
  };

  // Filter Logic
  const uniqueSpecializations = Array.from(new Set(doctors.map(d => d.specialization))).filter(Boolean);
  const filteredDoctors = doctors.filter(d =>
    !specializationFilter || d.specialization === specializationFilter
  );

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: '#f4f6f8' }}>
      <AppBar position="static" sx={{ backgroundColor: '#2e7d32' }}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Patient Portal
          </Typography>
          <Button color="inherit" onClick={() => setAddressManagerOpen(true)}>Manage Addresses</Button>
          <Button color="inherit" onClick={handleLogout}>Logout</Button>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Box sx={{ mb: 4 }}>
          <Typography variant="h4" gutterBottom>Welcome, {bookingData.patientName}</Typography>
          <Typography variant="body1" color="textSecondary">Manage your health and appointments.</Typography>
        </Box>

        <Grid container spacing={3}>
          {activeBooking && (
            <Grid item xs={12}>
              <Paper sx={{ p: 3, borderLeft: '6px solid #2e7d32', backgroundColor: '#e8f5e9' }}>
                <Typography variant="h6" color="primary" gutterBottom>Active Appointment</Typography>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={3}>
                    <Typography variant="caption" color="textSecondary">Doctor</Typography>
                    <Typography variant="body1" fontWeight="bold">{activeBooking.doctorName}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <Typography variant="caption" color="textSecondary">Date & Time</Typography>
                    <Typography variant="body1" fontWeight="bold">{activeBooking.bookingDate} @ {activeBooking.startTime}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <Typography variant="caption" color="textSecondary">Status</Typography>
                    <Chip label={activeBooking.status} color="success" size="small" />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    {activeBooking.paymentLink && activeBooking.status !== 'PAID' && (
                      <Button variant="contained" color="secondary" size="small" href={activeBooking.paymentLink} target="_blank">
                        Pay Now
                      </Button>
                    )}
                  </Grid>
                </Grid>
              </Paper>
            </Grid>
          )}

          <Grid item xs={12} md={8}>
            <Typography variant="h5" gutterBottom sx={{ mt: 2, fontWeight: 600 }}>Available Doctors</Typography>

            {/* Specialization Filter */}
            <Paper sx={{ p: 2, mb: 2 }}>
              <FormControl size="small" fullWidth>
                <InputLabel>Filter by Specialization</InputLabel>
                <Select
                  value={specializationFilter}
                  label="Filter by Specialization"
                  onChange={(e) => setSpecializationFilter(e.target.value)}
                >
                  <MenuItem value="">All Specializations</MenuItem>
                  {uniqueSpecializations.map(spec => (
                    <MenuItem key={spec} value={spec}>{spec}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Paper>

            <Paper>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow sx={{ backgroundColor: '#eeeeee' }}>
                      <TableCell>#</TableCell>
                      <TableCell>Doctor Name</TableCell>
                      <TableCell>Specialization</TableCell>
                      <TableCell align="center">Action</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {filteredDoctors.map((doc, index) => (
                      <TableRow key={doc.id} hover>
                        <TableCell>{index + 1}</TableCell>
                        <TableCell>
                          <Typography variant="subtitle2">{doc.name}</Typography>
                          <Typography variant="caption" color="textSecondary">{doc.qualification}</Typography>
                        </TableCell>
                        <TableCell>
                          <Chip label={doc.specialization} size="small" variant="outlined" />
                        </TableCell>
                        <TableCell align="center">
                          <Button
                            variant="contained"
                            size="small"
                            sx={{ backgroundColor: '#2e7d32' }}
                            onClick={() => handleStartBooking(doc)}
                          >
                            Book
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                    {filteredDoctors.length === 0 && (
                      <TableRow><TableCell colSpan={4} align="center">No doctors found.</TableCell></TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Paper>
          </Grid>

          <Grid item xs={12} md={4}>
            <Typography variant="h6" gutterBottom sx={{ mt: 2, fontWeight: 600 }}>Booking History</Typography>
            <Paper elevation={0} variant="outlined">
              <TableContainer sx={{ maxHeight: 400 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Date</TableCell>
                      <TableCell>Doctor</TableCell>
                      <TableCell>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {bookings.map(b => (
                      <TableRow key={b.id} hover>
                        <TableCell>{b.bookingDate}</TableCell>
                        <TableCell>{b.doctorName}</TableCell>
                        <TableCell>
                          <Typography variant="caption" sx={{
                            fontWeight: 'bold',
                            color: b.status === 'COMPLETED' ? 'blue' : b.status === 'CANCELLED' ? 'red' : 'green'
                          }}>
                            {b.status}
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ))}
                    {bookings.length === 0 && (
                      <TableRow><TableCell colSpan={3} align="center">No history found.</TableCell></TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Paper>
          </Grid>
        </Grid>
      </Container>

      {/* Booking Wizard */}
      <Dialog open={wizardOpen} onClose={() => setWizardOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Book Appointment</DialogTitle>
        <DialogContent dividers>
          <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 3 }}>
            {steps.map(label => <Step key={label}><StepLabel>{label}</StepLabel></Step>)}
          </Stepper>

          {activeStep === 1 && (
            <Box>
              <Typography variant="subtitle1" gutterBottom fontWeight="bold">Select Date for Dr. {bookingData.doctorName}</Typography>
              <LocalizationProvider dateAdapter={AdapterDateFns}>
                <DatePicker
                  label="Appointment Date"
                  value={bookingData.date}
                  onChange={handleDateChange}
                  minDate={new Date()}
                  sx={{ width: '100%', mb: 2 }}
                />
              </LocalizationProvider>

              <Typography variant="subtitle1" gutterBottom fontWeight="bold">Available Slots</Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', maxHeight: 150, overflowY: 'auto' }}>
                {availableSlots.length > 0 ? availableSlots.filter(s => s.isAvailable).map(slot => (
                  <Chip
                    key={slot.id}
                    label={slot.startTime}
                    clickable
                    color={bookingData.slotTime === slot.startTime ? 'primary' : 'default'}
                    onClick={() => setBookingData(prev => ({ ...prev, slotTime: slot.startTime }))}
                  />
                )) : (
                  <Typography variant="body2" color="textSecondary">Select a date to view slots.</Typography>
                )}
              </Box>
            </Box>
          )}

          {activeStep === 2 && (
            <Box>
              <Typography variant="subtitle1" gutterBottom fontWeight="bold">Patient Details</Typography>
              <TextField
                label="Patient Name"
                fullWidth
                size="small"
                margin="dense"
                value={bookingData.patientName}
                onChange={(e) => setBookingData(prev => ({ ...prev, patientName: e.target.value }))}
              />
              <TextField
                label="Patient Phone"
                fullWidth
                size="small"
                margin="dense"
                value={bookingData.patientPhone}
                onChange={(e) => setBookingData(prev => ({ ...prev, patientPhone: e.target.value }))}
                helperText="Required for WhatsApp updates"
              />

              <Typography variant="subtitle1" gutterBottom fontWeight="bold" sx={{ mt: 2 }}>Select Address</Typography>
              {userAddresses.length > 0 ? (
                <FormControl fullWidth margin="dense" size="small">
                  <InputLabel>Saved Addresses</InputLabel>
                  <Select
                    value={bookingData.addressId || ''}
                    label="Saved Addresses"
                    onChange={(e) => setBookingData(prev => ({ ...prev, addressId: Number(e.target.value) }))}
                  >
                    {userAddresses.map(addr => (
                      <MenuItem key={addr.id} value={addr.id}>
                        {addr.addressLine1}, {addr.city} {addr.isPrimary ? '(Primary)' : ''}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              ) : (
                <Typography color="warning.main" variant="body2">No addresses found.</Typography>
              )}

              <Button size="small" variant="text" onClick={() => setAddressManagerOpen(true)}>
                + Add New Address
              </Button>

              <TextField
                fullWidth
                multiline
                rows={3}
                label="Reason for Visit / Symptoms"
                margin="normal"
                value={bookingData.description}
                onChange={(e) => setBookingData(prev => ({ ...prev, description: e.target.value }))}
              />
            </Box>
          )}

          {activeStep === 3 && (
            <Box sx={{ p: 2, backgroundColor: '#f9f9f9', borderRadius: 1 }}>
              <Typography variant="h6" gutterBottom color="primary">Review Booking</Typography>
              <Grid container spacing={1}>
                <Grid item xs={4}><Typography variant="body2" color="textSecondary">Doctor:</Typography></Grid>
                <Grid item xs={8}><Typography variant="body2" fontWeight="bold">{bookingData.doctorName}</Typography></Grid>

                <Grid item xs={4}><Typography variant="body2" color="textSecondary">Date & Time:</Typography></Grid>
                <Grid item xs={8}><Typography variant="body2" fontWeight="bold">{bookingData.date?.toLocaleDateString()} at {bookingData.slotTime}</Typography></Grid>

                <Grid item xs={4}><Typography variant="body2" color="textSecondary">Patient:</Typography></Grid>
                <Grid item xs={8}><Typography variant="body2">{bookingData.patientName} ({bookingData.patientPhone})</Typography></Grid>

                <Grid item xs={4}><Typography variant="body2" color="textSecondary">Address:</Typography></Grid>
                <Grid item xs={8}><Typography variant="body2">{userAddresses.find(a => a.id === bookingData.addressId)?.addressLine1 || 'Selected Address'}</Typography></Grid>

                <Grid item xs={4}><Typography variant="body2" color="textSecondary">Reason:</Typography></Grid>
                <Grid item xs={8}><Typography variant="body2">{bookingData.description || 'N/A'}</Typography></Grid>
              </Grid>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setWizardOpen(false)}>Cancel</Button>
          {activeStep > 1 && <Button onClick={() => setActiveStep(prev => prev - 1)}>Back</Button>}
          {activeStep < 3 ? (
            <Button
              variant="contained"
              onClick={() => setActiveStep(prev => prev + 1)}
              disabled={
                (activeStep === 1 && !bookingData.slotTime) ||
                (activeStep === 2 && (!bookingData.patientPhone || !bookingData.patientName || !bookingData.addressId))
              }
            >
              Next
            </Button>
          ) : (
            <Button variant="contained" color="success" onClick={handleSubmitRequest} disabled={loading}>
              {loading ? <CircularProgress size={24} color="inherit" /> : 'Submit Request'}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <Dialog open={addressManagerOpen} onClose={handleAddressManagerClose} fullWidth maxWidth="md">
        <DialogTitle>Manage Addresses</DialogTitle>
        <DialogContent>
          <AddressManager userId={patientId} />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleAddressManagerClose}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PatientDashboard;
