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
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [creatingDoc, setCreatingDoc] = useState(false)
  const [newDocTitle, setNewDocTitle] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError('')
      try {
        const [boardsRes, docsRes] = await Promise.all([
          api.get(`/workspaces/${workspaceId}/boards`),
          api.get(`/workspaces/${workspaceId}/documents`),
        ])
        if (!cancelled) {
          setBoards(boardsRes.data || [])
          setDocuments(docsRes.data || [])
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

  const createBoard = async () => {
    if (!newTitle.trim()) return
    setCreating(true)
    setError('')
    try {
      const res = await api.post(`/workspaces/${workspaceId}/boards`, {
        title: newTitle.trim(),
        description: '',
      })
      // Seed the board with the default columns so it opens ready to use
      const boardId = res.data.id
      await Promise.all(
        DEFAULT_LISTS.map((title, i) =>
          api.post(`/boards/${boardId}/lists`, { title, position: i })
        )
      )
      setBoards((b) => [...b, res.data])
      setNewTitle('')
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not create the board')
    } finally {
      setCreating(false)
    }
  }

  const openBoard = (board) => {
    navigate(`/board/${board.id}`, { state: { title: board.title } })
  }

  const createDocument = async () => {
    if (!newDocTitle.trim()) return
    setCreatingDoc(true)
    setError('')
    try {
      const res = await api.post(`/workspaces/${workspaceId}/documents`, { title: newDocTitle.trim() })
      setDocuments((d) => [res.data, ...d])
      navigate(`/doc/${res.data.id}`, { state: { title: res.data.title } })
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not create the document')
    } finally {
      setCreatingDoc(false)
    }
  }

  const openDocument = (doc) => {
    navigate(`/doc/${doc.id}`, { state: { title: doc.title } })
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
          <button
            onClick={createBoard}
            disabled={creating}
            className="w-full bg-brand-600 text-white py-2 rounded-lg text-sm font-medium disabled:opacity-50"
          >
            {creating ? 'Creating…' : 'Create board'}
          </button>
        </div>
      </div>

      <h3 className="text-lg font-semibold mt-8 mb-3">Documents</h3>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {documents.map((doc) => (
          <motion.button
            key={doc.id}
            whileHover={{ y: -4 }}
            onClick={() => openDocument(doc)}
            className="p-4 bg-white rounded-xl shadow text-left"
          >
            <h4 className="font-semibold">{doc.title || 'Untitled'}</h4>
            <span className="inline-block mt-2 text-brand-600 text-sm font-medium">Open doc →</span>
          </motion.button>
        ))}
        <motion.button
          whileHover={{ scale: 1.02 }}
          onClick={createDocument}
          disabled={creatingDoc}
          className="p-4 bg-white rounded-xl shadow text-center text-brand-600 font-medium"
        >
          {creatingDoc ? 'Creating…' : '+ New document'}
        </motion.button>
      </div>
      <div className="mt-4 p-4 bg-white rounded-xl shadow">
        <h3 className="font-semibold mb-3">New document</h3>
        <div className="flex gap-2">
          <input
            className="flex-1 border p-2 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            placeholder="Document name"
            value={newDocTitle}
            onChange={(e) => setNewDocTitle(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && createDocument()}
          />
          <button
            onClick={createDocument}
            disabled={creatingDoc}
            className="bg-brand-600 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-50"
          >
            {creatingDoc ? 'Creating…' : 'Create'}
          </button>
        </div>
      </div>
    </motion.div>
  )
}
