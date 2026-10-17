import { motion } from 'framer-motion'
import { useCallback, useEffect, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import api from '../services/api'
import { useDocumentChannel } from '../hooks/useWebSocket'

const BLOCK_TYPES = ['paragraph', 'heading', 'todo', 'list']

const textClass = {
  heading: 'text-2xl font-bold',
  paragraph: 'text-base',
  list: 'text-base pl-4',
  todo: 'text-base',
}

function sortBlocks(list) {
  return [...list].sort((a, b) => (a.position ?? 0) - (b.position ?? 0))
}

/**
 * Collaborative document editor: a minimal block editor (paragraph, heading,
 * todo, list) backed by the documents/blocks API. Block edits are saved with
 * their version (simple OT), applied live from /topic/doc.{id}, and the header
 * shows who else is editing via presence.
 */
export default function Document() {
  const { id: documentId } = useParams()
  const location = useLocation()

  const [doc, setDoc] = useState(location.state?.title ? { title: location.state.title } : null)
  const [blocks, setBlocks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [newType, setNewType] = useState('paragraph')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [docRes, blocksRes] = await Promise.all([
        api.get(`/documents/${documentId}`),
        api.get(`/documents/${documentId}/blocks`),
      ])
      setDoc(docRes.data)
      setBlocks(sortBlocks(blocksRes.data || []))
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not load the document')
    } finally {
      setLoading(false)
    }
  }, [documentId])

  useEffect(() => {
    load()
  }, [load])

  const upsertBlock = useCallback((block) => {
    if (!block || !block.id) return
    setBlocks((prev) => {
      const exists = prev.some((b) => b.id === block.id)
      const next = exists ? prev.map((b) => (b.id === block.id ? block : b)) : [...prev, block]
      return sortBlocks(next)
    })
  }, [])

  // Live block deltas + editor presence for this document
  const editors = useDocumentChannel(documentId, (event) => {
    if (!event) return
    if (event.type === 'BLOCK_CREATED' || event.type === 'BLOCK_UPDATED') {
      upsertBlock(event.block)
    } else if (event.type === 'BLOCK_DELETED') {
      if (event.block?.id) setBlocks((prev) => prev.filter((b) => b.id !== event.block.id))
    } else if (event.type === 'DOCUMENT_UPDATED' && event.block) {
      setDoc((prev) => ({ ...prev, ...event.block }))
    }
  })

  const saveTitle = async () => {
    if (!doc?.title) return
    try {
      const res = await api.patch(`/documents/${documentId}`, { title: doc.title })
      setDoc(res.data)
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not save the title')
    }
  }

  const addBlock = async () => {
    try {
      const res = await api.post(`/documents/${documentId}/blocks`, {
        type: newType,
        text: '',
        position: blocks.length,
      })
      upsertBlock(res.data)
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not add a block')
    }
  }

  const changeText = (block, text) =>
    setBlocks((prev) => prev.map((b) => (b.id === block.id ? { ...b, text } : b)))

  const patchBlock = async (block, patch) => {
    upsertBlock({ ...block, ...patch }) // optimistic
    try {
      const res = await api.patch(`/documents/${documentId}/blocks/${block.id}`, {
        ...patch,
        version: block.version,
      })
      upsertBlock(res.data)
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not save the block')
      load()
    }
  }

  const deleteBlock = async (block) => {
    try {
      await api.delete(`/documents/${documentId}/blocks/${block.id}`)
      setBlocks((prev) => prev.filter((b) => b.id !== block.id))
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not delete the block')
    }
  }

  return (
    <div className="p-6 bg-gray-50 min-h-screen">
      <div className="flex items-center justify-between mb-4">
        <input
          className="text-2xl font-bold bg-transparent border-none focus:outline-none w-full"
          placeholder="Untitled document"
          value={doc?.title || ''}
          onChange={(e) => setDoc((prev) => ({ ...prev, title: e.target.value }))}
          onBlur={saveTitle}
        />
        <div className="flex -space-x-2 items-center shrink-0">
          {editors.map((p) => (
            <div
              key={p.userId}
              title={p.name}
              className="w-8 h-8 rounded-full bg-indigo-500 text-white flex items-center justify-center text-xs border-2 border-white uppercase"
            >
              {(p.name || '?').slice(0, 1)}
            </div>
          ))}
        </div>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">{error}</div>
      )}

      {loading ? (
        <div className="text-gray-500">Loading document…</div>
      ) : (
        <div className="bg-white rounded-2xl shadow p-4 max-w-3xl">
          {blocks.length === 0 ? (
            <div className="text-sm text-gray-500 py-6 text-center">
              This document is empty — add your first block below.
            </div>
          ) : (
            blocks.map((block) => (
              <motion.div
                key={block.id}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                className="group flex items-start gap-2 p-2 rounded-lg hover:bg-gray-50"
              >
                <select
                  value={block.type}
                  onChange={(e) => patchBlock(block, { type: e.target.value })}
                  className="mt-1 text-xs text-gray-400 bg-transparent focus:outline-none"
                  aria-label="Block type"
                >
                  {BLOCK_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
                {block.type === 'todo' && (
                  <input
                    type="checkbox"
                    className="mt-2"
                    checked={!!block.checked}
                    onChange={(e) => patchBlock(block, { checked: e.target.checked })}
                    aria-label="Toggle todo"
                  />
                )}
                <textarea
                  className={`flex-1 resize-none bg-transparent border-none focus:outline-none ${textClass[block.type] || ''}`}
                  rows={1}
                  placeholder={block.type === 'heading' ? 'Heading' : 'Type something…'}
                  value={block.text || ''}
                  onChange={(e) => changeText(block, e.target.value)}
                  onBlur={(e) => patchBlock(block, { text: e.target.value })}
                />
                <button
                  onClick={() => deleteBlock(block)}
                  className="opacity-0 group-hover:opacity-100 text-red-500 text-sm px-1"
                  aria-label="Delete block"
                >
                  ×
                </button>
              </motion.div>
            ))
          )}

          <div className="flex items-center gap-2 mt-4 pt-4 border-t">
            <select
              value={newType}
              onChange={(e) => setNewType(e.target.value)}
              className="border rounded-lg p-2 text-sm"
              aria-label="New block type"
            >
              {BLOCK_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            <button
              onClick={addBlock}
              className="bg-brand-600 text-white text-sm font-medium px-4 py-2 rounded-lg"
            >
              Add block
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
