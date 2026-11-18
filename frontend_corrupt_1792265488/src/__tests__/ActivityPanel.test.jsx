import { act } from '@testing-library/react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import ActivityPanel from '../components/ActivityPanel'

let activityHandler

vi.mock('../hooks/useWebSocket', () => ({
  useActivitySocket: (_workspaceId, handler) => {
    activityHandler = handler
  },
}))

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn((url) => {
      if (url.includes('page=1')) {
        return Promise.resolve({
          data: {
            content: [{ id: 'a9', type: 'COMMENT_ADDED', createdAt: new Date().toISOString() }],
            page: 1,
            size: 8,
            totalElements: 9,
            totalPages: 2,
          },
        })
      }
      return Promise.resolve({
        data: {
          content: [{ id: 'a1', type: 'CARD_CREATED', createdAt: new Date().toISOString() }],
          page: 0,
          size: 8,
          totalElements: 9,
          totalPages: 2,
        },
      })
    }),
  },
}))

describe('ActivityPanel', () => {
  beforeEach(() => {
    activityHandler = undefined
  })

  it('loads the first page from the paginated endpoint', async () => {
    render(<ActivityPanel workspaceId="ws-1" />)
    expect(await screen.findByText('created a card')).toBeInTheDocument()
  })

  it('loads the next page when "Load more" is clicked', async () => {
    render(<ActivityPanel workspaceId="ws-1" />)
    await screen.findByText('created a card')

    fireEvent.click(screen.getByText('Load more'))

    expect(await screen.findByText('added a comment')).toBeInTheDocument()
    expect(screen.getByText('created a card')).toBeInTheDocument()
  })

  it('prepends events pushed over the realtime topic', async () => {
    render(<ActivityPanel workspaceId="ws-1" />)
    await screen.findByText('created a card')

    act(() => {
      activityHandler({ id: 'live-1', type: 'CARD_UPDATED', createdAt: new Date().toISOString() })
    })

    expect(await screen.findByText('updated a card')).toBeInTheDocument()
  })

  it('does not duplicate a realtime event already in the list', async () => {
    render(<ActivityPanel workspaceId="ws-1" />)
    await screen.findByText('created a card')

    act(() => {
      activityHandler({ id: 'a1', type: 'CARD_CREATED', createdAt: new Date().toISOString() })
    })

    await waitFor(() => expect(screen.getAllByText('created a card')).toHaveLength(1))
  })
})
