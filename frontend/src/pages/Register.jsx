import { motion } from 'framer-motion'
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'

export default function Register(){
  const [firstName,setFirstName]=useState('')
  const [lastName,setLastName]=useState('')
  const [email,setEmail]=useState('')
  const [password,setPassword]=useState('')
  const [confirmPassword,setConfirmPassword]=useState('')
  const [error,setError]=useState('')
  const [accept,setAccept]=useState(false)
  const setUser = useAuthStore(s=>s.setUser)
  const navigate = useNavigate()

  const handleRegister = async () => {
    if(!accept){ setError('Please accept the Terms'); return }
    if(password !== confirmPassword){ setError('Passwords do not match'); return }
    if(!firstName.trim() || !lastName.trim()){ setError('First and last name are required'); return }
    try {
      setError('')
      const name = `${firstName.trim()} ${lastName.trim()}`
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
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-50 to-purple-50">
      <motion.div initial={{opacity:0,y:20}} animate={{opacity:1,y:0}} transition={{type:'spring',stiffness:120}} className="bg-white p-8 rounded-2xl shadow-xl w-full max-w-md">
        <h1 className="text-2xl font-bold mb-2 text-center">Create your account</h1>
        <p className="text-center text-sm text-neutral-500 mb-6">Join Omada and start collaborating on your projects</p>
        {error && <div className="mb-3 text-red-600 text-sm bg-red-50 border border-red-200 rounded-lg p-2">{error}</div>}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-neutral-600">First name</label>
            <input className="w-full border p-3 rounded-lg mb-3 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="Jane" value={firstName} onChange={e=>setFirstName(e.target.value)} />
          </div>
          <div>
            <label className="text-xs text-neutral-600">Last name</label>
            <input className="w-full border p-3 rounded-lg mb-3 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="Doe" value={lastName} onChange={e=>setLastName(e.target.value)} />
          </div>
        </div>
        <label className="text-xs text-neutral-600">Work email</label>
        <input className="w-full border p-3 rounded-lg mb-3 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="jane@company.com" value={email} onChange={e=>setEmail(e.target.value)} />
        <label className="text-xs text-neutral-600">Password</label>
        <input type="password" className="w-full border p-3 rounded-lg mb-3 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="••••••••" value={password} onChange={e=>setPassword(e.target.value)} />
        <label className="text-xs text-neutral-600">Confirm password</label>
        <input type="password" className="w-full border p-3 rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-brand-500" placeholder="••••••••" value={confirmPassword} onChange={e=>setConfirmPassword(e.target.value)} />
        <div className="flex items-center gap-2 mb-4">
          <input id="terms" type="checkbox" checked={accept} onChange={e=>setAccept(e.target.checked)} />
          <label htmlFor="terms" className="text-xs text-neutral-600">I agree to the Terms of Service and Privacy Policy</label>
        </div>
        <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.98}} onClick={handleRegister} className="w-full bg-brand-600 text-white p-3 rounded-lg font-medium">Create account</motion.button>
        <div className="text-center mt-4 text-sm">
          Already have an account? <Link to="/" className="text-brand-600">Sign in</Link>
        </div>
      </motion.div>
    </div>
  )
}
