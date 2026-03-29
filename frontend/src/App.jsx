import { Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import ChatPage from './pages/ChatPage'
import ProtectedRoute from './components/layout/ProtectedRoute'

export default function App() {
  return (
    <Routes>
      <Route path="/login"    element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route path="/" element={
        <ProtectedRoute><DashboardPage /></ProtectedRoute>
      } />

      <Route path="/chat" element={
        <ProtectedRoute><ChatPage /></ProtectedRoute>
      } />

      <Route path="/chat/:conversationId" element={
        <ProtectedRoute><ChatPage /></ProtectedRoute>
      } />

      {/* Catch-all → login */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}