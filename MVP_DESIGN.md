# MVP Technical Design — Real-Time Collaboration SaaS

## Design Principles
1. Multi-tenant by organization → workspace isolation
2. Event-driven for realtime: REST for CRUD, WebSocket for live updates
3. MongoDB flexible schema for boards & documents, versioned for optimistic concurrency
4. JWT stateless auth, RBAC enforced at service layer
5. MVP scope: users, workspaces, boards with lists/cards, comments, activity feed. No full document editor yet.

## High-Level MongoDB Schema

### users
{
  _id: ObjectId,
  email: String unique,
  name: String,
  passwordHash: String,
  avatarUrl: String,
  createdAt: Date,
  updatedAt: Date
}
Indexes: {email:1 unique}

### organizations
{
  _id: ObjectId,
  slug: String unique,
  name: String,
  createdBy: ObjectId → users,
  createdAt: Date
}

### memberships
{
  _id: ObjectId,
  organizationId: ObjectId,
  userId: ObjectId,
  role: Enum[OWNER, ADMIN, MEMBER, VIEWER],
  joinedAt: Date
}
Indexes: {organizationId:1, userId:1 unique}

### workspaces
{
  _id: ObjectId,
  organizationId: ObjectId,
  name: String,
  description: String,
  createdBy: ObjectId,
  createdAt: Date,
  updatedAt: Date
}
Indexes: {organizationId:1, name:1}

### boards
{
  _id: ObjectId,
  workspaceId: ObjectId,
  title: String,
  description: String,
  createdBy: ObjectId,
  createdAt: Date,
  updatedAt: Date,
  archived: Boolean
}
Indexes: {workspaceId:1, createdAt:-1}

### lists
{
  _id: ObjectId,
  boardId: ObjectId,
  title: String,
  position: Number,
  createdAt: Date
}
Indexes: {boardId:1, position:1}

### cards
{
  _id: ObjectId,
  listId: ObjectId,
  boardId: ObjectId,
  title: String,
  description: String,
  position: Number,
  assignees: [ObjectId],
  labels: [{name, color}],
  dueDate: Date,
  version: Number,
  createdBy: ObjectId,
  createdAt: Date,
  updatedAt: Date
}
Indexes: {boardId:1, listId:1, position:1}

### comments
{
  _id: ObjectId,
  cardId: ObjectId,
  authorId: ObjectId,
  body: String,
  createdAt: Date,
  updatedAt: Date
}
Indexes: {cardId:1, createdAt:1}

### activities
{
  _id: ObjectId,
  workspaceId: ObjectId,
  boardId: ObjectId,
  actorId: ObjectId,
  type: Enum[BOARD_CREATED, CARD_MOVED, CARD_CREATED, COMMENT_ADDED],
  entityId: ObjectId,
  metadata: Object,
  createdAt: Date
}
Indexes: {workspaceId:1, createdAt:-1}, {boardId:1, createdAt:-1}

## REST API Endpoints — MVP

Base path /api

### Auth
POST /auth/register {email,name,password}
POST /auth/login {email,password} → {accessToken, refreshToken}
POST /auth/refresh {refreshToken}
GET /auth/me

### Organizations
GET /organizations
POST /organizations {name,slug}
GET /organizations/{id}
PATCH /organizations/{id}

### Memberships
GET /organizations/{orgId}/members
POST /organizations/{orgId}/members {userId, role}
PATCH /organizations/{orgId}/members/{userId} {role}
DELETE /organizations/{orgId}/members/{userId}

### Workspaces
GET /workspaces?orgId=
POST /workspaces {organizationId,name,description}
GET /workspaces/{id}
PATCH /workspaces/{id}
DELETE /workspaces/{id}

### Boards
GET /workspaces/{workspaceId}/boards
POST /workspaces/{workspaceId}/boards {title,description}
GET /boards/{id}
PATCH /boards/{id}
DELETE /boards/{id}

### Lists
GET /boards/{boardId}/lists
POST /boards/{boardId}/lists {title,position}
PATCH /lists/{id} {title,position}
DELETE /lists/{id}

### Cards
GET /boards/{boardId}/cards?listId=
POST /boards/{boardId}/lists/{listId}/cards {title,description,position,assignees}
GET /cards/{id}
PATCH /cards/{id} {title,description,position,listId,assignees,labels,dueDate,version}
DELETE /cards/{id}

### Comments
GET /cards/{cardId}/comments
POST /cards/{cardId}/comments {body}
PATCH /comments/{id} {body}
DELETE /comments/{id}

### Activity Feed
GET /workspaces/{workspaceId}/activities?limit=&cursor=
GET /boards/{boardId}/activities?limit=&cursor=

## WebSocket Topics
Connect: /ws?token=JWT
Subscribe:
 /topic/workspace.{workspaceId}
 /topic/board.{boardId}
 /topic/card.{cardId}
Send:
 /app/board.{boardId}.move-card {cardId, fromListId, toListId, position}
 /app/board.{boardId}.update-card {cardId, patch, version}
Server broadcasts delta to subscribers.

## Architecture Reasoning

Why MongoDB?
Flexible schema for boards/lists/cards hierarchy, easy to evolve for future documents. ObjectId natural for references. Good for high-read workload with proper indexes.

Why separate services?
Auth, Workspace, Board, Realtime, Activity are bounded contexts. Allows independent scaling and team ownership. Gateway centralizes cross-cutting concerns.

Why REST + WebSocket?
REST for reliable CRUD and idempotency. WebSocket for low-latency live updates. Kafka decouples events: Board Service emits CardMoved → Activity Service consumes → feed materialized.

Why JWT + RBAC?
Stateless, scales horizontally. Role check on every request via Spring Security @PreAuthorize with organization membership lookup cached in Redis.

Why version field on cards?
Optimistic concurrency prevents lost updates during simultaneous edits. Client sends version, server rejects if stale.

Why activities collection?
Append-only event log enables feed, audit, and future analytics without querying full card history.

MVP scope trade-offs:
- No full document editor, just boards/cards for faster validation
- Comments are card-scoped only
- No offline sync, just optimistic UI
- Single Kafka topic per domain for simplicity, partition by workspaceId

Next step: generate Spring Boot entity classes + DTOs and React API client.
