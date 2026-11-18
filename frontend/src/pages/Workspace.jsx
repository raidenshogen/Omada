import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import api from '../services/api'
import InviteModal from '../components/InviteModal'
import InvitationList from '../components/InvitationList'
import MemberRow from '../components/MemberRow'
import { Plus, ArrowLeft } from 'lucide-react'
import { useAuthStore } from '../store/authStore'

const MANAGER_ROLES = ['owner', 'admin']

/**
 * Workspace page: shows the boards of this workspace, lets the user create
 * boards (seeded with Todo / Doing / Done lists) and open them.
 */
export default function Workspace() {
  const { id: workspaceId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const workspaceTitle = location.state?.title || 'Workspace'

  const [boards, setBoards] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [newDesc, setNewDesc] = useState('')
  const [inviteOpen, setInviteOpen] = useState(false)
  const [invitationRefreshKey, setInvitationRefreshKey] = useState(0)
  const [invitationNotice, setInvitationNotice] = useState(null)
  const [members, setMembers] = useState([])
  const [membersLoading, setMembersLoading] = useState(true)
  const [membersError, setMembersError] = useState('')
  const [roleLoaded, setRoleLoaded] = useState(false)
  const currentWorkspace = useAuthStore((s) => s.currentWorkspace)
  const role = useAuthStore((s) => s.role)

  useEffect(() => {
      let cancelled = false
      async function load() {
        setLoading(true)
        setMembersLoading(true)
        setError('')
        setMembersError('')
        setRoleLoaded(false)
        useAuthStore.getState().setRole(null)
        try {
          // Fetch boards first
          const boardsRes = await api.get(`/workspaces/${workspaceId}/boards`)
          // Load the workspace roster, including users invited directly to it.
          const membersRes = await api.get(`/workspaces/${workspaceId}/members`)
          if (!cancelled) {
            setBoards(boardsRes.data || [])
            setMembers(membersRes.data || [])
            const currentUserId = useAuthStore.getState().user?.id
            const currentMember = (membersRes.data || []).find((member) => member.userId === currentUserId)
            useAuthStore.getState().setRole(currentMember?.role || null)
            setRoleLoaded(true)
          }
        } catch (e) {
          if (!cancelled) {
            setError(e?.response?.data?.message || 'Could not load boards')
            setMembersError(e?.response?.data?.message || 'Could not load members')
          }
        } finally {
          if (!cancelled) {
            setLoading(false)
            setMembersLoading(false)
          }
        }
      }
      load()
      return () => {
        cancelled = true
      }
    }, [workspaceId])

  const createBoard = async () => {
    if (!newTitle.trim()) return
    setCreating(true)
    setError('')
    try {
      const res = await api.post(`/workspaces/${workspaceId}/boards`, {
        title: newTitle.trim(),
        description: newDesc.trim(),
      })
      const boardId = res.data.id
      setBoards((b) => [...b, res.data])
      setNewTitle('')
      setNewDesc('')
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not create the board')
    } finally {
      setCreating(false)
    }
  }

  const deleteBoard = async (boardId) => {
    if (!window.confirm('Delete this board? This cannot be undone.')) return
    try {
      await api.delete(`/workspaces/${workspaceId}/boards/${boardId}`)
      setBoards((b) => b.filter((b) => b.id !== boardId))
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not delete the board')
    }
  }

  const openBoard = (board) => {
    navigate(`/board/${board.id}`, { state: { title: board.title, workspaceId } })
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-6">
      <div className="flex items-center gap-3 mb-2">
        <button onClick={() => navigate('/dashboard')} className="text-neutral-600 hover:text-neutral-900 p-1" title="Back to workspaces">
          <ArrowLeft size={20} />
        </button>
        <h2 className="text-xl font-semibold">{workspaceTitle}</h2>
      </div>
      <div className="flex items-center gap-2 mt-1">
        <p className="text-gray-600 text-sm">Boards in this workspace</p>
        {MANAGER_ROLES.includes(role) && (
          <button
            onClick={() => setInviteOpen(true)}
            className="flex items-center gap-1.5 text-brand-600 text-sm font-medium hover:underline"
          >
            <Plus size={16} /> Invite
          </button>
        )}
      </div>

      {error && (
        <div className="mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">{error}</div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
        {loading ? (
          <div className="text-gray-500">Loading…</div>
        ) : (
          boards.map((board) => (
            <motion.button
              key={board.id}
              whileHover={{ y: -4 }}
              onClick={() => openBoard(board)}
              className="p-4 bg-white rounded-xl shadow text-left"
            >
              <h3 className="font-semibold">{board.title}</h3>
              <p className="text-sm text-gray-500 mt-1">{board.description || 'No description'}</p>
              <span className="inline-block mt-2 text-brand-600 text-sm font-medium">Open board →</span>
              {MANAGER_ROLES.includes(role) && (
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    deleteBoard(board.id)
                  }}
                  className="ml-2 text-xs text-red-600 hover:text-red-800 font-medium"
                >
                  Delete
                </button>
              )}
            </motion.button>
          ))
        )}

        <div className="p-4 bg-white rounded-xl shadow">
          <h3 className="font-semibold mb-3">New board</h3>
          <input
            className="w-full border p-2 rounded-lg mb-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            placeholder="Board name"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && createBoard()}
          />
          <textarea
            className="w-full border p-2 rounded-lg mb-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            placeholder="Description (optional)"
            rows={2}
            value={newDesc}
            onChange={(e) => setNewDesc(e.target.value)}
          />
          {role !== 'viewer' && (
            <button
              onClick={createBoard}
              disabled={creating}
              className="w-full bg-brand-600 text-white py-2 rounded-lg text-sm font-medium disabled:opacity-50"
            >
              {creating ? 'Creating…' : 'Create board'}
            </button>
          )}
        </div>
      </div>

      {membersError && (
        <div className="mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
          {membersError}
        </div>
      )}

      {role === 'viewer' && (
        <div className="mt-4 p-3 bg-blue-50 border border-blue-200 text-blue-800 text-sm rounded-lg" role="status">
          You are a viewer for this workspace. You can view its content, but you cannot make changes.
        </div>
      )}

      <section className="mt-8">
        <h3 className="font-semibold mb-2">Workspace members</h3>
        {membersLoading ? (
          <div className="text-gray-500 text-sm">Loading members…</div>
        ) : members.length === 0 ? (
          <div className="text-neutral-400 text-sm">No members found.</div>
        ) : (
          <div className="divide-y divide-neutral-200">
            {members.map((member) => (
              <MemberRow
                key={member.userId}
                member={member}
                workspaceId={workspaceId}
                onRemoved={(userId) => setMembers((current) => current.filter((item) => item.userId !== userId))}
                onRoleChanged={(userId, newRole) => setMembers((current) =>
                  current.map((item) => item.userId === userId ? { ...item, role: newRole } : item)
                )}
              />
            ))}
          </div>
        )}
      </section>

      <InviteModal
        open={inviteOpen}
        workspaceId={workspaceId}
        onClose={() => setInviteOpen(false)}
        onInvited={(invitation) => {
          setInviteOpen(false)
          setInvitationNotice({
            email: invitation?.email,
            role: invitation?.role,
          })
          setInvitationRefreshKey((key) => key + 1)
        }}
      />
      {roleLoaded && (role === "owner" || role === "admin") && (
        <InvitationList
          workspaceId={workspaceId}
          refreshKey={invitationRefreshKey}
          notice={invitationNotice}
        />
      )}
    </motion.div>
  )
}
