import React from 'react';
import { Navigate } from 'react-router-dom';
import useAuthStore from '../stores/authStore';

/**
 * PrivateRoute - guards a route by checking authentication AND role.
 *
 * Props:
 *  - requiredRole: 'ADMIN' | 'DRIVER'  (default: any authenticated user)
 *  - children: the layout / component to render
 *
 * Redirect logic:
 *  - Not authenticated → /login
 *  - Wrong role (ADMIN accessing /driver/* or DRIVER accessing /*)
 *    → redirect to their own home route
 */
const PrivateRoute = ({ children, requiredRole }) => {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const role = user?.role;

  if (requiredRole && role !== requiredRole) {
    // Redirect to the correct portal
    if (role === 'ADMIN') return <Navigate to="/dashboard" replace />;
    if (role === 'DRIVER') return <Navigate to="/driver" replace />;
    // Unknown role → logout
    return <Navigate to="/login" replace />;
  }

  return children;
};

export default PrivateRoute;
