/**
 * Realtime verification: connects a STOMP client to the deployed backend,
 * subscribes to a board topic, creates a card via REST, and asserts the
 * change arrives over the WebSocket. Exit 0 = realtime works.
 *
 * Run: node verify-realtime.mjs <boardId> <listId> <jwt>
 */
import { Client } from '@stomp/stompjs'


const [boardId, listId, token] = process.argv.slice(2)
if (!boardId || !listId || !token) {
  console.error('usage: node verify-realtime.mjs <boardId> <listId> <jwt>')
  process.exit(2)
}

const events = []
const client = new Client({
  // SockJS servers expose a raw WebSocket endpoint at <endpoint>/websocket
  brokerURL: 'ws://localhost:8080/ws/websocket',
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 2000,
  connectionTimeout: 15000,
  onWebSocketError: (e) => console.error('WS error:', e?.message || e),
})

const timeout = setTimeout(() => {
  console.error('TIMEOUT: no broadcast received within 25s')
  process.exit(1)
}, 25000)

client.onConnect = () => {
  console.log('WS connected, subscribing to /topic/board.' + boardId)
  client.subscribe(`/topic/board.${boardId}`, (msg) => {
    const body = JSON.parse(msg.body)
    events.push(body)
    console.log('RECEIVED:', JSON.stringify(body).slice(0, 220))
    if (body.type === 'CARD_CREATED') {
      clearTimeout(timeout)
      client.deactivate()
      console.log('SUCCESS: realtime broadcast verified')
      process.exit(0)
    }
  })
  // trigger a card creation via REST once subscribed
  setTimeout(() => {
    fetch('http://localhost:8080/api/boards/' + boardId + '/lists/' + listId + '/cards', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
      body: JSON.stringify({ title: 'Realtime probe ' + Date.now(), position: 0 }),
    })
      .then((r) => console.log('REST create status:', r.status))
      .catch((e) => {
        console.error('REST create failed:', e.message)
        process.exit(1)
      })
  }, 500)
}

client.activate()
