import React, { useState, useEffect } from 'react';
import {
  Container,
  Paper,
  Typography,
  Box,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Button,
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
  Tooltip,
} from '@mui/material';
import WhatsAppIcon from '@mui/icons-material/WhatsApp';
import PhoneCallbackIcon from '@mui/icons-material/PhoneCallback';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import RefreshIcon from '@mui/icons-material/Refresh';
import axios from 'axios';

interface FollowUpItem {
  followUp: {
    id: number;
    patientId: number;
    doctorId: number;
    dueDate: string;
    intervalPreset: string;
    reason?: string;
    status: 'PENDING' | 'BOOKED' | 'COMPLETED' | 'CANCELLED';
    contactStatus: 'NOT_CONTACTED' | 'CONTACTED' | 'UNREACHABLE' | 'DECLINED' | 'CALLBACK_REQUESTED';
    contactAttemptCount: number;
  };
  patient?: {
    id: number;
    patientUid: string;
    name: string;
    phone: string;
  };
}

const FollowUpWorklist: React.FC = () => {
  const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}` };

  const [activeTab, setActiveTab] = useState<'due_today' | 'upcoming' | 'overdue' | 'all'>('due_today');
  const [worklist, setWorklist] = useState<FollowUpItem[]>([]);
  const [loading, setLoading] = useState(false);

  // Call attempt dialog state
  const [callModalOpen, setCallModalOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState<FollowUpItem | null>(null);
  const [callStatus, setCallStatus] = useState<string>('CONTACTED');
  const [callNotes, setCallNotes] = useState<string>('');

  useEffect(() => {
    loadWorklist();
  }, [activeTab]);

  const loadWorklist = async () => {
    setLoading(true);
    try {
      const res = await axios.get(`${apiUrl}/clinical/followups/worklist`, {
        params: { clinicId: 1, filter: activeTab },
        headers,
      });
      setWorklist(res.data);
    } catch (err) {
      console.error('Failed to load follow-up worklist', err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenCallModal = (item: FollowUpItem) => {
    setSelectedItem(item);
    setCallStatus(item.followUp.contactStatus);
    setCallNotes('');
    setCallModalOpen(true);
  };

  const handleSaveCallAttempt = async () => {
    if (!selectedItem) return;
    try {
      await axios.post(
        `${apiUrl}/clinical/followups/${selectedItem.followUp.id}/call-attempt`,
        null,
        {
          params: {
            contactStatus: callStatus,
            notes: callNotes,
          },
          headers,
        }
      );
      setCallModalOpen(false);
      loadWorklist();
    } catch (err) {
      console.error('Failed to record call attempt', err);
      alert('Failed to update call status');
    }
  };

  const handleBookRevisit = async (item: FollowUpItem) => {
    if (!window.confirm(`Book follow-up revisit appointment for ${item.patient?.name || 'Patient'} on ${item.followUp.dueDate}?`)) {
      return;
    }
    try {
      const res = await axios.post(
        `${apiUrl}/clinical/followups/${item.followUp.id}/convert`,
        null,
        {
          params: { date: item.followUp.dueDate },
          headers,
        }
      );
      alert(`Revisit appointment booked successfully! Booking #: ${res.data.bookingNumber}`);
      loadWorklist();
    } catch (err) {
      console.error('Failed to book revisit', err);
      alert('Failed to book appointment');
    }
  };

  const handleWhatsAppClick = (item: FollowUpItem) => {
    const phone = item.patient?.phone?.replace(/\D/g, '') || '';
    const cleanPhone = phone.startsWith('91') ? phone : '91' + phone;
    const patientName = item.patient?.name || 'Valued Patient';
    const dueDate = item.followUp.dueDate;
    const msg = encodeURIComponent(
      `Hello ${patientName}, this is a gentle reminder from PMS Clinic regarding your doctor-recommended follow-up due on ${dueDate}. Please let us know if you would like us to reserve your consultation slot.`
    );
    window.open(`https://wa.me/${cleanPhone}?text=${msg}`, '_blank');
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" color="primary">
            Follow-Up & Patient Retention Worklist
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Proactively contact patients due for follow-ups and convert revisits.
          </Typography>
        </Box>
        <IconButton onClick={loadWorklist} color="primary">
          <RefreshIcon />
        </IconButton>
      </Box>

      <Paper sx={{ borderRadius: '12px', mb: 3 }}>
        <Tabs
          value={activeTab}
          onChange={(_, val) => setActiveTab(val)}
          indicatorColor="primary"
          textColor="primary"
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          <Tab value="due_today" label="Due Today" />
          <Tab value="overdue" label="Overdue Follow-ups" />
          <Tab value="upcoming" label="Upcoming (Next 14 Days)" />
          <Tab value="all" label="All Pending" />
        </Tabs>

        <TableContainer sx={{ p: 2, minHeight: 350 }}>
          <Table size="small">
            <TableHead>
              <TableRow sx={{ backgroundColor: '#f5f7fa' }}>
                <TableCell>Patient</TableCell>
                <TableCell>Mobile Number</TableCell>
                <TableCell>Due Date</TableCell>
                <TableCell>Interval</TableCell>
                <TableCell>Clinical Reason</TableCell>
                <TableCell>Contact Status</TableCell>
                <TableCell>Attempts</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {worklist.length > 0 ? (
                worklist.map((item) => (
                  <TableRow key={item.followUp.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight="bold">
                        {item.patient?.name || 'Walk-in'}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {item.patient?.patientUid || 'N/A'}
                      </Typography>
                    </TableCell>
                    <TableCell>{item.patient?.phone || 'N/A'}</TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight="bold" color={activeTab === 'overdue' ? 'error' : 'inherit'}>
                        {item.followUp.dueDate}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip size="small" label={item.followUp.intervalPreset} variant="outlined" />
                    </TableCell>
                    <TableCell>{item.followUp.reason || 'General review'}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={item.followUp.contactStatus}
                        color={
                          item.followUp.contactStatus === 'CONTACTED' ? 'success' :
                          item.followUp.contactStatus === 'CALLBACK_REQUESTED' ? 'warning' :
                          item.followUp.contactStatus === 'UNREACHABLE' ? 'error' : 'default'
                        }
                      />
                    </TableCell>
                    <TableCell>{item.followUp.contactAttemptCount}</TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                        <Tooltip title="Send WhatsApp Recall Message">
                          <IconButton
                            size="small"
                            color="success"
                            onClick={() => handleWhatsAppClick(item)}
                          >
                            <WhatsAppIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Log Call Outcome">
                          <IconButton
                            size="small"
                            color="primary"
                            onClick={() => handleOpenCallModal(item)}
                          >
                            <PhoneCallbackIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Button
                          variant="contained"
                          size="small"
                          color="secondary"
                          startIcon={<EventAvailableIcon />}
                          disabled={item.followUp.status === 'BOOKED'}
                          onClick={() => handleBookRevisit(item)}
                        >
                          {item.followUp.status === 'BOOKED' ? 'Booked' : 'Book Revisit'}
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4 }}>
                      {loading ? 'Loading follow-up records...' : 'No follow-up records matching this filter.'}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Call Outcome Logging Dialog */}
      <Dialog open={callModalOpen} onClose={() => setCallModalOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight="bold">Record Call Outcome</DialogTitle>
        <DialogContent dividers>
          <FormControl fullWidth size="small" sx={{ mb: 2, mt: 1 }}>
            <InputLabel>Contact Status</InputLabel>
            <Select
              value={callStatus}
              label="Contact Status"
              onChange={(e) => setCallStatus(e.target.value)}
            >
              <MenuItem value="CONTACTED">Patient Contacted (Interested)</MenuItem>
              <MenuItem value="CALLBACK_REQUESTED">Callback Requested Later</MenuItem>
              <MenuItem value="UNREACHABLE">Unreachable / Switched Off</MenuItem>
              <MenuItem value="DECLINED">Declined Follow-up (Feeling Better)</MenuItem>
            </Select>
          </FormControl>
          <TextField
            fullWidth
            multiline
            rows={3}
            size="small"
            label="Call Notes / Feedback"
            placeholder="e.g. Patient mentioned BP is stable, requested callback Friday afternoon"
            value={callNotes}
            onChange={(e) => setCallNotes(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCallModalOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveCallAttempt}>
            Save Status
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default FollowUpWorklist;

