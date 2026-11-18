import { create } from 'zustand'

/**
 * Client-side board state: lists with their cards, kept in sync with the
 * backend by the Board page (API calls happen there; this store only holds
 * state and applies local mutations after an API call succeeds).
 */
export const useBoardStore = create((set) => ({
  lists: [],

  /** Replace the whole board state (after loading lists + cards from the API). */
  setLists: (lists) => set({ lists }),

  /** Append a card to a list after the backend confirmed creation. */
  addCardLocal: (listId, card) =>
    set((s) => ({
      lists: s.lists.map((l) => (l.id === listId ? { ...l, cards: [...l.cards, card] } : l)),
    })),

  /** Remove a card after the backend confirmed deletion. */
  removeCardLocal: (listId, cardId) =>
    set((s) => ({
      lists: s.lists.map((l) => (l.id === listId ? { ...l, cards: l.cards.filter((c) => c.id !== cardId) } : l)),
    })),

  /** Replace one card after a successful update. */
  updateCardLocal: (listId, card) =>
    set((s) => ({
      lists: s.lists.map((l) =>
        l.id === listId ? { ...l, cards: l.cards.map((c) => (c.id === card.id ? card : c)) } : l
      ),
    })),

  /** Reorder inside one list after an in-column drop (optimistic). */
  reorderLocal: (listId, orderedIds) =>
    set((s) => ({
      lists: s.lists.map((l) => {
        if (l.id !== listId) return l
        const byId = new Map(l.cards.map((c) => [c.id, c]))
        const cards = orderedIds.map((id) => byId.get(id)).filter(Boolean)
        // keep any card not present in orderedIds (defensive) at the end
        const rest = l.cards.filter((c) => !orderedIds.includes(c.id))
        return { ...l, cards: [...cards, ...rest] }
      }),
    })),

  /** Optimistic move of a card between lists (used by the drag board). */
  moveCard: (fromListId, toListId, cardId, position) =>
    set((s) => {
      const newLists = [...s.lists]
      const fromList = newLists.find((l) => l.id === fromListId)
      const toList = newLists.find((l) => l.id === toListId)
      if (!fromList || !toList) return s
      const cardIndex = fromList.cards.findIndex((c) => c.id === cardId)
      if (cardIndex === -1) return s
      const [card] = fromList.cards.splice(cardIndex, 1)
      toList.cards.splice(position ?? toList.cards.length, 0, card)
      return { lists: newLists }
    }),

  /** Persist a whole column's order (used after in-column drag). */
  setListCardsLocal: (listId, cards) =>
    set((s) => ({
      lists: s.lists.map((l) => (l.id === listId ? { ...l, cards } : l)),
    })),

  /**
   * Apply a realtime event coming from the WebSocket topic:
   * removes the card from wherever it is, then re-inserts it in the
   * event's list (except for deletes). Handles create/update/move uniformly.
   */
  applyRemoteEvent: (event) =>
    set((s) => {
      const card = event.card
      if (!card || !card.id) return s
      let lists = s.lists.map((l) => ({ ...l, cards: l.cards.filter((c) => c.id !== card.id) }))
      if (event.type !== 'CARD_DELETED') {
        const target = lists.find((l) => l.id === event.listId)
        if (target) {
          const idx = Math.min(card.position ?? target.cards.length, target.cards.length)
          target.cards = [...target.cards.slice(0, idx), card, ...target.cards.slice(idx)]
        }
      }
      return { lists }
    }),
}))
