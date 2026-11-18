import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'

/**
 * /invite/:token page: accept or decline a workspace invitation. On accept,
 * redirects to the invitation's workspace.
 */
export default function InviteAccept() {
  const { token } = useParams()
  const navigate = useNavigate()
  const addMembership = useAuthStore((s) => s.addMembership)
  const [invitation, setInvitation] = useState(null)
  const [loading, setLoading] = useState(true)
  const [acting, setActing] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(null) // 'accepted' | 'declined' | null

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const res = await api.get(`/invitations/${token}`)
        if (!cancelled) setInvitation(res.data || null)
      } catch (e) {
        if (!cancelled) setError(e?.response?.data?.message || 'Could not load invitation')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [token])

  const respond = async (action) => {
    if (!invitation) return
    setActing(true)
    setError('')
    try {
      await api.post(`/invitations/${token}/${action}`)
      setDone(action)
      if (action === 'accept') {
        addMembership({ workspaceId: invitation.workspaceId, role: invitation.role.toLowerCase() })
        const workspaceId = invitation.workspaceId
        setTimeout(() => navigate(`/workspace/${workspaceId}`), 600)
      }
    } catch (e) {
      setError(e?.response?.data?.message || `Could not ${action} invitation`)
    } finally {
      setActing(false)
    }
  }

  if (loading) {
    return (
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="text-neutral-500 text-sm">Loading invitation…</div>
      </motion.div>
    )
  }

  if (error || !invitation) {
    return (
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="min-h-screen flex items-center justify-center bg-gray-50 p-6">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white rounded-2xl shadow-card p-6 max-w-sm text-center"
        >
          <h2 className="font-semibold mb-2">{error ? 'Invitation error' : 'Invitation not found'}</h2>
          <p className="text-sm text-neutral-500 mb-4">{error || 'This invitation does not exist or has expired.'}</p>
        </motion.div>
      </motion.div>
    )
  }

  if (invitation.status === 'REVOKED') {
    return (
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="min-h-screen flex items-center justify-center bg-gray-50 p-6">
        <motion.div className="bg-white rounded-2xl shadow-card p-6 max-w-sm text-center">
          <h2 className="font-semibold text-lg mb-2">Invitation revoked</h2>
          <p className="text-sm text-neutral-500">
            This invitation is no longer available. The workspace owner or administrator revoked it.
          </p>
        </motion.div>
      </motion.div>
    )
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex min-h-screen items-center justify-center bg-gray-50 p-6"
    >
      <motion.div className="bg-white rounded-2xl shadow-card p-6 max-w-sm text-center">
        <h2 className="font-semibold text-lg mb-1">Join workspace?</h2>
        <p className="text-sm text-neutral-500 mb-1">You were invited as <span className="font-medium">{invitation.role}</span>.</p>
        <p className="text-sm text-neutral-500 mb-6">{invitation.email}</p>

        {done ? (
          <div className="text-sm">
            {done === 'accepted'
              ? `Accepted — redirecting to ${invitation.workspaceName || 'workspace'}…`
              : 'Declined.'}
          </div>
        ) : (
          <div className="flex gap-2">
            <button
              onClick={() => respond('decline')}
              disabled={acting}
              className="flex-1 btn-ghost"
            >
              Decline
            </button>
            <button
              onClick={() => respond('accept')}
              disabled={acting}
              className="flex-1 btn-primary"
            >
              {acting ? 'Accepting…' : 'Accept invitation'}
            </button>
          </div>
        )}
      </motion.div>
    </motion.div>
  )
}
