import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import api from '../services/api'
import { useBoardStore } from '../store/boardStore'
import { useBoardSocket } from '../hooks/useWebSocket'
import { usePresence } from '../hooks/usePresence'
import CardItem from '../components/CardItem'

/**
 * Board page: loads lists and cards from the API for the board in the URL,
 * and creates / edits / deletes cards against the backend.
 */
export default function Board() {
  const { id: boardId } = useParams()
  const location = useLocation()
  const boardTitle = location.state?.title || 'Board'

  const lists = useBoardStore((s) => s.lists)
  const setLists = useBoardStore((s) => s.setLists)
  const addCardLocal = useBoardStore((s) => s.addCardLocal)
  const removeCardLocal = useBoardStore((s) => s.removeCardLocal)
  const updateCardLocal = useBoardStore((s) => s.updateCardLocal)

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [addingFor, setAddingFor] = useState(null) // listId with an open "add card" form
  const [newTitle, setNewTitle] = useState('')
  const [newDesc, setNewDesc] = useState('')
  const [editingCard, setEditingCard] = useState(null) // { listId, card }
  const [editTitle, setEditTitle] = useState('')
  const [editDesc, setEditDesc] = useState('')
  const [comments, setComments] = useState([])
  const [commentBody, setCommentBody] = useState('')
  const [commentsError, setCommentsError] = useState('')
  const [dragging, setDragging] = useState(null) // { cardId, fromListId }
  const [dragOverList, setDragOverList] = useState(null)
  const [dropTarget, setDropTarget] = useState(null) // { listId, index } insertion point
  const moveCardLocal = useBoardStore((s) => s.moveCard)
  const reorderLocal = useBoardStore((s) => s.reorderLocal)
  const applyRemoteEvent = useBoardStore((s) => s.applyRemoteEvent)

  // Realtime sync: apply card events broadcast to this board's topic.
  // Own changes also echo back, which is safe — applyRemoteEvent is idempotent.
  useBoardSocket(boardId, (event) => {
    if (event && event.boardId === boardId) applyRemoteEvent(event)
  })

  // Presence: who is viewing this board right now (avatars in the header)
  const presenceUsers = usePresence(boardId)

  /**
   * Drop handler for both cases: moving a card to another column, and
   * reordering within the same column. The insertion index comes from
   * dropTarget (set by card-level dragOver) or defaults to the column end.
   * Optimistic local update first, then PATCH persistence; on failure the
   * board reloads from the server.
   */
  const dropCard = async (toListId) => {
    setDragOverList(null)
    if (!dragging) {
      setDropTarget(null)
      return
    }
    const { cardId, fromListId } = dragging
    const list = lists.find((l) => l.id === toListId)
    const fallbackIndex = list ? list.cards.length : 0
    const target = dropTarget && dropTarget.listId === toListId ? dropTarget : { listId: toListId, index: fallbackIndex }
    setDropTarget(null)
    setDragging(null)

    if (fromListId === toListId) {
      // in-column reorder
      if (!list) return
      const origIdx = list.cards.findIndex((c) => c.id === cardId)
      if (origIdx === -1) return
      const ids = list.cards.map((c) => c.id).filter((id) => id !== cardId)
      let idx = target.index
      if (origIdx < idx) idx -= 1 // index was measured against the array WITH the dragged card
      idx = Math.max(0, Math.min(idx, ids.length))
      if (idx === origIdx) return // dropped back in place — nothing to do
      ids.splice(idx, 0, cardId)
      reorderLocal(toListId, ids)
      // persist every card whose position actually changed
      const originalOrder = list.cards.map((c) => c.id)
      const changed = ids
        .map((id, i) => ({ id, i }))
        .filter(({ id, i }) => originalOrder.indexOf(id) !== i)
      try {
        for (const { id, i } of changed) {
          await api.patch(`/boards/${boardId}/lists/${toListId}/cards/${id}`, { position: i })
        }
      } catch (e) {
        setError(e?.response?.data?.message || 'Could not reorder the cards')
        reloadBoard()
      }
      return
    }

    // cross-column move: optimistic, then persist
    moveCardLocal(fromListId, toListId, cardId, target.index)
    try {
      await api.patch(`/boards/${boardId}/lists/${fromListId}/cards/${cardId}`, {
        listId: toListId,
        position: target.index,
      })
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not move the card')
      reloadBoard()
    }
  }

  /** Reloads lists+cards from the API (used to resync after a failed mutation). */
  const reloadBoard = async () => {
    try {
      const listsRes = await api.get(`/boards/${boardId}/lists`)
      const withCards = await Promise.all(
        (listsRes.data || []).map(async (list) => {
          const cardsRes = await api.get(`/boards/${boardId}/lists/${list.id}/cards`)
          return { ...list, cards: cardsRes.data || [] }
        })
      )
      setLists(withCards)
    } catch {
      /* keep optimistic state; the banner shows the error */
    }
  }

  /** Card-level dragOver: compute the insertion index (top/bottom half of the card). */
  const cardDragOver = (e, list, cardIndex) => {
    e.preventDefault()
    e.stopPropagation()
    setDragOverList(list.id)
    const rect = e.currentTarget.getBoundingClientRect()
    const after = e.clientY > rect.top + rect.height / 2
    setDropTarget({ listId: list.id, index: cardIndex + (after ? 1 : 0) })
  }

  // Load the board's lists, then each list's cards, into the store
  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError('')
      try {
        const listsRes = await api.get(`/boards/${boardId}/lists`)
        const withCards = await Promise.all(
          (listsRes.data || []).map(async (list) => {
            const cardsRes = await api.get(`/boards/${boardId}/lists/${list.id}/cards`)
            return { ...list, cards: cardsRes.data || [] }
          })
        )
        if (!cancelled) setLists(withCards)
      } catch (e) {
        if (!cancelled) setError(e?.response?.data?.message || 'Could not load the board')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [boardId, setLists])

  const openAddForm = (listId) => {
    setAddingFor(listId)
    setNewTitle('')
    setNewDesc('')
    setError('')
  }

  const submitCard = async (listId) => {
    if (!newTitle.trim()) return
    try {
      const list = lists.find((l) => l.id === listId)
      const res = await api.post(`/boards/${boardId}/lists/${listId}/cards`, {
        title: newTitle.trim(),
        description: newDesc.trim(),
        position: list ? list.cards.length : 0,
      })
      addCardLocal(listId, res.data)
      setAddingFor(null)
      setNewTitle('')
      setNewDesc('')
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not add the card')
    }
  }

  const openEditCard = (listId, card) => {
    setEditingCard({ listId, card })
    setEditTitle(card.title)
    setEditDesc(card.description || '')
    setError('')
    setCommentBody('')
    loadComments(card.id)
  }

  /** Loads a card's comments (shown in the edit dialog). */
  const loadComments = async (cardId) => {
    setComments([])
    setCommentsError('')
    try {
      const res = await api.get(`/cards/${cardId}/comments`)
      setComments(res.data || [])
    } catch (e) {
      setCommentsError(e?.response?.data?.message || 'Could not load comments')
    }
  }

  /** Posts a new comment on the card open in the edit dialog. */
  const submitComment = async () => {
    const body = commentBody.trim()
    if (!body || !editingCard) return
    try {
      const res = await api.post(`/cards/${editingCard.card.id}/comments`, { body })
      setComments((c) => [...c, res.data])
      setCommentBody('')
    } catch (e) {
      setCommentsError(e?.response?.data?.message || 'Could not add the comment')
    }
  }

  const saveCard = async () => {
    const { listId, card } = editingCard
    try {
      const res = await api.patch(`/boards/${boardId}/lists/${listId}/cards/${card.id}`, {
        title: editTitle.trim(),
        description: editDesc.trim(),
      })
      updateCardLocal(listId, res.data)
      setEditingCard(null)
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not update the card')
    }
  }

  const deleteCard = async () => {
    const { listId, card } = editingCard
    try {
      await api.delete(`/boards/${boardId}/lists/${listId}/cards/${card.id}`)
      removeCardLocal(listId, card.id)
      setEditingCard(null)
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not delete the card')
    }
  }

  return (
    <div className="p-6 bg-neutral-50 min-h-screen">
      <div className="flex items-center justify-between mb-6">
        <motion.h2 initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="text-2xl font-bold text-neutral-900 tracking-tight">
          {boardTitle}
        </motion.h2>
        {/* live presence: avatar circles for everyone on this board */}
        <div className="flex -space-x-2 items-center">
          {presenceUsers.map((p) => (
            <div
              key={p.userId}
              title={p.name}
              className="w-7 h-7 rounded-full bg-brand-600 text-white flex items-center justify-center text-[10px] border-2 border-white uppercase font-mono font-medium"
            >
              {(p.name || '?').slice(0, 1).toUpperCase()}
            </div>
          ))}
        </div>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg">{error}</div>
      )}

      {loading ? (
        <div className="text-neutral-500">Loading board…</div>
      ) : (
        <div className="flex gap-3 overflow-x-auto pb-4">
          {lists.map((list) => (
            <motion.div
              key={list.id}
              layout
              className="min-w-[260px] bg-white rounded-xl shadow-soft border border-neutral-200 p-3 transition-all"
              onDragOver={(e) => {
                e.preventDefault()
                setDragOverList(list.id)
              }}
              onDragLeave={() => setDragOverList((cur) => (cur === list.id ? null : cur))}
              onDrop={() => dropCard(list.id)}
            >
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-semibold text-neutral-800 text-sm">
                  {list.title}
                  <span className="text-xs text-neutral-400 ml-1">({list.cards.length})</span>
                </h3>
              </div>

              <div className="space-y-2 min-h-[100px]">
                {list.cards.map((card, cardIndex) => (
                  <motion.div
                    key={card.id}
                    layout
                    whileHover={{ scale: 1.01 }}
                    draggable
                    onDragStart={() => setDragging({ cardId: card.id, fromListId: list.id })}
                    onDragEnd={() => setDragging(null)}
                    onDragOver={(e) => cardDragOver(e, list, cardIndex)}
                    className="card-base p-3 cursor-pointer mb-1"
                    onClick={() => openEditCard(list.id, card)}
                  >
                    <CardItem title={card.title} description={card.description} />
                  </motion.div>
                ))}
              </div>

              {addingFor === list.id ? (
                <div className="mt-3 space-y-2">
                  <input
                    autoFocus
                    className="input-field"
                    placeholder="Card title"
                    value={newTitle}
                    onChange={(e) => setNewTitle(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && submitCard(list.id)}
                  />
                  <textarea
                    className="input-field"
                    placeholder="Description (optional)"
                    rows={2}
                    value={newDesc}
                    onChange={(e) => setNewDesc(e.target.value)}
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={() => submitCard(list.id)}
                      className="flex-1 btn-primary"
                    >
                      Add card
                    </button>
                    <button onClick={() => setAddingFor(null)} className="flex-1 btn-ghost text-neutral-500 hover:bg-neutral-100">
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  onClick={() => openAddForm(list.id)}
                  className="mt-3 w-full text-sm text-brand-600 font-medium hover:underline"
                >
                  + Add card
                </motion.button>
              )}
            </motion.div>
          ))}
        </div>
      )}

      {/* Edit card dialog (inline modal) */}
      {editingCard && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4" onClick={() => setEditingCard(null)}>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-white rounded-2xl shadow-card-hover p-6 w-full max-w-md border border-neutral-100"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-lg text-neutral-900">Edit card</h3>
              <button onClick={() => setEditingCard(null)} className="text-neutral-400 hover:text-neutral-600">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>

            <input
              className="input-field mb-3"
              placeholder="Title"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
            />
            <textarea
              className="input-field mb-4"
              placeholder="Description"
              rows={3}
              value={editDesc}
              onChange={(e) => setEditDesc(e.target.value)}
            />

            <div className="mb-4">
              <h4 className="font-semibold text-sm text-neutral-700 mb-2">Comments</h4>
              {commentsError && <div className="mb-2 text-xs text-red-600">{commentsError}</div>}
              <div className="space-y-2 max-h-40 overflow-y-auto mb-3">
                {comments.length === 0 ? (
                  <div className="text-xs text-neutral-400">No comments yet</div>
                ) : (
                  comments.map((c) => (
                    <div key={c.id} className="text-sm bg-neutral-50 rounded-lg p-2">
                      <div className="text-xs text-neutral-400 mb-1">{c.authorId || 'user'}</div>
                      {c.body}
                    </div>
                  ))
                )}
              </div>
              <div className="flex gap-2">
                <input
                  className="flex-1 border border-neutral-300 p-2 rounded-lg text-sm focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                  placeholder="Add a comment…"
                  value={commentBody}
                  onChange={(e) => setCommentBody(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && submitComment()}
                />
                <button
                  onClick={submitComment}
                  className="px-3 py-2 rounded-lg text-sm bg-brand-600 text-white hover:bg-brand-700"
                >
                  Comment
                </button>
              </div>
            </div>

            <div className="flex gap-2">
              <button onClick={saveCard} className="flex-1 btn-primary">
                Save
              </button>
              <button onClick={deleteCard} className="px-4 py-2 rounded-lg text-sm text-red-600 hover:bg-red-50">
                Delete
              </button>
              <button onClick={() => setEditingCard(null)} className="px-4 py-2 rounded-lg text-sm text-neutral-500 hover:bg-neutral-100">
                Cancel
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  )
}
