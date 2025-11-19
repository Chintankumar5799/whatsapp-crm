import React from 'react';
import { Container, Typography, Paper } from '@mui/material';

const Billing: React.FC = () => {
  return (
    <Container>
      <Typography variant="h4" gutterBottom>
        Billing
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Typography>Billing and invoices will be displayed here</Typography>
      </Paper>
    </Container>
  );
};

export default Billing;

