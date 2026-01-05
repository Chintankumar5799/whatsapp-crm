import React from 'react';
import DoctorDashboard from './DoctorDashboard';
import PatientDashboard from './PatientDashboard';

const Dashboard: React.FC = () => {
    const storedUser = localStorage.getItem('user');
    const parsedUser = storedUser ? JSON.parse(storedUser) : null;
    const role = parsedUser?.role;

    if (role === 'PATIENT') {
        return <PatientDashboard />;
    }

    // Default to Doctor Dashboard for other roles (DOCTOR, ADMIN, STAFF)
    return <DoctorDashboard />;
};

export default Dashboard;
