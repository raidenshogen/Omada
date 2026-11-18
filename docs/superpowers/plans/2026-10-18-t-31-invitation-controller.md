# Invitation Controller (T-31) Implementation Plan

> Execution: natively in this session (execution method supplied by the user: implement + compile + verify, no commit unless asked).

**Goal:** Add `InvitationController` exposing the six invitation endpoints, backed by `InvitationService` (T-30), with `@PreAuthorize` RBAC: OWNER/ADMIN for create/list/revoke; permitAll (the unguessable token is the grant) for get/accept/decline.

**Tech Stack:** Spring Boot 3.3.2, Java 17, Spring MVC, Spring Security (`@EnableMethodSecurity`), Lombok.

**Spec (T-31):**
- `POST /api/workspaces/{id}/invitations` → `service.create(workspaceId, email, role, boardId)`
- `GET /api/workspaces/{id}/invitations` → `service.listByWorkspace(workspaceId)`
- `GET /api/invitations/{token}` → `service.getByToken(token)`
- `POST /api/invitations/{token}/accept` → `service.accept(token)`
- `POST /api/invitations/{token}/decline` → `service.decline(token)`
- `DELETE /api/invitations/{id}` → `service.revoke(id)` — `{id}` is the invitation token (the service revokes by token)

**Files:**
- Create `backend/src/main/java/com/omada/realtime/controller/InvitationController.java`
- Create `backend/src/main/java/com/omada/realtime/dto/request/CreateInvitationRequest.java`
- Create `backend/src/test/java/com/omada/realtime/controller/InvitationControllerSecurityTest.java` (container-free `@WebMvcTest`, mirrors `DocumentControllerSecurityTest`)
- Modify `backend/src/main/java/com/omada/realtime/service/InvitationService.java`: add `Invitation getByToken(UUID token)` (missing from T-30; required by the get-details endpoint)
- Modify `backend/src/main/java/com/omada/realtime/service/impl/InvitationServiceImpl.java`: implement `getByToken`
- Modify `backend/src/main/java/com/omada/realtime/security/WorkspaceSecurity.java`: inject `InvitationRepository`; add `isInvitationWorkspaceAdminOrOwner(String token, String userId)` so token-scoped revoke can be gated on OWNER/ADMIN of the invitation's workspace

**Authorization:** `@PreAuthorize("@workspaceSecurity.canWriteWorkspace(#id, authentication.name)")` — `canWriteWorkspace` = `isOrgAdmin`, which covers OWNER and ADMIN via the role hierarchy (OWNER > ADMIN > MEMBER > VIEWER, enforced in `MembershipService.hasRole`). Unknown workspace → deny (403). For DELETE, the new `WorkspaceSecurity` method resolves the workspace from the token and checks `isOrgAdmin || isOrgOwner`, denying (false → 403) on parse failure or unknown token.

**Verification:** `mvn compile` (main), then `mvn test -Dtest=InvitationControllerSecurityTest`.
