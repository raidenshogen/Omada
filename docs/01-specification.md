# 01-specification

## Problem and goals
Enable teams to collaborate on Kanban boards like Trello/Notion with real-time editing, presence, and instant feedback. Workspace isolation, role-based access and activity feed. Portfolio demo with zero production infra cost.

Goals:
- Secure JWT auth with refresh tokens
- Multi-tenant by organization → workspace
- CRUD boards/lists/cards/comments with optimistic concurrency
- Live updates via WebSocket STOMP with presence and user cursors
- Optimistic UI with immediate local feedback and conflict resolution
- Activity feed materialized from domain events

## Users and roles
- OWNER: manage organization, members, billing
- ADMIN: manage workspaces, members
- MEMBER: create boards/cards/comments
- VIEWER: read-only

## Scope
In scope:
- Auth register/login/refresh/me
- Organizations, memberships CRUD
- Workspaces CRUD
- Boards, lists, cards CRUD + move/reorder with drag-drop
- Comments on cards with live updates
- Activity feed per workspace/board
- WebSocket live board updates + presence + user cursors
- JWT RBAC enforcement
- Optimistic UI with immediate local feedback and conflict resolution via version field

Out of scope for MVP:
- Full document editor with blocks
- Offline sync
- OAuth providers
- Email invitations
- Production deployment

## Functional requirements
As a user I want to register and login so that I can access my workspaces.
Acceptance: POST /auth/register creates user with BCrypt hash; POST /auth/login returns access + refresh tokens; GET /auth/me returns profile.

As an OWNER I want to create an organization so that teams can be isolated.
Acceptance: POST /organizations creates org with unique slug.

As an ADMIN I want to invite members with roles so that access is controlled.
Acceptance: POST /organizations/{orgId}/members assigns role; RBAC blocks unauthorized ops.

As a MEMBER I want to create a workspace so that projects are grouped.
Acceptance: POST /workspaces creates workspace under org; GET /workspaces?orgId lists.

As a MEMBER I want to create boards with lists and cards so that work is tracked.
Acceptance: POST /workspaces/{workspaceId}/boards; POST /boards/{boardId}/lists; POST /boards/{boardId}/lists/{listId}/cards.

As a MEMBER I want to move cards between lists so that workflow progresses.
Acceptance: PATCH /cards/{id} with listId/position/version; optimistic concurrency rejects stale version.

As a user I want live updates so that I see changes instantly.
Acceptance: WebSocket connect with JWT; subscribe /topic/board.{id}; move card broadcasts delta to subscribers; presence shows online users; cursors optional for MVP.

As a user I want optimistic UI so edits feel instant.
Acceptance: Client updates UI immediately on drag/drop; server validates version; on conflict client receives error and reconciles.

As a user I want an activity feed so that I see what happened.
Acceptance: GET /workspaces/{workspaceId}/activities returns time-ordered events; events emitted on card create/move/comment.

## Non-functional requirements
Security: JWT Bearer, BCrypt password hashing, RBAC @PreAuthorize per service, CORS locked to frontend origin, no secrets in repo.
Performance: Stateless Spring Boot services, Mongo indexes on workspaceId, boardId, position; Redis for presence; client-side virtualization for large boards.
Availability: Local Docker Compose for demo; health endpoints exposed.
Maintainability: Clean architecture controller→service→repository, DTOs, MapStruct mappers, Javadoc, unit + integration tests.
Data: MongoDB flexible schema, atomic per document, version field for optimistic locking.
UX: SPA with instant navigation, optimistic UI updates, drag-drop feedback, presence indicators, Trello/Notion-like feel.

## Assumptions
- Single monolith Spring Boot for MVP; service classes separate bounded contexts.
- MongoDB runs locally via Docker Compose.
- WebSocket sticky sessions not required for demo.
- Kafka/Redpanda not yet wired; events can be emitted via Spring events for now.
- Frontend talks to backend at http://localhost:8080/api.

## Risks
- WebSocket scaling; mitigated by demo scope.
- Kafka complexity; use single-node Redpanda or emit via in-process events initially.
- Schema drift; version field + migration scripts.
- Missing Maven on PATH; need Maven wrapper or install.
