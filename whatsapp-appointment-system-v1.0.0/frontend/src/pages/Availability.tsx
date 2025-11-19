import React from 'react';
import { Container, Typography, Paper } from '@mui/material';

const Availability: React.FC = () => {
  return (
    <Container>
      <Typography variant="h4" gutterBottom>
        Availability Management
      </Typography>
      <Paper sx={{ p: 3 }}>
        <Typography>Availability editor will be displayed here</Typography>
      </Paper>
    </Container>
  );
};

export default Availability;

