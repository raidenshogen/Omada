import { motion } from 'framer-motion'
import { useState } from 'react'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'

const ROLES = ['admin', 'member', 'viewer']

/**
 * Invite modal: opens from the workspace page, accepts an email + role and
 * POSTs to /api/workspaces/{id}/invitations. Closed by the parent via `open`.
 */
export default function InviteModal({ open, workspaceId, onClose, onInvited }) {
  const [email, setEmail] = useState('')
  const [role, setRole] = useState('member')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const currentRole = useAuthStore((s) => s.role)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!email.trim()) return
    setSending(true)
    setError('')
    try {
      const response = await api.post(`/workspaces/${workspaceId}/invitations`, {
        email: email.trim(),
        role,
      })
      setEmail('')
      onClose()
      onInvited && onInvited(response.data)
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not send invitation')
    } finally {
      setSending(false)
    }
  }

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
      aria-labelledby="invite-modal-title"
    >
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-0 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
      />
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 20 }}
        className="relative z-10 bg-white rounded-2xl shadow-card p-6 w-full max-w-md"
      >
        <h2 id="invite-modal-title" className="text-lg font-semibold mb-1">Invite member</h2>
        <p className="text-sm text-neutral-500 mb-4">
          Add someone to this workspace ({currentRole ? `you: ${currentRole}` : ''})
        </p>

        {error && (
          <div className="mb-3 p-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="label" htmlFor="invite-email">Email</label>
            <input
              id="invite-email"
              className="input-field"
              type="email"
              placeholder="name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div>
            <label className="label" htmlFor="invite-role">Role</label>
            <select
              id="invite-role"
              className="input-field"
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r.charAt(0).toUpperCase() + r.slice(1)}
                </option>
              ))}
            </select>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 btn-ghost"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={sending}
              className="flex-1 btn-primary"
            >
              {sending ? 'Sending…' : 'Send invitation'}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  )
}
