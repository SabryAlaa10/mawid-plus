import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from '../components/layout/ProtectedRoute'
import DashboardShell from '../components/layout/DashboardShell'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import DashboardPage from '../pages/DashboardPage'
import AppointmentsPage from '../pages/AppointmentsPage'
import QueuePage from '../pages/QueuePage'
import SchedulePage from '../pages/SchedulePage'
import ProfilePage from '../pages/ProfilePage'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <DashboardShell />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="appointments" element={<AppointmentsPage />} />
        <Route path="queue" element={<QueuePage />} />
        <Route path="schedule" element={<SchedulePage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
