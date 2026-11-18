import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import MemberRow from '../components/MemberRow'
import { useAuthStore } from '../store/authStore'

vi.mock('../services/api', () => ({
  default: {
    patch: vi.fn(() => Promise.resolve({ data: { ok: true } })),
    delete: vi.fn(() => Promise.resolve({})),
  },
}))

const renderRow = (role = 'owner', currentUserId = 'u1', member = { userId: 'u2', email: 'bob@example.com', name: 'Bob', role: 'member' }, workspaceId = '1', onRoleChanged) => {
  useAuthStore.getState().setRole(role)
  useAuthStore.getState().setUser({ id: currentUserId, email: 'me@example.com' })
  return render(<MemberRow member={member} workspaceId={workspaceId} onRoleChanged={onRoleChanged} />)
}

describe('MemberRow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().logout()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('displays member email, name, and role badge', () => {
    renderRow()
    expect(screen.getByText('bob@example.com')).toBeInTheDocument()
    expect(screen.getByText('Bob')).toBeInTheDocument()
    expect(screen.getByText('Member')).toBeInTheDocument()
    expect(screen.getByLabelText('Workspace role: Member')).toBeInTheDocument()
  })

  it('does not show management actions for member role viewer', () => {
    renderRow('viewer')
    expect(screen.queryByText('Change role')).not.toBeInTheDocument()
    expect(screen.queryByText('Remove')).not.toBeInTheDocument()
  })

  it('shows change role and remove for owner on non-self member', () => {
    renderRow('owner')
    expect(screen.getByText('Change role')).toBeInTheDocument()
    expect(screen.getByText('Remove')).toBeInTheDocument()
  })

  it('shows change role and remove for admin on non-self member', () => {
    renderRow('admin')
    expect(screen.getByText('Change role')).toBeInTheDocument()
    expect(screen.getByText('Remove')).toBeInTheDocument()
  })

  it('hides management actions for self member even when owner', () => {
    renderRow('owner', 'u2', { userId: 'u2', email: 'bob@example.com', name: 'Bob', role: 'member' })
    expect(screen.queryByText('Change role')).not.toBeInTheDocument()
    expect(screen.queryByText('Remove')).not.toBeInTheDocument()
  })

  it('calls PATCH role change when Save clicked', async () => {
    const api = (await import('../services/api')).default
    renderRow('owner')
    fireEvent.click(screen.getByText('Change role'))
    const select = screen.getByRole('combobox')
    fireEvent.change(select, { target: { value: 'admin' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() =>
      expect(api.patch).toHaveBeenCalledWith('/workspaces/1/members/u2', { role: 'admin' })
    )
  })

  it('updates the visible role tag after a role change', async () => {
    const onRoleChanged = vi.fn()
    renderRow('owner', 'u1', { userId: 'u2', email: 'bob@example.com', role: 'member' }, '1', onRoleChanged)
    fireEvent.click(screen.getByText('Change role'))
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'admin' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => expect(onRoleChanged).toHaveBeenCalledWith('u2', 'admin'))
  })

  it('calls DELETE on remove after confirm', async () => {
    const api = (await import('../services/api')).default
    renderRow('owner')
    fireEvent.click(screen.getByText('Remove'))
    await waitFor(() =>
      expect(api.delete).toHaveBeenCalledWith('/workspaces/1/members/u2')
    )
  })

  it('does not delete when user cancels confirm', async () => {
    const api = (await import('../services/api')).default
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderRow('owner')
    fireEvent.click(screen.getByText('Remove'))
    await waitFor(() => expect(api.delete).not.toHaveBeenCalled())
  })

  it('shows saving state while role change is in flight', async () => {
    const api = (await import('../services/api')).default
    let resolvePatch
    api.patch.mockImplementationOnce(() => new Promise((resolve) => { resolvePatch = resolve }))
    renderRow('owner')
    fireEvent.click(screen.getByText('Change role'))
    const select = screen.getByRole('combobox')
    fireEvent.change(select, { target: { value: 'admin' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(screen.getByText('Saving…')).toBeInTheDocument()
    await act(async () => { resolvePatch({ data: { ok: true } }) })
    await waitFor(() => expect(screen.queryByText('Saving…')).not.toBeInTheDocument())
  })

  it('shows error when role change fails', async () => {
    const api = (await import('../services/api')).default
    api.patch.mockRejectedValueOnce({ response: { data: { message: 'Cannot downgrade owner' } } })
    renderRow('owner')
    fireEvent.click(screen.getByText('Change role'))
    const select = screen.getByRole('combobox')
    fireEvent.change(select, { target: { value: 'viewer' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() =>
      expect(screen.getByText('Cannot downgrade owner')).toBeInTheDocument()
    )
  })

  it('shows removing state while remove is in flight', async () => {
    const api = (await import('../services/api')).default
    let resolveDelete
    api.delete.mockImplementationOnce(() => new Promise((resolve) => { resolveDelete = resolve }))
    renderRow('owner')
    fireEvent.click(screen.getByText('Remove'))
    expect(screen.getByText('Removing…')).toBeInTheDocument()
    await act(async () => { resolveDelete({}) })
    await waitFor(() => expect(screen.queryByText('Removing…')).not.toBeInTheDocument())
  })
})
