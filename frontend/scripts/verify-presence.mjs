/**
 * Presence verification: connects a STOMP client, sends a JOIN heartbeat for a
 * board, and asserts the presence broadcast (from /topic/presence.{boardId})
 * contains this user. Exit 0 = presence works.
 *
 * Run from frontend/ (resolves @stomp/stompjs from its node_modules):
 * node ../scripts/verify-presence.mjs <boardId> <name> <jwt>
 */
import { Client } from '@stomp/stompjs'

const [boardId, name, token] = process.argv.slice(2)
if (!boardId || !name || !token) {
  console.error('usage: node verify-presence.mjs <boardId> <name> <jwt>')
  process.exit(2)
}

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/websocket',
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 2000,
  connectionTimeout: 15000,
  onWebSocketError: (e) => console.error('WS error:', e?.message || e),
})

const timeout = setTimeout(() => {
  console.error('TIMEOUT: no presence broadcast within 20s')
  process.exit(1)
}, 20000)

client.onConnect = () => {
  console.log('WS connected, subscribing to /topic/presence.' + boardId)
  client.subscribe(`/topic/presence.${boardId}`, (msg) => {
    const users = JSON.parse(msg.body)
    console.log('RECEIVED presence:', JSON.stringify(users))
    if (users.some((u) => u.name === name)) {
      clearTimeout(timeout)
      client.deactivate()
      console.log('SUCCESS: presence broadcast verified')
      process.exit(0)
    }
  })
  // announce ourselves a few times (JOIN then heartbeats)
  let n = 0
  const iv = setInterval(() => {
    client.publish({
      // matches @MessageMapping("/board/{boardId}/presence") + /app prefix
      destination: `/app/board/${boardId}/presence`,
      body: JSON.stringify({ action: n === 0 ? 'JOIN' : 'HEARTBEAT', name }),
    })
    if (++n >= 3) clearInterval(iv)
  }, 500)
}

client.activate()
