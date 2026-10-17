import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore } from '../store/authStore'

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
  })

  it('initially has null user', () => {
    expect(useAuthStore.getState().user).toBeNull()
  })

  it('setUser updates user state', () => {
    const user = { id: '1', email: 'test@example.com' }
    useAuthStore.getState().setUser(user)
    expect(useAuthStore.getState().user).toEqual(user)
  })

  it('logout clears user state', () => {
    useAuthStore.getState().setUser({ id: '1' })
    useAuthStore.getState().logout()
    expect(useAuthStore.getState().user).toBeNull()
  })
})
