import { motion } from 'framer-motion'
import { useAuthStore } from '../store/authStore'
import { ArrowLeft } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import api from '../services/api'

export default function Profile() {
  const user = useAuthStore(s => s.user)
  const setUser = useAuthStore(s => s.setUser)
  const navigate = useNavigate()
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    if (user?.name) {
      const parts = user.name.trim().split(' ')
      setFirstName(parts[0] || '')
      setLastName(parts.slice(1).join(' ') || '')
    }
    setEmail(user?.email || '')
  }, [user])

  const handleSave = async () => {
    setError('')
    setSuccess('')
    if (!firstName.trim() || !lastName.trim()) { setError('First and last name are required'); return }
    if (!email.includes('@')) { setError('Enter a valid working email'); return }
    setSaving(true)
    try {
      const name = `${firstName.trim()} ${lastName.trim()}`
      const res = await api.patch('/auth/me', { name, email })
      setUser(res.data)
      setSuccess('Profile updated')
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not update profile')
    } finally {
      setSaving(false)
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-8 max-w-2xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate(-1)} className="text-neutral-600 hover:text-neutral-900 p-1">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-2xl font-bold">Profile</h1>
      </div>

      <div className="bg-white rounded-2xl shadow p-6 border border-neutral-100 space-y-4">
        {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-2">{error}</div>}
        {success && <div className="text-sm text-green-600 bg-green-50 border border-green-200 rounded-lg p-2">{success}</div>}
        <p className="text-xs text-neutral-500">Use a working email to receive updates about projects and workspaces.</p>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-neutral-600">First name</label>
            <input className="w-full border p-3 rounded-lg mt-1" value={firstName} onChange={e=>setFirstName(e.target.value)} />
          </div>
          <div>
            <label className="text-xs text-neutral-600">Last name</label>
            <input className="w-full border p-3 rounded-lg mt-1" value={lastName} onChange={e=>setLastName(e.target.value)} />
          </div>
        </div>
        <div>
          <label className="text-xs text-neutral-600">Email</label>
          <input className="w-full border p-3 rounded-lg mt-1" value={email} onChange={e=>setEmail(e.target.value)} />
        </div>
        <button onClick={handleSave} disabled={saving} className="w-full bg-brand-600 text-white p-3 rounded-lg font-medium disabled:opacity-60">
          {saving ? 'Saving…' : 'Save changes'}
        </button>
      </div>
    </motion.div>
  )
}
