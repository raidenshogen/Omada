import { motion } from 'framer-motion'
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'

export default function Register(){
  const [name,setName]=useState('')
  const [email,setEmail]=useState('')
  const [password,setPassword]=useState('')
  const [error,setError]=useState('')
  const setUser = useAuthStore(s=>s.setUser)
  const navigate = useNavigate()

  const handleRegister = async () => {
    try {
      setError('')
      const res = await api.post('/auth/register',{ name, email, password })
      const { accessToken, refreshToken, user } = res.data
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      setUser(user)
      navigate('/dashboard')
    } catch(e){
      setError(e?.response?.data?.message || 'Registration failed')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-600">
      <motion.div initial={{opacity:0,y:20}} animate={{opacity:1,y:0}} transition={{type:'spring',stiffness:120}} className="bg-white p-8 rounded-2xl shadow-xl w-96">
        <h1 className="text-2xl font-bold mb-6 text-center">Create account</h1>
        {error && <div className="mb-3 text-red-600 text-sm">{error}</div>}
        <input className="w-full border p-3 rounded-lg mb-3 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="Name" value={name} onChange={e=>setName(e.target.value)} />
        <input className="w-full border p-3 rounded-lg mb-3 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="Email" value={email} onChange={e=>setEmail(e.target.value)} />
        <input type="password" className="w-full border p-3 rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="Password" value={password} onChange={e=>setPassword(e.target.value)} />
        <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.98}} onClick={handleRegister} className="w-full bg-brand-600 text-white p-3 rounded-lg font-medium">Register</motion.button>
        <div className="text-center mt-4 text-sm">
          Have an account? <Link to="/" className="text-brand-600">Login</Link>
        </div>
      </motion.div>
    </div>
  )
}
