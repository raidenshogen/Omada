import { Routes, Route, Navigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import Sidebar from './components/Sidebar'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Workspace from './pages/Workspace'
import Board from './pages/Board'
import Document from './pages/Document'
import { useAuthStore } from './store/authStore'
import { useEffect } from 'react'

function ProtectedRoute({ children }) {
  // isAuthenticated is set by login(); accessToken survives reloads via persist,
  // so accept either to avoid kicking a signed-in user back to the login page.
  const authed = useAuthStore(s => !!(s.isAuthenticated || s.accessToken))
  return authed ? children : <Navigate to="/" replace />
}

export default function App(){
  const initFromStorage = useAuthStore(s => s.initFromStorage)
  // token in storage means a logged-in session (persist rehydrates synchronously,
  // so the sidebar does not flash off/on between reloads)
  const authed = useAuthStore(s => !!(s.isAuthenticated || s.accessToken))
  useEffect(() => { initFromStorage() }, [])

  return (
    <div className="flex min-h-screen bg-gray-50">
      {authed && <Sidebar />}
      <div className="flex-1 overflow-auto">
        <AnimatePresence mode="wait">
          <Routes>
            <Route path="/" element={<motion.div initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}}><Login/></motion.div>} />
            <Route path="/register" element={<motion.div initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}}><Register/></motion.div>} />
            <Route path="/dashboard" element={<ProtectedRoute><Dashboard/></ProtectedRoute>} />
            <Route path="/workspace/:id" element={<ProtectedRoute><Workspace/></ProtectedRoute>} />
            <Route path="/board/:id" element={<ProtectedRoute><Board/></ProtectedRoute>} />
            <Route path="/doc/:id" element={<ProtectedRoute><Document/></ProtectedRoute>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AnimatePresence>
      </div>
    </div>
  )
}
