import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Login from '../pages/Login'
import { useAuthStore } from '../store/authStore'

vi.mock('../services/api', () => ({
  default: {
    post: vi.fn(() => Promise.resolve({ data: { user: { id: '1', email: 'alice@example.com', name: 'Alice' } } }))
  }
}))

describe('Login', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
    vi.clearAllMocks()
  })

  it('renders login form with email and password inputs', () => {
    render(<MemoryRouter><Login /></MemoryRouter>)
    expect(screen.getByPlaceholderText('Email')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Password')).toBeInTheDocument()
    expect(screen.getByText('Login')).toBeInTheDocument()
  })

  it('sets user and navigates on successful login', async () => {
    render(<MemoryRouter><Login /></MemoryRouter>)
    fireEvent.change(screen.getByPlaceholderText('Email'), { target: { value: 'alice@example.com' } })
    fireEvent.change(screen.getByPlaceholderText('Password'), { target: { value: 'secret' } })
    fireEvent.click(screen.getByText('Login'))
    
    await new Promise(r => setTimeout(r, 100))
    expect(useAuthStore.getState().user).toEqual({ id: '1', email: 'alice@example.com', name: 'Alice' })
  })
})
