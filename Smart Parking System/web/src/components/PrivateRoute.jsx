import React from 'react';
import { Navigate } from 'react-router-dom';
import useAuthStore from '../stores/authStore';

const PrivateRoute = ({ children }) => {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Ensure user is an ADMIN
  if (user?.role !== 'ADMIN') {
    return <Navigate to="/login" replace />;
  }

  return children;
};

export default PrivateRoute;
