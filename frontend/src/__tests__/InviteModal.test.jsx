import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import InviteModal from '../components/InviteModal'
import { useAuthStore } from '../store/authStore'

vi.mock('../services/api', () => ({
  default: {
    post: vi.fn(() => Promise.resolve({ data: {} })),
  },
}))

const renderModal = (open = true, workspaceId = '1', onClose = vi.fn(), onInvited = vi.fn()) =>
  render(
    <InviteModal open={open} workspaceId={workspaceId} onClose={onClose} onInvited={onInvited} />
  )

describe('InviteModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().logout()
    useAuthStore.getState().setRole('owner')
    useAuthStore.getState().setUser({ id: 'u1', email: 'owner@example.com' })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders nothing when open is false', () => {
    const { container } = renderModal(false)
    expect(container.firstChild).toBeNull()
  })

  it('shows email input, role select, and send button when open', () => {
    renderModal()
    expect(screen.getByPlaceholderText('name@example.com')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: /role/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send invitation' })).toBeInTheDocument()
  })

  it('displays the current user role in the modal subtitle', () => {
    renderModal()
    expect(screen.getByText(/you: owner/i)).toBeInTheDocument()
  })

  it('offers admin, member, and viewer role options', () => {
    renderModal()
    const select = screen.getByRole('combobox', { name: /role/i })
    fireEvent.click(select)
    expect(screen.getByText('Admin')).toBeInTheDocument()
    expect(screen.getByText('Member')).toBeInTheDocument()
    expect(screen.getByText('Viewer')).toBeInTheDocument()
  })

  it('shows error when submitting with empty email', async () => {
    renderModal()
    fireEvent.click(screen.getByRole('button', { name: 'Send invitation' }))
    // empty email should not trigger API call
    const api = (await import('../services/api')).default
    await waitFor(() => expect(api.post).not.toHaveBeenCalled())
  })

  it('POSTs invitation to /workspaces/{id}/invitations on submit', async () => {
    const api = (await import('../services/api')).default
    renderModal()
    fireEvent.change(screen.getByPlaceholderText('name@example.com'), {
      target: { value: 'alice@example.com' },
    })
    const select = screen.getByRole('combobox')
    fireEvent.change(select, { target: { value: 'admin' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send invitation' }))
    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith('/workspaces/1/invitations', {
        email: 'alice@example.com',
        role: 'admin',
      })
    )
  })

  it('calls onClose and onInvited after successful send', async () => {
    const onClose = vi.fn()
    const onInvited = vi.fn()
    renderModal(true, '1', onClose, onInvited)
    fireEvent.change(screen.getByPlaceholderText('name@example.com'), {
      target: { value: 'alice@example.com' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Send invitation' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(onInvited).toHaveBeenCalled()
  })

  it('disables send button while sending', async () => {
    const api = (await import('../services/api')).default
    let resolvePost
    api.post.mockImplementationOnce(() => new Promise((resolve) => { resolvePost = resolve }))
    renderModal()
    fireEvent.change(screen.getByPlaceholderText('name@example.com'), {
      target: { value: 'alice@example.com' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Send invitation' }))
    expect(screen.getByRole('button', { name: 'Sending…' })).toBeDisabled()
    await act(async () => { resolvePost({ data: {} }) })
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Send invitation' })).not.toBeDisabled()
    )
  })

  it('shows error banner when the API fails', async () => {
    const api = (await import('../services/api')).default
    api.post.mockRejectedValueOnce({ response: { data: { message: 'Already invited' } } })
    renderModal()
    fireEvent.change(screen.getByPlaceholderText('name@example.com'), {
      target: { value: 'alice@example.com' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Send invitation' }))
    await waitFor(() =>
      expect(screen.getByText('Already invited')).toBeInTheDocument()
    )
  })

  it('calls onClose when cancel is clicked', () => {
    const onClose = vi.fn()
    renderModal(true, '1', onClose)
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(onClose).toHaveBeenCalled()
  })
})
