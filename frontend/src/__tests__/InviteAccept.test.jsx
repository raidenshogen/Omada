import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import InviteAccept from '../pages/InviteAccept'
import { useAuthStore } from '../store/authStore'

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn((url) => {
      if (url === '/api/invitations/tok-123') {
        return Promise.resolve({
          data: {
            id: 'inv-1',
            token: 'tok-123',
            email: 'alice@example.com',
            role: 'member',
            workspaceId: 'w1',
            workspaceName: 'Acme',
          },
        })
      }
      return Promise.reject({ response: { data: { message: 'Not found' } } })
    }),
    post: vi.fn(() => Promise.resolve({ data: {} })),
  },
}))

const renderPage = (token = 'tok-123') =>
  render(
    <MemoryRouter initialEntries={[`/invite/${token}`]}>
      <Routes>
        <Route path="/invite/:token" element={<InviteAccept />} />
      </Routes>
    </MemoryRouter>
  )

describe('InviteAccept', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().logout()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  it('shows loading state while fetching invitation', async () => {
    const api = (await import('../services/api')).default
    let resolveGet
    api.get.mockImplementationOnce(() => new Promise((resolve) => { resolveGet = resolve }))
    renderPage()
    expect(screen.getByText('Loading invitation…')).toBeInTheDocument()
    await act(async () => { resolveGet({ data: { id: 'inv-1', token: 'tok-123', email: 'a@b.c', role: 'member', workspaceId: 'w1', workspaceName: 'W' } }) })
    await waitFor(() => expect(screen.queryByText('Loading invitation…')).not.toBeInTheDocument())
  })

  it('displays invitation details once loaded', async () => {
    renderPage()
    await screen.findByText('Join workspace?')
    expect(await screen.findByText('alice@example.com')).toBeInTheDocument()
    expect(await screen.findByText('member')).toBeInTheDocument()
  })

  it('shows error when invitation does not exist', async () => {
    renderPage('bad-token')
    await waitFor(() =>
      expect(screen.getByText('Invitation not found')).toBeInTheDocument()
    )
  })

  it('POSTs accept and calls addInvitation, then redirects on accept', async () => {
    const api = (await import('../services/api')).default
    const addInvitation = vi.spyOn(useAuthStore.getState(), 'addInvitation')
    const navigate = vi.fn()
    vi.spyOn(require('react-router-dom'), 'useNavigate').mockReturnValue(navigate)

    renderPage()
    await screen.findByText('Accept invitation')
    fireEvent.click(screen.getByRole('button', { name: 'Accept invitation' }))
    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/api/invitations/tok-123/accept')
    )
    expect(addInvitation).toHaveBeenCalledWith(
      expect.objectContaining({ email: 'alice@example.com', role: 'member', workspaceId: 'w1' })
    )
    await act(async () => { vi.advanceTimersByTime(600) })
    expect(navigate).toHaveBeenCalledWith('/workspace/w1')
  })

  it('POSTs decline and shows "Declined." on decline', async () => {
    const api = (await import('../services/api')).default
    renderPage()
    await screen.findByText('Decline')
    fireEvent.click(screen.getByRole('button', { name: 'Decline' }))
    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/api/invitations/tok-123/decline')
    )
    await waitFor(() => expect(screen.getByText('Declined.')).toBeInTheDocument())
  })

  it('disables buttons while acting', async () => {
    const api = (await import('../services/api')).default
    let resolvePost
    api.post.mockImplementationOnce(() => new Promise((resolve) => { resolvePost = resolve }))
    renderPage()
    await screen.findByText('Accept invitation')
    fireEvent.click(screen.getByRole('button', { name: 'Accept invitation' }))
    expect(screen.getByText('Accepting…')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Decline' })).toBeDisabled()
    await act(async () => { resolvePost({ data: {} }) })
    await waitFor(() => expect(screen.queryByText('Accepting…')).not.toBeInTheDocument())
  })
})
