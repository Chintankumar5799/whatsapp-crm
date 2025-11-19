import React from 'react';
import { Container, Typography, Paper } from '@mui/material';

const Appointments: React.FC = () => {
  return (
    <Container>
      <Typography variant="h4" gutterBottom>
        Appointments
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Typography>Today's appointments will be displayed here</Typography>
      </Paper>
    </Container>
  );
};

export default Appointments;

