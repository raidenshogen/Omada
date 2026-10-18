import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import api from '../services/api'

const DEFAULT_LISTS = ['Todo', 'Doing', 'Done']

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

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError('')
      try {
        const boardsRes = await api.get(`/workspaces/${workspaceId}/boards`)
        if (!cancelled) {
          setBoards(boardsRes.data || [])
        }
      } catch (e) {
        if (!cancelled) setError(e?.response?.data?.message || 'Could not load boards')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [workspaceId])

  const defaultCardFor = (listTitle) => {
    switch (listTitle) {
      case 'Todo': return 'Design the homepage'
      case 'Doing': return 'Set up the project structure'
      case 'Done': return 'Create the workspace'
      default: return 'Add your first task'
    }
  }

  const createBoard = async () => {
    if (!newTitle.trim()) return
    setCreating(true)
    setError('')
    try {
      const res = await api.post(`/workspaces/${workspaceId}/boards`, {
        title: newTitle.trim(),
        description: newDesc.trim(),
      })
      // Seed the board with default columns + starter cards.
      // Seed failures are non-fatal: the board already exists, so we log and continue.
      const boardId = res.data.id
      try {
        await Promise.all(
          DEFAULT_LISTS.map((title, i) =>
            api.post(`/boards/${boardId}/lists`, { title, position: i }).then((listRes) =>
              api.post(`/boards/${boardId}/lists/${listRes.data.id}/cards`, {
                title: defaultCardFor(title),
                description: `Example task in ${title}`,
                position: 0,
              })
            )
          )
        )
      } catch (seedErr) {
        console.error('Board seeded with warning:', seedErr)
      }
      setBoards((b) => [...b, res.data])
      setNewTitle('')
      setNewDesc('')
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not create the board')
    } finally {
      setCreating(false)
    }
  }

  const deleteBoard = async (boardId: string) => {
    if (!window.confirm('Delete this board? This cannot be undone.')) return
    try {
      await api.delete(`/workspaces/${workspaceId}/boards/${boardId}`)
      setBoards((b) => b.filter((b) => b.id !== boardId))
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not delete the board')
    }
  }

  const openBoard = (board) => {
    navigate(`/board/${board.id}`, { state: { title: board.title } })
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-6">
      <h2 className="text-xl font-semibold">{workspaceTitle}</h2>
      <p className="text-gray-600 mt-1 text-sm">Boards in this workspace</p>

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
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  deleteBoard(board.id)
                }}
                className="ml-2 text-xs text-red-600 hover:text-red-800 font-medium"
              >
                Delete
              </button>
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
          <button
            onClick={createBoard}
            disabled={creating}
            className="w-full bg-brand-600 text-white py-2 rounded-lg text-sm font-medium disabled:opacity-50"
          >
            {creating ? 'Creating…' : 'Create board'}
          </button>
        </div>
      </div>
    </motion.div>
  )
}
