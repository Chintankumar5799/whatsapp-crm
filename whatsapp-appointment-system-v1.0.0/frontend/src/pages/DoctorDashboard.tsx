import React, { useEffect, useState } from 'react';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Box,
  TextField,
  AppBar,
  Toolbar,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Menu,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import AccountCircle from '@mui/icons-material/AccountCircle';
import AddressManager from '../components/AddressManager';
import { useNavigate } from 'react-router-dom';
import { LocalizationProvider, DatePicker } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, LineChart, Line, ResponsiveContainer } from 'recharts';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { doctorTheme } from '../theme/dashboardThemes';

interface DashboardMetrics {
  slotsBooked: number;
  customersAttendedToday: number;
  chartData?: {
    period: string;
    dataPoints: Array<{ label: string; count: number; amount?: number }>;
  };
}

interface Appointment {
  id: number;
  patientId: number;
  startTime: string;
  patientName: string;
  patientPhone: string;
  status: string;
  totalAmount?: number;
  paymentStatus?: string;
  paymentLink?: string;
}

const DoctorDashboard: React.FC = () => {
  const storedUser = localStorage.getItem('user');
  const parsedUser = storedUser ? JSON.parse(storedUser) : null;
  const role = parsedUser?.role;
  // Patient check removed, this is predominantly Doctor logic now


  // Doctor Dashboard Logic
  const navigate = useNavigate();
  // ... rest of doctor logic ...

  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [metrics, setMetrics] = useState<DashboardMetrics>({
    slotsBooked: 0,
    customersAttendedToday: 0,
  });
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [chartPeriod, setChartPeriod] = useState('daily');
  const [dailyData, setDailyData] = useState<any[]>([]);
  const [weeklyData, setWeeklyData] = useState<any[]>([]);

  // Patient Search State
  const [searchPhone, setSearchPhone] = useState('');
  const [patientHistory, setPatientHistory] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  // Payment Modal State
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState<Appointment | null>(null);
  const [paymentAmount, setPaymentAmount] = useState<number | string>(500); // Default amount
  const [notes, setNotes] = useState('');

  // Completion Dialog State
  const [completionDialogOpen, setCompletionDialogOpen] = useState(false);
  const [completionRemarks, setCompletionRemarks] = useState('');
  const [bookingToComplete, setBookingToComplete] = useState<Appointment | null>(null);

  // User Menu & Address Dialog State
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [addressDialogOpen, setAddressDialogOpen] = useState(false);

  // Get logged-in user info
  const doctorId = parsedUser?.userId || parsedUser?.id;
  const username = parsedUser?.username;
  // role is already parsed at top

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token || !doctorId) {
      navigate('/login');
      return;
    }

    loadDashboardData();
    setupWebSocket();

    return () => {
      // Cleanup WebSocket
    };
  }, [selectedDate, chartPeriod, navigate]);

  const loadDashboardData = async () => {
    try {
      const token = localStorage.getItem('token');
      const dateStr = selectedDate.toISOString().split('T')[0];
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

      // Load metrics
      const metricsRes = await axios.get(`${apiUrl}/doctor/dashboard/metrics`, {
        params: { doctorId, date: dateStr },
        headers: { Authorization: `Bearer ${token}` }
      });
      setMetrics(metricsRes.data);

      // Load appointments
      const appointmentsRes = await axios.get(`${apiUrl}/doctor/dashboard/bookings`, {
        params: { doctorId, date: dateStr },
        headers: { Authorization: `Bearer ${token}` }
      });
      setAppointments(appointmentsRes.data);

      // Load chart data
      const chartRes = await axios.get(`${apiUrl}/doctor/dashboard/metrics/charts`, {
        params: { doctorId, period: chartPeriod, startDate: dateStr },
        headers: { Authorization: `Bearer ${token}` }
      });
      if (chartPeriod === 'daily') {
        setDailyData(chartRes.data.dataPoints);
      } else {
        setWeeklyData(chartRes.data.dataPoints);
      }
    } catch (error) {
      console.error('Failed to load dashboard data', error);
    }
  };

  const setupWebSocket = () => {
    const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
    const wsUrl = 'http://localhost:8080';
    const socket = new SockJS(`${wsUrl}/ws`);
    const stompClient = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
    });

    stompClient.onConnect = () => {
      stompClient.subscribe(`/topic/doctor/${doctorId}/pending-requests`, () => {
        loadDashboardData();
      });
    };

    stompClient.activate();
  };

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleAddressClick = () => {
    handleMenuClose();
    setAddressDialogOpen(true);
  };

  const handleLogout = () => {
    handleMenuClose();
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const handleMarkCompleted = (booking: Appointment) => {
    setBookingToComplete(booking);
    setCompletionRemarks(''); // Reset
    setCompletionDialogOpen(true);
  };

  const handleSubmitCompletion = async () => {
    if (!bookingToComplete) return;
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      await axios.post(`${apiUrl}/doctor/dashboard/bookings/${bookingToComplete.id}/complete`,
        completionRemarks,
        {
          headers: {
            'Content-Type': 'text/plain',
            'Authorization': `Bearer ${token}`
          }
        }
      );
      setCompletionDialogOpen(false);
      loadDashboardData();
    } catch (error) {
      console.error('Failed to mark completed', error);
      alert('Failed to mark appointment as completed');
    }
  };

  const handleSearchPatient = async () => {
    if (!searchPhone) return;
    setSearching(true);
    setHasSearched(true);
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const response = await axios.get(`${apiUrl}/doctor/dashboard/patients/search`, {
        params: { phone: searchPhone },
        headers: { Authorization: `Bearer ${token}` }
      });
      setPatientHistory(response.data);
    } catch (e) {
      console.error("Search failed", e);
      setPatientHistory([]);
    } finally {
      setSearching(false);
    }
  };

  const handleOpenPaymentModal = (booking: Appointment) => {
    setSelectedBooking(booking);
    setPaymentAmount(booking.totalAmount || 500);
    setPaymentModalOpen(true);
  };

  const handleCreatePaymentLink = async () => {
    if (!selectedBooking) return;

    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const request = {
        bookingId: selectedBooking.id,
        patientId: (selectedBooking as any).patientId,
        amount: Number(paymentAmount),
        currency: "INR",
        description: `Consultation fee`,
      };

      await axios.post(`${apiUrl}/payments/links`, request, {
        headers: { Authorization: `Bearer ${token}` }
      });

      setPaymentModalOpen(false);
      loadDashboardData();
      alert('Payment link generated and sent to patient!');
    } catch (error) {
      console.error('Failed to create payment link', error);
      alert('Failed to generate payment link');
    }
  };

  return (
    <Box sx={{ backgroundColor: doctorTheme.background, minHeight: '100vh' }}>
      <AppBar position="static" sx={{ backgroundColor: doctorTheme.primary, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 600 }}>
            Appointment Dashboard
          </Typography>
          <Button
            color="inherit"
            onClick={() => navigate('/pending-requests')}
            sx={{
              mr: 2,
              borderRadius: '8px',
              px: 3,
              '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' }
            }}
          >
            Pending Requests
          </Button>
          {username && role && (
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Box sx={{ mr: 2, textAlign: 'right' }}>
                <Typography variant="body2" sx={{ fontWeight: 500 }}>{username}</Typography>
                <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.8)', fontSize: '0.7rem' }}>
                  {role}
                </Typography>
              </Box>
              <IconButton
                size="large"
                aria-label="account of current user"
                aria-controls="menu-appbar"
                aria-haspopup="true"
                onClick={handleMenuClick}
                color="inherit"
              >
                <AccountCircle />
              </IconButton>
              <Menu
                id="menu-appbar"
                anchorEl={anchorEl}
                anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                keepMounted
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                open={Boolean(anchorEl)}
                onClose={handleMenuClose}
              >
                <MenuItem onClick={handleAddressClick}>Manage Addresses</MenuItem>
                <MenuItem onClick={handleLogout}>Logout</MenuItem>
              </Menu>
            </Box>
          )}
          {(!username || !role) && (
            <Button color="inherit" onClick={handleLogout}>Logout</Button>
          )}
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Grid container spacing={3}>
          {/* Patient Search Section */}
          <Grid item xs={12}>
            <Paper sx={{ p: 3, borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
              <Typography variant="h6" gutterBottom sx={{ color: doctorTheme.text, fontWeight: 600 }}>
                Search Patient History
              </Typography>
              <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
                <TextField
                  label="Patient Mobile Number"
                  variant="outlined"
                  size="small"
                  fullWidth
                  value={searchPhone}
                  onChange={(e) => setSearchPhone(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSearchPatient()}
                />
                <Button variant="contained" onClick={handleSearchPatient} disabled={searching} sx={{ backgroundColor: doctorTheme.primaryButton }}>
                  {searching ? 'Searching...' : 'Search'}
                </Button>
              </Box>
              {hasSearched && (
                <TableContainer sx={{ border: '1px solid #eee', borderRadius: '8px', maxHeight: '300px' }}>
                  <Table size="small" stickyHeader>
                    <TableHead>
                      <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                        <TableCell sx={{ fontWeight: 600 }}>Date</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Patient Description</TableCell>
                        <TableCell sx={{ fontWeight: 600 }}>Doctor Remarks</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {patientHistory.length > 0 ? (
                        patientHistory.map((visit) => (
                          <TableRow key={visit.id} hover>
                            <TableCell>{visit.bookingDate}</TableCell>
                            <TableCell>{visit.status}</TableCell>
                            <TableCell>{visit.diseaseDescription || '-'}</TableCell>
                            <TableCell>{visit.doctorNotes || '-'}</TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow><TableCell colSpan={4} align="center">No history found.</TableCell></TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Paper>
          </Grid>
          {/* Date Picker Row */}
          <Grid item xs={12} md={6}>
            <Typography variant="h6" gutterBottom>Select Date</Typography>
          </Grid>
          <Grid item xs={12} md={6}>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <DatePicker
                label="Select Date"
                value={selectedDate}
                onChange={(newValue) => setSelectedDate(newValue || new Date())}
                sx={{ width: '100%' }}
              />
            </LocalizationProvider>
          </Grid>
          {/* Summary Cards */}
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3, textAlign: 'center', borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
              <Typography variant="h3" sx={{ color: doctorTheme.primary, fontWeight: 700 }}>{metrics.slotsBooked}</Typography>
              <Typography variant="body1" color="textSecondary">Slots Booked</Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3, textAlign: 'center', borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
              <Typography variant="h3" sx={{ color: doctorTheme.accent, fontWeight: 700 }}>{metrics.customersAttendedToday}</Typography>
              <Typography variant="body1" color="textSecondary">Customers</Typography>
            </Paper>
          </Grid>
          {/* Charts */}
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3, borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
              <Typography variant="h6" gutterBottom sx={{ color: doctorTheme.text, fontWeight: 600 }}>Daily Bookings</Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={dailyData}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis /><Tooltip /><Legend /><Bar dataKey="count" fill="#8884d8" /></BarChart>
              </ResponsiveContainer>
            </Paper>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3, borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="h6">Booking Amount</Typography>
                <FormControl size="small" sx={{ minWidth: 120 }}>
                  <InputLabel>Period</InputLabel>
                  <Select value={chartPeriod} label="Period" onChange={(e) => setChartPeriod(e.target.value)}>
                    <MenuItem value="daily">Daily</MenuItem>
                    <MenuItem value="weekly">Weekly</MenuItem>
                  </Select>
                </FormControl>
              </Box>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={chartPeriod === 'daily' ? dailyData : weeklyData}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis /><Tooltip /><Legend /><Line type="monotone" dataKey="count" stroke="#8884d8" /></LineChart>
              </ResponsiveContainer>
            </Paper>
          </Grid>
          {/* Appointments Table */}
          <Grid item xs={12}>
            <Paper sx={{ p: 3, borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="h6" sx={{ color: doctorTheme.text, fontWeight: 600 }}>Appointments</Typography>
                <Typography variant="body2" color="textSecondary">{metrics.slotsBooked} slots booked</Typography>
              </Box>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow sx={{ backgroundColor: doctorTheme.primary }}>
                      <TableCell sx={{ color: '#fff', fontWeight: 600 }}>Time</TableCell>
                      <TableCell sx={{ color: '#fff', fontWeight: 600 }}>Mobile</TableCell>
                      <TableCell sx={{ color: '#fff', fontWeight: 600 }}>Patient</TableCell>
                      <TableCell sx={{ color: '#fff', fontWeight: 600 }}>Status</TableCell>
                      <TableCell sx={{ color: '#fff', fontWeight: 600 }}>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {appointments.length === 0 ? (
                      <TableRow><TableCell colSpan={5} align="center">No appointments for this date</TableCell></TableRow>
                    ) : (
                      appointments.map((appt) => (
                        <TableRow key={appt.id}>
                          <TableCell>{appt.startTime}</TableCell>
                          <TableCell>{appt.patientPhone || 'N/A'}</TableCell>
                          <TableCell>{appt.patientName || 'N/A'}</TableCell>
                          <TableCell>{appt.status}</TableCell>
                          <TableCell>
                            {appt.status !== 'COMPLETED' && appt.status !== 'PAID' && (
                              <Button
                                variant="contained"
                                size="small"
                                onClick={() => handleMarkCompleted(appt)}
                                sx={{ mr: 1, backgroundColor: doctorTheme.accent, '&:hover': { backgroundColor: '#E67E22' }, borderRadius: '8px', textTransform: 'none', fontWeight: 600 }}
                              >
                                Completed
                              </Button>
                            )}
                            <Button
                              variant="outlined"
                              size="small"
                              disabled={appt.status === 'PAID'}
                              onClick={() => handleOpenPaymentModal(appt)}
                              sx={{
                                borderColor: appt.status === 'PAID' ? '#00B894' : doctorTheme.primaryButton,
                                color: appt.status === 'PAID' ? '#00B894' : doctorTheme.primaryButton,
                                backgroundColor: appt.status === 'PAID' ? 'rgba(0, 184, 148, 0.1)' : 'transparent',
                                borderRadius: '8px',
                                textTransform: 'none',
                                fontWeight: 600,
                              }}
                            >
                              {appt.status === 'PAID' ? 'PAID' : (appt.paymentLink ? 'View Payment' : 'Payment')}
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Paper>
          </Grid>
        </Grid>
        {/* Payment Modal */}
        {paymentModalOpen && (
          <div style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', backgroundColor: 'white', padding: '20px', zIndex: 1000, boxShadow: '0 4px 8px rgba(0,0,0,0.2)', borderRadius: '8px', width: '300px' }}>
            <Typography variant="h6" gutterBottom>Generate Payment Link</Typography>
            <TextField fullWidth label="Amount (INR)" type="number" value={paymentAmount} onChange={(e) => setPaymentAmount(e.target.value)} sx={{ mb: 2 }} />
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
              <Button onClick={() => setPaymentModalOpen(false)}>Cancel</Button>
              <Button variant="contained" onClick={handleCreatePaymentLink}>OK</Button>
            </Box>
          </div>
        )}
      </Container>
      {/* Address Dialog */}
      <Dialog open={addressDialogOpen} onClose={() => setAddressDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Manage Addresses</DialogTitle>
        <DialogContent>
          <AddressManager userId={doctorId} onClose={() => setAddressDialogOpen(false)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddressDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
      {/* Completion Remarks Dialog */}
      <Dialog open={completionDialogOpen} onClose={() => setCompletionDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Complete Appointment</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="textSecondary" gutterBottom>Add remarks or diagnosis notes for this appointment.</Typography>
          <TextField autoFocus margin="dense" id="remarks" label="Doctor Remarks" type="text" fullWidth multiline rows={4} variant="outlined" value={completionRemarks} onChange={(e) => setCompletionRemarks(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCompletionDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSubmitCompletion} variant="contained" color="primary">Complete</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DoctorDashboard;
