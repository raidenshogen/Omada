# Real-Time Collaboration SaaS — High Level Architecture
Productivity tool with live collaboration like Trello / Notion

## Vision
Live editing of boards & documents, team workspaces, role-based access, activity feed.
Senior signal: WebSocket + Kafka + SaaS multi-tenancy.

## Tech Stack
Spring Boot 3 · Spring WebFlux / WebSocket STOMP · React 18 · MongoDB · Apache Kafka · Redis · JWT Auth · Docker · Nginx · DigitalOcean

## System Overview

Client React SPA
  ↓ HTTPS/WSS
Nginx Reverse Proxy + SSL
  ↓
API Gateway Spring Boot
  ├─ Auth Service
  ├─ Workspace Service
  ├─ Board / Document Service
  ├─ Realtime Service
  └─ Activity Service
  ↓
MongoDB  ← persistent data
Redis     ← presence, sessions, locks
Kafka     ← event bus for eventual consistency

## Core Services

### 1. API Gateway
Spring Cloud Gateway + Nginx SSL termination.
Routes /auth, /api/workspaces, /api/boards, /ws.
Rate limiting, JWT validation filter, tenant header injection.

### 2. Auth Service
JWT access + refresh tokens.
Users, Organizations, Memberships.
Roles: OWNER, ADMIN, MEMBER, VIEWER.
Password hashing BCrypt, OAuth2-ready.
Stores in MongoDB.

### 3. Workspace Service
Teams / Workspaces CRUD.
Members, roles, invitations.
RBAC enforcement via Spring Security @PreAuthorize.

### 4. Board / Document Service
MongoDB collections:
users, organizations, workspaces, boards, lists, cards, comments, documents, document_blocks
Schema designed for hierarchical boards + CRDT-friendly document blocks.
Optimistic concurrency + version numbers.

### 5. Realtime Service
Spring Boot WebSocket STOMP over SockJS.
Topics: /topic/workspace.{id}, /topic/board.{id}, /topic/doc.{id}
Presence channel: heartbeat + Redis SETEX for online users.
Operational transform simplified: send delta ops, broadcast to room.
Lock hints for simultaneous edits.

### 6. Event Bus Kafka
Topics:
board.events
document.events
activity.feed
presence.events
All state-changing services emit events → Activity Service consumes → builds feed.
Decouples realtime from persistence.

### 7. Activity Feed Service
Materialized view of Kafka events.
Powered MongoDB with time-ordered feed per workspace/board.

### 8. Front End React
React + Vite + Zustand/Redux Toolkit.
WebSocket client via @stomp/stompjs.
Offline queue with optimistic UI.
Components: Board view Kanban, Document editor, Sidebar workspaces, Activity panel.

## Real-Time Flow Example

1. User A moves card in Board X
2. React sends POST delta via WebSocket to Realtime Service
3. Service validates JWT + RBAC
4. Persists to MongoDB Board Service
5. Emit CardMoved event to Kafka
6. Realtime Service broadcasts to /topic/board.X subscribers
7. Clients update UI instantly, Activity Service consumes event → adds to feed
8. Presence updated in Redis

## Data Model Highlights

organizations { _id, name, slug }
workspaces { _id, orgId, name, members[] }
boards { _id, workspaceId, title, lists[] }
lists { _id, boardId, position }
cards { _id, listId, title, description, assignees[], due, position, version }
documents { _id, workspaceId, title, rootBlockId }
blocks { _id, docId, type, content, children[], version }

MongoDB indexes: workspaceId+createdAt, boardId+position

## Security
JWT Bearer in Authorization header.
WebSocket connect with token in query param → validated at connect.
RBAC per service layer.
CORS locked to frontend origin.
Nginx SSL with Let's Encrypt.

## Scalability
Stateless Spring Boot services → horizontal autoscale.
WebSocket sticky sessions via Nginx ip_hash or Redis session store.
Kafka partitions by workspaceId for ordering.
MongoDB sharding by orgId.
Redis Cluster for presence.

## Deployment
Local-first development with Docker Compose.
MongoDB Atlas free tier / local MongoDB.
Kafka local via Docker or Redpanda single-node.
Redis local.
Nginx local SSL via mkcert for testing.

### No-Production-Cost Setup
No droplet deployment.
Run everything locally / on free tiers for portfolio demo.
CI/CD with GitHub Actions for build+test only; Ansible playbooks documented for infra-as-code practice, not executed against cloud.

## Why this hits senior level
WebSocket for true live UX, Kafka for event-driven consistency, multi-tenant SaaS with RBAC, Mongo for flexible board/document schema, Docker + Nginx + SSL production ready.

## Next Steps
1. Scaffold services with Spring Boot starters + docker-compose
2. Set up OAuth JWT + Gateway
3. Prototype WebSocket echo
4. Board CRUD + realtime broadcast
5. Kafka events + Activity feed
