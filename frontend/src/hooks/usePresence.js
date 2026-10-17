import { useEffect, useRef, useState } from 'react'
import { connect, subscribePresence, sendPresence } from '../services/websocket'
import { useAuthStore } from '../store/authStore'

/**
 * Board presence: announces this user (JOIN), keeps the Redis entry fresh with
 * a 25s heartbeat (server TTL is 45s), listens to the presence topic and
 * announces LEAVE on unmount. Tab-close/network-drop are covered server-side
 * by the disconnect handler + TTL expiry.
 *
 * Returns the current online users as [{userId, name}].
 */
export function usePresence(boardId) {
  const token = useAuthStore((s) => s.accessToken)
  const user = useAuthStore((s) => s.user)
  const [users, setUsers] = useState([])
  const nameRef = useRef(user?.name || 'Anonymous')
  nameRef.current = user?.name || 'Anonymous'

  useEffect(() => {
    if (!token || !boardId) return undefined
    let subscription = null
    let heartbeat = null
    connect(token, {
      onConnect: () => {
        sendPresence(boardId, { action: 'JOIN', name: nameRef.current })
        heartbeat = setInterval(
          () => sendPresence(boardId, { action: 'HEARTBEAT', name: nameRef.current }),
          25000
        )
        subscription = subscribePresence(boardId, (list) =>
          setUsers(Array.isArray(list) ? list : [])
        )
      },
    })
    return () => {
      if (heartbeat) clearInterval(heartbeat)
      sendPresence(boardId, { action: 'LEAVE', name: nameRef.current })
      if (subscription) subscription.unsubscribe()
      setUsers([])
    }
  }, [boardId, token])

  return users
}
