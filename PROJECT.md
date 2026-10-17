# Omada — Real-Time Collaboration SaaS

## Purpose
Productivity tool with live collaboration like Trello / Notion. Live editing of boards & documents, team workspaces, role-based access, activity feed. Portfolio demo, zero production infra cost.

## Stack
Backend: Spring Boot 3.3.2, Java 17, Spring Web / WebSocket STOMP, Spring Security JWT, Spring Data MongoDB, Spring Kafka, MapStruct, Lombok, JJWT
Frontend: React 18, Vite 5, React Router 6, Zustand, @stomp/stompjs, SockJS, Framer Motion, TailwindCSS, Axios
Data: MongoDB 7, Redis 7
Infra: Docker Compose, Nginx, GitHub Actions CI

## Repo layout
Omada/
  ARCHITECTURE.md
  MVP_DESIGN.md
  ROADMAP.md
  PROJECT.md
  backend/
    pom.xml
    src/main/java/com/omada/realtime/...
    src/main/resources/application.yml
    infrastructure/docker/docker-compose.yml
  frontend/
    package.json
    src/

## Key decisions
- Monolith Spring Boot for MVP with service-layer separation for Auth, Workspace, Board, Card, Comment, Activity. Services map to MVP_DESIGN.md bounded contexts.
- MongoDB for flexible board/document schema, version field on cards for optimistic concurrency. Reference fields (workspaceId, boardId, listId) are stored and queried as Strings — UUID-typed queries against String fields silently match nothing (BSON type mismatch); this bug class is covered by integration tests.
- JWT access 15m + refresh 7d, stateless. JwtAuthenticationFilter installs the user id as principal; RBAC rules via @PreAuthorize exist but WorkspaceSecurity checks are permissive stubs for now.
- Realtime: card mutations publish a Spring ApplicationEvent (Observer) → BoardSocketPublisher broadcasts to STOMP topic /topic/board.{boardId}; frontend boardStore.applyRemoteEvent applies remote events (idempotent, handles own echo). SockJS transport on /ws, JWT required at CONNECT.
- Kafka for activity event bus (topic `activities`, consumer group omada-activity-feed) → ActivityConsumer materializes the feed into MongoDB. CLUSTER_ID is set in both compose files (KRaft).
- Frontend React + Vite, Zustand stores, STOMP client via sockjs-client (the backend endpoint is SockJS-only, so a plain ws:// brokerURL does not work).
- Local-first Docker Compose: kafka (KRaft), mongo, redis, app at repo root. No production deployment.

## Data model highlights
users, organizations, workspaces, boards, lists (TaskList), cards, comments, activities.
IDs are UUIDs generated in the service layer via IdGenerator.uuid(); reference fields are Strings.
Collections in MongoDB, indexes per MVP_DESIGN.md.

## API
Base /api
Auth: POST /auth/register, POST /auth/login, POST /auth/refresh, GET /auth/me
Organizations, Workspaces, Boards, Lists, Cards (PATCH accepts listId+position for drag & drop), Comments, Activity Feed
WebSocket: STOMP over SockJS /ws (Authorization: Bearer header at CONNECT), subscribe /topic/board.{boardId};
payloads {type: CARD_CREATED|CARD_UPDATED|CARD_DELETED, boardId, listId, previousListId?, card}

## Non-functional
Security: JWT Bearer, BCrypt, CORS locked, RBAC per service, no secrets in repo.
Performance: Stateless services, horizontal scale, Mongo indexes, Redis presence.
Quality: JUnit 5, Testcontainers, Vitest + RTL, SonarQube quality gate, dependency check.

## Constraints
- No production infra cost. Local dev only, Docker Compose.
- MVP scope: users, workspaces, boards with lists/cards, comments, activity feed. No full document editor yet.
- CI build+test only, Ansible playbooks documented but not executed.

## Current state
Working end-to-end (verified on the Docker stack): register/login (real JWT+BCrypt), orgs,
workspaces, boards with seeded Todo/Doing/Done lists, card create/edit/delete, drag & drop
between columns (persisted + optimistic), realtime WebSocket sync of card changes, activity
feed via Kafka consumer. Frontend pages (Dashboard/Workspace/Board/Sidebar) are fully wired
to the API; sidebar is workspace/board navigation, hidden for guests.
Tests: backend 11 integration tests green (Testcontainers Mongo/Redis/Kafka incl. Kafka flow),
frontend 14 Vitest tests green (incl. realtime store logic). scripts/verify-realtime.mjs
proves the broadcast live. Global exception handler returns {message} JSON errors.
Not done: presence (Redis unused), in-column reordering, real RBAC checks, comments UI,
document editor, production deployment.

## Ownership
Project owner: Zena/Zeo. Lead engineer Miles per SOUL.md lifecycle.
