Changed files:
- backend/src/main/java/com/omada/realtime/config/WebSocketConfig.java — added JWT auth interceptor for STOMP inbound channel
- backend/src/main/java/com/omada/realtime/service/impl/CardServiceImpl.java — optimistic version check retained, activity event stub added

How to run:
Backend: cd backend && mvn spring-boot:run
Docker: cd backend/infrastructure/docker && docker compose up -d

Tests run and result:
No Maven execution possible yet (mvn not in PATH). Frontend Vitest 8 tests passed previously. Backend JUnit5 skeleton present.

Assumptions:
JwtUtil.validate method exists and returns subject or null
ActivityRepository save can be wired via ActivityService later

Not done / risks:
- Refresh token rotation not implemented
- RBAC @PreAuthorize annotations still missing on controllers
- ActivityRepository not persisted in Card update stub
- SonarQube gate not executed

Suggested next ticket:
T-10 Complete RBAC enforcement on controllers
