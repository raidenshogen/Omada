# Test Suite Summary

Last full run: backend 11/11 green, frontend 14/14 green (all verified, not aspirational).

## Backend (JUnit 5 + Testcontainers)
- `AbstractIntegrationTest`: real MongoDB (mongo:7), Redis (redis:7-alpine) and Kafka
  (confluentinc/cp-kafka:7.5.0, KRaft) containers, wired via dynamic properties.
  Requires Docker running; Docker Desktop needs `~/.testcontainers.properties` with
  `docker.host=npipe:////./pipe/dockerDesktopLinuxEngine`.
- UserServiceIntegrationTest (4): register persists + returns real JWT, password hashed,
  login ok, wrong password throws, duplicate email throws
- BoardServiceIntegrationTest (2): create persists, findByWorkspace (String query —
  regression test for the BSON type-mismatch bug)
- CardServiceIntegrationTest (3): create, update (version bump), findByBoard
- KafkaActivityFlowIntegrationTest (2): publish → consumer materializes into Mongo feed;
  duplicate delivery is deduplicated (idempotent consumer)

Run: `cd backend && mvn test`

## Frontend (Vitest + React Testing Library)
- `vitest.config.js` (renamed from vite.config.test.js — vitest never loaded the old name)
  with jsdom environment + localStorage mock setup file
- authStore.test.js (3): initial state, setUser, logout
- Login.test.jsx (2): renders form, sets user on success
- Board.test.jsx (4): loads lists/cards from mocked API, adds a card through the API,
  error banner on failure, add-card button
- boardStore.test.js (5): realtime applyRemoteEvent (create / move / delete / idempotency)
  and optimistic moveCard

Run: `cd frontend && npm test`

## Live smoke checks (against the Docker stack)
- Full user flow: register → login → org → workspace → board → lists → card
  create/update/delete → activity feed (Kafka consumer) — verified via curl
- Realtime broadcast: `node scripts/verify-realtime.mjs <boardId> <listId> <jwt>`
  connects a STOMP client, creates a card via REST, asserts the CARD_CREATED
  broadcast arrives over the WebSocket (exit 0)

## Coverage
JaCoCo plugin is configured (report generated in target/site/jacoco) but no coverage
gate is enforced yet.
