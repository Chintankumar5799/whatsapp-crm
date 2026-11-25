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
  Card,
  CardContent,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import EventBusyIcon from '@mui/icons-material/EventBusy';
import ScheduleIcon from '@mui/icons-material/Schedule';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

interface AvailabilitySlot {
  id?: number;
  doctorId: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  isActive: boolean;
}

interface AvailabilityException {
  id?: number;
  doctorId: number;
  exceptionDate: string;
  exceptionType: string;
  reason: string;
}

const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

const Availability: React.FC = () => {
  const navigate = useNavigate();
  const [doctorId, setDoctorId] = useState<number>(1);
  const [slots, setSlots] = useState<AvailabilitySlot[]>([]);
  const [exceptions, setExceptions] = useState<AvailabilityException[]>([]);
  const [loading, setLoading] = useState(false);
  const [alertMsg, setAlertMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Add Shift Modal
  const [slotDialogOpen, setSlotDialogOpen] = useState(false);
  const [dayOfWeek, setDayOfWeek] = useState('MONDAY');
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('13:00');
  const [slotDuration, setSlotDuration] = useState(30);

  // Leave / Exception Modal
  const [exceptionDialogOpen, setExceptionDialogOpen] = useState(false);
  const [leaveDate, setLeaveDate] = useState('');
  const [leaveReason, setLeaveReason] = useState('Doctor on personal leave');

  const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
  const token = localStorage.getItem('token');
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  useEffect(() => {
    loadSchedules();
  }, [doctorId]);

  const loadSchedules = async () => {
    setLoading(true);
    try {
      const [slotsRes, excRes] = await Promise.all([
        axios.get(`${apiUrl}/availability/doctor/${doctorId}`, { headers }),
        axios.get(`${apiUrl}/availability/exceptions/${doctorId}`, { headers }),
      ]);
      setSlots(slotsRes.data);
      setExceptions(excRes.data);
    } catch (err) {
      console.error('Failed to load schedules', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSlot = async () => {
    try {
      await axios.post(
        `${apiUrl}/availability`,
        {
          doctorId,
          dayOfWeek,
          startTime,
          endTime,
          slotDurationMinutes: slotDuration,
          isActive: true,
        },
        { headers }
      );
      setSlotDialogOpen(false);
      setAlertMsg({ type: 'success', text: `Added shift for ${dayOfWeek} successfully.` });
      loadSchedules();
      setTimeout(() => setAlertMsg(null), 3000);
    } catch (err) {
      console.error('Save slot failed', err);
      setAlertMsg({ type: 'error', text: 'Failed to save shift timings.' });
    }
  };

  const handleDeleteSlot = async (id?: number) => {
    if (!id) return;
    try {
      await axios.delete(`${apiUrl}/availability/${id}`, { headers });
      setAlertMsg({ type: 'success', text: 'Shift removed.' });
      loadSchedules();
      setTimeout(() => setAlertMsg(null), 3000);
    } catch (err) {
      console.error('Delete slot failed', err);
    }
  };

  const handleAddException = async () => {
    if (!leaveDate) {
      alert('Please select a leave date.');
      return;
    }
    try {
      await axios.post(
        `${apiUrl}/availability/exceptions`,
        {
          doctorId,
          exceptionDate: leaveDate,
          exceptionType: 'BLOCKED',
          reason: leaveReason,
        },
        { headers }
      );
      setExceptionDialogOpen(false);
      setLeaveDate('');
      setAlertMsg({ type: 'success', text: 'Leave block recorded successfully.' });
      loadSchedules();
      setTimeout(() => setAlertMsg(null), 3000);
    } catch (err) {
      console.error('Add exception failed', err);
      setAlertMsg({ type: 'error', text: 'Failed to record leave block.' });
    }
  };

  const handleDeleteException = async (id?: number) => {
    if (!id) return;
    try {
      await axios.delete(`${apiUrl}/availability/exceptions/${id}`, { headers });
      setAlertMsg({ type: 'success', text: 'Leave entry removed.' });
      loadSchedules();
      setTimeout(() => setAlertMsg(null), 3000);
    } catch (err) {
      console.error('Delete exception failed', err);
    }
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
      {/* Header */}
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
            <ScheduleIcon color="primary" /> Doctor Schedules & Shift Timings
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Select Doctor</InputLabel>
            <Select
              value={doctorId}
              label="Select Doctor"
              onChange={(e) => setDoctorId(Number(e.target.value))}
            >
              <MenuItem value={1}>Dr. Attending Physician</MenuItem>
              <MenuItem value={2}>Dr. Senior Consultant</MenuItem>
            </Select>
          </FormControl>
          <Button
            variant="contained"
            startIcon={<AddCircleOutlineIcon />}
            onClick={() => setSlotDialogOpen(true)}
          >
            Add Regular Shift
          </Button>
          <Button
            variant="outlined"
            color="warning"
            startIcon={<EventBusyIcon />}
            onClick={() => setExceptionDialogOpen(true)}
          >
            Mark Leave / Holiday
          </Button>
        </Box>
      </Box>

      {alertMsg && (
        <Alert severity={alertMsg.type} sx={{ mb: 2 }}>
          {alertMsg.text}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Weekly Regular Schedules */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2.5, borderRadius: 2 }}>
            <Typography variant="h6" fontWeight="bold" gutterBottom>
              Weekly OPD Consultation Shifts
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Recurring weekly working hours. Appointments and time slots are generated strictly within these operating shifts.
            </Typography>

            <TableContainer>
              <Table size="small">
                <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 'bold' }}>Day of Week</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Shift Hours</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Slot Duration</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
                    <TableCell align="center" sx={{ fontWeight: 'bold' }}>Action</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {slots.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                        No shifts configured for this doctor. Click "Add Regular Shift" to setup timings.
                      </TableCell>
                    </TableRow>
                  ) : (
                    slots.map((s) => (
                      <TableRow key={s.id}>
                        <TableCell sx={{ fontWeight: 'bold' }}>{s.dayOfWeek}</TableCell>
                        <TableCell>
                          {s.startTime} — {s.endTime}
                        </TableCell>
                        <TableCell>
                          <Chip label={`${s.slotDurationMinutes} mins`} size="small" variant="outlined" />
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={s.isActive ? 'Active' : 'Inactive'}
                            size="small"
                            color={s.isActive ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell align="center">
                          <IconButton
                            color="error"
                            size="small"
                            onClick={() => handleDeleteSlot(s.id)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        </Grid>

        {/* Leave & Holiday Exceptions */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2.5, borderRadius: 2 }}>
            <Typography variant="h6" fontWeight="bold" gutterBottom>
              Leaves & Blocked Dates
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Dates where regular OPD is suspended or closed.
            </Typography>

            {exceptions.length === 0 ? (
              <Box sx={{ py: 3, textAlign: 'center', color: 'text.secondary' }}>
                <Typography variant="body2">No active leaves or blocked dates recorded.</Typography>
              </Box>
            ) : (
              exceptions.map((exc) => (
                <Card key={exc.id} variant="outlined" sx={{ mb: 1.5 }}>
                  <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <Typography variant="subtitle2" fontWeight="bold" color="error">
                          {new Date(exc.exceptionDate).toLocaleDateString(undefined, { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' })}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {exc.reason}
                        </Typography>
                      </div>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDeleteException(exc.id)}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  </CardContent>
                </Card>
              ))
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Add Shift Dialog */}
      <Dialog open={slotDialogOpen} onClose={() => setSlotDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight="bold">Add Weekly Consultation Shift</DialogTitle>
        <DialogContent dividers>
          <FormControl fullWidth size="small" sx={{ mb: 2, mt: 1 }}>
            <InputLabel>Day of Week</InputLabel>
            <Select
              value={dayOfWeek}
              label="Day of Week"
              onChange={(e) => setDayOfWeek(e.target.value)}
            >
              {DAYS_OF_WEEK.map((d) => (
                <MenuItem key={d} value={d}>{d}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <Grid container spacing={2}>
            <Grid item xs={6}>
              <TextField
                fullWidth
                size="small"
                type="time"
                label="Start Time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                size="small"
                type="time"
                label="End Time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
          </Grid>

          <FormControl fullWidth size="small" sx={{ mt: 2 }}>
            <InputLabel>Slot Duration</InputLabel>
            <Select
              value={slotDuration}
              label="Slot Duration"
              onChange={(e) => setSlotDuration(Number(e.target.value))}
            >
              <MenuItem value={15}>15 Minutes (Fast OPD)</MenuItem>
              <MenuItem value={20}>20 Minutes</MenuItem>
              <MenuItem value={30}>30 Minutes (Standard)</MenuItem>
              <MenuItem value={45}>45 Minutes (Specialty)</MenuItem>
              <MenuItem value={60}>60 Minutes (Comprehensive)</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSlotDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveSlot}>
            Save Shift Timing
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add Leave Exception Dialog */}
      <Dialog open={exceptionDialogOpen} onClose={() => setExceptionDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight="bold">Record Leave or Holiday Block</DialogTitle>
        <DialogContent dividers>
          <TextField
            fullWidth
            size="small"
            type="date"
            label="Leave Date"
            value={leaveDate}
            onChange={(e) => setLeaveDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
            sx={{ mb: 2, mt: 1 }}
          />
          <TextField
            fullWidth
            size="small"
            label="Reason"
            value={leaveReason}
            onChange={(e) => setLeaveReason(e.target.value)}
            placeholder="e.g. Attending Medical Conference, Diwali Holiday"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExceptionDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" color="warning" onClick={handleAddException}>
            Block Date
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default Availability;
