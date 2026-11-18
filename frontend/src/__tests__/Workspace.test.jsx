import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Workspace from '../pages/Workspace'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'

const MEMBERS = [
  { userId: 'u1', email: 'alice@example.com', name: 'Alice', role: 'owner' },
  { userId: 'u2', email: 'bob@example.com', name: 'Bob', role: 'admin' },
  { userId: 'u3', email: 'carol@example.com', name: 'Carol', role: 'member' },
  { userId: 'u4', email: 'dave@example.com', name: 'Dave', role: 'viewer' },
]

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn((url) => {
      if (url === '/workspaces/1/boards') {
        return Promise.resolve({ data: [] })
      }
      if (url === '/workspaces/1/members') {
        return Promise.resolve({ data: MEMBERS })
      }
      return Promise.resolve({ data: [] })
    }),
    patch: vi.fn(() => Promise.resolve({ data: { ok: true } })),
    delete: vi.fn(() => Promise.resolve({})),
    post: vi.fn(() => Promise.resolve({ data: {} })),
  },
}))

const renderWorkspace = (userRole = 'owner', userId = 'u1') => {
  const testMembers = MEMBERS.map((member) =>
    member.userId === userId ? { ...member, role: userRole } : member
  )
  api.get.mockImplementation((url) => {
    if (url === '/workspaces/1/boards') return Promise.resolve({ data: [] })
    if (url === '/workspaces/1/members') return Promise.resolve({ data: testMembers })
    return Promise.resolve({ data: [] })
  })
  // Set the auth store role and user before rendering
  useAuthStore.getState().setRole(userRole)
  useAuthStore.getState().setUser({ id: userId, email: 'me@example.com' })
  return render(
    <MemoryRouter initialEntries={['/workspace/1']}>
      <Routes>
        <Route path="/workspace/:id" element={<Workspace />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('Workspace members section', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
    vi.clearAllMocks()
  })

  it('loads members from the API', async () => {
    renderWorkspace('owner')
    expect(await screen.findByText('alice@example.com')).toBeInTheDocument()
    expect(await screen.findByText('bob@example.com')).toBeInTheDocument()
    expect(await screen.findByText('carol@example.com')).toBeInTheDocument()
    expect(await screen.findByText('dave@example.com')).toBeInTheDocument()
  })

  it('displays role badges for each member', async () => {
    renderWorkspace('owner')
    expect(await screen.findByText('Owner')).toBeInTheDocument()
    expect(await screen.findByText('Admin')).toBeInTheDocument()
    expect(await screen.findByText('Member')).toBeInTheDocument()
    expect(await screen.findByText('Viewer')).toBeInTheDocument()
  })

  it('shows role change dropdown and remove button for Owner (non-self members)', async () => {
    renderWorkspace('owner', 'u1')
    await screen.findByText('bob@example.com')
    const changeRoleButtons = screen.getAllByText('Change role')
    const removeButtons = screen.getAllByText('Remove')
    expect(changeRoleButtons.length).toBeGreaterThan(0)
    expect(removeButtons.length).toBeGreaterThan(0)
    expect(removeButtons.length).toBe(3)
  })

  it('does NOT show role change or remove for Member role', async () => {
    renderWorkspace('member')
    await screen.findByText('alice@example.com')
    await waitFor(() => {
      expect(screen.queryByText('Change role')).not.toBeInTheDocument()
      expect(screen.queryByText('Remove')).not.toBeInTheDocument()
    })
  })

  it('does NOT show role change or remove for Viewer role', async () => {
    renderWorkspace('viewer')
    await screen.findByText('alice@example.com')
    await waitFor(() => {
      expect(screen.queryByText('Change role')).not.toBeInTheDocument()
      expect(screen.queryByText('Remove')).not.toBeInTheDocument()
    })
  })

  it('calls PATCH /workspaces/1/members/{userId} on role change', async () => {
    const api = (await import('../services/api')).default
    renderWorkspace('owner', 'u1')
    await screen.findByText('bob@example.com')
    fireEvent.click(screen.getAllByText('Change role')[0])
    const select = screen.getAllByRole('combobox')[0]
    fireEvent.change(select, { target: { value: 'member' } })
    fireEvent.click(screen.getAllByText('Save')[0])
    await waitFor(() => {
      expect(api.patch).toHaveBeenCalledWith(
        '/workspaces/1/members/u2',
        expect.objectContaining({ role: 'member' })
      )
    })
  })

  it('calls DELETE /api/workspaces/1/members/{userId} on remove', async () => {
    const api = (await import('../services/api')).default
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderWorkspace('owner', 'u1')
    await screen.findByText('bob@example.com')
    fireEvent.click(screen.getAllByText('Remove')[0])
    await waitFor(() => {
      expect(api.delete).toHaveBeenCalledWith('/workspaces/1/members/u2')
    })
  })

  it('does not show management actions for the current user (self)', async () => {
    renderWorkspace('owner', 'u1')
    await screen.findByText('alice@example.com')
    const aliceRow = screen.getByText('alice@example.com').closest('div')
    expect(aliceRow.querySelector('button')).toBeNull()
    const removeButtons = screen.getAllByText('Remove')
    expect(removeButtons.length).toBe(3)
  })
})
