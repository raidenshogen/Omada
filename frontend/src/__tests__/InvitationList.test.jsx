import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import InvitationList from '../components/InvitationList'
import { useAuthStore } from '../store/authStore'

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn((url) => {
      if (url === '/workspaces/1/invitations') {
        return Promise.resolve({
          data: [
            { id: 'inv1', token: 'token1', email: 'alice@example.com', role: 'member', status: 'pending' },
            { id: 'inv2', token: 'token2', email: 'bob@example.com', role: 'admin', status: 'pending' },
          ],
        })
      }
      return Promise.resolve({ data: [] })
    }),
    delete: vi.fn(() => Promise.resolve({})),
  },
}))

const renderList = (workspaceId = '1') =>
  render(<InvitationList workspaceId={workspaceId} />)

describe('InvitationList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().logout()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows loading state while fetching invitations', async () => {
    const api = (await import('../services/api')).default
    let resolveGet
    api.get.mockImplementationOnce(() => new Promise((resolve) => { resolveGet = resolve }))
    renderList()
    expect(screen.getByText('Loading invitations…')).toBeInTheDocument()
    act(() => { resolveGet({ data: [] }) })
  })

  it('lists pending invitations with email, role, and status', async () => {
    renderList()
    await screen.findByText('alice@example.com')
    await screen.findByText('bob@example.com')
    expect(await screen.findByText('Member')).toBeInTheDocument()
    expect(await screen.findByText('Admin')).toBeInTheDocument()
    expect(await screen.findAllByText('pending')).toHaveLength(2)
  })

  it('shows "No pending invitations" when list is empty', async () => {
    const api = (await import('../services/api')).default
    api.get.mockResolvedValueOnce({ data: [] })
    renderList()
    await screen.findByText('No pending invitations.')
  })

  it('calls DELETE /invitations/{token} on revoke', async () => {
    const api = (await import('../services/api')).default
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderList()
    await screen.findByText('alice@example.com')
    fireEvent.click(screen.getAllByText('Revoke')[0])
    await waitFor(() =>
      expect(api.delete).toHaveBeenCalledWith('/invitations/token1')
    )
  })

  it('does not call delete API when user cancels revoke', async () => {
    const api = (await import('../services/api')).default
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderList()
    await screen.findByText('alice@example.com')
    fireEvent.click(screen.getAllByText('Revoke')[0])
    await waitFor(() => expect(api.delete).not.toHaveBeenCalled())
  })

  it('shows "Revoking…" and disables button while revoking', async () => {
    const api = (await import('../services/api')).default
    let resolveDelete
    api.delete.mockImplementationOnce(() => new Promise((resolve) => { resolveDelete = resolve }))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderList()
    await screen.findByText('alice@example.com')
    fireEvent.click(screen.getAllByText('Revoke')[0])
    expect(screen.getByText('Revoking…')).toBeInTheDocument()
    await act(async () => { resolveDelete({}) })
    await waitFor(() => expect(screen.queryByText('Revoking…')).not.toBeInTheDocument())
  })

  it('shows error banner when revoke fails', async () => {
    const api = (await import('../services/api')).default
    api.delete.mockRejectedValueOnce({ response: { data: { message: 'Cannot revoke' } } })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderList()
    await screen.findByText('alice@example.com')
    fireEvent.click(screen.getAllByText('Revoke')[0])
    await waitFor(() => expect(screen.getByText('Cannot revoke')).toBeInTheDocument())
  })

  it('shows error banner when loading invitations fails', async () => {
    const api = (await import('../services/api')).default
    api.get.mockRejectedValueOnce({ response: { data: { message: 'Workspace not found' } } })
    renderList()
    await waitFor(() => expect(screen.getByText('Workspace not found')).toBeInTheDocument())
  })
})
