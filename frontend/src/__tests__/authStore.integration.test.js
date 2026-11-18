import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useAuthStore } from '../store/authStore'

describe('authStore integration — memberships / currentWorkspace / role transitions', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {})
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {})
  })

  afterEach(() => {
    useAuthStore.getState().logout()
    vi.restoreAllMocks()
  })

  it('starts with null user, empty memberships, null workspace, null role', () => {
    const s = useAuthStore.getState()
    expect(s.user).toBeNull()
    expect(s.memberships).toEqual([])
    expect(s.currentWorkspace).toBeNull()
    expect(s.role).toBeNull()
    expect(s.isAuthenticated).toBe(false)
    expect(s.pendingInvitations).toEqual([])
  })

  it('setUser marks authenticated and stores user', () => {
    useAuthStore.getState().setUser({ id: 'u1', email: 'a@b.c' })
    const s = useAuthStore.getState()
    expect(s.user).toEqual({ id: 'u1', email: 'a@b.c' })
    expect(s.isAuthenticated).toBe(true)
  })

  it('logout clears user, tokens, memberships, workspace, role, and invitations', () => {
    useAuthStore.getState().login(
      { id: 'u1', email: 'a@b.c' },
      'access-token',
      'refresh-token'
    )
    useAuthStore.getState().setMemberships([
      { workspaceId: 'w1', role: 'member' },
    ])
    useAuthStore.getState().setCurrentWorkspace({ id: 'w1', name: 'Acme' })
    useAuthStore.getState().setRole('member')
    useAuthStore.getState().addInvitation({ id: 'inv1', email: 'a@b.c', role: 'member', workspaceId: 'w1' })

    useAuthStore.getState().logout()

    const s = useAuthStore.getState()
    expect(s.user).toBeNull()
    expect(s.accessToken).toBeNull()
    expect(s.refreshToken).toBeNull()
    expect(s.isAuthenticated).toBe(false)
    expect(s.memberships).toEqual([])
    expect(s.currentWorkspace).toBeNull()
    expect(s.role).toBeNull()
    expect(s.pendingInvitations).toEqual([])
  })

  it('setMemberships replaces the whole memberships array', () => {
    useAuthStore.getState().setMemberships([{ workspaceId: 'w1', role: 'member' }])
    expect(useAuthStore.getState().memberships).toEqual([{ workspaceId: 'w1', role: 'member' }])

    useAuthStore.getState().setMemberships([
      { workspaceId: 'w1', role: 'admin' },
      { workspaceId: 'w2', role: 'viewer' },
    ])
    expect(useAuthStore.getState().memberships).toHaveLength(2)
    expect(useAuthStore.getState().memberships.find((m) => m.workspaceId === 'w2').role).toBe('viewer')
  })

  it('setCurrentWorkspace sets the workspace context', () => {
    useAuthStore.getState().setCurrentWorkspace({ id: 'w1', name: 'Acme' })
    expect(useAuthStore.getState().currentWorkspace).toEqual({ id: 'w1', name: 'Acme' })
  })

  it('setRole transitions the current workspace role', () => {
    useAuthStore.getState().setRole('member')
    expect(useAuthStore.getState().role).toBe('member')

    useAuthStore.getState().setRole('admin')
    expect(useAuthStore.getState().role).toBe('admin')

    useAuthStore.getState().setRole('owner')
    expect(useAuthStore.getState().role).toBe('owner')
  })

  it('addInvitation dedupes by id', () => {
    const inv = { id: 'inv1', email: 'a@b.c', role: 'member', workspaceId: 'w1' }
    useAuthStore.getState().addInvitation(inv)
    useAuthStore.getState().addInvitation(inv) // duplicate
    expect(useAuthStore.getState().pendingInvitations).toHaveLength(1)

    const inv2 = { id: 'inv2', email: 'b@b.c', role: 'admin', workspaceId: 'w1' }
    useAuthStore.getState().addInvitation(inv2)
    expect(useAuthStore.getState().pendingInvitations).toHaveLength(2)
  })

  it('removeInvitation removes by id', () => {
    useAuthStore.getState().addInvitation({ id: 'inv1', email: 'a@b.c', role: 'member', workspaceId: 'w1' })
    useAuthStore.getState().addInvitation({ id: 'inv2', email: 'b@b.c', role: 'admin', workspaceId: 'w1' })
    useAuthStore.getState().removeInvitation('inv1')
    expect(useAuthStore.getState().pendingInvitations).toHaveLength(1)
    expect(useAuthStore.getState().pendingInvitations[0].id).toBe('inv2')
  })

  it('full membership lifecycle: login → set memberships → set workspace → set role → logout', () => {
    // login
    useAuthStore.getState().login({ id: 'u1', email: 'a@b.c' }, 'at', 'rt')
    let s = useAuthStore.getState()
    expect(s.isAuthenticated).toBe(true)

    // load memberships
    useAuthStore.getState().setMemberships([
      { workspaceId: 'w1', role: 'member' },
      { workspaceId: 'w2', role: 'admin' },
    ])
    s = useAuthStore.getState()
    expect(s.memberships).toHaveLength(2)

    // select a workspace
    useAuthStore.getState().setCurrentWorkspace({ id: 'w1', name: 'Acme' })
    s = useAuthStore.getState()
    expect(s.currentWorkspace).toEqual({ id: 'w1', name: 'Acme' })

    // assume role within that workspace
    useAuthStore.getState().setRole('member')
    s = useAuthStore.getState()
    expect(s.role).toBe('member')

    // logout resets everything
    useAuthStore.getState().logout()
    s = useAuthStore.getState()
    expect(s.user).toBeNull()
    expect(s.memberships).toEqual([])
    expect(s.currentWorkspace).toBeNull()
    expect(s.role).toBeNull()
    expect(s.isAuthenticated).toBe(false)
  })

  it('initFromStorage sets authenticated from tokens without user', () => {
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'accessToken') return 'at'
      if (key === 'refreshToken') return 'rt'
      return null
    })
    useAuthStore.getState().initFromStorage()
    const s = useAuthStore.getState()
    expect(s.accessToken).toBe('at')
    expect(s.refreshToken).toBe('rt')
    expect(s.isAuthenticated).toBe(true)
    getItemSpy.mockRestore()
  })
})
