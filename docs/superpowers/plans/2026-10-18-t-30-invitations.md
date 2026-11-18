# Invitation (T-30) Implementation Plan

> Executed natively in this session (execution method supplied by the user: implement + compile, no commit unless asked).

**Goal:** Add an Invitation entity, Spring Data MongoDB repository, and service layer (create/list/accept/decline/revoke) for workspace invitations, with idempotent acceptance.

**Architecture:** Follow T-29 WorkspaceMembership conventions exactly: Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor` entity with `@Id UUID id`, `@Document(collection = "invitations")`; `MongoRepository<Invitation, UUID>` with derived query methods; service interface + `@Service` impl with constructor injection, `VALID_ROLES` set, `normalizeRole`, `IdGenerator.uuid()`, `Instant.now()`. Acceptance delegates membership creation to `WorkspaceMembershipService` and is idempotent via a status guard (accepted invitations short-circuit before any membership write).

**Tech Stack:** Spring Boot 3.3.2, Java 17, Spring Data MongoDB, Lombok; JUnit 5 + AssertJ + Testcontainers for tests.

**Spec (T-30):** token UUIDv4, email, workspaceId, boardId optional, role, status PENDING/ACCEPTED/DECLINED/EXPIRED, expiresAt 24h from now.

**Global Constraints:** Package `com.omada.realtime.*`; do not touch T-4 org-scoped Membership; no commits unless asked; verify with `mvn compile` (scratch Maven 3.9.6, Java 17).

**Review Focus:** (1) expired invitation acceptance must fail and flip status to EXPIRED; (2) accepting twice must not create two WorkspaceMemberships; (3) unknown token must throw, never return null; (4) role validated against OWNER/ADMIN/MEMBER/VIEWER.

### Task 1: Invitation entity
Files: Create `backend/src/main/java/com/omada/realtime/entity/Invitation.java`
Interfaces: Produces: `Invitation` (id, token UUIDv4, email, workspaceId, boardId optional, role, status, expiresAt 24h).
- [ ] Write entity; verify field set matches spec.

### Task 2: InvitationRepository
Files: Create `backend/src/main/java/com/omada/realtime/repository/InvitationRepository.java`
Interfaces: Consumes: Invitation entity. Produces: `findByWorkspaceId`, `findByWorkspaceIdAndStatus`, `findByToken`, `existsByToken`, `deleteByToken`.

### Task 3: InvitationService interface + impl
Files: Create `service/InvitationService.java`, `service/impl/InvitationServiceImpl.java`
Interfaces: Consumes: InvitationRepository, WorkspaceMembershipService, WorkspaceMembershipRepository, UserRepository. Produces: create/list/accept/decline/revoke; accept idempotent.

### Task 4: Integration test
Files: Create `test/.../service/InvitationServiceIntegrationTest.java`
Interfaces: Consumes: service + repositories. Produces: create/list/accept/decline/revoke, idempotent accept, expired reject, unknown token.

### Task 5: Compile + test
Run `mvn compile` and `mvn test -Dtest=InvitationServiceIntegrationTest`; fix anything red.
