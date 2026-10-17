# Implementation Roadmap — Real-Time Collaboration SaaS
Spring Boot · WebSocket · React · MongoDB · Kafka · Docker · JWT · GitHub Actions + Ansible practice

Goal: Working portfolio demo with live boards + documents, multi-workspace, RBAC, activity feed.
Cost: Zero production infra. Local-first Docker Compose + free tiers.

## Phase 0 — Foundations & Repo Setup [Day 1-2]
Objectives: Project scaffolding, version control, CI skeleton.

- Create GitHub repo with monorepo structure
- Initialize services:
  backend/
    auth-service
    gateway
    workspace-service
    board-service
    document-service
    realtime-service
    activity-service
  frontend/
    web
- Docker Compose base: mongodb, redis, redpanda/kafka, zookeeper
- GitHub Actions workflow: build Java, npm ci, run unit tests, lint
- Ansible skeleton: inventory/hosts, roles/docker, roles/nginx, playbooks for local dry-run
- mkcert for local HTTPS
Deliverable: `docker compose up` brings up infra; CI green on push

## Phase 1 — Auth & Multi-Tenancy Core [Day 3-5]
Objectives: Secure JWT, org/workspace model, RBAC

- Auth Service:
  Spring Boot 3 + Spring Security
  JWT access 15m + refresh 7d
  Endpoints: /auth/register, /login, /refresh, /me
  Mongo collections: users, organizations, memberships
- Gateway:
  Spring Cloud Gateway, JWT filter, tenant header injection
  Routes to services, CORS for localhost
- RBAC annotations @PreAuthorize
- Seed data script for demo org/workspace
Deliverable: Login flow works, protected API via gateway

## Phase 2 — Workspace & Board CRUD [Day 6-9]
Objectives: Data model, REST APIs for boards/kanban

- Workspace Service:
  CRUD workspaces, members, invitations
- Board Service:
  Mongo collections: boards, lists, cards, comments
  REST: boards, lists CRUD, cards move/reorder
  Optimistic version field
- React shell:
  Vite + React 18 + Zustand
  Auth context, protected routes
  Sidebar workspaces list
Deliverable: Create workspace → create board → Kanban lists & cards CRUD

## Phase 3 — Real-Time Collaboration [Day 10-14]
Objectives: Live updates, presence

- Realtime Service:
  Spring WebSocket STOMP over SockJS
  Topics: /topic/workspace.{id}, /topic/board.{id}
  Redis for presence SETEX
- Board Service emits domain events → Kafka topic board.events
- Realtime Service consumes Kafka + broadcasts to room
- React WebSocket client with @stomp/stompjs
  Optimistic UI, reconnect logic
Deliverable: Two browser tabs moving same card updates instantly

## Phase 4 — Document Editor & Operational Model [Day 15-18]
Objectives: Live document with blocks

- Document Service:
  Collections: documents, blocks
  Block types: paragraph, heading, todo, list
  REST CRUD for blocks
- Realtime Service:
  /topic/doc.{id} for block ops
  Simple OT: send delta {blockId, op, version}
- React editor:
  Minimal block editor with contenteditable per block
  Live cursors via presence
Deliverable: Collaborative document with live typing

## Phase 5 — Activity Feed & Events [Day 19-21]
Objectives: Event-driven feed

- Kafka topics: board.events, document.events, activity.feed
- Activity Service consumes events, materializes feed per workspace/board
- Mongo collection activities with index {workspaceId, createdAt}
- React Activity Panel component
Deliverable: Feed shows CardMoved, CommentAdded, DocumentEdited in real time

## Phase 6 — UI Polish & UX [Day 22-24]
Objectives: Production-like UX

- React components: Kanban board drag-drop @dnd-kit
- Workspaces switcher, role badges
- Empty states, skeletons, error boundaries
- Toast notifications for realtime events
- Responsive layout
Deliverable: Demo-ready UI

## Phase 7 — Testing & Quality [Day 25-27]
Objectives: Tests + CI

- Backend: JUnit 5 + Testcontainers for Mongo/Redis/Kafka
  Integration tests for Auth, Board, Realtime
- Frontend: Vitest + RTL for critical flows
- GitHub Actions: matrix Java 17/21, Node 20
  Steps: lint → test → build docker images → Ansible syntax check
- Add health endpoints for all services
Deliverable: CI pipeline green, >70% coverage core services

## Phase 8 — Documentation & Demo Prep [Day 28-30]
Objectives: Portfolio ready

- ARCHITECTURE.md refined
- README with local setup: `docker compose up`, `npm run dev`, `mnb` profile
- Postman collection for APIs
- Demo script: create org → invite member → live board edit → live doc edit → activity feed
- Record 2-min walkthrough GIF
Deliverable: Repo ready to showcase

## Milestones Summary

M0 Repo + infra up
M1 Auth + RBAC working
M2 Boards CRUD working
M3 Real-time board updates
M4 Collaborative document
M5 Activity feed live
M6 Polished UI
M7 Tests + CI green
M8 Demo ready

## Risks & Mitigations

WebSocket scaling → sticky sessions via Nginx ip_hash for demo
Kafka complexity → use Redpanda single-node, one partition per workspace
Mongo schema drift → version field + migration scripts
CORS/auth in dev → gateway handles origin allowlist

## Next Immediate Action
Scaffold monorepo folders + docker-compose.yml with mongo/redis/redpanda + Spring Boot auth-service skeleton.
