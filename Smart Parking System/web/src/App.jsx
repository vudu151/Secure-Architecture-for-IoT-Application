import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import useAuthStore from './stores/authStore';
import { connectWebSocket, disconnectWebSocket } from './utils/websocket';

// Layouts & Guards
import AdminLayout from './layouts/AdminLayout';
import DriverLayout from './layouts/DriverLayout';
import PrivateRoute from './components/PrivateRoute';

// Auth
import LoginPage from './pages/LoginPage';

// Admin pages
import DashboardPage from './pages/DashboardPage';
import ParkingMapPage from './pages/ParkingMapPage';
import RevenuePage from './pages/RevenuePage';
import UsersPage from './pages/UsersPage';
import DevicesPage from './pages/DevicesPage';
import AuditLogsPage from './pages/AuditLogsPage';

// Driver pages
import DriverHomePage from './pages/driver/DriverHomePage';
import DriverParkingPage from './pages/driver/DriverParkingPage';
import DriverBookingsPage from './pages/driver/DriverBookingsPage';
import DriverWalletPage from './pages/driver/DriverWalletPage';
import DriverVehiclesPage from './pages/driver/DriverVehiclesPage';

const App = () => {
  const { isAuthenticated, user, checkAuth } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
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

        {/* ── ADMIN routes ── */}
        <Route
          path="/"
          element={
            <PrivateRoute requiredRole="ADMIN">
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

        {/* ── DRIVER routes ── */}
        <Route
          path="/driver"
          element={
            <PrivateRoute requiredRole="DRIVER">
              <DriverLayout />
            </PrivateRoute>
          }
        >
          <Route index element={<DriverHomePage />} />
          <Route path="parking" element={<DriverParkingPage />} />
          <Route path="bookings" element={<DriverBookingsPage />} />
          <Route path="wallet" element={<DriverWalletPage />} />
          <Route path="vehicles" element={<DriverVehiclesPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
