# 04-security-checklist

Status legend: [x] done & verified · [~] done with known limits · [ ] open

## Authentication
- [x] JWT access token issued on login, 15m expiry
- [x] Refresh token issued, 7d expiry (POST /api/auth/refresh implemented)
- [x] Passwords hashed with BCryptPasswordEncoder (verified: hash ≠ plain, login matches)
- [x] JwtAuthenticationFilter validates token and installs the user id as principal
- [x] @EnableMethodSecurity enabled — @PreAuthorize actually enforced (was silently
      ignored before; caught by live RBAC probes)
- [x] JWT secret read from JWT_SECRET env var (dev fallback documented as dev-only)
- [ ] Refresh token rotation (single fixed refresh token per login)
- [ ] Token revocation list for logout (tokens valid until expiry)

## Authorization
- [x] Roles: OWNER > ADMIN > MEMBER > VIEWER with a real hierarchy in MembershipService
- [x] RBAC checks enforced: members API (list=member, add/remove=ADMIN+, roles=OWNER),
      org update/delete (OWNER), board endpoints (read=member-org, write=ADMIN+, delete=OWNER)
- [x] Organization creator automatically becomes its first OWNER
- [x] GET /organizations returns only the caller's organizations (tenant isolation)
- [x] AccessDeniedException mapped to clean 403 {"message":"Forbidden"}
- [x] Live verification matrix executed (MEMBER denied promotion/add, OWNER allowed)
- [x] Card/list/comment endpoints guarded at method level with @PreAuthorize + WorkspaceSecurity/DocumentSecurity (edit = MEMBER+, read = VIEWER+)
- [x] Document endpoints fully guarded (create workspace-scoped MEMBER+, read/update/delete via DocumentSecurity VIEWER+/MEMBER+)
- [ ] Organization membership lookup cached in Redis (direct Mongo query per check)

## Input validation
- [x] @NotBlank on RegisterRequest, LoginRequest
- [x] AddMembershipRequest / UpdateRoleRequest validated (@NotBlank + role whitelist)
- [x] @Valid on controller request bodies
- [~] CreateBoardRequest / CreateCardRequest / CreateListRequest lack constraints
- [ ] Request size limits configured

## Secrets management
- [x] JWT secret from env (JWT_SECRET), no hardcoded production secret
- [x] Mongo URI from MONGODB_URI env var, Kafka bootstrap from KAFKA_BOOTSTRAP_SERVERS
- [x] No secrets committed (dev fallback value is a public placeholder)
- [x] .env.example file for new developers

## CORS & transport
- [x] CORS locked to http://localhost:5173 in SecurityConfig + application.yml
- [x] WebSocket CONNECT requires a valid JWT (Authorization header, validated in
      WebSocketConfig interceptor; invalid/missing token rejected at CONNECT)
- [ ] HTTPS enforced in production (local-only deployment for now)

## Error handling
- [x] GlobalExceptionHandler: consistent {"message":"..."} bodies
- [x] No stack traces leaked (catch-all returns generic 500 body)
- [x] 400 validation / 403 denied / 404 unknown-path / 409 conflict mapped correctly

## Data protection
- [x] MongoDB connection via URI env var
- [x] UserResponse exposes only id/email/name (never passwordHash)
- [ ] Audit logging for auth events (register/login/denials)

## Dependencies
- [ ] OWASP Dependency-Check run, no critical CVEs
- [ ] SonarQube quality gate passed

## Operational
- [x] /actuator/health exposed (permits container healthcheck; rest of actuator
      stays behind authentication)
- [ ] Rate limiting on auth endpoints
- [ ] Logging to stdout (yes) — PII review pending

## Verification log (2026-10-11, live Docker stack)
MEMBER listing members → 200 (granted role) · MEMBER demoting OWNER → 403 ·
MEMBER adding user → 403 · OWNER adding member → 200 (duplicate → 400 "already a
member") · MEMBER renaming org → 403 · MEMBER deleting org → 403 · no token → 403 ·
403 body = {"message":"Forbidden"} · unauthenticated org list → empty
