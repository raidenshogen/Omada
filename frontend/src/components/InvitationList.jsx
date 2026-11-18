import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'

/**
 * Invitation list: shows pending invitations for the current workspace, each
 * with a revoke button that DELETEs /api/workspaces/{id}/invitations/{id}.
 */
export default function InvitationList({ workspaceId, refreshKey = 0, notice = null }) {
  const removeInvitation = useAuthStore((s) => s.removeInvitation)
  const [invitations, setInvitations] = useState([])
  const [loading, setLoading] = useState(true)
  const [revoking, setRevoking] = useState(new Set())
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError('')
      try {
        const res = await api.get(`/workspaces/${workspaceId}/invitations`)
        if (!cancelled) setInvitations(res.data || [])
      } catch (e) {
        if (!cancelled) setError(e?.response?.data?.message || 'Could not load invitations')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [workspaceId, refreshKey])

  const revoke = async (invitation) => {
    if (!window.confirm('Revoke this invitation? The recipient will no longer be able to accept it.')) return
    const rowId = invitation.id
    const revokeToken = invitation.token
    setRevoking((s) => new Set([...s, rowId]))
    try {
      await api.delete(`/invitations/${revokeToken}`)
      removeInvitation(rowId)
      setInvitations((list) => list.filter((i) => i.id !== rowId))
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not revoke invitation')
    } finally {
      setRevoking((s) => { const n = new Set(s); n.delete(rowId); return n })
    }
  }

  if (loading) return <div className="text-gray-500 text-sm">Loading invitations…</div>

  if (error) {
    return <div className="mt-2 p-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">{error}</div>
  }

  return (
    <section className="mt-6">
      <h3 className="font-semibold mb-2">Pending invitations</h3>
      {notice && (
        <div
          className="mb-3 p-3 bg-amber-50 border border-amber-200 text-amber-800 text-sm rounded-lg"
          role="status"
          aria-live="polite"
        >
          Invitation sent to <strong>{notice.email}</strong>. Waiting for them to accept.
        </div>
      )}
      {invitations.length === 0 ? (
        <div className="text-neutral-400 text-sm">No pending invitations.</div>
      ) : (
        <div className="divide-y divide-neutral-200">
          {invitations.map((inv) => (
            <motion.div
              key={inv.id}
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="p-3 bg-white rounded-lg flex items-center justify-between"
            >
              <div>
                <div className="text-sm font-medium">{inv.email}</div>
                <div className="text-xs text-neutral-500">
                  <span
                    className="inline-flex items-center rounded-full px-2 py-0.5 bg-neutral-100 text-neutral-700 font-medium"
                    title="Role this person will have after accepting"
                  >
                    {(inv.role || '').charAt(0).toUpperCase() + (inv.role || '').slice(1).toLowerCase()}
                  </span>
                  <span aria-hidden="true"> • </span>
                  <span>{(inv.status || 'pending').toLowerCase()}</span>
                </div>
              </div>
              <button
                onClick={() => revoke(inv)}
                disabled={revoking.has(inv.id)}
                className="text-xs text-red-600 hover:text-red-800 font-medium disabled:opacity-50"
              >
                {revoking.has(inv.id) ? 'Revoking…' : 'Revoke'}
              </button>
            </motion.div>
          ))}
        </div>
      )}
    </section>
  )
}
