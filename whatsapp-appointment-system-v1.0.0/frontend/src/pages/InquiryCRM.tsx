import React, { useState, useEffect } from 'react';
import {
  Container,
  Paper,
  Typography,
  Box,
  Grid,
  TextField,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  IconButton,
  Tooltip,
} from '@mui/material';
import PersonAddAltIcon from '@mui/icons-material/PersonAddAlt';
import WhatsAppIcon from '@mui/icons-material/WhatsApp';
import CallIcon from '@mui/icons-material/Call';
import HowToRegIcon from '@mui/icons-material/HowToReg';
import RefreshIcon from '@mui/icons-material/Refresh';
import axios from 'axios';

interface Inquiry {
  id: number;
  name: string;
  phone: string;
  requestDescription?: string;
  source: 'GOOGLE_BUSINESS' | 'WHATSAPP' | 'WEBSITE' | 'PHONE_CALL' | 'REFERRAL' | 'WALK_IN';
  status: 'NEW' | 'CONTACTED' | 'FOLLOW_UP_REQUIRED' | 'BOOKED' | 'NOT_INTERESTED' | 'CLOSED';
  nextFollowUpDate?: string;
  convertedPatientId?: number;
  convertedBookingId?: number;
  createdAt: string;
}

const InquiryCRM: React.FC = () => {
  const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}` };

  const [inquiries, setInquiries] = useState<Inquiry[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [newModalOpen, setNewModalOpen] = useState(false);

  // New Inquiry Form State
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [source, setSource] = useState('PHONE_CALL');
  const [requestDescription, setRequestDescription] = useState('');

  useEffect(() => {
    loadInquiries();
  }, [statusFilter]);

  const loadInquiries = async () => {
    try {
      const res = await axios.get(`${apiUrl}/crm/inquiries`, {
        params: {
          clinicId: 1,
          status: statusFilter === 'ALL' ? undefined : statusFilter,
        },
        headers,
      });
      setInquiries(res.data);
    } catch (err) {
      console.error('Failed to load inquiries', err);
    }
  };

  const handleCreateInquiry = async () => {
    if (!name.trim() || !phone.trim()) {
      alert('Name and Phone are mandatory.');
      return;
    }
    try {
      await axios.post(
        `${apiUrl}/crm/inquiries`,
        {
          name,
          phone,
          source,
          requestDescription,
          clinicId: 1,
        },
        { headers }
      );
      alert('Inquiry logged successfully!');
      setNewModalOpen(false);
      setName('');
      setPhone('');
      setRequestDescription('');
      loadInquiries();
    } catch (err) {
      console.error('Failed to log inquiry', err);
      alert('Failed to log inquiry');
    }
  };

  const handleUpdateStatus = async (id: number, newStatus: string) => {
    try {
      await axios.put(
        `${apiUrl}/crm/inquiries/${id}/status`,
        null,
        {
          params: { status: newStatus },
          headers,
        }
      );
      loadInquiries();
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  const handleConvertToPatient = async (inquiry: Inquiry) => {
    if (!window.confirm(`Convert ${inquiry.name} to registered patient?`)) return;
    try {
      await axios.post(
        `${apiUrl}/crm/inquiries/${inquiry.id}/convert`,
        null,
        {
          params: { doctorId: 1 },
          headers,
        }
      );
      alert(`${inquiry.name} converted to Registered Patient successfully!`);
      loadInquiries();
    } catch (err) {
      console.error('Conversion failed', err);
      alert('Failed to convert inquiry to patient');
    }
  };

  const handleWhatsAppClick = (inquiry: Inquiry) => {
    const rawPhone = inquiry.phone.replace(/\D/g, '');
    const cleanPhone = rawPhone.startsWith('91') ? rawPhone : '91' + rawPhone;
    const msg = encodeURIComponent(
      `Hello ${inquiry.name}, thank you for contacting PMS Healthcare Clinic regarding: "${inquiry.requestDescription || 'Consultation'}". How can we assist you with booking an appointment today?`
    );
    window.open(`https://wa.me/${cleanPhone}?text=${msg}`, '_blank');
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" color="primary">
            Patient Inquiry CRM & Lead Pipeline
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Capture patient leads from Google, WhatsApp, Phone calls, and Website.
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="contained"
            color="success"
            startIcon={<PersonAddAltIcon />}
            onClick={() => setNewModalOpen(true)}
          >
            Register New Inquiry
          </Button>
          <IconButton onClick={loadInquiries} color="primary">
            <RefreshIcon />
          </IconButton>
        </Box>
      </Box>

      {/* Filter and Table */}
      <Paper sx={{ p: 2, borderRadius: '12px' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Filter by Status</InputLabel>
            <Select
              value={statusFilter}
              label="Filter by Status"
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <MenuItem value="ALL">All Statuses</MenuItem>
              <MenuItem value="NEW">New Inquiries</MenuItem>
              <MenuItem value="CONTACTED">Contacted</MenuItem>
              <MenuItem value="FOLLOW_UP_REQUIRED">Follow-Up Required</MenuItem>
              <MenuItem value="BOOKED">Converted / Booked</MenuItem>
              <MenuItem value="CLOSED">Closed</MenuItem>
            </Select>
          </FormControl>
          <Typography variant="body2" color="text.secondary">
            Total Leads: {inquiries.length}
          </Typography>
        </Box>

        <TableContainer sx={{ minHeight: 400 }}>
          <Table size="small">
            <TableHead>
              <TableRow sx={{ backgroundColor: '#f5f7fa' }}>
                <TableCell>Lead Name</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Source</TableCell>
                <TableCell>Inquiry Request</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Pipeline Status</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {inquiries.length > 0 ? (
                inquiries.map((inq) => (
                  <TableRow key={inq.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight="bold">
                        {inq.name}
                      </Typography>
                    </TableCell>
                    <TableCell>{inq.phone}</TableCell>
                    <TableCell>
                      <Chip size="small" label={inq.source} variant="outlined" />
                    </TableCell>
                    <TableCell>{inq.requestDescription || 'General Inquiry'}</TableCell>
                    <TableCell>{new Date(inq.createdAt).toLocaleDateString()}</TableCell>
                    <TableCell>
                      <FormControl size="small">
                        <Select
                          value={inq.status}
                          size="small"
                          onChange={(e) => handleUpdateStatus(inq.id, e.target.value)}
                        >
                          <MenuItem value="NEW">NEW</MenuItem>
                          <MenuItem value="CONTACTED">CONTACTED</MenuItem>
                          <MenuItem value="FOLLOW_UP_REQUIRED">FOLLOW UP</MenuItem>
                          <MenuItem value="BOOKED">BOOKED</MenuItem>
                          <MenuItem value="NOT_INTERESTED">NOT INTERESTED</MenuItem>
                          <MenuItem value="CLOSED">CLOSED</MenuItem>
                        </Select>
                      </FormControl>
                    </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                        <Tooltip title="Direct WhatsApp Chat">
                          <IconButton
                            size="small"
                            color="success"
                            onClick={() => handleWhatsAppClick(inq)}
                          >
                            <WhatsAppIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Direct Call">
                          <IconButton
                            size="small"
                            color="primary"
                            onClick={() => window.open(`tel:${inq.phone}`)}
                          >
                            <CallIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Button
                          variant="contained"
                          size="small"
                          color="primary"
                          startIcon={<HowToRegIcon />}
                          disabled={inq.status === 'BOOKED'}
                          onClick={() => handleConvertToPatient(inq)}
                        >
                          {inq.status === 'BOOKED' ? 'Converted' : 'Convert to Patient'}
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <Typography variant="body2" color="text.secondary" sx={{ py: 4 }}>
                      No inquiries found. Register incoming inquiries from calls or website leads.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* New Inquiry Dialog */}
      <Dialog open={newModalOpen} onClose={() => setNewModalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight="bold">Register New Patient Inquiry</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Full Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Mobile Number"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Inquiry Source</InputLabel>
                <Select
                  value={source}
                  label="Inquiry Source"
                  onChange={(e) => setSource(e.target.value)}
                >
                  <MenuItem value="PHONE_CALL">Direct Phone Call</MenuItem>
                  <MenuItem value="WHATSAPP">WhatsApp Message</MenuItem>
                  <MenuItem value="GOOGLE_BUSINESS">Google Business Profile</MenuItem>
                  <MenuItem value="WEBSITE">Website Contact Form</MenuItem>
                  <MenuItem value="WALK_IN">Walk-in Inquiry</MenuItem>
                  <MenuItem value="REFERRAL">Doctor / Patient Referral</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Patient Request / Notes"
                placeholder="e.g. Inquiring about diabetes consultation timing and specialist availability"
                value={requestDescription}
                onChange={(e) => setRequestDescription(e.target.value)}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNewModalOpen(false)}>Cancel</Button>
          <Button variant="contained" color="success" onClick={handleCreateInquiry}>
            Log Lead
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default InquiryCRM;

