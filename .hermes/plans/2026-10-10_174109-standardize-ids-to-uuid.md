# Plan: Standardize Backend IDs to UUID

## Goal
Make the entire Omada backend consistent on UUID IDs: entities, DTOs, repositories, services, controllers, mappers, security, and WebSocket handlers use `java.util.UUID` only, with no String<->UUID conversions.

## Current context / assumptions
- Spring Boot 3.3.2, Spring Data MongoDB 7, Java 17 monolith.
- Entities already have `@Id private UUID id` for 8 types: Card, TaskList, Board, Workspace, Organization, User, Membership, Comment, Activity.
- Repositories currently mixed String/UUID generic type; some derived queries accept String ids.
- Services and controllers accept String path variables and convert via `UUID.fromString`.
- DTOs (request/response) likely use String for id fields.
- Mappers may convert between String and UUID.
- `IdGenerator.uuid()` returns UUID; some code still expects String.
- `spring.data.mongodb.uuid-representation` not set.
- No @ControllerAdvice mapping for NoSuchElementException to 404.
- WorkspaceSecurity SpEL bean methods may accept String ids.

Assumptions:
- No production data migration needed; this is pre-release.
- MongoDB stores UUID as binary/string depending on config; we will enforce standard representation.

## Architecture / proposed approach
Enforce strict UUID typing end-to-end. Change repositories to `MongoRepository<E, UUID>`, DTO ids to UUID, service signatures to UUID, controller path variables to UUID, and derived queries to UUID parameters. Add `spring.data.mongodb.uuid-representation=standard` in application.yml. Add @ControllerAdvice for 404. Keep business logic unchanged.

## Step-by-step tasks

### Phase 0: Inventory
1. List current id types.
Command:
```
find backend/src/main/java -name "*.java" -exec grep -l "private.*id" {} \; | xargs grep -n "private.*id" | head -n 200
```
Output expected: table of file, field, type.

2. Create inventory markdown for review.

### Phase 1: Configuration
File: `backend/src/main/resources/application.yml`
Action: Add:
```yaml
spring:
  data:
    mongodb:
      uuid-representation: standard
```
Verify: `grep -n "uuid-representation" backend/src/main/resources/application.yml`

### Phase 2: Entities – verify and standardize references
Files: all `entity/*.java`
Action: Ensure every reference field like `workspaceId`, `organizationId`, `boardId`, `listId`, `cardId`, `authorId`, `actorId`, `entityId` is `UUID` not String.
Example check: `backend/src/main/java/com/omada/realtime/entity/Card.java`
If String found, change to UUID.

### Phase 3: DTOs
Files:
- `dto/request/*.java`
- `dto/response/*.java`
Action: Change `id` fields from String to UUID.
Example: `backend/src/main/java/com/omada/realtime/dto/response/CardResponse.java`
Current: `private String id;` → New: `private UUID id;`

### Phase 4: Mappers
Files: `mapper/*.java`
Action: Remove all `UUID.fromString` / `String.valueOf(uuid)` conversions. Map UUID directly.
Example: `backend/src/main/java/com/omada/realtime/mapper/CardMapper.java`
Ensure MapStruct mapping uses UUID.

### Phase 5: Repositories
Files: `repository/*.java`
Action: Change generic type to UUID and query parameters.
Example: `backend/src/main/java/com/omada/realtime/repository/BoardRepository.java`
Before:
```java
public interface BoardRepository extends MongoRepository<Board,String> { List<Board> findByWorkspaceId(String wsId); }
```
After:
```java
public interface BoardRepository extends MongoRepository<Board,UUID> { List<Board> findByWorkspaceId(UUID wsId); }
```
Repeat for Activity, Membership, Card, Workspace, User, Organization, Comment, ListRepository.

### Phase 6: Services interface
Files: `service/*.java`
Action: Change method signatures id params to UUID.
Example: `backend/src/main/java/com/omada/realtime/service/CardService.java`
```java
CardResponse update(UUID id, UpdateCardRequest req);
CardResponse getById(UUID id);
void delete(UUID id);
```

### Phase 7: Services impl
Files: `service/impl/*.java`
Action: Remove `UUID.fromString(id)` / `String.valueOf`. Use UUID directly for repo calls.
Add scoped lookups: `findByIdAndWorkspaceId(id, workspaceId)` pattern.
Example: `backend/src/main/java/com/omada/realtime/service/impl/CardServiceImpl.java`
Replace:
```java
repo.findById(UUID.fromString(id))
```
with:
```java
repo.findById(id)
```
And for security scope:
```java
repo.findByIdAndWorkspaceId(id, workspaceId).orElseThrow();
```

### Phase 8: Controllers
Files: `controller/*.java`
Action: Change `@PathVariable String id` to `@PathVariable UUID id`.
Example: `backend/src/main/java/com/omada/realtime/controller/CardController.java`
```java
@GetMapping("/{id}")
public CardResponse get(@PathVariable UUID id) { ... }
```

### Phase 9: Security
Files:
- `security/WorkspaceSecurity.java`
- `security/JwtUtil.java`
Action: Change methods that accept ids to UUID.
Ensure SpEL expressions work with UUID parameters.

### Phase 10: WebSocket & Config
Files:
- `config/WebSocketConfig.java`
- `config/WebSocketEventHandler.java`
Action: Ensure message handlers accept UUID ids, no string conversions.

### Phase 11: Exception handling
File: `exception/GlobalExceptionHandler.java`
Action: Add mapping:
```java
@ExceptionHandler(NoSuchElementException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ErrorResponse handleNotFound(NoSuchElementException ex) { ... }
```

### Phase 12: Build verification
Command:
```
mvn -f backend/pom.xml clean compile
```
Expected output: BUILD SUCCESS, 0 errors.

Command:
```
mvn -f backend/pom.xml test
```
Expected output: Tests run, BUILD SUCCESS.

## Tests / validation
For each changed module:
1. Write failing test asserting UUID type usage.
2. Run test, verify failure.
3. Implement change.
4. Run test, verify pass.
5. Commit with message `refactor(ids): standardize to UUID in <module>`.

Integration test:
- Start docker compose mongo/redis.
- Hit `/api/boards/{uuid}` endpoint, verify 200.
- Verify Mongo documents store UUID correctly.

## Risks, tradeoffs, and open questions
- Risk: Existing data with String ids would break. Mitigation: pre-release, no data.
- Tradeoff: UUID standard representation increases document size vs binary; acceptable for security.
- Open question: Do reference fields in entities need `@DBRef`? Probably not; keep as UUID.
- Open question: Frontend sends String UUIDs in JSON; Jackson will map String to UUID automatically.
- Risk: MapStruct may need explicit mapping for UUID; verify compilation.

## Deliverables
- All files changed list.
- Summary of assumptions made.
- Build green confirmation.
