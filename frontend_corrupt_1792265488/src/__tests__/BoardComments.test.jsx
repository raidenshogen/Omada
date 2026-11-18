import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Board from '../pages/Board'
import { useBoardStore } from '../store/boardStore'

// A board with one list/card; the card has one comment and accepts new ones.
vi.mock('../services/api', () => ({
  default: {
    get: vi.fn((url) => {
      if (url === '/boards/1/lists') {
        return Promise.resolve({ data: [{ id: 'l1', title: 'Todo', position: 0 }] })
      }
      if (url === '/boards/1/lists/l1/cards') {
        return Promise.resolve({ data: [{ id: 'c1', title: 'Seeded card', description: '', position: 0, version: 1 }] })
      }
      if (url === '/cards/c1/comments') {
        return Promise.resolve({ data: [{ id: 'cm1', body: 'hello world', authorId: 'u1' }] })
      }
      return Promise.resolve({ data: [] })
    }),
    post: vi.fn((url, body) => Promise.resolve({ data: { id: 'cm2', authorId: 'u1', ...body } })),
    patch: vi.fn((url, body) => Promise.resolve({ data: { id: 'c1', ...body, version: 2 } })),
    delete: vi.fn(() => Promise.resolve({})),
  },
}))

describe('Board comments', () => {
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

  it('loads and shows a card\'s comments when opened', async () => {
    renderBoard()
    fireEvent.click(await screen.findByText('Seeded card'))
    expect(await screen.findByText('hello world')).toBeInTheDocument()
  })

  it('posts a new comment through the API', async () => {
    renderBoard()
    fireEvent.click(await screen.findByText('Seeded card'))
    await screen.findByText('hello world')

    fireEvent.change(screen.getByPlaceholderText('Add a comment…'), { target: { value: 'looks good' } })
    fireEvent.click(screen.getByText('Comment'))

    await waitFor(() => expect(screen.getByText('looks good')).toBeInTheDocument())
    const api = (await import('../services/api')).default
    expect(api.post).toHaveBeenCalledWith('/cards/c1/comments', { body: 'looks good' })
  })
})
