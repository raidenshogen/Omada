import { describe, it, expect, beforeEach } from 'vitest'
import { useBoardStore } from '../store/boardStore'

const seed = [
  { id: 'l1', title: 'Todo', cards: [{ id: 'c1', title: 'A', position: 0 }] },
  { id: 'l2', title: 'Doing', cards: [] },
  { id: 'l3', title: 'Done', cards: [{ id: 'c9', title: 'Old', position: 0 }] },
]

describe('boardStore realtime (applyRemoteEvent) and drag (moveCard)', () => {
  beforeEach(() => {
    // deep copy: store mutations touch nested card arrays, a shallow copy
    // would leak state between tests
    useBoardStore.getState().setLists(JSON.parse(JSON.stringify(seed)))
  })

  it('applyRemoteEvent inserts a CARD_CREATED into the target list', () => {
    useBoardStore.getState().applyRemoteEvent({
      type: 'CARD_CREATED',
      listId: 'l2',
      card: { id: 'c2', title: 'New', position: 0 },
    })
    const lists = useBoardStore.getState().lists
    expect(lists.find((l) => l.id === 'l2').cards.map((c) => c.title)).toEqual(['New'])
  })

  it('applyRemoteEvent moves a card between lists on CARD_UPDATED', () => {
    useBoardStore.getState().applyRemoteEvent({
      type: 'CARD_UPDATED',
      listId: 'l3',
      previousListId: 'l1',
      card: { id: 'c1', title: 'A', position: 1 },
    })
    const lists = useBoardStore.getState().lists
    expect(lists.find((l) => l.id === 'l1').cards).toHaveLength(0)
    // position 1 in a 1-card list = appended after c9 (drag sends target length)
    expect(lists.find((l) => l.id === 'l3').cards.map((c) => c.id)).toEqual(['c9', 'c1'])
  })

  it('applyRemoteEvent removes the card on CARD_DELETED', () => {
    useBoardStore.getState().applyRemoteEvent({
      type: 'CARD_DELETED',
      listId: 'l1',
      card: { id: 'c1' },
    })
    const lists = useBoardStore.getState().lists
    expect(lists.find((l) => l.id === 'l1').cards).toHaveLength(0)
  })

  it('applyRemoteEvent is idempotent when the same event echoes twice', () => {
    const event = { type: 'CARD_CREATED', listId: 'l2', card: { id: 'c2', title: 'New', position: 0 } }
    useBoardStore.getState().applyRemoteEvent(event)
    useBoardStore.getState().applyRemoteEvent(event)
    const lists = useBoardStore.getState().lists
    expect(lists.find((l) => l.id === 'l2').cards).toHaveLength(1)
  })

  it('moveCard relocates a card optimistically', () => {
    useBoardStore.getState().moveCard('l1', 'l2', 'c1', 0)
    const lists = useBoardStore.getState().lists
    expect(lists.find((l) => l.id === 'l1').cards).toHaveLength(0)
    expect(lists.find((l) => l.id === 'l2').cards[0].id).toBe('c1')
  })

  it('reorderLocal moves a card down within the column', () => {
    // dedicated fixture: l1 needs 3 cards for reordering
    useBoardStore.getState().setLists([
      { id: 'l1', title: 'Todo', cards: [
        { id: 'c1', title: 'A', position: 0 },
        { id: 'c2', title: 'B', position: 1 },
        { id: 'c3', title: 'C', position: 2 },
      ]},
      { id: 'l2', title: 'Doing', cards: [] },
    ])
    useBoardStore.getState().reorderLocal('l1', ['c2', 'c1', 'c3'])
    expect(useBoardStore.getState().lists.find((l) => l.id === 'l1').cards.map((c) => c.id)).toEqual([
      'c2', 'c1', 'c3',
    ])
  })

  it('reorderLocal moves a card up within the column', () => {
    useBoardStore.getState().setLists([
      { id: 'l1', title: 'Todo', cards: [
        { id: 'c1', title: 'A', position: 0 },
        { id: 'c2', title: 'B', position: 1 },
        { id: 'c3', title: 'C', position: 2 },
      ]},
      { id: 'l2', title: 'Doing', cards: [] },
    ])
    useBoardStore.getState().reorderLocal('l1', ['c3', 'c1', 'c2'])
    expect(useBoardStore.getState().lists.find((l) => l.id === 'l1').cards.map((c) => c.id)).toEqual([
      'c3', 'c1', 'c2',
    ])
  })

  it('reorderLocal keeps cards missing from orderedIds at the end (defensive)', () => {
    useBoardStore.getState().setLists([
      { id: 'l1', title: 'Todo', cards: [
        { id: 'c1', title: 'A', position: 0 },
        { id: 'c2', title: 'B', position: 1 },
        { id: 'c3', title: 'C', position: 2 },
      ]},
      { id: 'l2', title: 'Doing', cards: [] },
    ])
    useBoardStore.getState().reorderLocal('l1', ['c2'])
    const ids = useBoardStore.getState().lists.find((l) => l.id === 'l1').cards.map((c) => c.id)
    expect(ids).toEqual(['c2', 'c1', 'c3'])
  })

  it('reorderLocal does not touch other lists', () => {
    useBoardStore.getState().setLists([
      { id: 'l1', title: 'Todo', cards: [
        { id: 'c1', title: 'A', position: 0 },
        { id: 'c2', title: 'B', position: 1 },
        { id: 'c3', title: 'C', position: 2 },
      ]},
      { id: 'l2', title: 'Doing', cards: [] },
    ])
    useBoardStore.getState().reorderLocal('l1', ['c3', 'c2', 'c1'])
    expect(useBoardStore.getState().lists.find((l) => l.id === 'l2').cards).toHaveLength(0)
  })
})
