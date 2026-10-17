# Plan: Standardize Backend IDs to UUID

## Goal
Make the entire Omada backend consistent on UUID IDs with no String<->UUID conversions, enabling clean compilation and secure opaque identifiers.

## Current context / assumptions
- Spring Boot 3.3.2, Spring Data MongoDB 7, Java 17 monolith.
- Entities already use `@Id private UUID id` for 8 types: Activity, Board, Card, Comment, Membership, Organization, TaskList, User, Workspace.
- Reference fields mixed: some entities have `String workspaceId/boardId/...` while others have UUID.
- Repositories currently extend `MongoRepository<Entity,String>` in some cases, others `MongoRepository<Entity,UUID>`.
- Services/controllers accept String ids and convert via `UUID.fromString`.
- DTOs use String id fields.
- `IdGenerator.uuid()` returns UUID.
- No `spring.data.mongodb.uuid-representation` configured.
- WorkspaceSecurity SpEL methods may accept String.
- No @ControllerAdvice for NoSuchElementException → 404.

Assumptions:
- No production data to migrate.
- Frontend sends UUIDs as strings; Jackson maps automatically.

## Architecture / proposed approach
Enforce strict UUID typing end-to-end: change all entity reference fields to UUID, repositories to `MongoRepository<E, UUID>`, DTO ids to UUID, service/controller signatures to UUID, remove all conversions. Add Mongo UUID representation config and 404 advice. Keep business logic unchanged.

## Step-by-step tasks

### Step 0: Inventory id types
Task: list every file using id.
File: generate inventory.
Command:
```
find backend/src/main/java -name "*.java" -exec grep -n -E "private.*id|@PathVariable.*id|@RequestParam.*id|public.*(String|UUID) id" {} \; > .hermes/inventory_ids.txt
```
Expected output: file list with line numbers.

Task: create inventory table.
File: `.hermes/inventory_ids.md`
Action: compile table with file, field/param, current type.

### Step 1: Configure Mongo UUID representation
File: `backend/src/main/resources/application.yml`
Action: add:
```yaml
spring:
  data:
    mongodb:
      uuid-representation: standard
```
Verify:
```
grep -n "uuid-representation" backend/src/main/resources/application.yml
```
Expected: line with `standard`.

### Step 2: Standardize entity reference fields
Files to update:
- `backend/src/main/java/com/omada/realtime/entity/Activity.java`
- `backend/src/main/java/com/omada/realtime/entity/Board.java`
- `backend/src/main/java/com/omada/realtime/entity/Card.java`
- `backend/src/main/java/com/omada/realtime/entity/Comment.java`
- `backend/src/main/java/com/omada/realtime/entity/Membership.java`
- `backend/src/main/java/com/omada/realtime/entity/TaskList.java`
- `backend/src/main/java/com/omada/realtime/entity/Workspace.java`
- `backend/src/main/java/com/omada/realtime/entity/Organization.java`

Action per file: change reference fields to UUID.
Example `Card.java`:
```java
// Before
private String boardId;
private String listId;
private String workspaceId;
// After
private UUID boardId;
private UUID listId;
private UUID workspaceId;
```
Verify compilation of entity package.

### Step 3: Update DTOs
Files:
- `backend/src/main/java/com/omada/realtime/dto/request/CreateBoardRequest.java`
- `backend/src/main/java/com/omada/realtime/dto/request/CreateCardRequest.java`
- `backend/src/main/java/com/omada/realtime/dto/request/CreateCommentRequest.java`
- `backend/src/main/java/com/omada/realtime/dto/request/CreateListRequest.java`
- `backend/src/main/java/com/omada/realtime/dto/request/CreateOrganizationRequest.java`
- `backend/src/main/java/com/omada/realtime/dto/request/CreateWorkspaceRequest.java`
- `backend/src/main/java/com/omada/realtime/dto/response/*.java`

Action: change `private String id;` to `private UUID id;`
Also change reference fields to UUID.

Verify:
```
grep -n "private String id;" backend/src/main/java/com/omada/realtime/dto/response/*.java
```
Expected: no matches.

### Step 4: Update Mappers
Files: `backend/src/main/java/com/omada/realtime/mapper/*.java`
Action: remove all `UUID.fromString` / `String.valueOf` conversions.
Example `CardMapper.java`:
```java
@Mapping(source = "id", target = "id")
CardResponse toResponse(Card entity);
```
No custom conversion methods.

Verify:
```
grep -rn "UUID.fromString" backend/src/main/java/com/omada/realtime/mapper
```
Expected: no matches.

### Step 5: Update Repositories
Files:
- `backend/src/main/java/com/omada/realtime/repository/ActivityRepository.java`
- `backend/src/main/java/com/omada/realtime/repository/BoardRepository.java`
- `backend/src/main/java/com/omada/realtime/repository/CardRepository.java`
- `backend/src/main/java/com/omada/realtime/repository/CommentRepository.java`
- `backend/src/main/java/com/omada/realtime/repository/ListRepository.java`
- `backend/src/main/java/com/omada/realtime/repository/MembershipRepository.java`
- `backend/src/main/java/com/omada/realtime/repository/OrganizationRepository.java`
- `backend/src/main/java/com/omada/realtime/repository/UserRepository.java`
- `backend/src/main/java/com/omada/realtime/repository/WorkspaceRepository.java`

Action per file: change generic and query params.
Example `BoardRepository.java`:
```java
public interface BoardRepository extends MongoRepository<Board,UUID> {
    List<Board> findByWorkspaceId(UUID workspaceId);
    Optional<Board> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
```
Repeat for all.

Verify:
```
grep -n "MongoRepository<.* ,String>" backend/src/main/java/com/omada/realtime/repository/*.java
```
Expected: no matches.

### Step 6: Update Service interfaces
Files:
- `backend/src/main/java/com/omada/realtime/service/ActivityService.java`
- `backend/src/main/java/com/omada/realtime/service/BoardService.java`
- `backend/src/main/java/com/omada/realtime/service/CardService.java`
- `backend/src/main/java/com/omada/realtime/service/CommentService.java`
- `backend/src/main/java/com/omada/realtime/service/ListService.java`
- `backend/src/main/java/com/omada/realtime/service/OrganizationService.java`
- `backend/src/main/java/com/omada/realtime/service/UserService.java`
- `backend/src/main/java/com/omada/realtime/service/WorkspaceService.java`

Action: change signatures.
Example `CardService.java`:
```java
CardResponse create(CreateCardRequest req);
CardResponse update(UUID id, UpdateCardRequest req);
CardResponse getById(UUID id);
void delete(UUID id);
List<CardResponse> findByBoard(UUID boardId);
```

### Step 7: Update Service implementations
Files:
- `backend/src/main/java/com/omada/realtime/service/impl/ActivityServiceImpl.java`
- `backend/src/main/java/com/omada/realtime/service/impl/BoardServiceImpl.java`
- `backend/src/main/java/com/omada/realtime/service/impl/CardServiceImpl.java`
- `backend/src/main/java/com/omada/realtime/service/impl/CommentServiceImpl.java`
- `backend/src/main/java/com/omada/realtime/service/impl/ListServiceImpl.java`
- `backend/src/main/java/com/omada/realtime/service/impl/OrganizationServiceImpl.java`
- `backend/src/main/java/com/omada/realtime/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/omada/realtime/service/impl/WorkspaceServiceImpl.java`

Action: remove `UUID.fromString`, use repo methods with UUID and scoped lookups.
Example `CardServiceImpl.java`:
```java
@Override
public CardResponse getById(UUID id) {
    Card card = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    return mapper.toResponse(card);
}
```
For scoped access:
```java
Optional<Card> card = repo.findByIdAndBoardIdAndBoardWorkspaceId(id, boardId, workspaceId);
```

Verify:
```
grep -rn "UUID.fromString" backend/src/main/java/com/omada/realtime/service/impl
```
Expected: no matches.

### Step 8: Update Controllers
Files:
- `backend/src/main/java/com/omada/realtime/controller/ActivityController.java`
- `backend/src/main/java/com/omada/realtime/controller/BoardController.java`
- `backend/src/main/java/com/omada/realtime/controller/CardController.java`
- `backend/src/main/java/com/omada/realtime/controller/CommentController.java`
- `backend/src/main/java/com/omada/realtime/controller/ListController.java`
- `backend/src/main/java/com/omada/realtime/controller/OrganizationController.java`
- `backend/src/main/java/com/omada/realtime/controller/WorkspaceController.java`

Action: change path variables.
Example `CardController.java`:
```java
@GetMapping("/{id}")
public CardResponse get(@PathVariable UUID id) {
    return service.getById(id);
}
```

Verify:
```
grep -rn "@PathVariable String id" backend/src/main/java/com/omada/realtime/controller
```
Expected: no matches.

### Step 9: Update Security
Files:
- `backend/src/main/java/com/omada/realtime/security/WorkspaceSecurity.java`
- `backend/src/main/java/com/omada/realtime/security/JwtUtil.java`

Action: change method params to UUID.
Example `WorkspaceSecurity.java`:
```java
public boolean hasAccess(UUID workspaceId) { ... }
```

Verify compilation.

### Step 10: Update WebSocket handlers
Files:
- `backend/src/main/java/com/omada/realtime/config/WebSocketConfig.java`
- `backend/src/main/java/com/omada/realtime/config/WebSocketEventHandler.java`

Action: ensure message payload ids are UUID, no string conversions.

### Step 11: Add 404 advice
File: `backend/src/main/java/com/omada/realtime/exception/GlobalExceptionHandler.java`
Action: add:
```java
@ExceptionHandler(NoSuchElementException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ErrorResponse handleNotFound(NoSuchElementException ex) {
    return new ErrorResponse("NOT_FOUND", ex.getMessage());
}
```

Verify:
```
grep -n "NoSuchElementException" backend/src/main/java/com/omada/realtime/exception/GlobalExceptionHandler.java
```
Expected: handler present.

### Step 12: Build verification
Command:
```
mvn -f backend/pom.xml clean compile
```
Expected output: `BUILD SUCCESS`

Command:
```
mvn -f backend/pom.xml test
```
Expected output: `Tests run: X, Failures: 0`

If errors remain, fix iteratively.

## Tests / validation
Per module, follow TDD:
1. Write test `CardServiceUuidTest` asserting `service.getById(UUID.randomUUID())` compiles.
2. Run test, expect failure.
3. Implement change.
4. Run test, expect pass.
5. Commit.

Integration:
- Start docker compose mongo.
- POST `/api/boards` → returns UUID.
- GET `/api/boards/{uuid}` → 200.

## Risks, tradeoffs, and open questions
- Risk: MapStruct may require explicit UUID mapping; verify compilation.
- Tradeoff: Changing reference fields to UUID changes Mongo document shape; pre-release so acceptable.
- Open question: Do we need `@DBRef` for relationships? No, keep denormalized UUID references.
- Open question: Frontend sends UUID strings; Jackson handles conversion automatically.
- Risk: SpEL expressions in @PreAuthorize with UUID may need type conversion; test security.

## Deliverables
- Inventory table.
- All files changed with git diff.
- Build green confirmation.
- Summary of assumptions.
