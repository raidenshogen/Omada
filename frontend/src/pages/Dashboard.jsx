import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'
import ActivityPanel from '../components/ActivityPanel'

/**
 * Dashboard: lists the user's workspaces (creating a default organization +
 * workspace on first visit) and lets them create new ones. Clicking a
 * workspace opens its boards.
 */
export default function Dashboard() {
  const user = useAuthStore((s) => s.user)
  const navigate = useNavigate()
  const [workspaces, setWorkspaces] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')

  // Load the organization (create one on first visit), then its workspaces
  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError('')
      try {
        let orgs = (await api.get('/organizations')).data || []
        if (orgs.length === 0) {
          const created = await api.post('/organizations', { name: 'My Org', slug: 'my-org' })
          orgs = [created.data]
        }
        const orgId = orgs[0].id
        const ws = (await api.get(`/workspaces?orgId=${encodeURIComponent(orgId)}`)).data || []
        if (!cancelled) setWorkspaces(ws)
      } catch (e) {
        if (!cancelled) setError(e?.response?.data?.message || 'Could not load workspaces')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  const createWorkspace = async () => {
    if (!newName.trim()) return
    setCreating(true)
    setError('')
    try {
      const orgs = (await api.get('/organizations')).data || []
      const orgId = orgs[0]?.id
      const res = await api.post('/workspaces', { organizationId: orgId, name: newName.trim(), description: '' })
      setWorkspaces((w) => [...w, res.data])
      setNewName('')
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not create the workspace')
    } finally {
      setCreating(false)
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-8 bg-gray-50 min-h-screen">
      <h1 className="text-3xl font-bold">Welcome{user?.name ? `, ${user.name}` : ''}</h1>
      <p className="text-gray-600 mt-1">Your workspaces — click one to see its boards</p>

      {error && (
        <div className="mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">{error}</div>
      )}

      <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-6">
        {loading ? (
          <div className="text-gray-500">Loading…</div>
        ) : (
          workspaces.map((ws, i) => (
            <motion.button
              key={ws.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
              whileHover={{ y: -4 }}
              onClick={() => navigate(`/workspace/${ws.id}`, { state: { title: ws.name } })}
              className="bg-white rounded-2xl shadow p-6 text-left"
            >
              <h3 className="font-semibold text-lg">{ws.name}</h3>
              <p className="text-sm text-gray-500 mt-1">{ws.description || 'No description'}</p>
              <span className="inline-block mt-4 text-brand-600 font-medium">Open →</span>
            </motion.button>
          ))
        )}

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="bg-white rounded-2xl shadow p-6">
          <h3 className="font-semibold text-lg mb-3">New workspace</h3>
          <input
            className="w-full border p-2 rounded-lg mb-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            placeholder="Workspace name"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && createWorkspace()}
          />
          <button
            onClick={createWorkspace}
            disabled={creating}
            className="w-full bg-brand-600 text-white py-2 rounded-lg text-sm font-medium disabled:opacity-50"
          >
            {creating ? 'Creating…' : 'Create'}
          </button>
        </motion.div>
      </div>

      {workspaces.length > 0 && <ActivityPanel workspaceId={workspaces[0].id} />}
    </motion.div>
  )
}
