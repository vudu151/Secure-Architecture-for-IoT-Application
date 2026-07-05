import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import useAuthStore from './stores/authStore';
import { connectWebSocket, disconnectWebSocket } from './utils/websocket';

// Layouts & Guards
import AdminLayout from './layouts/AdminLayout';
import PrivateRoute from './components/PrivateRoute';

// Pages
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ParkingMapPage from './pages/ParkingMapPage';
import RevenuePage from './pages/RevenuePage';
import UsersPage from './pages/UsersPage';
import DevicesPage from './pages/DevicesPage';
import AuditLogsPage from './pages/AuditLogsPage';

const App = () => {
  const { isAuthenticated, checkAuth } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      // Connect to WebSocket STOMP broker for real-time dashboard and map updates
      connectWebSocket(
        () => console.log('[App] WebSocket connected successfully'),
        (err) => console.error('[App] WebSocket connection error:', err)
      );
    } else {
      disconnectWebSocket();
    }

    return () => {
      disconnectWebSocket();
    };
  }, [isAuthenticated]);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        
        <Route
          path="/"
          element={
            <PrivateRoute>
              <AdminLayout />
            </PrivateRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="parking-map" element={<ParkingMapPage />} />
          <Route path="revenue" element={<RevenuePage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="devices" element={<DevicesPage />} />
          <Route path="audit-logs" element={<AuditLogsPage />} />
        </Route>
        
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
