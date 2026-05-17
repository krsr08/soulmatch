import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import UsersPage from './pages/UsersPage';
import { RuntimeConfigProvider } from './context/RuntimeConfigContext';
const Protected = ({ children }) => localStorage.getItem('adminToken') ? children : <Navigate to="/login" />;
export default function App() {
  return (
    <RuntimeConfigProvider>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={<Protected><DashboardPage /></Protected>} />
          <Route path="/dashboard/:section" element={<Protected><DashboardPage /></Protected>} />
          <Route path="/users" element={<Protected><UsersPage /></Protected>} />
          <Route path="*" element={<Navigate to="/dashboard" />} />
        </Routes>
      </BrowserRouter>
    </RuntimeConfigProvider>
  );
}
