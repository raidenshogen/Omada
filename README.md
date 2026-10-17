# Omada — Real-Time Collaboration SaaS

Real-time Kanban boards with Trello/Notion-like collaboration: workspaces, boards with
Todo/Doing/Done columns, drag & drop, live sync between open boards, and an activity feed.

**What works today** (all verified running under Docker Compose):
- Register / login with JWT (access + refresh), BCrypt-hashed passwords
- Organizations → workspaces → boards → lists (Todo/Doing/Done seeded) → cards
- Create, edit, delete and **drag & drop cards between columns** (persisted, optimistic UI)
- **Realtime sync**: every card change is broadcast over WebSocket (STOMP) to everyone
  viewing the same board — a second browser sees drags/edits instantly
- Activity feed materialized from Kafka events (CARD_CREATED / UPDATED / DELETED)
- Trello/Notion-style sidebar with your workspaces and boards (hidden for logged-out users)

## Tech stack
Backend: Spring Boot 3.3.2, Java 17, Spring Security JWT, Spring Data MongoDB, WebSocket STOMP, Kafka, MapStruct, Lombok
Frontend: React 18, Vite 5, Zustand, @stomp/stompjs, TailwindCSS
Data: MongoDB 7, Redis 7
Infra: Docker Compose

## Architecture overview
React SPA → Spring Boot monolith REST + WebSocket STOMP → MongoDB + Redis
Services: Auth, Organization, Workspace, Board, List, Card, Comment, Activity, Realtime

## Prerequisites
- Docker
- Java 17
- Node 20+

## How to run locally
```bash
# full stack (mongo, redis, kafka, backend)
docker compose -p omada up -d --build
# frontend dev server
cd frontend
npm install
npm run dev
```
Frontend runs at http://localhost:5173, backend at http://localhost:8080/api

Alternative backend-only workflow: `cd backend/infrastructure/docker && docker compose up -d`,
then `mvn spring-boot:run` from `backend/`.

## Demo accounts
No seed data yet. Register via POST /api/auth/register, or create a user through the UI.

## API summary
Auth: POST /api/auth/register, POST /api/auth/login, POST /api/auth/refresh, GET /api/auth/me
Organizations: POST /api/organizations, GET /api/organizations
Workspaces: POST /api/workspaces, GET /api/workspaces?orgId=
Boards: POST /api/workspaces/{wsId}/boards, GET /api/workspaces/{wsId}/boards
Lists: POST /api/boards/{boardId}/lists, GET /api/boards/{boardId}/lists
Cards: POST /api/boards/{boardId}/lists/{listId}/cards,
       PATCH /api/boards/{boardId}/lists/{listId}/cards/{cardId}  (also moves a card: send listId+position),
       DELETE /api/boards/{boardId}/lists/{listId}/cards/{cardId}
Activity: GET /api/activities?boardId= or ?workspaceId=

## Realtime protocol
Connect: STOMP over SockJS to `/ws` with header `Authorization: Bearer <JWT>`
(the browser client uses `sockjs-client`; the Vite dev proxy forwards `/ws`).
Subscribe: `/topic/board.{boardId}`.
Every card mutation broadcasts a JSON payload:
```json
{ "type": "CARD_CREATED|CARD_UPDATED|CARD_DELETED",
  "boardId": "...", "listId": "current list",
  "previousListId": "set only when the card moved", "card": { ...card state } }
```
Clients apply the event to their board state (see `boardStore.applyRemoteEvent`).
Flow: CardService → Spring ApplicationEvent (Observer) → BoardSocketPublisher → STOMP topic.

## How to run tests
Backend: `cd backend && mvn test` (Testcontainers: Mongo, Redis, Kafka — needs Docker)
Frontend: `cd frontend && npm test` (Vitest + React Testing Library)
Realtime smoke check (backend stack must be running):
`node scripts/verify-realtime.mjs <boardId> <listId> <jwt>` — connects a STOMP client,
creates a card via REST, asserts the broadcast arrives (exit 0).

## What I would improve next
- Full document editor with blocks
- Reordering cards *within* a column (drag currently appends to the target column)
- Realtime presence (who is viewing the board) — Redis is wired but unused
- Harden WorkspaceSecurity: permission checks are permissive stubs (return true)
- Offline sync
- Production deployment with Nginx + Let's Encrypt
- SonarQube quality gate
