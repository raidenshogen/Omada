import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Board from '../pages/Board'
import { useBoardStore } from '../store/boardStore'

// Mock the API layer: board-1 has one list "Todo" with one card
vi.mock('../services/api', () => ({
  default: {
    get: vi.fn((url) => {
      if (url === '/boards/1/lists') {
        return Promise.resolve({ data: [{ id: 'l1', title: 'Todo', position: 0 }] })
      }
      if (url === '/boards/1/lists/l1/cards') {
        return Promise.resolve({ data: [{ id: 'c1', title: 'Seeded card', description: '', position: 0, version: 1 }] })
      }
      return Promise.resolve({ data: [] })
    }),
    post: vi.fn((url, body) => Promise.resolve({ data: { id: 'c2', ...body, version: 1 } })),
    patch: vi.fn((url, body) => Promise.resolve({ data: { id: 'c1', ...body, version: 2 } })),
    delete: vi.fn(() => Promise.resolve({})),
  },
}))

describe('Board', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useBoardStore.getState().setLists([])
  })

  const renderBoard = () =>
    render(
      <MemoryRouter initialEntries={['/board/1']}>
        <Routes>
          <Route path="/board/:id" element={<Board />} />
        </Routes>
      </MemoryRouter>
    )

  it('loads lists and cards from the API', async () => {
    renderBoard()
    expect(await screen.findByText('Todo')).toBeInTheDocument()
    expect(await screen.findByText('Seeded card')).toBeInTheDocument()
  })

  it('adds a card through the API when submitted', async () => {
    renderBoard()
    fireEvent.click(await screen.findByText('+ Add card'))
    fireEvent.change(screen.getByPlaceholderText('Card title'), { target: { value: 'My new card' } })
    fireEvent.click(screen.getByText('Add card'))

    await waitFor(() => expect(screen.getByText('My new card')).toBeInTheDocument())
    const api = (await import('../services/api')).default
    expect(api.post).toHaveBeenCalledWith('/boards/1/lists/l1/cards', expect.objectContaining({ title: 'My new card' }))
  })

  it('shows an error banner when loading fails', async () => {
    const api = (await import('../services/api')).default
    api.get.mockRejectedValueOnce({ response: { data: { message: 'Board not found' } } })
    renderBoard()
    expect(await screen.findByText('Board not found')).toBeInTheDocument()
  })

  it('shows the add-card button', async () => {
    renderBoard()
    expect(await screen.findByText('+ Add card')).toBeInTheDocument()
  })
})
