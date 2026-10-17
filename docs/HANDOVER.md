# HANDOVER

Welcome to Omada. This doc lets a new developer be productive in ~1 hour.

## Folder map
Omada/
  backend/ Spring Boot monolith
    src/main/java/com/omada/realtime/
      controller/ REST controllers
      service/ interfaces + impl
      repository/ Mongo repositories
      entity/ Mongo documents with UUID ids
      dto/ request/response
      mapper/ MapStruct mappers
      security/ JwtUtil, JwtAuthenticationFilter, IdGenerator
      config/ WebSocket, Mongo, Redis, Kafka, Security
  frontend/ React Vite SPA
    src/pages/ Login, Register, Dashboard, Workspace, Board
    src/components/ Sidebar, CardItem, ActivityFeed
    src/store/ Zustand stores
    src/services/ API client, WebSocket service
  docs/ 01-specification, 02-system-design, 03-backlog, 04-security-checklist
  backend/infrastructure/docker/docker-compose.yml

## How to run/test/debug
Run infra:
cd backend/infrastructure/docker && docker compose up -d
Run backend:
cd backend && mvn spring-boot:run
Run frontend:
cd frontend && npm install && npm run dev

Tests:
Backend: mvn test
Frontend: npm test

Debug: Spring Boot devtools enabled, React Vite HMR.

## Key decisions
Monolith for MVP with service layer separation
MongoDB UUID ids for flexible schema
Optimistic locking via version field on Card
WebSocket STOMP for live updates
JWT stateless auth with refresh tokens
SPA with optimistic UI for Trello/Notion feel

## Conventions
Constructor injection only, final fields
Controller -> Service -> Repository
DTOs for all API boundaries
MapStruct for mapping
Javadoc on public classes/methods
No secrets in code, env vars only

## Known issues
Maven wrapper not on PATH, use local mvn
WebSocket JWT auth interceptor not fully wired
Activity feed event emission not connected to Kafka yet
RBAC @PreAuthorize annotations need enforcement per endpoint
Refresh token rotation not implemented

## Next tickets
T-10 WebSocket JWT auth
T-11 Presence Redis
T-12 Card move broadcast
T-14 Activity collection
T-20 Backend tests with Testcontainers execution
T-22 Security checklist completion
