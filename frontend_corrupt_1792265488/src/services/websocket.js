import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * STOMP-over-SockJS connection to the backend /ws endpoint.
 * The backend registers the endpoint with SockJS transport, so the plain
 * brokerURL WebSocket does not work — we use the SockJS factory instead,
 * which also flows through the Vite dev proxy (see vite.config.js '/ws').
 */
let client = null

/** Connect (or return the existing active client) with a JWT for auth. */
export function connect(token, { onConnect } = {}) {
  if (client && client.active) return client
  client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      if (onConnect) onConnect()
    },
    onWebSocketError: (err) => console.error('WS error', err),
  })
  client.activate()
  return client
}

/** Subscribe to one board's realtime topic. Returns the subscription or null. */
export function subscribeBoard(boardId, handler) {
  if (!client || !client.active) return null
  return client.subscribe(`/topic/board.${boardId}`, (msg) => {
    try {
      handler(JSON.parse(msg.body))
    } catch (e) {
      console.error('Malformed realtime payload', e)
    }
  })
}

/** Subscribe to a board's presence topic (list of online users). */
export function subscribePresence(boardId, handler) {
  if (!client || !client.active) return null
  return client.subscribe(`/topic/presence.${boardId}`, (msg) => {
    try {
      handler(JSON.parse(msg.body))
    } catch (e) {
      console.error('Malformed presence payload', e)
    }
  })
}

/** Subscribe to a document's realtime topic (block deltas). Returns the subscription or null. */
export function subscribeDoc(documentId, handler) {
  if (!client || !client.active) return null
  return client.subscribe(`/topic/doc.${documentId}`, (msg) => {
    try {
      handler(JSON.parse(msg.body))
    } catch (e) {
      console.error('Malformed document payload', e)
    }
  })
}

/** Subscribe to a document's presence topic (list of live editors). */
export function subscribeDocPresence(documentId, handler) {
  if (!client || !client.active) return null
  return client.subscribe(`/topic/docpresence.${documentId}`, (msg) => {
    try {
      handler(JSON.parse(msg.body))
    } catch (e) {
      console.error('Malformed document presence payload', e)
    }
  })
}

/** Publish a document presence action (JOIN / HEARTBEAT / LEAVE). No-op when offline. */
export function sendDocPresence(documentId, payload) {
  if (!client || !client.active) return
  client.publish({
    // matches @MessageMapping("/doc/{docId}/presence") + /app prefix
    destination: `/app/doc/${documentId}/presence`,
    body: JSON.stringify(payload),
  })
}

/** Subscribe to a workspace's live activity feed. Returns the subscription or null. */
export function subscribeActivity(workspaceId, handler) {
  if (!client || !client.active) return null
  return client.subscribe(`/topic/activity.${workspaceId}`, (msg) => {
    try {
      handler(JSON.parse(msg.body))
    } catch (e) {
      console.error('Malformed activity payload', e)
    }
  })
}

/** Publish a presence action (JOIN / HEARTBEAT / LEAVE) for a board. No-op when offline. */
export function sendPresence(boardId, payload) {
  if (!client || !client.active) return
  client.publish({
    // matches @MessageMapping("/board/{boardId}/presence") + /app prefix
    destination: `/app/board/${boardId}/presence`,
    body: JSON.stringify(payload),
  })
}

/** Close the WebSocket session (used on logout). */
export function disconnect() {
  if (client) {
    client.deactivate()
    client = null
  }
}
