import { motion } from 'framer-motion'
import { useCallback, useEffect, useState } from 'react'
import api from '../services/api'
import { useActivitySocket } from '../hooks/useWebSocket'

const PAGE_SIZE = 8

function describe(event) {
  switch (event.type) {
    case 'CARD_CREATED':
      return 'created a card'
    case 'CARD_UPDATED':
      return 'updated a card'
    case 'CARD_DELETED':
      return 'deleted a card'
    case 'COMMENT_ADDED':
      return 'added a comment'
    default:
      return (event.type || 'did something').toLowerCase().replace(/_/g, ' ')
  }
}

function timeAgo(iso) {
  if (!iso) return ''
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  return `${Math.floor(hours / 24)}d ago`
}

/**
 * Live, paginated activity feed for a workspace. Loads a page at a time and
 * prepends new events pushed over the realtime activity topic.
 */
export default function ActivityPanel({ workspaceId, title = 'Recent Activity' }) {
  const [items, setItems] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(
    async (nextPage) => {
      if (!workspaceId) return
      setLoading(true)
      setError('')
      try {
        const res = await api.get(
          `/activities?workspaceId=${encodeURIComponent(workspaceId)}&page=${nextPage}&size=${PAGE_SIZE}`
        )
        const data = res.data || {}
        const content = Array.isArray(data) ? data : data.content || []
        setItems((prev) => (nextPage === 0 ? content : [...prev, ...content]))
        setPage(data.page ?? nextPage)
        setTotalPages(data.totalPages ?? 0)
      } catch (e) {
        setError(e?.response?.data?.message || 'Could not load activity')
      } finally {
        setLoading(false)
      }
    },
    [workspaceId]
  )

  useEffect(() => {
    setItems([])
    setPage(0)
    setTotalPages(0)
    load(0)
  }, [workspaceId, load])

  useActivitySocket(workspaceId, (event) => {
    setItems((prev) => (prev.some((i) => i.id === event.id) ? prev : [event, ...prev]))
  })

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 0.4 }}
      className="mt-8 bg-white rounded-2xl shadow p-6"
    >
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-semibold">{title}</h3>
        {items.length > 0 && <span className="text-xs text-gray-400">{items.length} shown</span>}
      </div>

      {error && <div className="mb-2 text-sm text-red-600">{error}</div>}

      {items.length === 0 && !loading ? (
        <div className="text-sm text-gray-500">No activity yet — create a board and add some cards.</div>
      ) : (
        <div className="space-y-2 text-sm text-gray-600">
          {items.map((event) => (
            <motion.div
              key={event.id}
              initial={{ x: -10, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              className="flex items-center justify-between gap-3"
            >
              <span>{describe(event)}</span>
              <span className="text-xs text-gray-400 shrink-0">{timeAgo(event.createdAt)}</span>
            </motion.div>
          ))}
        </div>
      )}

      {page + 1 < totalPages && (
        <button
          onClick={() => load(page + 1)}
          disabled={loading}
          className="mt-4 text-sm font-medium text-brand-600 disabled:opacity-50"
        >
          {loading ? 'Loading…' : 'Load more'}
        </button>
      )}
    </motion.div>
  )
}
