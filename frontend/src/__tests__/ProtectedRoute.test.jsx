import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '../components/ProtectedRoute'
import { useAuthStore } from '../store/authStore'

const Child = () => <div data-testid="child">Protected content</div>

const Login = () => <div data-testid="login">Login</div>

const renderGuard = (
  authed = true,
  memberships = [],
  requiredRole = undefined,
  workspaceParam = undefined,
  path = '/dashboard'
) => {
  useAuthStore.getState().setRole(memberships.find((m) => m.workspaceId === 'w1')?.role ?? null)
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route
          path={workspaceParam ? '/workspace/:id' : '/dashboard'}
          element={
            <ProtectedRoute requiredRole={requiredRole} workspaceParam={workspaceParam}>
              <Child />
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.getState().logout()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('redirects unauthenticated user to login', () => {
    useAuthStore.getState().setUser(null)
    useAuthStore.getState().setRole(null)
    renderGuard(false)
    expect(screen.getByText('Login')).toBeInTheDocument()
  })

  it('renders children when authenticated and no workspace constraint', () => {
    useAuthStore.getState().setUser({ id: 'u1', email: 'a@b.c' })
    useAuthStore.getState().setRole('member')
    renderGuard(true, [], undefined, undefined, '/dashboard')
    expect(screen.getByTestId('child')).toBeInTheDocument()
  })

  it('redirects to dashboard when not a member of the workspace', () => {
    useAuthStore.getState().setUser({ id: 'u1', email: 'a@b.c' })
    useAuthStore.getState().setRole(null)
    renderGuard(true, [], 'member', 'id', '/workspace/w1')
    expect(screen.queryByTestId('child')).not.toBeInTheDocument()
  })

  it('renders children when user is a member with sufficient role', () => {
    useAuthStore.getState().setUser({ id: 'u1', email: 'a@b.c' })
    useAuthStore.getState().setRole('admin')
    renderGuard(
      true,
      [{ workspaceId: 'w1', role: 'admin' }],
      'member',
      'id',
      '/workspace/w1'
    )
    expect(screen.getByTestId('child')).toBeInTheDocument()
  })

  it('redirects to dashboard when user role is below required role', () => {
    useAuthStore.getState().setUser({ id: 'u1', email: 'a@b.c' })
    useAuthStore.getState().setRole('viewer')
    renderGuard(
      true,
      [{ workspaceId: 'w1', role: 'viewer' }],
      'admin',
      'id',
      '/workspace/w1'
    )
    expect(screen.queryByTestId('child')).not.toBeInTheDocument()
  })

  it('grants access when role exactly meets required rank', () => {
    useAuthStore.getState().setUser({ id: 'u1', email: 'a@b.c' })
    useAuthStore.getState().setRole('admin')
    renderGuard(
      true,
      [{ workspaceId: 'w1', role: 'admin' }],
      'admin',
      'id',
      '/workspace/w1'
    )
    expect(screen.getByTestId('child')).toBeInTheDocument()
  })

  it('grants access to owner for any required role', () => {
    useAuthStore.getState().setUser({ id: 'u1', email: 'a@b.c' })
    useAuthStore.getState().setRole('owner')
    renderGuard(
      true,
      [{ workspaceId: 'w1', role: 'owner' }],
      'owner',
      'id',
      '/workspace/w1'
    )
    expect(screen.getByTestId('child')).toBeInTheDocument()
  })

  it('renders children when workspaceParam set but no requiredRole and user is a member', () => {
    useAuthStore.getState().setUser({ id: 'u1', email: 'a@b.c' })
    useAuthStore.getState().setRole('member')
    renderGuard(
      true,
      [{ workspaceId: 'w1', role: 'member' }],
      undefined,
      'id',
      '/workspace/w1'
    )
    expect(screen.getByTestId('child')).toBeInTheDocument()
  })
})
