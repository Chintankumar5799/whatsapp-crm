import React, { useEffect, useState } from 'react';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Box,
  AppBar,
  Toolbar,
  Button,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import TodayIcon from '@mui/icons-material/Today';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import axios from 'axios';

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState({ today: 0, pending: 0 });
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    // Fetch dashboard stats
    // This would call actual API endpoints
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Appointment Dashboard
          </Typography>
          <Button color="inherit" onClick={handleLogout}>
            Logout
          </Button>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3, display: 'flex', alignItems: 'center' }}>
              <TodayIcon sx={{ fontSize: 40, mr: 2, color: 'primary.main' }} />
              <Box>
                <Typography variant="h4">{stats.today}</Typography>
                <Typography variant="body2" color="textSecondary">
                  Today's Appointments
                </Typography>
              </Box>
            </Paper>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3, display: 'flex', alignItems: 'center' }}>
              <PendingActionsIcon sx={{ fontSize: 40, mr: 2, color: 'warning.main' }} />
              <Box>
                <Typography variant="h4">{stats.pending}</Typography>
                <Typography variant="body2" color="textSecondary">
                  Pending Approvals
                </Typography>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </Container>
    </>
  );
};

export default Dashboard;

