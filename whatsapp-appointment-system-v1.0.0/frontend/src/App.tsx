import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import PendingRequests from './pages/PendingRequests';
import Appointments from './pages/Appointments';
import Availability from './pages/Availability';
import Billing from './pages/Billing';
import ReceptionistDesk from './pages/ReceptionistDesk';
import FollowUpWorklist from './pages/FollowUpWorklist';
import InquiryCRM from './pages/InquiryCRM';
import ClinicSettings from './pages/ClinicSettings';
import PrivateRoute from './components/PrivateRoute';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/dashboard"
            element={
              <PrivateRoute>
                <Dashboard />
              </PrivateRoute>
            }
          />
          <Route
            path="/pending-requests"
            element={
              <PrivateRoute>
                <PendingRequests />
              </PrivateRoute>
            }
          />
          <Route
            path="/appointments"
            element={
              <PrivateRoute>
                <Appointments />
              </PrivateRoute>
            }
          />
          <Route
            path="/availability"
            element={
              <PrivateRoute>
                <Availability />
              </PrivateRoute>
            }
          />
          <Route
            path="/billing"
            element={
              <PrivateRoute>
                <Billing />
              </PrivateRoute>
            }
          />
          <Route
            path="/reception"
            element={
              <PrivateRoute>
                <ReceptionistDesk />
              </PrivateRoute>
            }
          />
          <Route
            path="/follow-ups"
            element={
              <PrivateRoute>
                <FollowUpWorklist />
              </PrivateRoute>
            }
          />
          <Route
            path="/inquiries"
            element={
              <PrivateRoute>
                <InquiryCRM />
              </PrivateRoute>
            }
          />
          <Route
            path="/clinic-settings"
            element={
              <PrivateRoute>
                <ClinicSettings />
              </PrivateRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;

