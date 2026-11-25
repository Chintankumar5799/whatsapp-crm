import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Box,
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
  Alert,
  IconButton,
  Tooltip,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import HowToRegIcon from '@mui/icons-material/HowToReg';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import RefreshIcon from '@mui/icons-material/Refresh';
import ContactPhoneIcon from '@mui/icons-material/ContactPhone';
import SupportAgentIcon from '@mui/icons-material/SupportAgent';
import SettingsIcon from '@mui/icons-material/Settings';
import MedicalServicesIcon from '@mui/icons-material/MedicalServices';
import EventIcon from '@mui/icons-material/Event';
import ScheduleIcon from '@mui/icons-material/Schedule';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import CallMergeIcon from '@mui/icons-material/CallMerge';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import Patient360Dialog from '../components/Patient360Dialog';
import PosBillingModal from '../components/PosBillingModal';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

interface PatientDoc {
  id: number;
  fileName: string;
  documentType: string;
  fileSize: number;
  fileType: string;
  notes: string;
  createdAt: string;
}

interface Patient {
  id: number;
  patientUid: string;
  name: string;
  phone: string;
  age?: number;
  gender?: string;
  bloodGroup?: string;
  allergies?: string;
  chronicDiseases?: string;
  totalVisitsCount?: number;
  lastVisitDate?: string;
}

interface QueueItem {
  id: number;
  queueNumber: number;
  patientId: number;
  doctorId: number;
  status: 'WAITING' | 'IN_CONSULTATION' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
  arrivalTime: string;
  priorityFlag: boolean;
  bookingId?: number;
}

const ReceptionistDesk: React.FC = () => {
  const navigate = useNavigate();
  const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}` };

  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Patient[]>([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState<number>(1);
  const [queue, setQueue] = useState<QueueItem[]>([]);
  
  // Registration Dialog State
  const [registerOpen, setRegisterOpen] = useState(false);
  const [regForm, setRegForm] = useState({
    name: '',
    phone: '',
    age: '',
    gender: 'MALE',
    bloodGroup: '',
    address: '',
    allergies: '',
    chronicDiseases: '',
  });
  const [duplicateWarning, setDuplicateWarning] = useState<string | null>(null);
  const [duplicatePatientFound, setDuplicatePatientFound] = useState<Patient | null>(null);

  // Documents state
  const [docDialogOpen, setDocDialogOpen] = useState(false);
  const [selectedPatientForDoc, setSelectedPatientForDoc] = useState<Patient | null>(null);
  const [patientDocs, setPatientDocs] = useState<PatientDoc[]>([]);
  const [docType, setDocType] = useState('LAB_REPORT');
  const [docNotes, setDocNotes] = useState('');
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadLoading, setUploadLoading] = useState(false);

  // Merge state
  const [mergeDialogOpen, setMergeDialogOpen] = useState(false);
  const [selectedPrimaryPatient, setSelectedPrimaryPatient] = useState<Patient | null>(null);
  const [duplicateQuery, setDuplicateQuery] = useState('');
  const [duplicateResults, setDuplicateResults] = useState<Patient[]>([]);
  const [selectedDuplicateId, setSelectedDuplicateId] = useState<number | null>(null);
  const [mergeLoading, setMergeLoading] = useState(false);
  const [mergeSuccess, setMergeSuccess] = useState<string | null>(null);

  // Patient 360 Dialog State
  const [patient360Open, setPatient360Open] = useState(false);
  const [selectedPatient360Id, setSelectedPatient360Id] = useState<number | null>(null);

  const handleOpenPatient360 = (patientId: number) => {
    setSelectedPatient360Id(patientId);
    setPatient360Open(true);
  };

  // POS Billing Modal State
  const [posModalOpen, setPosModalOpen] = useState(false);
  const [posPatientData, setPosPatientData] = useState<{ patientId: number; patientName: string; bookingId?: number } | null>(null);

  const handleOpenPosForQueue = (q: QueueItem) => {
    setPosPatientData({ patientId: q.patientId, patientName: `Patient #${q.patientId}`, bookingId: q.bookingId });
    setPosModalOpen(true);
  };

  const handleOpenPosForPatient = (p: Patient) => {
    setPosPatientData({ patientId: p.id, patientName: p.name });
    setPosModalOpen(true);
  };

  useEffect(() => {
    loadQueue();
  }, [selectedDoctorId]);

  const loadQueue = async () => {
    try {
      const res = await axios.get(`${apiUrl}/clinical/queue/doctor/${selectedDoctorId}`, { headers });
      setQueue(res.data);
    } catch (err) {
      console.error('Failed to load queue', err);
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    try {
      const res = await axios.get(`${apiUrl}/patients/search`, {
        params: { clinicId: 1, query: searchQuery.trim() },
        headers,
      });
      setSearchResults(res.data);
    } catch (err) {
      console.error('Search failed', err);
    }
  };

  const handlePhoneBlur = async () => {
    if (!regForm.phone.trim()) return;
    try {
      const res = await axios.get(`${apiUrl}/patients/check-duplicates`, {
        params: { clinicId: 1, phone: regForm.phone.trim() },
        headers,
      });
      if (res.data && res.data.length > 0) {
        setDuplicatePatientFound(res.data[0]);
        setDuplicateWarning(`Patient with phone ${regForm.phone} already exists (${res.data[0].name}, UID: ${res.data[0].patientUid})`);
      } else {
        setDuplicatePatientFound(null);
        setDuplicateWarning(null);
      }
    } catch (err) {
      console.error('Duplicate check error', err);
    }
  };

  const handleOpenDocuments = async (p: Patient) => {
    setSelectedPatientForDoc(p);
    setDocDialogOpen(true);
    try {
      const res = await axios.get(`${apiUrl}/patients/${p.id}/documents`, { headers });
      setPatientDocs(res.data);
    } catch (err) {
      console.error('Failed to load patient documents', err);
    }
  };

  const handleUploadDocument = async () => {
    if (!uploadFile || !selectedPatientForDoc) return;
    try {
      setUploadLoading(true);
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('documentType', docType);
      if (docNotes) formData.append('notes', docNotes);

      await axios.post(`${apiUrl}/patients/${selectedPatientForDoc.id}/documents?clinicId=1`, formData, {
        headers: { ...headers, 'Content-Type': 'multipart/form-data' }
      });

      const res = await axios.get(`${apiUrl}/patients/${selectedPatientForDoc.id}/documents`, { headers });
      setPatientDocs(res.data);
      setUploadFile(null);
      setDocNotes('');
    } catch (err) {
      console.error('Document upload failed', err);
      alert('Document upload failed');
    } finally {
      setUploadLoading(false);
    }
  };

  const handleOpenMerge = (p: Patient) => {
    setSelectedPrimaryPatient(p);
    setSelectedDuplicateId(null);
    setDuplicateQuery('');
    setDuplicateResults([]);
    setMergeSuccess(null);
    setMergeDialogOpen(true);
  };

  const handleSearchDuplicate = async () => {
    if (!duplicateQuery.trim()) return;
    try {
      const res = await axios.get(`${apiUrl}/patients/search`, {
        params: { clinicId: 1, query: duplicateQuery.trim() },
        headers,
      });
      setDuplicateResults(res.data.filter((x: any) => x.id !== selectedPrimaryPatient?.id));
    } catch (err) {
      console.error('Search duplicate error', err);
    }
  };

  const handleExecuteMerge = async () => {
    if (!selectedPrimaryPatient || !selectedDuplicateId) return;
    try {
      setMergeLoading(true);
      const res = await axios.post(
        `${apiUrl}/patients/${selectedPrimaryPatient.id}/merge/${selectedDuplicateId}?clinicId=1`,
        null,
        { headers }
      );
      setMergeSuccess(`Successfully merged into ${res.data.patientUid}! Total visits updated to ${res.data.totalVisitsCount}.`);
      setSelectedDuplicateId(null);
      setDuplicateResults([]);
      handleSearch();
    } catch (err: any) {
      console.error('Merge failed', err);
      alert('Merge failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setMergeLoading(false);
    }
  };

  const handleRegisterPatient = async () => {
    if (!regForm.name || !regForm.phone) {
      alert('Name and Phone are mandatory');
      return;
    }
    try {
      const payload = {
        name: regForm.name,
        phone: regForm.phone,
        age: regForm.age ? parseInt(regForm.age) : undefined,
        gender: regForm.gender,
        bloodGroup: regForm.bloodGroup,
        address: regForm.address,
        allergies: regForm.allergies,
        chronicDiseases: regForm.chronicDiseases,
      };
      const res = await axios.post(`${apiUrl}/patients/register?clinicId=1`, payload, { headers });
      alert(`Patient registered successfully! UID: ${res.data.patientUid}`);
      setRegisterOpen(false);
      setRegForm({
        name: '',
        phone: '',
        age: '',
        gender: 'MALE',
        bloodGroup: '',
        address: '',
        allergies: '',
        chronicDiseases: '',
      });
      setDuplicateWarning(null);
      setSearchResults([res.data]);
    } catch (err) {
      console.error('Registration failed', err);
      alert('Failed to register patient');
    }
  };

  const handleCheckIn = async (patient: Patient, priority = false) => {
    try {
      await axios.post(`${apiUrl}/clinical/queue/checkin`, null, {
        params: {
          clinicId: 1,
          doctorId: selectedDoctorId,
          patientId: patient.id,
          priority,
        },
        headers,
      });
      alert(`Patient ${patient.name} checked in to Queue!`);
      loadQueue();
    } catch (err) {
      console.error('Checkin failed', err);
      alert('Failed to check in patient');
    }
  };

  const handleMarkNoShow = async (queueId: number) => {
    try {
      await axios.post(`${apiUrl}/clinical/queue/${queueId}/no-show`, null, { headers });
      loadQueue();
    } catch (err) {
      console.error('No show update failed', err);
    }
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold" color="primary">
          PMS Receptionist Desk & OPD Queue
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <Button
            variant="outlined"
            color="secondary"
            startIcon={<SupportAgentIcon />}
            onClick={() => navigate('/inquiries')}
          >
            Inquiry CRM
          </Button>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<EventIcon />}
            onClick={() => navigate('/appointments')}
          >
            Appointments
          </Button>
          <Button
            variant="outlined"
            color="info"
            startIcon={<ContactPhoneIcon />}
            onClick={() => navigate('/follow-ups')}
          >
            Follow-Ups
          </Button>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<ScheduleIcon />}
            onClick={() => navigate('/availability')}
          >
            Schedules
          </Button>
          <Button
            variant="outlined"
            color="inherit"
            startIcon={<SettingsIcon />}
            onClick={() => navigate('/clinic-settings')}
          >
            Settings
          </Button>
          <Button
            variant="contained"
            color="success"
            startIcon={<PersonAddIcon />}
            onClick={() => setRegisterOpen(true)}
          >
            Register Patient
          </Button>
          <IconButton onClick={loadQueue} color="primary">
            <RefreshIcon />
          </IconButton>
          <Button
            variant="contained"
            color="primary"
            startIcon={<MedicalServicesIcon />}
            onClick={() => navigate('/dashboard')}
            sx={{ fontWeight: 'bold', borderRadius: '20px', px: 2.5, textTransform: 'none', boxShadow: '0 2px 8px rgba(25,118,210,0.3)', ml: 1 }}
          >
            Switch to Doctor Desk ➔
          </Button>
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Left Column: Patient Search & Check-in */}
        <Grid item xs={12} md={5}>
          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography variant="h6" gutterBottom fontWeight="medium">
              Patient Quick Lookup
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
              <TextField
                fullWidth
                size="small"
                label="Search by Mobile, Name, or Patient UID"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              />
              <Button variant="contained" onClick={handleSearch}>
                Search
              </Button>
            </Box>

            {searchResults.length > 0 ? (
              <TableContainer sx={{ maxHeight: 350 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
                      <TableCell>UID / Name</TableCell>
                      <TableCell>Phone</TableCell>
                      <TableCell align="center">Action</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {searchResults.map((p) => (
                      <TableRow key={p.id}>
                        <TableCell>
                          <Typography variant="body2" fontWeight="bold">{p.name}</Typography>
                          <Typography variant="caption" color="text.secondary">{p.patientUid}</Typography>
                        </TableCell>
                        <TableCell>{p.phone}</TableCell>
                        <TableCell align="center">
                          <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'center' }}>
                            <Button
                              variant="contained"
                              size="small"
                              color="primary"
                              startIcon={<HowToRegIcon />}
                              onClick={() => handleCheckIn(p, false)}
                            >
                              Queue
                            </Button>
                            <Tooltip title="View 360° EHR Profile">
                              <IconButton size="small" color="primary" onClick={() => handleOpenPatient360(p.id)}>
                                <VisibilityIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Medical Documents / Scans">
                              <IconButton size="small" color="info" onClick={() => handleOpenDocuments(p)}>
                                <AttachFileIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Merge Duplicate Profile">
                              <IconButton size="small" color="warning" onClick={() => handleOpenMerge(p)}>
                                <CallMergeIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="In-Clinic POS Billing & Printable Receipt">
                              <IconButton size="small" color="success" onClick={() => handleOpenPosForPatient(p)}>
                                <PointOfSaleIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </Box>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
                Enter patient phone or name to check in.
              </Typography>
            )}
          </Paper>
        </Grid>

        {/* Right Column: Today's Live OPD Queue */}
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" fontWeight="medium">
                Today's OPD Waiting Queue
              </Typography>
              <Chip label={`Total in Queue: ${queue.length}`} color="primary" variant="outlined" />
            </Box>

            <TableContainer sx={{ maxHeight: 450 }}>
              <Table size="small">
                <TableHead>
                  <TableRow sx={{ backgroundColor: '#f0f4f8' }}>
                    <TableCell>Token #</TableCell>
                    <TableCell>Patient ID</TableCell>
                    <TableCell>Arrival Time</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {queue.length > 0 ? (
                    queue.map((q) => (
                      <TableRow key={q.id}>
                        <TableCell>
                          <Typography variant="subtitle1" fontWeight="bold" color="primary">
                            #{q.queueNumber}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Button
                            size="small"
                            variant="text"
                            sx={{ fontWeight: 'bold', textTransform: 'none', p: 0 }}
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
                            <Tooltip title="Mark as No-Show">
                              <IconButton size="small" color="error" onClick={() => handleMarkNoShow(q.id)}>
                                <CancelIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {q.status === 'COMPLETED' && (
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5 }}>
                              <CheckCircleIcon color="success" fontSize="small" />
                              <Button
                                size="small"
                                variant="outlined"
                                color="success"
                                startIcon={<PointOfSaleIcon fontSize="small" />}
                                sx={{ textTransform: 'none', py: 0.2, px: 0.8, fontSize: '0.75rem' }}
                                onClick={() => handleOpenPosForQueue(q)}
                              >
                                Receipt
                              </Button>
                            </Box>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography variant="body2" color="text.secondary" sx={{ py: 3 }}>
                          No patients waiting in queue today. Check in patients from the lookup desk.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        </Grid>
      </Grid>

      {/* Patient Registration Dialog */}
      <Dialog open={registerOpen} onClose={() => setRegisterOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight="bold">Register New Patient</DialogTitle>
        <DialogContent dividers>
          {duplicateWarning && (
            <Alert
              severity="warning"
              sx={{ mb: 2 }}
              action={
                duplicatePatientFound && (
                  <Button
                    color="inherit"
                    size="small"
                    onClick={() => {
                      setRegisterOpen(false);
                      handleCheckIn(duplicatePatientFound, false);
                    }}
                  >
                    Check In Existing
                  </Button>
                )
              }
            >
              {duplicateWarning}
            </Alert>
          )}
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Full Name"
                value={regForm.name}
                onChange={(e) => setRegForm({ ...regForm, name: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                required
                label="Mobile Number"
                value={regForm.phone}
                onChange={(e) => setRegForm({ ...regForm, phone: e.target.value })}
                onBlur={handlePhoneBlur}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Age"
                type="number"
                value={regForm.age}
                onChange={(e) => setRegForm({ ...regForm, age: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <FormControl fullWidth>
                <InputLabel>Gender</InputLabel>
                <Select
                  value={regForm.gender}
                  label="Gender"
                  onChange={(e) => setRegForm({ ...regForm, gender: e.target.value })}
                >
                  <MenuItem value="MALE">Male</MenuItem>
                  <MenuItem value="FEMALE">Female</MenuItem>
                  <MenuItem value="OTHER">Other</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth
                label="Blood Group"
                placeholder="e.g. O+, B+"
                value={regForm.bloodGroup}
                onChange={(e) => setRegForm({ ...regForm, bloodGroup: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Address / City"
                value={regForm.address}
                onChange={(e) => setRegForm({ ...regForm, address: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Allergies (if any)"
                placeholder="e.g. Penicillin, Sulfa"
                value={regForm.allergies}
                onChange={(e) => setRegForm({ ...regForm, allergies: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Chronic Diseases (if any)"
                placeholder="e.g. Hypertension, Diabetes Type 2"
                value={regForm.chronicDiseases}
                onChange={(e) => setRegForm({ ...regForm, chronicDiseases: e.target.value })}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRegisterOpen(false)}>Cancel</Button>
          <Button variant="contained" color="success" onClick={handleRegisterPatient}>
            Save & Register
          </Button>
        </DialogActions>
      </Dialog>

      {/* Patient Documents Dialog */}
      <Dialog open={docDialogOpen} onClose={() => setDocDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle fontWeight="bold">
          Medical Documents & Reports — {selectedPatientForDoc?.name} ({selectedPatientForDoc?.patientUid})
        </DialogTitle>
        <DialogContent dividers>
          <Box sx={{ mb: 3, p: 2, bgcolor: '#fbfbfb', borderRadius: 2 }}>
            <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
              Upload New Document (Lab Report, Scan, Prescription)
            </Typography>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={4}>
                <FormControl fullWidth size="small">
                  <InputLabel>Document Type</InputLabel>
                  <Select
                    value={docType}
                    label="Document Type"
                    onChange={(e) => setDocType(e.target.value)}
                  >
                    <MenuItem value="LAB_REPORT">Lab Blood / Urine Report</MenuItem>
                    <MenuItem value="SCAN">Imaging Scan / X-Ray / MRI</MenuItem>
                    <MenuItem value="PRESCRIPTION_SCAN">Previous Prescription Scan</MenuItem>
                    <MenuItem value="ID_PROOF">Govt ID / Health Card</MenuItem>
                    <MenuItem value="OTHER">Other Clinical Document</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  size="small"
                  label="Clinical Notes / Observation"
                  value={docNotes}
                  onChange={(e) => setDocNotes(e.target.value)}
                  placeholder="e.g. Fasting sugar 142 mg/dL"
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Button
                  variant="outlined"
                  component="label"
                  startIcon={<CloudUploadIcon />}
                  fullWidth
                  size="small"
                >
                  {uploadFile ? uploadFile.name : 'Select File'}
                  <input
                    type="file"
                    hidden
                    onChange={(e) => setUploadFile(e.target.files ? e.target.files[0] : null)}
                  />
                </Button>
              </Grid>
              <Grid item xs={12}>
                <Button
                  variant="contained"
                  color="primary"
                  onClick={handleUploadDocument}
                  disabled={uploadLoading || !uploadFile}
                  size="small"
                >
                  {uploadLoading ? 'Uploading...' : 'Upload & Attach to Patient Record'}
                </Button>
              </Grid>
            </Grid>
          </Box>

          <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
            Attached Documents ({patientDocs.length})
          </Typography>
          <TableContainer>
            <Table size="small">
              <TableHead sx={{ bgcolor: '#f0f0f0' }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 'bold' }}>File Name</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Type</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Notes</TableCell>
                  <TableCell sx={{ fontWeight: 'bold' }}>Uploaded Date</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold' }}>Download</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {patientDocs.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                      No documents attached to this patient yet.
                    </TableCell>
                  </TableRow>
                ) : (
                  patientDocs.map((doc) => (
                    <TableRow key={doc.id}>
                      <TableCell sx={{ fontWeight: 500 }}>{doc.fileName}</TableCell>
                      <TableCell><Chip label={doc.documentType} size="small" /></TableCell>
                      <TableCell>{doc.notes || '—'}</TableCell>
                      <TableCell>{new Date(doc.createdAt).toLocaleDateString()}</TableCell>
                      <TableCell align="center">
                        <IconButton
                          color="primary"
                          size="small"
                          href={`${apiUrl}/patients/documents/${doc.id}/download`}
                          target="_blank"
                        >
                          <DownloadIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDocDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Controlled Patient Merge Dialog */}
      <Dialog open={mergeDialogOpen} onClose={() => setMergeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight="bold">Controlled Profile Merging</DialogTitle>
        <DialogContent dividers>
          {mergeSuccess && (
            <Alert severity="success" sx={{ mb: 2 }}>{mergeSuccess}</Alert>
          )}

          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            You are merging into Primary Patient: <strong>{selectedPrimaryPatient?.name} ({selectedPrimaryPatient?.patientUid})</strong>.
            All bookings, past consultations, OPD queue tokens, and documents from the duplicate patient will be transferred to this profile.
          </Typography>

          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <TextField
              fullWidth
              size="small"
              label="Search duplicate patient by name, phone, or UID"
              value={duplicateQuery}
              onChange={(e) => setDuplicateQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearchDuplicate()}
            />
            <Button variant="outlined" onClick={handleSearchDuplicate}>
              Find
            </Button>
          </Box>

          {duplicateResults.length > 0 && (
            <TableContainer sx={{ maxHeight: 200, mb: 2 }}>
              <Table size="small">
                <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                  <TableRow>
                    <TableCell>Duplicate Patient</TableCell>
                    <TableCell>Phone</TableCell>
                    <TableCell align="center">Select</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {duplicateResults.map((dup) => (
                    <TableRow key={dup.id} selected={selectedDuplicateId === dup.id}>
                      <TableCell>
                        <Typography variant="body2" fontWeight="bold">{dup.name}</Typography>
                        <Typography variant="caption" color="text.secondary">{dup.patientUid}</Typography>
                      </TableCell>
                      <TableCell>{dup.phone}</TableCell>
                      <TableCell align="center">
                        <Button
                          variant={selectedDuplicateId === dup.id ? 'contained' : 'outlined'}
                          size="small"
                          color="warning"
                          onClick={() => setSelectedDuplicateId(dup.id)}
                        >
                          {selectedDuplicateId === dup.id ? 'Selected' : 'Select'}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}

          {selectedDuplicateId && (
            <Alert severity="warning">
              This action will mark duplicate patient record #{selectedDuplicateId} as merged and consolidate all visits. This cannot be undone.
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMergeDialogOpen(false)}>Close</Button>
          <Button
            variant="contained"
            color="warning"
            disabled={!selectedDuplicateId || mergeLoading}
            onClick={handleExecuteMerge}
          >
            {mergeLoading ? 'Merging...' : 'Confirm & Merge Profiles'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Patient 360-Degree EHR Profile Modal */}
      <Patient360Dialog
        open={patient360Open}
        patientId={selectedPatient360Id}
        onClose={() => setPatient360Open(false)}
      />

      {/* In-Clinic POS Billing & Receipt Modal */}
      {posPatientData && (
        <PosBillingModal
          open={posModalOpen}
          onClose={() => setPosModalOpen(false)}
          patientId={posPatientData.patientId}
          patientName={posPatientData.patientName}
          bookingId={posPatientData.bookingId}
          onSuccess={loadQueue}
        />
      )}
    </Container>
  );
};

export default ReceptionistDesk;
