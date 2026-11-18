# T-29 Workspace Membership Implementation Plan

**Goal:** Add a workspace-scoped membership entity/repository/service so a workspace Owner/Admin can manage workspace members with roles (T-29).

**Architecture:** New `WorkspaceMembership` entity (userId, workspaceId, role, joinedAt) backed by Spring Data MongoDB, plus a service layer with create/findByWorkspace/changeRole/remove. The existing committed org-scoped `Membership` (T-4) is left untouched; `Membership` = organization scope, `WorkspaceMembership` = workspace scope (mirrors Organization/Workspace being separate entities).

**Tech Stack:** Spring Boot 3.3.2, Java 17, Spring Data MongoDB, Lombok (@Data/@Builder), jakarta.validation (@NotNull), IdGenerator.uuid(), Instant.now().

**Global constraints:** Constructor injection; service layer generates IDs via IdGenerator.uuid(); do not modify T-4's org-scoped Membership/MembershipService/MembershipController or its tests; existing tests must still pass.

**Files:**
- Create `backend/src/main/java/com/omada/realtime/entity/WorkspaceMembership.java`
- Create `backend/src/main/java/com/omada/realtime/repository/WorkspaceMembershipRepository.java`
- Create `backend/src/main/java/com/omada/realtime/service/WorkspaceMembershipService.java`
- Create `backend/src/main/java/com/omada/realtime/service/impl/WorkspaceMembershipServiceImpl.java`
- Create `backend/src/test/java/com/omada/realtime/service/WorkspaceMembershipServiceIntegrationTest.java`

**Verification:** `mvn -q compile` and `mvn -q test-compile` clean; run existing + new tests via Testcontainers (Docker available).
