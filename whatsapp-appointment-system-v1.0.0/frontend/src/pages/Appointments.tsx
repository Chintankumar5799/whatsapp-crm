import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Paper,
  Box,
  Grid,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Alert,
  Tooltip,
} from '@mui/material';
import EventIcon from '@mui/icons-material/Event';
import EditCalendarIcon from '@mui/icons-material/EditCalendar';
import CancelIcon from '@mui/icons-material/Cancel';
import HowToRegIcon from '@mui/icons-material/HowToReg';
import WhatsAppIcon from '@mui/icons-material/WhatsApp';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import Patient360Dialog from '../components/Patient360Dialog';
import PosBillingModal from '../components/PosBillingModal';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

interface BookingRecord {
  id: number;
  bookingNumber: string;
  patientId?: number;
  patientName: string;
  patientPhone: string;
  doctorId: number;
  bookingDate: string;
  startTime: string;
  endTime: string;
  status: string;
  caseType: string;
  appointmentSource: string;
  totalAmount: number;
  paymentStatus: string;
  notes?: string;
}

const Appointments: React.FC = () => {
  const navigate = useNavigate();
  const [doctorId, setDoctorId] = useState<number | ''>('');
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [bookings, setBookings] = useState<BookingRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [alertMsg, setAlertMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Reschedule Dialog State
  const [rescheduleDialogOpen, setRescheduleDialogOpen] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState<BookingRecord | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState('');
  const [rescheduleTime, setRescheduleTime] = useState('10:00');
  const [rescheduleReason, setRescheduleReason] = useState('Patient requested change of timing');

  // Cancel Dialog State
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('Patient unable to visit clinic');

  // Patient 360 Dialog State
  const [patient360Open, setPatient360Open] = useState(false);
  const [selectedPatient360Id, setSelectedPatient360Id] = useState<number | null>(null);

  const handleOpenPatient360 = (patientId: number) => {
    setSelectedPatient360Id(patientId);
    setPatient360Open(true);
  };

  // POS Billing Modal State
  const [posModalOpen, setPosModalOpen] = useState(false);
  const [selectedBillingBooking, setSelectedBillingBooking] = useState<BookingRecord | null>(null);

  const handleOpenBilling = (b: BookingRecord) => {
    setSelectedBillingBooking(b);
    setPosModalOpen(true);
  };

  const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
  const token = localStorage.getItem('token');
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  useEffect(() => {
    loadBookings();
  }, [selectedDate, doctorId, statusFilter]);

  const loadBookings = async () => {
    setLoading(true);
    try {
      const params: any = { clinicId: 1 };
      if (selectedDate) params.date = selectedDate;
      if (doctorId) params.doctorId = doctorId;
      if (statusFilter !== 'ALL') params.status = statusFilter;

      const res = await axios.get(`${apiUrl}/bookings`, { params, headers });
      setBookings(res.data);
    } catch (err) {
      console.error('Failed to load appointments', err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenReschedule = (b: BookingRecord) => {
    setSelectedBooking(b);
    setRescheduleDate(b.bookingDate || new Date().toISOString().split('T')[0]);
    setRescheduleTime(b.startTime ? b.startTime.substring(0, 5) : '10:00');
    setRescheduleDialogOpen(true);
  };

  const handleExecuteReschedule = async () => {
    if (!selectedBooking || !rescheduleDate) return;
    try {
      await axios.post(
        `${apiUrl}/bookings/${selectedBooking.id}/reschedule`,
        {
          newDate: rescheduleDate,
          newStartTime: rescheduleTime ? `${rescheduleTime}:00` : '10:00:00',
          newEndTime: '10:30:00',
          reason: rescheduleReason,
        },
        { headers }
      );
      setRescheduleDialogOpen(false);
      setAlertMsg({ type: 'success', text: `Appointment #${selectedBooking.id} rescheduled to ${rescheduleDate} at ${rescheduleTime}.` });
      loadBookings();
      setTimeout(() => setAlertMsg(null), 3000);
    } catch (err) {
      console.error('Reschedule failed', err);
      setAlertMsg({ type: 'error', text: 'Failed to reschedule appointment.' });
    }
  };

  const handleOpenCancel = (b: BookingRecord) => {
    setSelectedBooking(b);
    setCancelDialogOpen(true);
  };

  const handleExecuteCancel = async () => {
    if (!selectedBooking) return;
    try {
      await axios.post(
        `${apiUrl}/bookings/${selectedBooking.id}/cancel`,
        { reason: cancelReason },
        { headers }
      );
      setCancelDialogOpen(false);
      setAlertMsg({ type: 'success', text: `Appointment #${selectedBooking.id} cancelled.` });
      loadBookings();
      setTimeout(() => setAlertMsg(null), 3000);
    } catch (err) {
      console.error('Cancellation failed', err);
      setAlertMsg({ type: 'error', text: 'Failed to cancel appointment.' });
    }
  };

  const handleQuickQueueIn = async (b: BookingRecord) => {
    try {
      // Find patient ID or check in
      await axios.post(
        `${apiUrl}/clinical/queue/checkin?clinicId=1&doctorId=${b.doctorId || 1}&patientId=1&bookingId=${b.id}`,
        null,
        { headers }
      );
      setAlertMsg({ type: 'success', text: `Patient checked in to today's OPD waiting queue!` });
      setTimeout(() => setAlertMsg(null), 3000);
    } catch (err) {
      console.error('Queue check in failed', err);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED': return 'success';
      case 'PENDING': return 'warning';
      case 'COMPLETED': return 'info';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
      {/* Top Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Button
            startIcon={<ArrowBackIcon />}
            variant="outlined"
            size="small"
            onClick={() => navigate('/dashboard')}
          >
            Dashboard
          </Button>
          <Typography variant="h5" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <EventIcon color="primary" /> Clinic Appointment Calendar & Roster
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            onClick={() => setSelectedDate(new Date().toISOString().split('T')[0])}
          >
            Today
          </Button>
          <Button
            variant="outlined"
            onClick={() => {
              const tm = new Date();
              tm.setDate(tm.getDate() + 1);
              setSelectedDate(tm.toISOString().split('T')[0]);
            }}
          >
            Tomorrow
          </Button>
          <Button
            variant="outlined"
            onClick={() => setSelectedDate('')}
          >
            View All Dates
          </Button>
          <IconButton onClick={loadBookings} color="primary">
            <RefreshIcon />
          </IconButton>
        </Box>
      </Box>

      {alertMsg && (
        <Alert severity={alertMsg.type} sx={{ mb: 2 }}>
          {alertMsg.text}
        </Alert>
      )}

      {/* Filter Toolbar */}
      <Paper sx={{ p: 2, mb: 3, borderRadius: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="Appointment Date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>Doctor Filter</InputLabel>
              <Select
                value={doctorId}
                label="Doctor Filter"
                onChange={(e) => setDoctorId(e.target.value as any)}
              >
                <MenuItem value="">All Doctors</MenuItem>
                <MenuItem value={1}>Dr. Attending Physician</MenuItem>
                <MenuItem value={2}>Dr. Senior Consultant</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="ALL">All Statuses</MenuItem>
                <MenuItem value="CONFIRMED">CONFIRMED</MenuItem>
                <MenuItem value="PENDING">PENDING</MenuItem>
                <MenuItem value="COMPLETED">COMPLETED</MenuItem>
                <MenuItem value="CANCELLED">CANCELLED</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>

      {/* Appointments Table */}
      <Paper sx={{ borderRadius: 2 }}>
        <TableContainer>
          <Table size="small">
            <TableHead sx={{ bgcolor: '#f5f5f5' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 'bold' }}>ID / Token</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Date & Time</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Patient Name</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Phone</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Case Type</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Source</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
                <TableCell sx={{ fontWeight: 'bold' }}>Fee (₹)</TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold' }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {bookings.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={9} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No appointments found for the selected date and filters.
                  </TableCell>
                </TableRow>
              ) : (
                bookings.map((b) => (
                  <TableRow key={b.id} hover>
                    <TableCell sx={{ fontWeight: 'bold' }}>
                      {b.bookingNumber || `#${b.id}`}
                    </TableCell>
                    <TableCell>
                      {b.bookingDate} {b.startTime ? `| ${b.startTime.substring(0, 5)}` : ''}
                    </TableCell>
                    <TableCell sx={{ fontWeight: 500 }}>
                      {b.patientId ? (
                        <Button
                          size="small"
                          variant="text"
                          sx={{ p: 0, textTransform: 'none', fontWeight: 600 }}
                          onClick={() => handleOpenPatient360(b.patientId!)}
                        >
                          {b.patientName || 'Patient'} 🔍
                        </Button>
                      ) : (
                        b.patientName || 'Walk-in / Unregistered'
                      )}
                    </TableCell>
                    <TableCell>{b.patientPhone || '—'}</TableCell>
                    <TableCell>
                      <Chip
                        label={b.caseType || 'NEW_CASE'}
                        size="small"
                        color={b.caseType === 'OLD_CASE' ? 'info' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Chip label={b.appointmentSource || 'ONLINE'} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={b.status}
                        size="small"
                        color={getStatusColor(b.status) as any}
                      />
                    </TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>
                      ₹{b.totalAmount || 500}
                    </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'center' }}>
                        {b.status === 'CONFIRMED' && (
                          <Tooltip title="Check in to today's OPD waiting queue">
                            <Button
                              variant="outlined"
                              size="small"
                              color="primary"
                              startIcon={<HowToRegIcon />}
                              onClick={() => handleQuickQueueIn(b)}
                            >
                              Queue
                            </Button>
                          </Tooltip>
                        )}
                        {b.status !== 'CANCELLED' && b.status !== 'COMPLETED' && (
                          <Tooltip title="Reschedule Date / Time">
                            <IconButton
                              size="small"
                              color="primary"
                              onClick={() => handleOpenReschedule(b)}
                            >
                              <EditCalendarIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {b.status !== 'CANCELLED' && b.status !== 'COMPLETED' && (
                          <Tooltip title="Cancel Appointment">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleOpenCancel(b)}
                            >
                              <CancelIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {b.patientPhone && (
                          <Tooltip title="WhatsApp Patient">
                            <IconButton
                              size="small"
                              color="success"
                              href={`https://wa.me/${b.patientPhone.replace(/[^0-9]/g, '')}?text=${encodeURIComponent(`Hello ${b.patientName || 'Patient'}, confirming your appointment at PMS Healthcare Clinic on ${b.bookingDate} at ${b.startTime || 'scheduled time'}.`)}`}
                              target="_blank"
                            >
                              <WhatsAppIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        <Tooltip title="In-Clinic POS Cash Desk & Printable Receipt">
                          <IconButton
                            size="small"
                            color="success"
                            onClick={() => handleOpenBilling(b)}
                          >
                            <PointOfSaleIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Reschedule Dialog */}
      <Dialog open={rescheduleDialogOpen} onClose={() => setRescheduleDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight="bold">Reschedule Appointment</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Rescheduling for: <strong>{selectedBooking?.patientName}</strong> ({selectedBooking?.bookingNumber})
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                size="small"
                type="date"
                label="New Appointment Date"
                value={rescheduleDate}
                onChange={(e) => setRescheduleDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                size="small"
                type="time"
                label="New Time Slot"
                value={rescheduleTime}
                onChange={(e) => setRescheduleTime(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                size="small"
                label="Reason for Rescheduling"
                value={rescheduleReason}
                onChange={(e) => setRescheduleReason(e.target.value)}
                placeholder="e.g. Doctor schedule change, patient requested later slot"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRescheduleDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={handleExecuteReschedule}>
            Confirm Reschedule
          </Button>
        </DialogActions>
      </Dialog>

      {/* Cancel Dialog */}
      <Dialog open={cancelDialogOpen} onClose={() => setCancelDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight="bold">Cancel Appointment</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Are you sure you want to cancel the booking for <strong>{selectedBooking?.patientName}</strong>?
          </Typography>
          <TextField
            fullWidth
            size="small"
            label="Cancellation Reason"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder="e.g. Patient called to cancel, fever subsided"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelDialogOpen(false)}>Keep Appointment</Button>
          <Button variant="contained" color="error" onClick={handleExecuteCancel}>
            Confirm Cancellation
          </Button>
        </DialogActions>
      </Dialog>

      {/* Patient 360-Degree EHR Profile Modal */}
      <Patient360Dialog
        open={patient360Open}
        patientId={selectedPatient360Id}
        onClose={() => setPatient360Open(false)}
      />

      {/* In-Clinic POS Billing & Printable Receipt Modal */}
      {selectedBillingBooking && (
        <PosBillingModal
          open={posModalOpen}
          onClose={() => setPosModalOpen(false)}
          patientId={selectedBillingBooking.patientId || 1}
          patientName={selectedBillingBooking.patientName || 'Patient'}
          doctorId={selectedBillingBooking.doctorId || 1}
          bookingId={selectedBillingBooking.id}
          defaultFee={Number(selectedBillingBooking.totalAmount) || 500}
          onSuccess={loadBookings}
        />
      )}
    </Container>
  );
};

export default Appointments;
