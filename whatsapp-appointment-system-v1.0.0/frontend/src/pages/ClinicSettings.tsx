import React, { useState, useEffect } from 'react';
import {
  Container,
  Paper,
  Typography,
  Box,
  Tabs,
  Tab,
  TextField,
  Button,
  Grid,
  Chip,
  LinearProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Divider,
  Switch,
  FormControlLabel,
  Stack,
  IconButton,
  Tooltip,
} from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import MessageIcon from '@mui/icons-material/Message';
import StarIcon from '@mui/icons-material/Star';
import WhatsAppIcon from '@mui/icons-material/WhatsApp';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import SaveIcon from '@mui/icons-material/Save';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

interface ClinicData {
  id: number;
  code: string;
  name: string;
  phone: string;
  email: string;
  whatsappNumber: string;
  addressLine1: string;
  city: string;
  followUpValidityDays: number;
  newCaseConsultationFee: number;
  oldCaseConsultationFee: number;
  subscriptionPlan: string;
  subscriptionStatus: string;
  subscriptionEndsAt: string;
  maxDoctors: number;
  maxStaff: number;
  prescriptionHeaderHtml: string;
  prescriptionFooterHtml: string;
  googleReviewUrl?: string;
}

interface CommLog {
  id: number;
  recipientPhone: string;
  channel: string;
  messageType: string;
  messageBody: string;
  status: string;
  sentAt: string;
}

interface MessageTemplateItem {
  id: number;
  clinicId: number;
  templateType: string;
  title: string;
  templateBody: string;
  isActive: boolean;
  updatedAt?: string;
}

const ClinicSettings: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState(0);
  const [clinic, setClinic] = useState<ClinicData | null>(null);
  const [loading, setLoading] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Template State
  const [templates, setTemplates] = useState<MessageTemplateItem[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<MessageTemplateItem | null>(null);
  const [templateEditTitle, setTemplateEditTitle] = useState('');
  const [templateEditBody, setTemplateEditBody] = useState('');
  const [templateEditActive, setTemplateEditActive] = useState(true);
  const [templateSaveSuccess, setTemplateSaveSuccess] = useState(false);

  // CSV Import State
  const [csvText, setCsvText] = useState(
    'Name,Phone,Age,Gender,BloodGroup,Address,Allergies,ChronicDiseases\n' +
    'Ramesh Sharma,+919876543210,45,Male,O+,MG Road Bangalore,Penicillin,Hypertension\n' +
    'Sunita Patel,+919876543211,38,Female,B+,Indiranagar Bangalore,None,Thyroid\n' +
    'Anil Kumar,+919876543212,52,Male,A+,Koramangala Bangalore,Sulfa,Diabetes'
  );
  const [importResult, setImportResult] = useState<any>(null);
  const [importLoading, setImportLoading] = useState(false);

  // Comm Logs State
  const [commLogs, setCommLogs] = useState<CommLog[]>([]);
  const [reminderTriggerMsg, setReminderTriggerMsg] = useState('');

  // Subscription modal
  const [subModalOpen, setSubModalOpen] = useState(false);
  const [newPlan, setNewPlan] = useState('PROFESSIONAL');
  const [extendDays, setExtendDays] = useState(30);

  useEffect(() => {
    fetchClinicData();
    fetchCommLogs();
    fetchTemplates();
  }, []);

  const fetchClinicData = async () => {
    try {
      setLoading(true);
      const res = await axios.get('/api/admin/clinics/1');
      setClinic(res.data);
    } catch (err) {
      console.error('Error fetching clinic data', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchCommLogs = async () => {
    try {
      const res = await axios.get('/api/notifications/logs?clinicId=1');
      setCommLogs(res.data);
    } catch (err) {
      console.error('Error fetching communication logs', err);
    }
  };

  const fetchTemplates = async () => {
    try {
      const res = await axios.get('/api/templates?clinicId=1');
      setTemplates(res.data);
      if (res.data.length > 0 && !selectedTemplate) {
        selectTemplateForEdit(res.data[0]);
      }
    } catch (err) {
      console.error('Error fetching templates', err);
    }
  };

  const selectTemplateForEdit = (tpl: MessageTemplateItem) => {
    setSelectedTemplate(tpl);
    setTemplateEditTitle(tpl.title);
    setTemplateEditBody(tpl.templateBody);
    setTemplateEditActive(tpl.isActive);
  };

  const handleSaveSettings = async () => {
    if (!clinic) return;
    try {
      setLoading(true);
      await axios.put('/api/admin/clinics/1', clinic);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err) {
      console.error('Failed to update clinic settings', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveTemplate = async () => {
    if (!selectedTemplate) return;
    try {
      setLoading(true);
      const res = await axios.put(`/api/templates/${selectedTemplate.id}`, {
        title: templateEditTitle,
        templateBody: templateEditBody,
        isActive: templateEditActive,
      });
      setTemplateSaveSuccess(true);
      setTimeout(() => setTemplateSaveSuccess(false), 3000);
      fetchTemplates();
      setSelectedTemplate(res.data);
    } catch (err) {
      console.error('Failed to save message template', err);
    } finally {
      setLoading(false);
    }
  };

  const handleInsertPlaceholder = (placeholder: string) => {
    setTemplateEditBody((prev) => prev + placeholder);
  };

  const renderSamplePreview = (body: string) => {
    if (!body) return '';
    return body
      .replace(/{patient_name}/g, 'Rahul Sharma')
      .replace(/{doctor_name}/g, 'Dr. Amit Verma')
      .replace(/{date}/g, '26-Nov-2026')
      .replace(/{time}/g, '10:30 AM')
      .replace(/{clinic_name}/g, clinic?.name || 'PMS Health Care')
      .replace(/{clinic_phone}/g, clinic?.phone || '+91 98765 43210')
      .replace(/{booking_id}/g, 'BK-1082')
      .replace(/{request_notes}/g, 'Follow-up consultation')
      .replace(/{prescription_link}/g, 'https://pms.app/rx/demo-rx-101')
      .replace(/{review_link}/g, clinic?.googleReviewUrl || 'https://g.page/r/pms-clinic/review');
  };

  const handleUpdateSubscription = async () => {
    try {
      await axios.put('/api/admin/clinics/1/subscription', {
        subscriptionPlan: newPlan,
        subscriptionStatus: 'ACTIVE',
        extendDays: extendDays,
        maxDoctors: newPlan === 'ENTERPRISE' ? 25 : newPlan === 'PROFESSIONAL' ? 5 : 2,
        maxStaff: newPlan === 'ENTERPRISE' ? 50 : newPlan === 'PROFESSIONAL' ? 20 : 5,
      });
      setSubModalOpen(false);
      fetchClinicData();
    } catch (err) {
      console.error('Failed to update subscription', err);
    }
  };

  const handleRunCsvImport = async () => {
    try {
      setImportLoading(true);
      setImportResult(null);
      const res = await axios.post('/api/data/patients/import-text?clinicId=1', {
        csvData: csvText,
      });
      setImportResult(res.data);
    } catch (err: any) {
      console.error('CSV import failed', err);
      setImportResult({ error: 'Import failed: ' + (err.response?.data?.message || err.message) });
    } finally {
      setImportLoading(false);
    }
  };

  const handleTriggerReminders = async () => {
    try {
      const res = await axios.post('/api/notifications/trigger-reminders');
      setReminderTriggerMsg(res.data.status || 'Reminders processed successfully.');
      fetchCommLogs();
    } catch (err) {
      setReminderTriggerMsg('Failed to trigger reminders.');
    }
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
      {/* Top Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Button
            startIcon={<ArrowBackIcon />}
            variant="outlined"
            size="small"
            onClick={() => navigate('/dashboard')}
          >
            Dashboard
          </Button>
          <Typography variant="h5" sx={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 1 }}>
            <BusinessIcon color="primary" /> Clinic Administration & Settings
          </Typography>
        </Box>
        {clinic && (
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <Chip
              label={`Plan: ${clinic.subscriptionPlan}`}
              color={clinic.subscriptionPlan === 'ENTERPRISE' ? 'primary' : 'success'}
              variant="outlined"
            />
            <Chip
              label={`Return Validity: ${clinic.followUpValidityDays} Days`}
              color="info"
              variant="outlined"
            />
          </Box>
        )}
      </Box>

      {saveSuccess && (
        <Alert severity="success" sx={{ mb: 2 }}>
          Clinic settings updated successfully!
        </Alert>
      )}

      <Paper sx={{ width: '100%' }}>
        <Tabs
          value={activeTab}
          onChange={(_, val) => setActiveTab(val)}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab label="1. Clinic Profile & Pricing" />
          <Tab label="2. SaaS Subscription & Limits" />
          <Tab label="3. Message Templates & Reviews" />
          <Tab label="4. Bulk Patient Import (CSV)" />
          <Tab label="5. Data Export & Ledgers" />
          <Tab label="6. Communication & Privacy Audit" />
        </Tabs>

        {/* Tab 0: Clinic Profile & Pricing */}
        {activeTab === 0 && clinic && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Clinic Operational Configuration
            </Typography>
            <Typography variant="body2" color="textSecondary" sx={{ mb: 3 }}>
              Manage clinic identity, consultation fee tiers (New vs Old case), and prescription letterhead.
            </Typography>

            <Grid container spacing={3}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Clinic Name"
                  value={clinic.name || ''}
                  onChange={(e) => setClinic({ ...clinic, name: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={3}>
                <TextField
                  fullWidth
                  label="Clinic Code (Identifier)"
                  value={clinic.code || ''}
                  disabled
                  helperText="Unique clinic code for MRN generation"
                />
              </Grid>
              <Grid item xs={12} sm={3}>
                <TextField
                  fullWidth
                  label="Contact Phone"
                  value={clinic.phone || ''}
                  onChange={(e) => setClinic({ ...clinic, phone: e.target.value })}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Clinic Address Line 1"
                  value={clinic.addressLine1 || ''}
                  onChange={(e) => setClinic({ ...clinic, addressLine1: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={3}>
                <TextField
                  fullWidth
                  label="City"
                  value={clinic.city || ''}
                  onChange={(e) => setClinic({ ...clinic, city: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={3}>
                <TextField
                  fullWidth
                  label="WhatsApp Helpline"
                  value={clinic.whatsappNumber || ''}
                  onChange={(e) => setClinic({ ...clinic, whatsappNumber: e.target.value })}
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Google Review URL"
                  value={clinic.googleReviewUrl || ''}
                  onChange={(e) => setClinic({ ...clinic, googleReviewUrl: e.target.value })}
                  placeholder="https://g.page/r/your-clinic-code/review"
                  helperText="Patients will receive this link to leave 5-star Google reviews after consultations."
                  InputProps={{
                    endAdornment: clinic.googleReviewUrl ? (
                      <Tooltip title="Test Google Review Link">
                        <IconButton
                          color="primary"
                          size="small"
                          onClick={() => window.open(clinic.googleReviewUrl, '_blank')}
                        >
                          <OpenInNewIcon />
                        </IconButton>
                      </Tooltip>
                    ) : undefined,
                  }}
                />
              </Grid>

              <Grid item xs={12}>
                <Divider sx={{ my: 1 }} />
                <Typography variant="subtitle1" sx={{ fontWeight: 'bold', mt: 1, mb: 1 }}>
                  Case Classification & Dual Fee Configuration
                </Typography>
              </Grid>

              <Grid item xs={12} sm={4}>
                <FormControl fullWidth>
                  <InputLabel>Follow-up Return Validity</InputLabel>
                  <Select
                    value={clinic.followUpValidityDays || 30}
                    label="Follow-up Return Validity"
                    onChange={(e) => setClinic({ ...clinic, followUpValidityDays: Number(e.target.value) })}
                  >
                    <MenuItem value={15}>15 Days</MenuItem>
                    <MenuItem value={30}>30 Days (Standard)</MenuItem>
                    <MenuItem value={60}>60 Days</MenuItem>
                    <MenuItem value={90}>90 Days</MenuItem>
                    <MenuItem value={180}>180 Days (Semi-Annual)</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  type="number"
                  label="New Case Fee (₹)"
                  value={clinic.newCaseConsultationFee || ''}
                  onChange={(e) => setClinic({ ...clinic, newCaseConsultationFee: Number(e.target.value) })}
                  helperText="Standard fee for first-time or expired return visits"
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  type="number"
                  label="Old Case Fee (₹)"
                  value={clinic.oldCaseConsultationFee || ''}
                  onChange={(e) => setClinic({ ...clinic, oldCaseConsultationFee: Number(e.target.value) })}
                  helperText="Discounted revisit fee within return validity period"
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Prescription Header Notes"
                  value={clinic.prescriptionHeaderHtml || ''}
                  onChange={(e) => setClinic({ ...clinic, prescriptionHeaderHtml: e.target.value })}
                  placeholder="e.g. Timings: 10:00 AM - 08:00 PM | Mon - Sat"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Prescription Footer / Disclaimer"
                  value={clinic.prescriptionFooterHtml || ''}
                  onChange={(e) => setClinic({ ...clinic, prescriptionFooterHtml: e.target.value })}
                  placeholder="e.g. Not valid for medico-legal purposes. In emergency, visit casualty."
                />
              </Grid>
            </Grid>

            <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                size="large"
                onClick={handleSaveSettings}
                disabled={loading}
              >
                Save Clinic Settings
              </Button>
            </Box>
          </Box>
        )}

        {/* Tab 1: SaaS Subscription & Limits */}
        {activeTab === 1 && clinic && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Subscription Tier & Resource Quotas
            </Typography>
            <Grid container spacing={3} sx={{ mt: 1 }}>
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom>
                      Active Plan
                    </Typography>
                    <Typography variant="h4" sx={{ fontWeight: 'bold', color: 'primary.main', mb: 1 }}>
                      {clinic.subscriptionPlan}
                    </Typography>
                    <Typography variant="body2" sx={{ mb: 2 }}>
                      Status: <strong>{clinic.subscriptionStatus}</strong>
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      Renewal / Expiry Date: {clinic.subscriptionEndsAt ? new Date(clinic.subscriptionEndsAt).toLocaleDateString() : 'Lifetime / Active'}
                    </Typography>
                    <Button
                      variant="contained"
                      sx={{ mt: 3 }}
                      onClick={() => setSubModalOpen(true)}
                    >
                      Upgrade / Extend Subscription
                    </Button>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom>
                      Doctor Accounts Quota
                    </Typography>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body1">Permitted Doctors</Typography>
                      <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
                        Max {clinic.maxDoctors}
                      </Typography>
                    </Box>
                    <LinearProgress variant="determinate" value={40} sx={{ height: 8, borderRadius: 4, mb: 3 }} />

                    <Typography color="textSecondary" gutterBottom>
                      Receptionist & Compounder Staff Quota
                    </Typography>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body1">Permitted Staff Accounts</Typography>
                      <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
                        Max {clinic.maxStaff}
                      </Typography>
                    </Box>
                    <LinearProgress variant="determinate" value={20} color="secondary" sx={{ height: 8, borderRadius: 4 }} />
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </Box>
        )}

        {/* Tab 2: Message Templates & Google Reviews */}
        {activeTab === 2 && (
          <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Box>
                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <MessageIcon color="primary" /> WhatsApp Message Templates & Google Review Engine
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Customise automated patient notifications for appointments, revisits, digital Rx delivery, and Google feedback.
                </Typography>
              </Box>
              {clinic?.googleReviewUrl && (
                <Button
                  variant="outlined"
                  size="small"
                  color="warning"
                  startIcon={<StarIcon />}
                  endIcon={<OpenInNewIcon />}
                  onClick={() => window.open(clinic.googleReviewUrl, '_blank')}
                >
                  Test Review Link
                </Button>
              )}
            </Box>

            {templateSaveSuccess && (
              <Alert severity="success" sx={{ mb: 2 }}>
                Message template saved and activated successfully!
              </Alert>
            )}

            {/* Template Selector Pills */}
            <Box sx={{ mb: 3, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {templates.map((tpl) => (
                <Chip
                  key={tpl.id}
                  label={tpl.title}
                  clickable
                  color={selectedTemplate?.id === tpl.id ? 'primary' : 'default'}
                  variant={selectedTemplate?.id === tpl.id ? 'filled' : 'outlined'}
                  onClick={() => selectTemplateForEdit(tpl)}
                  icon={tpl.templateType === 'REVIEW_REQUEST' ? <StarIcon /> : <WhatsAppIcon />}
                  sx={{ fontWeight: selectedTemplate?.id === tpl.id ? 'bold' : 'normal' }}
                />
              ))}
            </Box>

            {selectedTemplate && (
              <Grid container spacing={3}>
                {/* Editor Column */}
                <Grid item xs={12} md={7}>
                  <Card variant="outlined">
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                        <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
                          Template Settings ({selectedTemplate.templateType})
                        </Typography>
                        <FormControlLabel
                          control={
                            <Switch
                              checked={templateEditActive}
                              onChange={(e) => setTemplateEditActive(e.target.checked)}
                              color="primary"
                            />
                          }
                          label={templateEditActive ? 'Active' : 'Disabled'}
                        />
                      </Box>

                      <TextField
                        fullWidth
                        label="Template Display Title"
                        value={templateEditTitle}
                        onChange={(e) => setTemplateEditTitle(e.target.value)}
                        sx={{ mb: 2 }}
                      />

                      <TextField
                        fullWidth
                        multiline
                        rows={6}
                        label="Message Content (WhatsApp Text)"
                        value={templateEditBody}
                        onChange={(e) => setTemplateEditBody(e.target.value)}
                        helperText="Use placeholders below to inject dynamic patient & clinic data."
                        sx={{ mb: 2 }}
                      />

                      <Typography variant="caption" sx={{ fontWeight: 'bold', display: 'block', mb: 1 }}>
                        Click to insert dynamic variable:
                      </Typography>
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 3 }}>
                        {[
                          '{patient_name}',
                          '{doctor_name}',
                          '{date}',
                          '{time}',
                          '{clinic_name}',
                          '{clinic_phone}',
                          '{booking_id}',
                          '{review_link}',
                          '{prescription_link}',
                        ].map((ph) => (
                          <Chip
                            key={ph}
                            label={ph}
                            size="small"
                            variant="outlined"
                            clickable
                            onClick={() => handleInsertPlaceholder(ph)}
                            sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}
                          />
                        ))}
                      </Stack>

                      <Button
                        variant="contained"
                        startIcon={<SaveIcon />}
                        onClick={handleSaveTemplate}
                        disabled={loading}
                      >
                        Save Template
                      </Button>
                    </CardContent>
                  </Card>
                </Grid>

                {/* Live Preview Column */}
                <Grid item xs={12} md={5}>
                  <Card variant="outlined" sx={{ bgcolor: '#ece5dd', height: '100%', minHeight: 350 }}>
                    <Box sx={{ bgcolor: '#075e54', color: 'white', p: 1.5, display: 'flex', alignItems: 'center', gap: 1 }}>
                      <WhatsAppIcon />
                      <Box>
                        <Typography variant="subtitle2" sx={{ lineHeight: 1.2 }}>
                          {clinic?.name || 'PMS Health Clinic'}
                        </Typography>
                        <Typography variant="caption" sx={{ opacity: 0.8 }}>
                          WhatsApp Business Verified
                        </Typography>
                      </Box>
                    </Box>

                    <CardContent sx={{ p: 2 }}>
                      <Box
                        sx={{
                          bgcolor: 'white',
                          borderRadius: 2,
                          p: 2,
                          boxShadow: '0 1px 2px rgba(0,0,0,0.15)',
                          position: 'relative',
                          maxWidth: '90%',
                          whiteSpace: 'pre-line',
                        }}
                      >
                        <Typography variant="body2" sx={{ color: '#111', fontSize: '0.9rem', lineHeight: 1.5 }}>
                          {renderSamplePreview(templateEditBody)}
                        </Typography>
                        <Typography
                          variant="caption"
                          sx={{
                            display: 'block',
                            textAlign: 'right',
                            color: '#888',
                            fontSize: '0.7rem',
                            mt: 1,
                          }}
                        >
                          10:30 AM ✓✓
                        </Typography>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
            )}
          </Box>
        )}

        {/* Tab 3: Bulk Patient Import */}
        {activeTab === 3 && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Bulk Patient Data Importer (CSV)
            </Typography>
            <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
              Quickly onboard an existing clinic by importing patient records in bulk. Duplicate phone numbers are automatically detected and preserved.
            </Typography>

            <Alert severity="info" sx={{ mb: 2 }}>
              Format: <code>Name,Phone,Age,Gender,BloodGroup,Address,Allergies,ChronicDiseases</code>
            </Alert>

            <TextField
              fullWidth
              multiline
              rows={8}
              value={csvText}
              onChange={(e) => setCsvText(e.target.value)}
              sx={{ fontFamily: 'monospace', mb: 2 }}
            />

            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
              <Button
                variant="contained"
                startIcon={<CloudUploadIcon />}
                onClick={handleRunCsvImport}
                disabled={importLoading || !csvText.trim()}
              >
                {importLoading ? 'Importing Patients...' : 'Execute CSV Patient Import'}
              </Button>
            </Box>

            {importResult && (
              <Box sx={{ mt: 3 }}>
                {importResult.error ? (
                  <Alert severity="error">{importResult.error}</Alert>
                ) : (
                  <Alert severity="success">
                    Import Finished! <strong>{importResult.importedCount}</strong> patients registered successfully. ({importResult.skippedCount} skipped as duplicates/invalid).
                  </Alert>
                )}
                {importResult.errorMessages && importResult.errorMessages.length > 0 && (
                  <Paper sx={{ mt: 2, p: 2, bgcolor: '#fbfbfb', maxHeight: 200, overflowY: 'auto' }}>
                    <Typography variant="subtitle2" color="textSecondary">
                      Import Notes / Duplicate Warnings:
                    </Typography>
                    {importResult.errorMessages.map((msg: string, idx: number) => (
                      <Typography key={idx} variant="caption" display="block" color="textSecondary">
                        • {msg}
                      </Typography>
                    ))}
                  </Paper>
                )}
              </Box>
            )}
          </Box>
        )}

        {/* Tab 4: Data Export Center */}
        {activeTab === 4 && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Clinic Operational Data Export
            </Typography>
            <Typography variant="body2" color="textSecondary" sx={{ mb: 3 }}>
              Download complete, unencrypted CSV ledgers of your clinic records for backup, offline accounting (Tally/Excel), or compliance audits.
            </Typography>

            <Grid container spacing={3}>
              <Grid item xs={12} sm={4}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="h6">Patient Master Registry</Typography>
                    <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
                      All registered patients, contact info, total visits, and medical history.
                    </Typography>
                    <Button
                      variant="outlined"
                      startIcon={<DownloadIcon />}
                      href="/api/data/patients/export-csv?clinicId=1"
                      download
                    >
                      Export Patients (.csv)
                    </Button>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={4}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="h6">Appointment & Revenue Ledger</Typography>
                    <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
                      Booking dates, case type, doctor, attendance status, and fee collected.
                    </Typography>
                    <Button
                      variant="outlined"
                      startIcon={<DownloadIcon />}
                      href="/api/data/appointments/export-csv?clinicId=1"
                      download
                    >
                      Export Appointments (.csv)
                    </Button>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} sm={4}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="h6">Consultation & E-Prescription Audit</Typography>
                    <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
                      Diagnosis, vitals summary, follow-up dates, and prescribed medication items.
                    </Typography>
                    <Button
                      variant="outlined"
                      startIcon={<DownloadIcon />}
                      href="/api/data/consultations/export-csv?clinicId=1"
                      download
                    >
                      Export Consultations (.csv)
                    </Button>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </Box>
        )}

        {/* Tab 5: Communication & Privacy Audit */}
        {activeTab === 5 && (
          <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Box>
                <Typography variant="h6">Communication & Privacy Audit Trail</Typography>
                <Typography variant="body2" color="textSecondary">
                  Log of automated WhatsApp reminders, follow-up recall alerts, and privacy consents.
                </Typography>
              </Box>
              <Button
                variant="outlined"
                color="primary"
                startIcon={<NotificationsActiveIcon />}
                onClick={handleTriggerReminders}
              >
                Run Scheduled Reminders Now
              </Button>
            </Box>

            {reminderTriggerMsg && (
              <Alert severity="info" sx={{ mb: 2 }}>
                {reminderTriggerMsg}
              </Alert>
            )}

            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 'bold' }}>Recipient</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Channel</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Message Type</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Content Preview</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Sent Timestamp</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {commLogs.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                        No communication logs recorded yet. Automated reminders trigger at 9 AM and 10 AM daily.
                      </TableCell>
                    </TableRow>
                  ) : (
                    commLogs.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell>{log.recipientPhone}</TableCell>
                        <TableCell>
                          <Chip label={log.channel} size="small" />
                        </TableCell>
                        <TableCell>{log.messageType}</TableCell>
                        <TableCell sx={{ maxWidth: 300, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {log.messageBody}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={log.status}
                            size="small"
                            color={log.status === 'SENT' ? 'success' : 'error'}
                          />
                        </TableCell>
                        <TableCell>{new Date(log.sentAt).toLocaleString()}</TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}
      </Paper>

      {/* Subscription Upgrade Modal */}
      <Dialog open={subModalOpen} onClose={() => setSubModalOpen(false)}>
        <DialogTitle>Update Clinic Subscription Plan</DialogTitle>
        <DialogContent sx={{ minWidth: 400, pt: 2 }}>
          <FormControl fullWidth sx={{ mb: 2, mt: 1 }}>
            <InputLabel>Subscription Tier</InputLabel>
            <Select
              value={newPlan}
              label="Subscription Tier"
              onChange={(e) => setNewPlan(e.target.value)}
            >
              <MenuItem value="STARTER">STARTER (1 Doctor, 3 Staff)</MenuItem>
              <MenuItem value="PROFESSIONAL">PROFESSIONAL (5 Doctors, 20 Staff)</MenuItem>
              <MenuItem value="ENTERPRISE">ENTERPRISE (25 Doctors, 50 Staff)</MenuItem>
            </Select>
          </FormControl>

          <TextField
            fullWidth
            type="number"
            label="Extend Validity (Days)"
            value={extendDays}
            onChange={(e) => setExtendDays(Number(e.target.value))}
            helperText="Number of days to extend active subscription"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSubModalOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleUpdateSubscription}>
            Apply Plan Update
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ClinicSettings;
