import { useEffect, useRef, useState } from 'react'
import {
  connect,
  subscribeBoard,
  subscribeActivity,
  subscribeDoc,
  subscribeDocPresence,
  sendDocPresence,
  disconnect,
} from '../services/websocket'
import { useAuthStore } from '../store/authStore'

/**
 * Realtime hook: connects the shared STOMP client once per signed-in
 * session and subscribes to the given board's topic. `onCardEvent`
 * receives { type, boardId, listId, previousListId, card } payloads.
 * Re-subscribes when the board changes; closes the socket on logout.
 */
export function useBoardSocket(boardId, onCardEvent) {
  const token = useAuthStore((s) => s.accessToken)
  const handlerRef = useRef(onCardEvent)
  handlerRef.current = onCardEvent

  useEffect(() => {
    if (!token || !boardId) return undefined
    let subscription = null
    connect(token, {
      onConnect: () => {
        subscription = subscribeBoard(boardId, (event) => handlerRef.current(event))
      },
    })
    return () => {
      if (subscription) subscription.unsubscribe()
    }
  }, [boardId, token])

  // Global cleanup when the user signs out (token becomes null)
  useEffect(() => {
    if (!token) disconnect()
  }, [token])
}

/**
 * Realtime activity hook: subscribes to a workspace's live activity feed
 * (/topic/activity.{workspaceId}) so panels can prepend new items as they
 * happen. Re-subscribes when the workspace changes.
 */
export function useActivitySocket(workspaceId, onActivity) {
  const token = useAuthStore((s) => s.accessToken)
  const handlerRef = useRef(onActivity)
  handlerRef.current = onActivity

  useEffect(() => {
    if (!token || !workspaceId) return undefined
    let subscription = null
    connect(token, {
      onConnect: () => {
        subscription = subscribeActivity(workspaceId, (event) => handlerRef.current(event))
      },
    })
    return () => {
      if (subscription) subscription.unsubscribe()
    }
  }, [workspaceId, token])
}

/**
 * Document collaboration hook: on one connection it subscribes to the
 * document's block deltas (/topic/doc.{id}) and its presence topic, announces
 * this editor (JOIN + 25s heartbeat, LEAVE on unmount) and returns the live
 * list of editors as [{userId, name}]. Server handles tab-close via the
 * disconnect handler + Redis TTL.
 */
export function useDocumentChannel(documentId, onBlock) {
  const token = useAuthStore((s) => s.accessToken)
  const user = useAuthStore((s) => s.user)
  const [users, setUsers] = useState([])

  const blockRef = useRef(onBlock)
  blockRef.current = onBlock
  const nameRef = useRef(user?.name || 'Anonymous')
  nameRef.current = user?.name || 'Anonymous'

  useEffect(() => {
    if (!token || !documentId) return undefined
    let blockSub = null
    let presenceSub = null
    let heartbeat = null
    connect(token, {
      onConnect: () => {
        blockSub = subscribeDoc(documentId, (event) => blockRef.current && blockRef.current(event))
        sendDocPresence(documentId, { action: 'JOIN', name: nameRef.current })
        heartbeat = setInterval(
          () => sendDocPresence(documentId, { action: 'HEARTBEAT', name: nameRef.current }),
          25000
        )
        presenceSub = subscribeDocPresence(documentId, (list) =>
          setUsers(Array.isArray(list) ? list : [])
        )
      },
    })
    return () => {
      if (heartbeat) clearInterval(heartbeat)
      sendDocPresence(documentId, { action: 'LEAVE', name: nameRef.current })
      if (blockSub) blockSub.unsubscribe()
      if (presenceSub) presenceSub.unsubscribe()
      setUsers([])
    }
  }, [documentId, token])

  return users
}
