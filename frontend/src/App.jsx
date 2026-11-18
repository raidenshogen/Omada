import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Workspace from './pages/Workspace'
import Board from './pages/Board'
import Document from './pages/Document'
import Profile from './pages/Profile'
import InviteAccept from './pages/InviteAccept'
import { useAuthStore } from './store/authStore'
import { ProtectedRoute } from './components/ProtectedRoute'
import { useEffect } from 'react'

export default function App() {
  const initFromStorage = useAuthStore((s) => s.initFromStorage)
  // token in storage means a logged-in session (persist rehydrates synchronously,
  // so the sidebar does not flash off/on between reloads)
  const authed = useAuthStore((s) => !!(s.isAuthenticated || s.accessToken))
  const location = useLocation()
  const hideSidebar = location.pathname.startsWith('/doc') && new URLSearchParams(location.search).has('modal')
  useEffect(() => { initFromStorage() }, [])

  // Board and document ids are NOT workspace ids, so those routes must not
  // use workspaceParam (it compared them against workspace memberships and
  // always redirected to the dashboard).
  return (
    <div className="flex min-h-screen bg-paper">
      {authed && !hideSidebar && <Sidebar />}
      <main className="min-w-0 flex-1">
        <Routes>
          <Route path="/" element={authed ? <Navigate to="/dashboard" replace /> : <Login />} />
          <Route path="/register" element={authed ? <Navigate to="/dashboard" replace /> : <Register />} />
          <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/workspace/:id" element={<ProtectedRoute workspaceParam="id"><Workspace /></ProtectedRoute>} />
          <Route path="/invite/:token" element={<InviteAccept />} />
          <Route path="/board/:id" element={<ProtectedRoute><Board /></ProtectedRoute>} />
          <Route path="/doc/:id" element={<ProtectedRoute><Document /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}