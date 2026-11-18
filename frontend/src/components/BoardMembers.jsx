import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import api from '../services/api'
import { useAuthStore } from '../store/authStore'

/**
 * BoardMembers: modal panel listing the board's members and letting a
 * workspace Owner/Admin add or remove members.
 *
 * API:
 *   GET    /api/boards/{id}/members   list members  (any board member)
 *   POST   /api/boards/{id}/members   add member    (owner/admin only, body { userId })
 *   DELETE /api/boards/{id}/members/:userId  remove member (owner/admin only)
 *
 * Props:
 *   boardId        - the board in the URL
 *   open / onClose - dialog visibility, controlled by the board page header
 *   workspaceMembers - optional workspace roster; the add-form select offers
 *                      roster members not yet on the board
 */
export default function BoardMembers({ boardId, open = false, onClose, workspaceMembers = [] }) {
  const role = useAuthStore((s) => s.role)
  const user = useAuthStore((s) => s.user)
  const isManager = role === 'owner' || role === 'admin'

  const [members, setMembers] = useState([])
  const [candidates, setCandidates] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [adding, setAdding] = useState(false) // add form open
  const [newMemberId, setNewMemberId] = useState('')
  const [addingMember, setAddingMember] = useState(false)
  const [removing, setRemoving] = useState(new Set())

  const loadMembers = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get(`/boards/${boardId}/members`)
      setMembers(res.data || [])
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not load board members')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let cancelled = false
    if (!cancelled) loadMembers()
    return () => { cancelled = true }
  }, [boardId])

  // Candidates = workspace members not yet on the board
  useEffect(() => {
    const onBoard = new Set(members.map((m) => m.userId))
    setCandidates(workspaceMembers.filter((m) => !onBoard.has(m.userId)))
  }, [workspaceMembers, members])

  const addMember = async (e) => {
    e.preventDefault()
    if (!isManager || addingMember || !newMemberId.trim()) return
    setAddingMember(true)
    setError('')
    try {
      await api.post(`/boards/${boardId}/members`, { userId: newMemberId.trim() })
      setAdding(false)
      setNewMemberId('')
      await loadMembers()
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not add member')
    } finally {
      setAddingMember(false)
    }
  }

  const removeMember = async (userId) => {
    if (!isManager || removing.has(userId)) return
    if (userId === user?.id) {
      setError("You can't remove yourself from the board")
      return
    }
    if (!window.confirm('Remove this member from the board?')) return
    setRemoving((s) => new Set([...s, userId]))
    try {
      await api.delete(`/boards/${boardId}/members/${userId}`)
      setMembers((ms) => ms.filter((m) => m.userId !== userId))
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not remove member')
    } finally {
      setRemoving((s) => { const n = new Set(s); n.delete(userId); return n })
    }
  }

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
      aria-labelledby="board-members-title"
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
        className="relative z-10 bg-white rounded-2xl shadow-card p-6 w-full max-w-md border border-neutral-100"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 id="board-members-title" className="font-semibold text-lg text-neutral-900">
            Board members ({members.length})
          </h3>
          <button
            onClick={onClose}
            className="text-neutral-400 hover:text-neutral-600"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {error && (
          <div className="mb-3 p-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
            {error}
          </div>
        )}

        {loading ? (
          <div className="text-neutral-500 text-sm">Loading members…</div>
        ) : (
          <>
            <div className="mb-4">
              <h4 className="font-semibold text-sm text-neutral-700 mb-2">Members</h4>
              {members.length === 0 ? (
                <div className="text-neutral-400 text-sm">No members yet.</div>
              ) : (
                <div className="divide-y divide-neutral-200">
                  {members.map((m) => (
                    <motion.div
                      key={m.userId}
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: 'auto' }}
                      exit={{ opacity: 0, height: 0 }}
                      className="p-3 bg-white rounded-lg flex items-center justify-between gap-3"
                    >
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        <div className="w-8 h-8 rounded-full bg-neutral-200 flex items-center justify-center text-neutral-500 text-sm font-medium shrink-0">
                          {m.userId?.charAt(0).toUpperCase() || '?'}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-medium">{m.name || m.email || m.userId}</div>
                          {(m.name || m.email) && (
                            <div className="text-xs text-neutral-500">{m.email || m.userId}</div>
                          )}
                          {m.joinedAt && (
                            <div className="text-xs text-neutral-500">
                              Joined {new Date(m.joinedAt).toLocaleDateString()}
                            </div>
                          )}
                        </div>
                      </div>
                      {isManager && m.userId !== user?.id && (
                        <button
                          onClick={() => removeMember(m.userId)}
                          disabled={removing.has(m.userId)}
                          className="text-xs text-red-600 hover:text-red-800 font-medium disabled:opacity-50"
                        >
                          {removing.has(m.userId) ? 'Removing…' : 'Remove'}
                        </button>
                      )}
                    </motion.div>
                  ))}
                </div>
              )}
            </div>
          </>
        )}
      </motion.div>
    </div>
  )
}
