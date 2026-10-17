import { motion } from 'framer-motion'
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'

export default function Login(){
  const [email,setEmail]=useState('')
  const [password,setPassword]=useState('')
  const setUser = useAuthStore(s=>s.setUser)
  const navigate = useNavigate()
  const login = useAuthStore(s=>s.login)

  const handleLogin = async () => {
    try {
      const res = await api.post('/auth/login',{ email, password })
      const { accessToken, refreshToken, user } = res.data
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      login(user, accessToken, refreshToken)
      navigate('/dashboard')
    } catch(e){ alert('Login failed') }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-600">
      <motion.div initial={{opacity:0,y:20}} animate={{opacity:1,y:0}} transition={{type:'spring',stiffness:120}} className="bg-white p-8 rounded-2xl shadow-xl w-96">
        <h1 className="text-2xl font-bold mb-6 text-center">Omada</h1>
        <input className="w-full border p-3 rounded-lg mb-3 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="Email" value={email} onChange={e=>setEmail(e.target.value)} />
        <input type="password" className="w-full border p-3 rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="Password" value={password} onChange={e=>setPassword(e.target.value)} />
        <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.98}} onClick={handleLogin} className="w-full bg-brand-600 text-white p-3 rounded-lg font-medium">
          Login
        </motion.button>
        <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{delay:0.3}} className="text-center mt-4 text-sm">
          No account? <Link to="/register" className="text-brand-600">Register</Link>
        </motion.div>
      </motion.div>
    </div>
  )
}
