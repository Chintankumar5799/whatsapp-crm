import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Grid,
  TextField,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Chip,
  RadioGroup,
  FormControlLabel,
  Radio,
  Alert,
  Divider,
  Stack,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import PrintIcon from '@mui/icons-material/Print';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import axios from 'axios';

interface PosBillItem {
  itemName: string;
  quantity: number;
  unitPrice: number;
}

interface PosBillingModalProps {
  open: boolean;
  onClose: () => void;
  patientId: number;
  patientName: string;
  doctorId?: number;
  doctorName?: string;
  bookingId?: number;
  consultationId?: number;
  defaultFee?: number;
  onSuccess?: () => void;
}

const COMMON_PROCEDURES = [
  { name: 'Consultation Fee', price: 500 },
  { name: 'Wound Dressing / Bandage', price: 150 },
  { name: 'IM / IV Injection', price: 50 },
  { name: 'Nebulization Session', price: 200 },
  { name: '12-Lead ECG Test', price: 300 },
  { name: 'Random Blood Sugar (RBS)', price: 80 },
  { name: 'Suture Removal', price: 250 },
];

const PosBillingModal: React.FC<PosBillingModalProps> = ({
  open,
  onClose,
  patientId,
  patientName,
  doctorId = 1,
  doctorName = 'Attending Physician',
  bookingId,
  consultationId,
  defaultFee = 500,
  onSuccess,
}) => {
  const [items, setItems] = useState<PosBillItem[]>([
    { itemName: 'Outpatient Consultation Fee', quantity: 1, unitPrice: defaultFee },
  ]);
  const [newItemName, setNewItemName] = useState('');
  const [newItemPrice, setNewItemPrice] = useState<number | ''>('');
  const [discountAmount, setDiscountAmount] = useState<number>(0);
  const [paymentMode, setPaymentMode] = useState<'CASH' | 'UPI_QR' | 'CARD' | 'ONLINE'>('CASH');
  const [upiId, setUpiId] = useState<string>('pms.clinic@upi');
  const [clinicName, setClinicName] = useState<string>('PMS Healthcare Clinic');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setItems([{ itemName: 'Outpatient Consultation Fee', quantity: 1, unitPrice: defaultFee }]);
      setDiscountAmount(0);
      setPaymentMode('CASH');
      setError(null);
      fetchClinicUpi();
    }
  }, [open, defaultFee]);

  const fetchClinicUpi = async () => {
    try {
      const res = await axios.get('/api/payments/clinic-upi?clinicId=1');
      if (res.data?.upiId) setUpiId(res.data.upiId);
      if (res.data?.clinicName) setClinicName(res.data.clinicName);
    } catch (e) {
      // Use defaults
    }
  };

  const handleAddItem = (name: string, price: number) => {
    setItems((prev) => [...prev, { itemName: name, quantity: 1, unitPrice: price }]);
  };

  const handleAddCustomItem = () => {
    if (!newItemName.trim() || !newItemPrice || Number(newItemPrice) <= 0) return;
    setItems((prev) => [
      ...prev,
      { itemName: newItemName.trim(), quantity: 1, unitPrice: Number(newItemPrice) },
    ]);
    setNewItemName('');
    setNewItemPrice('');
  };

  const handleRemoveItem = (index: number) => {
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const subtotal = items.reduce((sum, it) => sum + it.unitPrice * (it.quantity || 1), 0);
  const netTotal = Math.max(0, subtotal - (discountAmount || 0));

  // Dynamic UPI payment URI for Indian BHIM/UPI apps
  const upiUri = `upi://pay?pa=${encodeURIComponent(upiId)}&pn=${encodeURIComponent(
    clinicName
  )}&am=${netTotal}&cu=INR&tn=${encodeURIComponent(`Bill-${patientName}`)}`;
  const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(
    upiUri
  )}`;

  const handleCollectAndPrint = async () => {
    try {
      setSubmitting(true);
      setError(null);

      const payload = {
        clinicId: 1,
        patientId,
        doctorId,
        bookingId,
        consultationId,
        paymentMode,
        discountAmount,
        items: items.map((it) => ({
          itemName: it.itemName,
          quantity: it.quantity,
          unitPrice: it.unitPrice,
        })),
      };

      const res = await axios.post('/api/payments/pos-bill', payload);
      const invoiceId = res.data.id;

      // Automatically launch receipt PDF in new tab
      window.open(`/api/payments/invoices/${invoiceId}/pdf`, '_blank');

      if (onSuccess) {
        onSuccess();
      }
      onClose();
    } catch (err: any) {
      console.error('Failed to generate POS bill', err);
      setError(err.response?.data?.message || err.message || 'Payment processing failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ m: 0, p: 2, bgcolor: '#f8fafc', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <PointOfSaleIcon color="primary" fontSize="large" />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 'bold', lineHeight: 1.2 }}>
              Clinic Cash Desk & Procedure Billing
            </Typography>
            <Typography variant="caption" color="textSecondary">
              Itemized receipt generator, procedure billing, and instant UPI QR settlement
            </Typography>
          </Box>
        </Box>
        <IconButton onClick={onClose} size="small">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers sx={{ p: 2.5 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* Patient & Doctor Context Bar */}
        <Paper variant="outlined" sx={{ p: 1.5, mb: 2.5, bgcolor: '#fafafa' }}>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" color="textSecondary" display="block">
                PATIENT
              </Typography>
              <Typography variant="subtitle1" fontWeight="bold">
                {patientName} <Typography variant="caption" color="textSecondary">(ID #{patientId})</Typography>
              </Typography>
            </Grid>
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" color="textSecondary" display="block">
                DOCTOR
              </Typography>
              <Typography variant="subtitle1" fontWeight="bold">
                {doctorName}
              </Typography>
            </Grid>
          </Grid>
        </Paper>

        {/* Procedure Quick-Add Pills */}
        <Box sx={{ mb: 2 }}>
          <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'text.secondary', display: 'block', mb: 1 }}>
            QUICK ADD CLINIC PROCEDURES:
          </Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            {COMMON_PROCEDURES.map((proc, i) => (
              <Chip
                key={i}
                label={`${proc.name} (₹${proc.price})`}
                size="small"
                variant="outlined"
                clickable
                color="primary"
                icon={<AddCircleOutlineIcon />}
                onClick={() => handleAddItem(proc.name, proc.price)}
              />
            ))}
          </Stack>
        </Box>

        {/* Custom Procedure Line Adder */}
        <Box sx={{ display: 'flex', gap: 1, mb: 2, alignItems: 'center' }}>
          <TextField
            size="small"
            placeholder="Custom procedure or medicine charge"
            value={newItemName}
            onChange={(e) => setNewItemName(e.target.value)}
            sx={{ flexGrow: 1 }}
          />
          <TextField
            size="small"
            type="number"
            placeholder="Rate (₹)"
            value={newItemPrice}
            onChange={(e) => setNewItemPrice(e.target.value ? Number(e.target.value) : '')}
            sx={{ width: 130 }}
          />
          <Button
            variant="outlined"
            size="medium"
            startIcon={<AddCircleOutlineIcon />}
            onClick={handleAddCustomItem}
            disabled={!newItemName.trim() || !newItemPrice}
          >
            Add
          </Button>
        </Box>

        {/* Bill Items Table */}
        <TableContainer component={Paper} variant="outlined" sx={{ mb: 2.5 }}>
          <Table size="small">
            <TableHead sx={{ bgcolor: '#f8fafc' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 'bold' }}>Item / Clinical Service</TableCell>
                <TableCell sx={{ fontWeight: 'bold', width: 70 }} align="center">Qty</TableCell>
                <TableCell sx={{ fontWeight: 'bold', width: 120 }} align="right">Rate (₹)</TableCell>
                <TableCell sx={{ fontWeight: 'bold', width: 120 }} align="right">Total (₹)</TableCell>
                <TableCell sx={{ width: 50 }} align="center"></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((it, idx) => (
                <TableRow key={idx}>
                  <TableCell sx={{ fontWeight: 500 }}>{it.itemName}</TableCell>
                  <TableCell align="center">{it.quantity}</TableCell>
                  <TableCell align="right">₹{it.unitPrice.toFixed(2)}</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                    ₹{(it.unitPrice * (it.quantity || 1)).toFixed(2)}
                  </TableCell>
                  <TableCell align="center">
                    <IconButton size="small" color="error" onClick={() => handleRemoveItem(idx)}>
                      <DeleteOutlineIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        {/* Totals & Payment Method Row */}
        <Grid container spacing={3}>
          {/* Payment Method Selector */}
          <Grid item xs={12} md={7}>
            <Paper variant="outlined" sx={{ p: 2, height: '100%' }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                Select Settlement Mode
              </Typography>
              <RadioGroup
                row
                value={paymentMode}
                onChange={(e) => setPaymentMode(e.target.value as any)}
                sx={{ mb: 2 }}
              >
                <FormControlLabel value="CASH" control={<Radio size="small" />} label="Cash" />
                <FormControlLabel value="UPI_QR" control={<Radio size="small" />} label="UPI (Scan QR)" />
                <FormControlLabel value="CARD" control={<Radio size="small" />} label="Card / POS" />
              </RadioGroup>

              {paymentMode === 'UPI_QR' && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, bgcolor: '#f0fdf4', p: 1.5, borderRadius: 2, border: '1px dashed #22c55e' }}>
                  <Box
                    component="img"
                    src={qrCodeUrl}
                    alt="UPI QR Code"
                    sx={{ width: 120, height: 120, borderRadius: 1, bgcolor: 'white', p: 0.5, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}
                  />
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: 'success.main' }}>
                      Scan & Pay ₹{netTotal}
                    </Typography>
                    <Typography variant="caption" color="textSecondary" display="block">
                      UPI ID: <strong>{upiId}</strong>
                    </Typography>
                    <Typography variant="caption" color="textSecondary" display="block">
                      Works with GPay, PhonePe, Paytm, BHIM
                    </Typography>
                  </Box>
                </Box>
              )}

              {paymentMode === 'CASH' && (
                <Typography variant="body2" color="textSecondary" sx={{ bgcolor: '#fafafa', p: 1.5, borderRadius: 1 }}>
                  Patient pays in direct physical currency. An authorized cash receipt will be generated.
                </Typography>
              )}

              {paymentMode === 'CARD' && (
                <Typography variant="body2" color="textSecondary" sx={{ bgcolor: '#fafafa', p: 1.5, borderRadius: 1 }}>
                  Swipe or tap debit/credit card on clinic EDC card machine.
                </Typography>
              )}
            </Paper>
          </Grid>

          {/* Amount Calculation Box */}
          <Grid item xs={12} md={5}>
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2" color="textSecondary">Gross Subtotal:</Typography>
                <Typography variant="body1">₹{subtotal.toFixed(2)}</Typography>
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Typography variant="body2" color="textSecondary">Authorized Discount:</Typography>
                <TextField
                  size="small"
                  type="number"
                  value={discountAmount}
                  onChange={(e) => setDiscountAmount(Math.max(0, Number(e.target.value)))}
                  sx={{ width: 100 }}
                  InputProps={{ startAdornment: <Typography variant="caption" sx={{ mr: 0.5 }}>₹</Typography> }}
                />
              </Box>

              <Divider sx={{ my: 1 }} />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="subtitle1" fontWeight="bold">Net Payable:</Typography>
                <Typography variant="h5" fontWeight="bold" color="primary.main">
                  ₹{netTotal.toFixed(2)}
                </Typography>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions sx={{ p: 2, bgcolor: '#f8fafc', display: 'flex', justifyContent: 'space-between' }}>
        <Button onClick={onClose} color="inherit">
          Cancel
        </Button>
        <Button
          variant="contained"
          color="success"
          size="large"
          startIcon={<PrintIcon />}
          onClick={handleCollectAndPrint}
          disabled={submitting || items.length === 0}
        >
          {submitting ? 'Generating Receipt...' : 'Collect ₹' + netTotal + ' & Print Receipt'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default PosBillingModal;

