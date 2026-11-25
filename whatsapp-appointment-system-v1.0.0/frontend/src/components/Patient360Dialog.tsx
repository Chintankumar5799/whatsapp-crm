import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Tabs,
  Tab,
  Chip,
  Grid,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Alert,
  Divider,
  CircularProgress,
  IconButton,
  Stack,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import PersonIcon from '@mui/icons-material/Person';
import MedicalServicesIcon from '@mui/icons-material/MedicalServices';
import PrintIcon from '@mui/icons-material/Print';
import DescriptionIcon from '@mui/icons-material/Description';
import FavoriteIcon from '@mui/icons-material/Favorite';
import EventIcon from '@mui/icons-material/Event';
import DownloadIcon from '@mui/icons-material/Download';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import axios from 'axios';

interface Patient360Data {
  patient: {
    id: number;
    patientUid: string;
    name: string;
    phone: string;
    age: number;
    gender: string;
    bloodGroup: string;
    address: string;
    allergies: string;
    chronicDiseases: string;
    totalVisitsCount: number;
    lastVisitDate: string;
    isMerged: boolean;
  };
  totalLifetimeVisits: number;
  totalLifetimeRevenue: number;
  consultations: Array<{
    id: number;
    doctorId: number;
    doctorName: string;
    caseType: string;
    consultationFee: number;
    chiefComplaint: string;
    clinicalExamination: string;
    diagnosis: string;
    doctorNotes: string;
    finalizedAt: string;
    bpSystolic?: number;
    bpDiastolic?: number;
    pulse?: number;
    temperature?: number;
    weight?: number;
    spo2?: number;
    pdfUrl: string;
    dischargePdfUrl: string;
    prescriptionItemsCount: number;
    prescriptionItems: Array<{
      medicineName: string;
      dosage: string;
      frequency: string;
      durationDays: number;
      instructions: string;
    }>;
  }>;
  vitalsHistory: Array<{
    recordedAt: string;
    bp: string;
    bpSystolic?: number;
    bpDiastolic?: number;
    pulse?: number;
    temperature?: number;
    weight?: number;
    spo2?: number;
  }>;
  appointments: Array<{
    id: number;
    bookingNumber: string;
    bookingDate: string;
    startTime: string;
    doctorName: string;
    status: string;
    amount: number;
    paymentStatus: string;
    appointmentSource: string;
  }>;
  documents: Array<{
    id: number;
    documentType: string;
    fileName: string;
    fileType: string;
    fileSize: number;
    notes: string;
    uploadedAt: string;
    downloadUrl: string;
  }>;
  followUps: Array<{
    id: number;
    dueDate: string;
    doctorName: string;
    reason: string;
    status: string;
    contactStatus: string;
  }>;
}

interface Patient360DialogProps {
  patientId: number | null;
  open: boolean;
  onClose: () => void;
}

const Patient360Dialog: React.FC<Patient360DialogProps> = ({ patientId, open, onClose }) => {
  const [activeTab, setActiveTab] = useState(0);
  const [data, setData] = useState<Patient360Data | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open && patientId) {
      fetchPatient360View(patientId);
    } else {
      setData(null);
      setActiveTab(0);
      setError(null);
    }
  }, [open, patientId]);

  const fetchPatient360View = async (id: number) => {
    try {
      setLoading(true);
      setError(null);
      const res = await axios.get(`/api/patients/${id}/360-view`);
      setData(res.data);
    } catch (err: any) {
      console.error('Error fetching patient 360 view', err);
      setError('Could not load patient clinical history. ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  if (!open) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle sx={{ m: 0, p: 2, bgcolor: '#f8fafc', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <PersonIcon color="primary" fontSize="large" />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 'bold', lineHeight: 1.2 }}>
              {data ? data.patient.name : 'Patient Medical Profile'}
            </Typography>
            <Typography variant="caption" color="textSecondary">
              Comprehensive 360° Electronic Health Record & Visit Timeline
            </Typography>
          </Box>
        </Box>
        <IconButton onClick={onClose} size="small">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers sx={{ p: 0 }}>
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        )}

        {error && (
          <Box sx={{ p: 3 }}>
            <Alert severity="error">{error}</Alert>
          </Box>
        )}

        {data && !loading && (
          <Box>
            {/* Top Demographics Strip */}
            <Box sx={{ p: 2.5, bgcolor: '#ffffff', borderBottom: '1px solid #e2e8f0' }}>
              <Grid container spacing={2} alignItems="center">
                <Grid item xs={12} md={7}>
                  <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
                    <Chip label={`UID: ${data.patient.patientUid || 'N/A'}`} color="primary" size="small" />
                    <Chip label={`${data.patient.age || 'Unknown'} Yrs / ${data.patient.gender || 'Unknown'}`} size="small" variant="outlined" />
                    {data.patient.bloodGroup && (
                      <Chip label={`Blood: ${data.patient.bloodGroup}`} color="error" size="small" variant="outlined" />
                    )}
                    <Chip label={`Total Visits: ${data.totalLifetimeVisits}`} color="success" size="small" />
                    <Chip label={`Lifetime Spend: ₹${data.totalLifetimeRevenue}`} size="small" variant="outlined" />
                  </Stack>
                  <Typography variant="body2" color="textSecondary">
                    <strong>Phone:</strong> {data.patient.phone} | <strong>Address:</strong> {data.patient.address || 'Not specified'}
                  </Typography>
                </Grid>

                <Grid item xs={12} md={5}>
                  {(data.patient.allergies || data.patient.chronicDiseases) && (
                    <Alert severity="warning" icon={<WarningAmberIcon />} sx={{ py: 0.5, px: 1.5 }}>
                      {data.patient.allergies && (
                        <Typography variant="caption" display="block">
                          <strong>Allergies:</strong> {data.patient.allergies}
                        </Typography>
                      )}
                      {data.patient.chronicDiseases && (
                        <Typography variant="caption" display="block">
                          <strong>Chronic:</strong> {data.patient.chronicDiseases}
                        </Typography>
                      )}
                    </Alert>
                  )}
                </Grid>
              </Grid>
            </Box>

            {/* Tab Navigation */}
            <Tabs
              value={activeTab}
              onChange={(_, val) => setActiveTab(val)}
              indicatorColor="primary"
              textColor="primary"
              variant="fullWidth"
              sx={{ borderBottom: '1px solid #e2e8f0', bgcolor: '#fbfbfb' }}
            >
              <Tab icon={<MedicalServicesIcon fontSize="small" />} iconPosition="start" label={`Visits & Prescriptions (${data.consultations.length})`} />
              <Tab icon={<FavoriteIcon fontSize="small" />} iconPosition="start" label="Vitals History" />
              <Tab icon={<DescriptionIcon fontSize="small" />} iconPosition="start" label={`Documents (${data.documents.length})`} />
              <Tab icon={<EventIcon fontSize="small" />} iconPosition="start" label={`Appointments (${data.appointments.length})`} />
            </Tabs>

            {/* Tab 0: Visit History & Prescriptions */}
            {activeTab === 0 && (
              <Box sx={{ p: 2.5 }}>
                {data.consultations.length === 0 ? (
                  <Alert severity="info">No clinical consultations recorded yet for this patient.</Alert>
                ) : (
                  <Stack spacing={2.5}>
                    {data.consultations.map((c) => (
                      <Card key={c.id} variant="outlined" sx={{ borderRadius: 2 }}>
                        <CardContent>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1.5 }}>
                            <Box>
                              <Typography variant="subtitle1" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                                Consultation on {new Date(c.finalizedAt).toLocaleDateString()} at {new Date(c.finalizedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </Typography>
                              <Typography variant="body2" color="textSecondary">
                                Attending Doctor: <strong>{c.doctorName}</strong> | Case: <Chip label={c.caseType} size="small" sx={{ ml: 0.5 }} /> | Fee: ₹{c.consultationFee}
                              </Typography>
                            </Box>
                            <Box sx={{ display: 'flex', gap: 1 }}>
                              <Button
                                size="small"
                                variant="outlined"
                                startIcon={<PrintIcon />}
                                onClick={() => window.open(c.pdfUrl, '_blank')}
                              >
                                Prescription PDF
                              </Button>
                              <Button
                                size="small"
                                variant="outlined"
                                color="secondary"
                                startIcon={<DescriptionIcon />}
                                onClick={() => window.open(c.dischargePdfUrl, '_blank')}
                              >
                                Discharge Summary
                              </Button>
                            </Box>
                          </Box>

                          <Divider sx={{ my: 1 }} />

                          {/* Chief Complaint & Diagnosis */}
                          <Grid container spacing={2} sx={{ mb: 1.5 }}>
                            <Grid item xs={12} sm={6}>
                              <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'text.secondary', display: 'block' }}>
                                CHIEF COMPLAINT
                              </Typography>
                              <Typography variant="body2">{c.chiefComplaint || 'None'}</Typography>
                            </Grid>
                            <Grid item xs={12} sm={6}>
                              <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'text.secondary', display: 'block' }}>
                                PROVISIONAL DIAGNOSIS
                              </Typography>
                              <Typography variant="body2" sx={{ fontWeight: 500, color: 'error.main' }}>
                                {c.diagnosis || 'None'}
                              </Typography>
                            </Grid>
                          </Grid>

                          {/* Vitals Snapshot */}
                          {(c.bpSystolic || c.pulse || c.temperature || c.weight) && (
                            <Box sx={{ p: 1, bgcolor: '#f8fafc', borderRadius: 1, mb: 1.5, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                              {c.bpSystolic && (
                                <Typography variant="caption"><strong>BP:</strong> {c.bpSystolic}/{c.bpDiastolic} mmHg</Typography>
                              )}
                              {c.pulse && (
                                <Typography variant="caption"><strong>Pulse:</strong> {c.pulse} bpm</Typography>
                              )}
                              {c.temperature && (
                                <Typography variant="caption"><strong>Temp:</strong> {c.temperature} °F</Typography>
                              )}
                              {c.weight && (
                                <Typography variant="caption"><strong>Weight:</strong> {c.weight} kg</Typography>
                              )}
                              {c.spo2 && (
                                <Typography variant="caption"><strong>SpO2:</strong> {c.spo2} %</Typography>
                              )}
                            </Box>
                          )}

                          {/* Prescribed Items Table */}
                          {c.prescriptionItems && c.prescriptionItems.length > 0 && (
                            <Box sx={{ mt: 1 }}>
                              <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'text.secondary', display: 'block', mb: 0.5 }}>
                                PRESCRIBED MEDICATIONS ({c.prescriptionItems.length})
                              </Typography>
                              <TableContainer component={Paper} variant="outlined">
                                <Table size="small">
                                  <TableHead sx={{ bgcolor: '#f1f5f9' }}>
                                    <TableRow>
                                      <TableCell sx={{ fontWeight: 'bold' }}>Medicine</TableCell>
                                      <TableCell sx={{ fontWeight: 'bold' }}>Dosage</TableCell>
                                      <TableCell sx={{ fontWeight: 'bold' }}>Frequency</TableCell>
                                      <TableCell sx={{ fontWeight: 'bold' }}>Duration</TableCell>
                                      <TableCell sx={{ fontWeight: 'bold' }}>Instructions</TableCell>
                                    </TableRow>
                                  </TableHead>
                                  <TableBody>
                                    {c.prescriptionItems.map((rx, idx) => (
                                      <TableRow key={idx}>
                                        <TableCell sx={{ fontWeight: 500 }}>{rx.medicineName}</TableCell>
                                        <TableCell>{rx.dosage || '-'}</TableCell>
                                        <TableCell>{rx.frequency || '-'}</TableCell>
                                        <TableCell>{rx.durationDays ? `${rx.durationDays} Days` : '-'}</TableCell>
                                        <TableCell>{rx.instructions || '-'}</TableCell>
                                      </TableRow>
                                    ))}
                                  </TableBody>
                                </Table>
                              </TableContainer>
                            </Box>
                          )}
                        </CardContent>
                      </Card>
                    ))}
                  </Stack>
                )}
              </Box>
            )}

            {/* Tab 1: Vitals History */}
            {activeTab === 1 && (
              <Box sx={{ p: 2.5 }}>
                {data.vitalsHistory.length === 0 ? (
                  <Alert severity="info">No vitals readings recorded across visits yet.</Alert>
                ) : (
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1.5 }}>
                      Chronological Vitals Trend Across Visits
                    </Typography>
                    <TableContainer component={Paper} variant="outlined">
                      <Table size="small">
                        <TableHead sx={{ bgcolor: '#f8fafc' }}>
                          <TableRow>
                            <TableCell sx={{ fontWeight: 'bold' }}>Visit Date & Time</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Blood Pressure (mmHg)</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Pulse (bpm)</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Temperature (°F)</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Weight (kg)</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>SpO2 (%)</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {data.vitalsHistory.map((v, i) => (
                            <TableRow key={i}>
                              <TableCell>{new Date(v.recordedAt).toLocaleString()}</TableCell>
                              <TableCell sx={{ fontWeight: 'bold', color: 'primary.main' }}>{v.bp || '-'}</TableCell>
                              <TableCell>{v.pulse ? `${v.pulse} bpm` : '-'}</TableCell>
                              <TableCell>{v.temperature ? `${v.temperature} °F` : '-'}</TableCell>
                              <TableCell>{v.weight ? `${v.weight} kg` : '-'}</TableCell>
                              <TableCell>{v.spo2 ? `${v.spo2}%` : '-'}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </Box>
                )}
              </Box>
            )}

            {/* Tab 2: Medical Documents */}
            {activeTab === 2 && (
              <Box sx={{ p: 2.5 }}>
                {data.documents.length === 0 ? (
                  <Alert severity="info">No documents or reports attached to this patient profile.</Alert>
                ) : (
                  <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                      <TableHead sx={{ bgcolor: '#f8fafc' }}>
                        <TableRow>
                          <TableCell sx={{ fontWeight: 'bold' }}>Category</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }}>File Name</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }}>Notes / Description</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }}>Uploaded At</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }} align="center">Action</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {data.documents.map((d) => (
                          <TableRow key={d.id}>
                            <TableCell>
                              <Chip label={d.documentType} size="small" color="primary" variant="outlined" />
                            </TableCell>
                            <TableCell sx={{ fontWeight: 500 }}>{d.fileName}</TableCell>
                            <TableCell>{d.notes || '-'}</TableCell>
                            <TableCell>{new Date(d.uploadedAt).toLocaleString()}</TableCell>
                            <TableCell align="center">
                              <Button
                                size="small"
                                variant="outlined"
                                startIcon={<DownloadIcon />}
                                href={d.downloadUrl}
                                target="_blank"
                              >
                                View / Download
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </Box>
            )}

            {/* Tab 3: Appointments & Follow-ups */}
            {activeTab === 3 && (
              <Box sx={{ p: 2.5 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1.5 }}>
                  Appointment History & Revisit Recalls
                </Typography>
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 'bold' }}>Booking #</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Date & Slot</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Attending Doctor</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Source</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {data.appointments.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={5} align="center">No appointment records found.</TableCell>
                        </TableRow>
                      ) : (
                        data.appointments.map((a) => (
                          <TableRow key={a.id}>
                            <TableCell sx={{ fontWeight: 'bold' }}>{a.bookingNumber || `#${a.id}`}</TableCell>
                            <TableCell>{a.bookingDate} at {a.startTime}</TableCell>
                            <TableCell>{a.doctorName}</TableCell>
                            <TableCell>
                              <Chip label={a.appointmentSource} size="small" variant="outlined" />
                            </TableCell>
                            <TableCell>
                              <Chip
                                label={a.status}
                                size="small"
                                color={a.status === 'COMPLETED' ? 'success' : a.status === 'CANCELLED' ? 'error' : 'primary'}
                              />
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            )}
          </Box>
        )}
      </DialogContent>

      <DialogActions sx={{ p: 2, bgcolor: '#f8fafc' }}>
        <Button onClick={onClose} variant="contained">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default Patient360Dialog;

