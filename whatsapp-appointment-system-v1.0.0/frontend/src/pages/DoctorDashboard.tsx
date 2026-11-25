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
  Chip,
  Alert,
  Tooltip,
  Divider,
  Tabs,
  Tab,
  ToggleButton,
  ToggleButtonGroup,
  Stack,
} from '@mui/material';
import AccountCircle from '@mui/icons-material/AccountCircle';
import MedicalServicesIcon from '@mui/icons-material/MedicalServices';
import PrintIcon from '@mui/icons-material/Print';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import DeskIcon from '@mui/icons-material/Desk';
import AssessmentIcon from '@mui/icons-material/Assessment';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import AddressManager from '../components/AddressManager';
import Patient360Dialog from '../components/Patient360Dialog';
import PosBillingModal from '../components/PosBillingModal';
import { useNavigate } from 'react-router-dom';
import { LocalizationProvider, DatePicker } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as ChartTooltip, Legend, LineChart, Line, ResponsiveContainer } from 'recharts';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { doctorTheme } from '../theme/dashboardThemes';

interface DashboardMetrics {
  slotsBooked: number;
  customersAttendedToday: number;
  completedAppointments?: number;
  walkInCount?: number;
  cancelledAppointments?: number;
  noShowAppointments?: number;
  newPatientsCount?: number;
  oldPatientsCount?: number;
  pendingInquiriesCount?: number;
  dueFollowUpsToday?: number;
  totalRevenueToday?: number;
  attendanceRatePercent?: number;
  weeklyPatientsCount?: number;
  monthlyPatientsCount?: number;
  chartData?: {
    period: string;
    dailyTrends?: Array<{
      date: string;
      dayOfWeek: string;
      newCases: number;
      oldCases: number;
      totalPatients: number;
      revenue: number;
    }>;
    dataPoints?: Array<{ label: string; count: number; amount?: number }>;
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

interface QueueItem {
  id: number;
  queueNumber: number;
  patientId: number;
  doctorId: number;
  bookingId?: number;
  status: 'WAITING' | 'IN_CONSULTATION' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
  arrivalTime: string;
  priorityFlag: boolean;
}

interface PrescriptionRow {
  customMedicineName: string;
  dosage: string;
  frequency: string;
  durationDays: number;
  instructions: string;
}

const DoctorDashboard: React.FC = () => {
  const storedUser = localStorage.getItem('user');
  const parsedUser = storedUser ? JSON.parse(storedUser) : null;
  const role = parsedUser?.role;
  const navigate = useNavigate();

  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [metrics, setMetrics] = useState<DashboardMetrics>({
    slotsBooked: 0,
    customersAttendedToday: 0,
  });
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [opdQueue, setOpdQueue] = useState<QueueItem[]>([]);
  const [activeWorkspaceTab, setActiveWorkspaceTab] = useState<'WORKSPACE' | 'ANALYSIS'>('WORKSPACE');
  const [analysisPeriod, setAnalysisPeriod] = useState<string>('7days');
  const [customStartDate, setCustomStartDate] = useState<Date>(new Date(Date.now() - 6 * 24 * 60 * 60 * 1000));
  const [customEndDate, setCustomEndDate] = useState<Date>(new Date());
  const [chartPeriod, setChartPeriod] = useState('7days');
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
  const [paymentAmount, setPaymentAmount] = useState<number | string>(500);

  // User Menu State
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [addressDialogOpen, setAddressDialogOpen] = useState(false);

  // Patient 360 Dialog State
  const [patient360Open, setPatient360Open] = useState(false);
  const [selectedPatient360Id, setSelectedPatient360Id] = useState<number | null>(null);

  const handleOpenPatient360 = (patientId: number) => {
    setSelectedPatient360Id(patientId);
    setPatient360Open(true);
  };

  // --- Clinical Consultation Workspace State ---
  const [consultationDialogOpen, setConsultationDialogOpen] = useState(false);
  const [activeQueueItem, setActiveQueueItem] = useState<QueueItem | null>(null);
  const [activePatientDetails, setActivePatientDetails] = useState<any>(null);
  const [feeAssessment, setFeeAssessment] = useState<any>(null);

  // Clinical Form Fields
  const [caseType, setCaseType] = useState<'NEW_CASE' | 'OLD_CASE'>('NEW_CASE');
  const [consultationFee, setConsultationFee] = useState<number>(500);
  const [feeOverride, setFeeOverride] = useState<boolean>(false);
  const [feeOverrideReason, setFeeOverrideReason] = useState<string>('');
  const [chiefComplaint, setChiefComplaint] = useState<string>('');
  const [diagnosis, setDiagnosis] = useState<string>('');
  const [clinicalExam, setClinicalExam] = useState<string>('');
  const [doctorNotes, setDoctorNotes] = useState<string>('');

  // Vitals
  const [bpSystolic, setBpSystolic] = useState<string>('');
  const [bpDiastolic, setBpDiastolic] = useState<string>('');
  const [pulse, setPulse] = useState<string>('');
  const [temperature, setTemperature] = useState<string>('');
  const [weight, setWeight] = useState<string>('');
  const [spo2, setSpo2] = useState<string>('');

  // Medications
  const [medications, setMedications] = useState<PrescriptionRow[]>([
    { customMedicineName: 'Paracetamol 650mg', dosage: '1-0-1', frequency: 'Twice daily', durationDays: 3, instructions: 'After food' }
  ]);

  // Follow-up
  const [followUpInterval, setFollowUpInterval] = useState<string>('7D');
  const [followUpDate, setFollowUpDate] = useState<string>('');
  const [followUpReason, setFollowUpReason] = useState<string>('');
  const [submittingConsultation, setSubmittingConsultation] = useState(false);

  const doctorId = parsedUser?.userId || parsedUser?.id || 1;
  const username = parsedUser?.username;

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token || !doctorId) {
      navigate('/login');
      return;
    }

    loadDashboardData();
    loadOpdQueue();
    setupWebSocket();
  }, [selectedDate, chartPeriod, navigate]);

  useEffect(() => {
    loadAnalysisData();
  }, [analysisPeriod, customStartDate, customEndDate]);

  const loadAnalysisData = async () => {
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const params: any = { doctorId, period: analysisPeriod };
      if (analysisPeriod === 'custom') {
        params.startDate = customStartDate.toISOString().split('T')[0];
        params.endDate = customEndDate.toISOString().split('T')[0];
      } else if (analysisPeriod === 'today') {
        params.startDate = new Date().toISOString().split('T')[0];
      }
      const chartRes = await axios.get(`${apiUrl}/doctor/dashboard/metrics/charts`, {
        params,
        headers: { Authorization: `Bearer ${token}` }
      });
      if (chartRes.data?.dailyTrends && chartRes.data.dailyTrends.length > 0) {
        setDailyData(chartRes.data.dailyTrends);
      } else if (chartRes.data?.dataPoints) {
        setDailyData(chartRes.data.dataPoints);
      }
    } catch (err) {
      console.error('Failed to load analysis chart data', err);
    }
  };

  const loadDashboardData = async () => {
    try {
      const token = localStorage.getItem('token');
      const dateStr = selectedDate.toISOString().split('T')[0];
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

      const metricsRes = await axios.get(`${apiUrl}/doctor/dashboard/metrics`, {
        params: { doctorId, date: dateStr },
        headers: { Authorization: `Bearer ${token}` }
      });
      setMetrics(metricsRes.data);

      const appointmentsRes = await axios.get(`${apiUrl}/doctor/dashboard/bookings`, {
        params: { doctorId, date: dateStr },
        headers: { Authorization: `Bearer ${token}` }
      });
      setAppointments(appointmentsRes.data);
    } catch (error) {
      console.error('Failed to load dashboard data', error);
    }
  };

  const loadOpdQueue = async () => {
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const res = await axios.get(`${apiUrl}/clinical/queue/doctor/${doctorId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setOpdQueue(res.data);
    } catch (err) {
      console.error('Failed to load OPD queue', err);
    }
  };

  const setupWebSocket = () => {
    const wsUrl = 'http://localhost:8080';
    const socket = new SockJS(`${wsUrl}/ws`);
    const stompClient = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
    });

    stompClient.onConnect = () => {
      stompClient.subscribe(`/topic/doctor/${doctorId}/pending-requests`, () => {
        loadDashboardData();
        loadOpdQueue();
      });
    };

    stompClient.activate();
  };

  const handleStartConsultation = async (qItem: QueueItem) => {
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

      // Call patient in queue
      await axios.post(`${apiUrl}/clinical/queue/${qItem.id}/call`, null, {
        headers: { Authorization: `Bearer ${token}` }
      });

      // Fetch patient details
      const patientRes = await axios.get(`${apiUrl}/patients/${qItem.patientId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setActivePatientDetails(patientRes.data);

      // Assess Fee (New vs Old case)
      const feeRes = await axios.get(`${apiUrl}/clinical/assess-fee`, {
        params: { clinicId: 1, doctorId, patientId: qItem.patientId },
        headers: { Authorization: `Bearer ${token}` }
      });
      setFeeAssessment(feeRes.data);
      setCaseType(feeRes.data.caseType || 'NEW_CASE');
      setConsultationFee(feeRes.data.consultationFee || 500);

      // Default follow up date: 7 days from now
      const defaultFollowUp = new Date();
      defaultFollowUp.setDate(defaultFollowUp.getDate() + 7);
      setFollowUpDate(defaultFollowUp.toISOString().split('T')[0]);

      setActiveQueueItem(qItem);
      setConsultationDialogOpen(true);
      loadOpdQueue();
    } catch (err) {
      console.error('Failed to start consultation', err);
      alert('Error opening consultation workspace');
    }
  };

  const handleAddMedication = () => {
    setMedications([
      ...medications,
      { customMedicineName: '', dosage: '1-0-1', frequency: 'Daily', durationDays: 5, instructions: 'After food' }
    ]);
  };

  const handleRemoveMedication = (index: number) => {
    const updated = medications.filter((_, i) => i !== index);
    setMedications(updated);
  };

  const handleMedicationChange = (index: number, field: keyof PrescriptionRow, value: any) => {
    const updated = [...medications];
    updated[index] = { ...updated[index], [field]: value };
    setMedications(updated);
  };

  const handleSetQuickInterval = (days: number, preset: string) => {
    setFollowUpInterval(preset);
    const date = new Date();
    date.setDate(date.getDate() + days);
    setFollowUpDate(date.toISOString().split('T')[0]);
  };

  const handleFinalizeConsultation = async (isDischarge = false) => {
    if (!chiefComplaint.trim() || !diagnosis.trim()) {
      alert('Please fill in Chief Complaint and Diagnosis.');
      return;
    }
    if (!activeQueueItem) return;

    setSubmittingConsultation(true);
    try {
      const token = localStorage.getItem('token');
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

      const payload = {
        clinicId: 1,
        bookingId: activeQueueItem.bookingId,
        patientId: activeQueueItem.patientId,
        doctorId,
        queueId: activeQueueItem.id,
        caseType,
        consultationFee,
        feeOverride,
        feeOverrideReason,
        chiefComplaint,
        bpSystolic: bpSystolic ? parseInt(bpSystolic) : undefined,
        bpDiastolic: bpDiastolic ? parseInt(bpDiastolic) : undefined,
        pulse: pulse ? parseInt(pulse) : undefined,
        temperature: temperature ? parseFloat(temperature) : undefined,
        weight: weight ? parseFloat(weight) : undefined,
        spo2: spo2 ? parseInt(spo2) : undefined,
        clinicalExamination: clinicalExam,
        diagnosis,
        doctorPrivateNotes: doctorNotes,
        prescriptionItems: medications.filter(m => m.customMedicineName.trim().length > 0),
        followUpDate: followUpDate || undefined,
        followUpInterval,
        followUpReason,
      };

      const res = await axios.post(`${apiUrl}/clinical/consultations/finalize`, payload, {
        headers: { Authorization: `Bearer ${token}` }
      });

      const consultationId = res.data.id;

      if (isDischarge) {
        await axios.post(`${apiUrl}/clinical/consultations/${consultationId}/discharge`, {
          dischargeCondition: 'Recovered / Stable',
          dischargeSummary: `Diagnosis: ${diagnosis}. Outpatient care completed with prescribed medications.`,
        }, {
          headers: { Authorization: `Bearer ${token}` }
        });
        alert('Consultation Finalized & Discharged! Opening Summary PDF...');
        window.open(`${apiUrl}/clinical/consultations/${consultationId}/discharge-pdf`, '_blank');
      } else {
        alert('Consultation Finalized! Generating Prescription PDF...');
        window.open(`${apiUrl}/clinical/consultations/${consultationId}/pdf`, '_blank');
      }

      setConsultationDialogOpen(false);
      loadOpdQueue();
      loadDashboardData();
    } catch (err) {
      console.error('Failed to finalize consultation', err);
      alert('Failed to finalize consultation');
    } finally {
      setSubmittingConsultation(false);
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
          <MedicalServicesIcon sx={{ mr: 1 }} />
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 600 }}>
            PMS Doctor Workspace & OPD Queue
          </Typography>
          <Button
            color="inherit"
            onClick={() => navigate('/appointments')}
            sx={{ mr: 1, borderRadius: '8px', px: 1.5, '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' } }}
          >
            Appointments
          </Button>
          <Button
            color="inherit"
            onClick={() => navigate('/inquiries')}
            sx={{ mr: 1, borderRadius: '8px', px: 1.5, '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' } }}
          >
            Inquiries
          </Button>
          <Button
            color="inherit"
            onClick={() => navigate('/follow-ups')}
            sx={{ mr: 1, borderRadius: '8px', px: 1.5, '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' } }}
          >
            Follow-Ups
          </Button>
          <Button
            color="inherit"
            onClick={() => navigate('/availability')}
            sx={{ mr: 1, borderRadius: '8px', px: 1.5, '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' } }}
          >
            Schedules
          </Button>
          <Button
            color="inherit"
            onClick={() => navigate('/clinic-settings')}
            sx={{ mr: 1.5, borderRadius: '8px', px: 1.5, '&:hover': { backgroundColor: 'rgba(255,255,255,0.1)' } }}
          >
            Settings
          </Button>
          <Button
            variant="contained"
            startIcon={<DeskIcon />}
            onClick={() => navigate('/reception')}
            sx={{
              mr: 2,
              bgcolor: '#ffffff',
              color: '#1976d2',
              fontWeight: 'bold',
              borderRadius: '20px',
              textTransform: 'none',
              px: 2,
              boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
              '&:hover': { bgcolor: '#f0f4f8' }
            }}
          >
            Switch to Reception Desk ➔
          </Button>
          {username && (
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Box sx={{ mr: 2, textAlign: 'right' }}>
                <Typography variant="body2" sx={{ fontWeight: 500 }}>{username}</Typography>
                <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.8)', fontSize: '0.7rem' }}>
                  {role}
                </Typography>
              </Box>
              <IconButton size="large" onClick={(e) => setAnchorEl(e.currentTarget)} color="inherit">
                <AccountCircle />
              </IconButton>
              <Menu
                anchorEl={anchorEl}
                open={Boolean(anchorEl)}
                onClose={() => setAnchorEl(null)}
              >
                <MenuItem onClick={() => { setAnchorEl(null); setAddressDialogOpen(true); }}>Manage Addresses</MenuItem>
                <MenuItem onClick={() => { setAnchorEl(null); localStorage.clear(); navigate('/login'); }}>Logout</MenuItem>
              </Menu>
            </Box>
          )}
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
        {/* Workspace vs Analysis Tab Bar */}
        <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3, bgcolor: '#ffffff', p: 1, borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
          <Tabs
            value={activeWorkspaceTab}
            onChange={(e, val) => setActiveWorkspaceTab(val)}
            textColor="primary"
            indicatorColor="primary"
          >
            <Tab
              icon={<MedicalServicesIcon />}
              iconPosition="start"
              label="OPD Workspace & Live Queue"
              value="WORKSPACE"
              sx={{ fontWeight: 'bold', textTransform: 'none', fontSize: '0.95rem' }}
            />
            <Tab
              icon={<AssessmentIcon />}
              iconPosition="start"
              label="Practice Analysis & Insights"
              value="ANALYSIS"
              sx={{ fontWeight: 'bold', textTransform: 'none', fontSize: '0.95rem' }}
            />
          </Tabs>
        </Box>

        {activeWorkspaceTab === 'WORKSPACE' && (
          <Grid container spacing={3}>
            {/* Top Live OPD Queue Panel */}
            <Grid item xs={12}>
              <Paper sx={{ p: 2.5, borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <MedicalServicesIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                      Today's OPD Waiting Queue
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Chip label={`Waiting: ${opdQueue.filter(q => q.status === 'WAITING').length}`} color="warning" />
                    <Chip label={`In-Consultation: ${opdQueue.filter(q => q.status === 'IN_CONSULTATION').length}`} color="info" />
                    <Chip label={`Completed: ${opdQueue.filter(q => q.status === 'COMPLETED').length}`} color="success" />
                  </Box>
                </Box>

                <TableContainer sx={{ maxHeight: 280 }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow sx={{ backgroundColor: '#f5f7fa' }}>
                        <TableCell>Token</TableCell>
                        <TableCell>Patient ID</TableCell>
                        <TableCell>Arrival Time</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell align="center">Clinical Action</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {opdQueue.length > 0 ? (
                        opdQueue.map((q) => (
                          <TableRow key={q.id} hover>
                            <TableCell>
                              <Typography variant="subtitle2" fontWeight="bold" color="primary">
                                #{q.queueNumber}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Button
                                size="small"
                                variant="text"
                                sx={{ fontWeight: 'bold', textTransform: 'none' }}
                                onClick={() => handleOpenPatient360(q.patientId)}
                              >
                                Patient #{q.patientId} 🔍
                              </Button>
                            </TableCell>
                            <TableCell>
                              {q.arrivalTime ? new Date(q.arrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-'}
                            </TableCell>
                            <TableCell>
                              <Chip
                                size="small"
                                label={q.status}
                                color={
                                  q.status === 'WAITING' ? 'warning' :
                                  q.status === 'IN_CONSULTATION' ? 'info' :
                                  q.status === 'COMPLETED' ? 'success' : 'default'
                                }
                              />
                            </TableCell>
                            <TableCell align="center">
                              {q.status === 'WAITING' && (
                                <Button
                                  variant="contained"
                                  size="small"
                                  color="primary"
                                  startIcon={<PlayArrowIcon />}
                                  onClick={() => handleStartConsultation(q)}
                                >
                                  Call Patient
                                </Button>
                              )}
                              {q.status === 'IN_CONSULTATION' && (
                                <Button
                                  variant="contained"
                                  size="small"
                                  color="info"
                                  startIcon={<MedicalServicesIcon />}
                                  onClick={() => handleStartConsultation(q)}
                                >
                                  Resume Consultation
                                </Button>
                              )}
                              {q.status === 'COMPLETED' && (
                                <Chip size="small" label="Finished" color="success" variant="outlined" />
                              )}
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={5} align="center">
                            <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                              No patients currently in queue. Patients checked in at the Reception Desk will appear here.
                            </Typography>
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Paper>
            </Grid>

            {/* Operational Metrics Strip */}
            <Grid item xs={12} sm={6} md={3}>
              <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #1976d2' }}>
                <Typography variant="h5" color="primary" fontWeight="bold">{metrics.slotsBooked || 0}</Typography>
                <Typography variant="caption" color="textSecondary" display="block">Today's Appointments</Typography>
              </Paper>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #2e7d32' }}>
                <Typography variant="h5" color="success.main" fontWeight="bold">
                  {metrics.completedAppointments || metrics.customersAttendedToday || 0}
                </Typography>
                <Typography variant="caption" color="textSecondary" display="block">Consultations Completed</Typography>
              </Paper>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #ed6c02' }}>
                <Typography variant="h5" color="warning.main" fontWeight="bold">{metrics.walkInCount || 0}</Typography>
                <Typography variant="caption" color="textSecondary" display="block">Walk-In Check-Ins</Typography>
              </Paper>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #9c27b0' }}>
                <Typography variant="h5" color="secondary.main" fontWeight="bold">₹{metrics.totalRevenueToday || 0}</Typography>
                <Typography variant="caption" color="textSecondary" display="block">Revenue Collected (Today)</Typography>
              </Paper>
            </Grid>

            {/* Secondary Operational Metrics & Date Picker */}
            <Grid item xs={6} sm={3} md={2}>
              <Paper sx={{ p: 1.5, textAlign: 'center', borderRadius: '10px' }}>
                <Typography variant="h6" fontWeight="bold" color="primary">{metrics.newPatientsCount || 0}</Typography>
                <Typography variant="caption" color="textSecondary">New Cases</Typography>
              </Paper>
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <Paper sx={{ p: 1.5, textAlign: 'center', borderRadius: '10px' }}>
                <Typography variant="h6" fontWeight="bold" color="info.main">{metrics.oldPatientsCount || 0}</Typography>
                <Typography variant="caption" color="textSecondary">Old/Revisit Cases</Typography>
              </Paper>
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <Paper sx={{ p: 1.5, textAlign: 'center', borderRadius: '10px' }}>
                <Typography variant="h6" fontWeight="bold" color="warning.main">{metrics.dueFollowUpsToday || 0}</Typography>
                <Typography variant="caption" color="textSecondary">Follow-ups Due</Typography>
              </Paper>
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <Paper sx={{ p: 1.5, textAlign: 'center', borderRadius: '10px' }}>
                <Typography variant="h6" fontWeight="bold" color="error.main">{metrics.pendingInquiriesCount || 0}</Typography>
                <Typography variant="caption" color="textSecondary">Pending Inquiries</Typography>
              </Paper>
            </Grid>
            <Grid item xs={12} md={4}>
              <Paper sx={{ p: 1.5, borderRadius: '10px' }}>
                <LocalizationProvider dateAdapter={AdapterDateFns}>
                  <DatePicker
                    label="Filter Today's View by Date"
                    value={selectedDate}
                    onChange={(d) => setSelectedDate(d || new Date())}
                    slotProps={{ textField: { size: 'small', fullWidth: true } }}
                  />
                </LocalizationProvider>
              </Paper>
            </Grid>

            {/* Appointments Table */}
            <Grid item xs={12}>
              <Paper sx={{ p: 3, borderRadius: '12px' }}>
                <Typography variant="h6" fontWeight="bold" gutterBottom>
                  Confirmed Appointments ({appointments.length})
                </Typography>
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow sx={{ backgroundColor: '#f5f7fa' }}>
                        <TableCell>Time</TableCell>
                        <TableCell>Patient</TableCell>
                        <TableCell>Mobile</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell align="center">Billing / Payment</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {appointments.length > 0 ? (
                        appointments.map((appt) => (
                          <TableRow key={appt.id}>
                            <TableCell>{appt.startTime}</TableCell>
                            <TableCell>
                              <Button
                                size="small"
                                variant="text"
                                sx={{ fontWeight: 'bold', textTransform: 'none', p: 0 }}
                                onClick={() => handleOpenPatient360(appt.patientId)}
                              >
                                {appt.patientName || 'N/A'} 🔍
                              </Button>
                            </TableCell>
                            <TableCell>{appt.patientPhone || 'N/A'}</TableCell>
                            <TableCell>
                              <Chip size="small" label={appt.status} color={appt.status === 'COMPLETED' ? 'success' : 'default'} />
                            </TableCell>
                            <TableCell align="center">
                              <Button
                                variant="outlined"
                                size="small"
                                onClick={() => handleOpenPaymentModal(appt)}
                              >
                                {appt.status === 'PAID' ? 'PAID' : 'Generate Payment'}
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={5} align="center">No bookings for this date.</TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Paper>
            </Grid>
          </Grid>
        )}

        {/* Practice Analysis & Insights Tab View */}
        {activeWorkspaceTab === 'ANALYSIS' && (
          <Box>
            {/* Filter Controls Bar */}
            <Paper sx={{ p: 2.5, mb: 3, borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <TrendingUpIcon color="primary" />
                  <Typography variant="h6" fontWeight="bold">
                    Practice Analytics & Performance Insights
                  </Typography>
                </Box>
                
                <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
                  <ToggleButtonGroup
                    value={analysisPeriod}
                    exclusive
                    onChange={(e, val) => { if (val) setAnalysisPeriod(val); }}
                    size="small"
                    color="primary"
                  >
                    <ToggleButton value="today" sx={{ px: 2, fontWeight: 'bold', textTransform: 'none' }}>Today</ToggleButton>
                    <ToggleButton value="7days" sx={{ px: 2, fontWeight: 'bold', textTransform: 'none' }}>Last 7 Days</ToggleButton>
                    <ToggleButton value="month" sx={{ px: 2, fontWeight: 'bold', textTransform: 'none' }}>This Month</ToggleButton>
                    <ToggleButton value="custom" sx={{ px: 2, fontWeight: 'bold', textTransform: 'none' }}>Custom Range</ToggleButton>
                  </ToggleButtonGroup>

                  {analysisPeriod === 'custom' && (
                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                      <LocalizationProvider dateAdapter={AdapterDateFns}>
                        <DatePicker
                          label="Start Date"
                          value={customStartDate}
                          onChange={(d) => d && setCustomStartDate(d)}
                          slotProps={{ textField: { size: 'small', sx: { width: 140 } } }}
                        />
                        <DatePicker
                          label="End Date"
                          value={customEndDate}
                          onChange={(d) => d && setCustomEndDate(d)}
                          slotProps={{ textField: { size: 'small', sx: { width: 140 } } }}
                        />
                      </LocalizationProvider>
                      <Button variant="contained" size="small" onClick={() => loadAnalysisData()} sx={{ textTransform: 'none' }}>
                        Filter
                      </Button>
                    </Box>
                  )}
                </Stack>
              </Box>
            </Paper>

            {/* Aggregated KPI Strip for Selected Period */}
            <Grid container spacing={2.5} sx={{ mb: 3 }}>
              <Grid item xs={12} sm={6} md={2.4}>
                <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #1976d2', bgcolor: '#ffffff' }}>
                  <Typography variant="h5" color="primary" fontWeight="bold">
                    {dailyData.reduce((acc, c) => acc + (c.totalPatients || (c.newCases || 0) + (c.oldCases || 0)), 0)}
                  </Typography>
                  <Typography variant="caption" color="textSecondary" display="block">Total Consultations</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={2.4}>
                <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #0288d1', bgcolor: '#ffffff' }}>
                  <Typography variant="h5" color="info.main" fontWeight="bold">
                    {dailyData.reduce((acc, c) => acc + (c.newCases || 0), 0)}
                  </Typography>
                  <Typography variant="caption" color="textSecondary" display="block">New Patient Cases</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={2.4}>
                <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #2e7d32', bgcolor: '#ffffff' }}>
                  <Typography variant="h5" color="success.main" fontWeight="bold">
                    {dailyData.reduce((acc, c) => acc + (c.oldCases || 0), 0)}
                  </Typography>
                  <Typography variant="caption" color="textSecondary" display="block">Returning / Revisit Cases</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={2.4}>
                <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #ed6c02', bgcolor: '#ffffff' }}>
                  <Typography variant="h5" color="warning.main" fontWeight="bold">
                    ₹{dailyData.reduce((acc, c) => acc + (c.revenue || 0), 0).toLocaleString()}
                  </Typography>
                  <Typography variant="caption" color="textSecondary" display="block">Total Revenue Collected</Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6} md={2.4}>
                <Paper sx={{ p: 2, textAlign: 'center', borderRadius: '12px', borderLeft: '4px solid #9c27b0', bgcolor: '#ffffff' }}>
                  <Typography variant="h5" color="secondary.main" fontWeight="bold">
                    ₹{(() => {
                      const totalVis = dailyData.reduce((acc, c) => acc + (c.totalPatients || (c.newCases || 0) + (c.oldCases || 0)), 0);
                      const totalRev = dailyData.reduce((acc, c) => acc + (c.revenue || 0), 0);
                      return totalVis > 0 ? Math.round(totalRev / totalVis) : 0;
                    })()}
                  </Typography>
                  <Typography variant="caption" color="textSecondary" display="block">Avg Fee / Consultation</Typography>
                </Paper>
              </Grid>
            </Grid>

            {/* Visual Charts */}
            <Grid container spacing={3} sx={{ mb: 3 }}>
              <Grid item xs={12} md={6}>
                <Paper sx={{ p: 2.5, borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
                  <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                    Patient Volume Breakdown (New vs Returning Cases)
                  </Typography>
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={dailyData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="dayOfWeek" />
                      <YAxis allowDecimals={false} />
                      <ChartTooltip />
                      <Legend />
                      <Bar dataKey="newCases" name="New Cases" fill="#1976d2" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="oldCases" name="Returning Cases" fill="#2e7d32" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>

              <Grid item xs={12} md={6}>
                <Paper sx={{ p: 2.5, borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
                  <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
                    Consultation Revenue Trend (₹ Collections)
                  </Typography>
                  <ResponsiveContainer width="100%" height={260}>
                    <LineChart data={dailyData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="dayOfWeek" />
                      <YAxis />
                      <ChartTooltip />
                      <Legend />
                      <Line type="monotone" dataKey="revenue" name="Daily Revenue (₹)" stroke="#ed6c02" strokeWidth={3} dot={{ r: 4 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </Paper>
              </Grid>
            </Grid>

            {/* Detailed Daily Table */}
            <Paper sx={{ p: 3, borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Daily Summary Table for Selected Period
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ backgroundColor: '#f5f7fa' }}>
                      <TableCell>Date / Day</TableCell>
                      <TableCell align="center">New Cases</TableCell>
                      <TableCell align="center">Returning Cases</TableCell>
                      <TableCell align="center">Total Consultations</TableCell>
                      <TableCell align="right">Revenue (₹)</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {dailyData.length > 0 ? (
                      dailyData.map((row, idx) => (
                        <TableRow key={idx} hover>
                          <TableCell>
                            <Typography variant="body2" fontWeight="medium">{row.date} ({row.dayOfWeek})</Typography>
                          </TableCell>
                          <TableCell align="center">
                            <Chip size="small" label={row.newCases || 0} color="primary" variant="outlined" />
                          </TableCell>
                          <TableCell align="center">
                            <Chip size="small" label={row.oldCases || 0} color="success" variant="outlined" />
                          </TableCell>
                          <TableCell align="center">
                            <Typography variant="body2" fontWeight="bold">{(row.newCases || 0) + (row.oldCases || 0)}</Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="body2" fontWeight="bold" color="warning.main">₹{(row.revenue || 0).toLocaleString()}</Typography>
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell colSpan={5} align="center">No data available for selected period.</TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Paper>
          </Box>
        )}
      </Container>

      {/* --- Clinical Consultation & Prescription Dialog --- */}
      <Dialog
        open={consultationDialogOpen}
        onClose={() => setConsultationDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ backgroundColor: '#1976d2', color: '#fff', fontWeight: 'bold' }}>
          Consultation Workspace — Token #{activeQueueItem?.queueNumber} (Patient: {activePatientDetails?.name || 'Walk-in'})
        </DialogTitle>
        <DialogContent dividers sx={{ p: 3 }}>
          {/* Fee & Case Assessment Banner */}
          {feeAssessment && (
            <Alert
              severity={feeAssessment.caseType === 'OLD_CASE' ? 'success' : 'info'}
              sx={{ mb: 2 }}
              action={
                <Button
                  color="inherit"
                  size="small"
                  onClick={() => setFeeOverride(!feeOverride)}
                >
                  {feeOverride ? 'Auto Fee' : 'Override Fee'}
                </Button>
              }
            >
              <Typography variant="body2" fontWeight="bold">
                {feeAssessment.caseType === 'OLD_CASE'
                  ? `OLD CASE: Last visit was ${feeAssessment.daysSinceLastVisit} days ago (within ${feeAssessment.validityDays} days validity). Fee: ₹${consultationFee}`
                  : `NEW CASE: First consultation or expired validity. Fee: ₹${consultationFee}`}
              </Typography>
            </Alert>
          )}

          {feeOverride && (
            <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
              <TextField
                label="Custom Fee (₹)"
                type="number"
                size="small"
                value={consultationFee}
                onChange={(e) => setConsultationFee(Number(e.target.value))}
              />
              <TextField
                fullWidth
                label="Override Reason"
                size="small"
                placeholder="e.g. Discretionary discount, follow-up exemption"
                value={feeOverrideReason}
                onChange={(e) => setFeeOverrideReason(e.target.value)}
              />
            </Box>
          )}

          {/* Section 1: Vitals */}
          <Typography variant="subtitle2" fontWeight="bold" color="primary" gutterBottom>
            Patient Vitals
          </Typography>
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid item xs={4} sm={2}>
              <TextField fullWidth size="small" label="BP Systolic" placeholder="120" value={bpSystolic} onChange={(e) => setBpSystolic(e.target.value)} />
            </Grid>
            <Grid item xs={4} sm={2}>
              <TextField fullWidth size="small" label="BP Diastolic" placeholder="80" value={bpDiastolic} onChange={(e) => setBpDiastolic(e.target.value)} />
            </Grid>
            <Grid item xs={4} sm={2}>
              <TextField fullWidth size="small" label="Pulse (bpm)" placeholder="72" value={pulse} onChange={(e) => setPulse(e.target.value)} />
            </Grid>
            <Grid item xs={4} sm={2}>
              <TextField fullWidth size="small" label="Temp (°F)" placeholder="98.6" value={temperature} onChange={(e) => setTemperature(e.target.value)} />
            </Grid>
            <Grid item xs={4} sm={2}>
              <TextField fullWidth size="small" label="Weight (kg)" placeholder="70" value={weight} onChange={(e) => setWeight(e.target.value)} />
            </Grid>
            <Grid item xs={4} sm={2}>
              <TextField fullWidth size="small" label="SpO2 (%)" placeholder="98" value={spo2} onChange={(e) => setSpo2(e.target.value)} />
            </Grid>
          </Grid>

          <Divider sx={{ my: 2 }} />

          {/* Section 2: Clinical Evaluation */}
          <Typography variant="subtitle2" fontWeight="bold" color="primary" gutterBottom>
            Clinical Examination & Diagnosis
          </Typography>
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                multiline
                rows={2}
                size="small"
                label="Chief Complaint"
                placeholder="e.g. Fever, dry cough since 3 days"
                value={chiefComplaint}
                onChange={(e) => setChiefComplaint(e.target.value)}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                multiline
                rows={2}
                size="small"
                label="Diagnosis"
                placeholder="e.g. Acute Upper Respiratory Tract Infection"
                value={diagnosis}
                onChange={(e) => setDiagnosis(e.target.value)}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                size="small"
                label="Clinical Examination / Findings"
                placeholder="Chest clear, throat congested, no signs of wheeze"
                value={clinicalExam}
                onChange={(e) => setClinicalExam(e.target.value)}
              />
            </Grid>
          </Grid>

          <Divider sx={{ my: 2 }} />

          {/* Section 3: Prescription (Medications) */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
            <Typography variant="subtitle2" fontWeight="bold" color="primary">
              Prescription (Rx) Line Items
            </Typography>
            <Button
              size="small"
              startIcon={<AddCircleOutlineIcon />}
              onClick={handleAddMedication}
            >
              Add Medicine
            </Button>
          </Box>

          {medications.map((med, index) => (
            <Grid container spacing={1} key={index} alignItems="center" sx={{ mb: 1 }}>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  size="small"
                  label="Medicine Name"
                  placeholder="e.g. Amoxicillin 500mg"
                  value={med.customMedicineName}
                  onChange={(e) => handleMedicationChange(index, 'customMedicineName', e.target.value)}
                />
              </Grid>
              <Grid item xs={6} sm={2}>
                <TextField
                  fullWidth
                  size="small"
                  label="Dosage"
                  placeholder="1-0-1"
                  value={med.dosage}
                  onChange={(e) => handleMedicationChange(index, 'dosage', e.target.value)}
                />
              </Grid>
              <Grid item xs={6} sm={2}>
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  label="Days"
                  value={med.durationDays}
                  onChange={(e) => handleMedicationChange(index, 'durationDays', Number(e.target.value))}
                />
              </Grid>
              <Grid item xs={10} sm={3}>
                <TextField
                  fullWidth
                  size="small"
                  label="Instructions"
                  placeholder="After food"
                  value={med.instructions}
                  onChange={(e) => handleMedicationChange(index, 'instructions', e.target.value)}
                />
              </Grid>
              <Grid item xs={2} sm={1}>
                <IconButton size="small" color="error" onClick={() => handleRemoveMedication(index)}>
                  <DeleteOutlineIcon />
                </IconButton>
              </Grid>
            </Grid>
          ))}

          <Divider sx={{ my: 2 }} />

          {/* Section 4: Follow-up Scheduling */}
          <Typography variant="subtitle2" fontWeight="bold" color="primary" gutterBottom>
            Follow-Up Recommendation (Retention)
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
            <Chip label="+3 Days" clickable color={followUpInterval === '3D' ? 'primary' : 'default'} onClick={() => handleSetQuickInterval(3, '3D')} />
            <Chip label="+7 Days" clickable color={followUpInterval === '7D' ? 'primary' : 'default'} onClick={() => handleSetQuickInterval(7, '7D')} />
            <Chip label="+15 Days" clickable color={followUpInterval === '15D' ? 'primary' : 'default'} onClick={() => handleSetQuickInterval(15, '15D')} />
            <Chip label="+30 Days" clickable color={followUpInterval === '30D' ? 'primary' : 'default'} onClick={() => handleSetQuickInterval(30, '30D')} />
          </Box>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                size="small"
                type="date"
                label="Follow-Up Date"
                InputLabelProps={{ shrink: true }}
                value={followUpDate}
                onChange={(e) => { setFollowUpDate(e.target.value); setFollowUpInterval('CUSTOM'); }}
              />
            </Grid>
            <Grid item xs={12} sm={8}>
              <TextField
                fullWidth
                size="small"
                label="Follow-up Clinical Reason"
                placeholder="e.g. Review symptoms and assess response to antibiotic"
                value={followUpReason}
                onChange={(e) => setFollowUpReason(e.target.value)}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 2, gap: 1 }}>
          <Button onClick={() => setConsultationDialogOpen(false)}>Cancel</Button>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<PrintIcon />}
            disabled={submittingConsultation}
            onClick={() => handleFinalizeConsultation(true)}
          >
            Finalize & Discharge Summary
          </Button>
          <Button
            variant="contained"
            color="success"
            startIcon={<PrintIcon />}
            disabled={submittingConsultation}
            onClick={() => handleFinalizeConsultation(false)}
          >
            {submittingConsultation ? 'Saving...' : 'Finalize & Print Prescription'}
          </Button>
        </DialogActions>
      </Dialog>

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

      {/* Patient 360-Degree EHR Profile Modal */}
      <Patient360Dialog
        open={patient360Open}
        patientId={selectedPatient360Id}
        onClose={() => setPatient360Open(false)}
      />

      {/* Clinic POS Billing & Instant Receipt Modal */}
      {selectedBooking && (
        <PosBillingModal
          open={paymentModalOpen}
          onClose={() => setPaymentModalOpen(false)}
          patientId={selectedBooking.patientId}
          patientName={selectedBooking.patientName || 'Patient'}
          doctorId={doctorId}
          doctorName="Attending Doctor"
          bookingId={selectedBooking.id}
          defaultFee={Number(selectedBooking.totalAmount) || 500}
          onSuccess={() => {
            loadDashboardData();
            loadOpdQueue();
          }}
        />
      )}
    </Box>
  );
};

export default DoctorDashboard;
