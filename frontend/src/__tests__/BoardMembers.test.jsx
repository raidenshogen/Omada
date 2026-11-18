import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react'
import { useAuthStore } from '../store/authStore'
import BoardMembers from '../components/BoardMembers'

// Mock the API layer: board-1 has two members, no workspace roster provided.
vi.mock('../services/api', () => ({
  default: {
    get: vi.fn((url) => {
      if (url === '/boards/1/members') {
        return Promise.resolve({
          data: [
            { id: 'm1', boardId: '1', userId: 'u1', joinedAt: '2026-01-01T00:00:00Z' },
            { id: 'm2', boardId: '1', userId: 'u2', joinedAt: '2026-01-02T00:00:00Z' },
          ],
        })
      }
      return Promise.resolve({ data: [] })
    }),
    post: vi.fn((url, body) =>
      Promise.resolve({ data: { id: 'm3', boardId: '1', userId: body.userId, joinedAt: '2026-01-03T00:00:00Z' } })
    ),
    delete: vi.fn(() => Promise.resolve({})),
  },
}))

const renderMembers = (open = true, workspaceMembers = []) =>
  render(
    <BoardMembers boardId="1" open={open} onClose={vi.fn()} workspaceMembers={workspaceMembers} />
  )

describe('BoardMembers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().logout()
    useAuthStore.getState().setRole('owner')
    useAuthStore.getState().setUser({ id: 'u1', email: 'owner@example.com' })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('lists the board members', async () => {
    renderMembers()
    expect(await screen.findByText('u1')).toBeInTheDocument()
    expect(await screen.findByText('u2')).toBeInTheDocument()
  })

  it('shows a loading state while members load', async () => {
    const api = (await import('../services/api')).default
    let resolveGet
    api.get.mockImplementationOnce(() => new Promise((resolve) => { resolveGet = resolve }))
    renderMembers()
    expect(screen.getByText('Loading members…')).toBeInTheDocument()
    await act(async () => { resolveGet({ data: [] }) })
    await waitFor(() => expect(screen.queryByText('Loading members…')).not.toBeInTheDocument())
  })

  it('adds a member through the API when the form is submitted', async () => {
    const api = (await import('../services/api')).default
    renderMembers(true, [{ userId: 'u3', email: 'u3@example.com' }])
    await screen.findByText('u1') // wait for members to load
    fireEvent.click(screen.getByRole('button', { name: '+ Add member' }))
    fireEvent.change(screen.getByLabelText('Member'), { target: { value: 'u3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Add member' }))

    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/boards/1/members', { userId: 'u3' })
    )
  })

  it('removes a member through the API', async () => {
    const api = (await import('../services/api')).default
    renderMembers()
    await screen.findByText('u2')
    fireEvent.click(screen.getAllByText('Remove')[0])
    await waitFor(() =>
      expect(api.delete).toHaveBeenCalledWith('/boards/1/members/u2')
    )
    expect(screen.queryByText('u2')).not.toBeInTheDocument()
  })

  it('does not show add/remove for non-owner roles', async () => {
    useAuthStore.getState().setRole('member')
    renderMembers()
    expect(screen.queryByText('+ Add member')).not.toBeInTheDocument()
    await screen.findByText('u1')
    expect(screen.queryByText('Remove')).not.toBeInTheDocument()
  })

  it('filters the add-select to workspace members not yet on the board', async () => {
    renderMembers(true, [
      { userId: 'u1', email: 'u1@example.com' }, // already on the board
      { userId: 'u3', email: 'u3@example.com' }, // not on the board
    ])
    await screen.findByText('u1') // wait for members to load
    fireEvent.click(screen.getByRole('button', { name: '+ Add member' }))
    const select = screen.getByLabelText('Member')
    expect(select.querySelector('option[value="u3"]')).toBeInTheDocument()
    expect(select.querySelector('option[value="u1"]')).not.toBeInTheDocument()
  })

  it('shows an error banner when loading members fails', async () => {
    const api = (await import('../services/api')).default
    api.get.mockRejectedValueOnce({ response: { data: { message: 'Board not found' } } })
    renderMembers()
    expect(await screen.findByText('Board not found')).toBeInTheDocument()
  })
})
