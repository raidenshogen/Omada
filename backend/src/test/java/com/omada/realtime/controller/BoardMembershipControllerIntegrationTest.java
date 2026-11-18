package com.omada.realtime.controller;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.dto.request.AddBoardMemberRequest;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.response.AuthResponse;
import com.omada.realtime.entity.Board;
import com.omada.realtime.entity.Membership;
import com.omada.realtime.entity.Organization;
import com.omada.realtime.entity.User;
import com.omada.realtime.entity.Workspace;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.BoardMembershipRepository;
import com.omada.realtime.repository.MembershipRepository;
import com.omada.realtime.repository.OrganizationRepository;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.repository.WorkspaceRepository;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the board membership controller endpoints:
 * add/remove (workspace admin), list (board member), and authorization boundaries.
 */
@AutoConfigureMockMvc
public class BoardMembershipControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardMembershipRepository boardMembershipRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    private String registerUser(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setName(email.split("@")[0]);
        req.setPassword("password123");
        AuthResponse auth = userService.register(req);
        return auth.getUser().getId();
    }

    private String uniqueEmail(String base) {
        return base.replace("@", "-" + UUID.randomUUID().toString().substring(0, 8) + "@");
    }

    private String createOrg() {
        Organization org = Organization.builder()
                .id(IdGenerator.uuid())
                .name("Test Org")
                .slug("test-org-" + UUID.randomUUID())
                .createdAt(Instant.now())
                .build();
        return organizationRepository.save(org).getId().toString();
    }

    private void makeOrgAdmin(String orgId, String userId) {
        Membership membership = Membership.builder()
                .id(IdGenerator.uuid())
                .organizationId(orgId)
                .userId(userId)
                .role("ADMIN")
                .joinedAt(Instant.now())
                .build();
        membershipRepository.save(membership);
    }

    private String createWorkspace(String orgId) {
        Workspace ws = Workspace.builder()
                .id(IdGenerator.uuid())
                .organizationId(orgId)
                .name("Test Workspace")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return workspaceRepository.save(ws).getId().toString();
    }

    private String createBoard(String workspaceId) {
        Board board = Board.builder()
                .id(IdGenerator.uuid())
                .workspaceId(workspaceId)
                .title("Test Board")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return boardRepository.save(board).getId().toString();
    }

    private String asJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void add_boardMember_returnsCreated_forWorkspaceAdmin() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("admin@example.com"));
        makeOrgAdmin(orgId, userId);
        String wsId = createWorkspace(orgId);
        String boardId = createBoard(wsId);

        AddBoardMemberRequest req = new AddBoardMemberRequest();
        req.setUserId("member-user");

        mockMvc.perform(post("/api/boards/{id}/members", boardId)
                        .with(user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("member-user"))
                .andExpect(jsonPath("$.boardId").value(boardId));
    }

    @Test
    void add_boardMember_rejected_forNonAdmin() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("viewer@example.com"));
        Membership membership = Membership.builder()
                .id(IdGenerator.uuid())
                .organizationId(orgId)
                .userId(userId)
                .role("VIEWER")
                .joinedAt(Instant.now())
                .build();
        membershipRepository.save(membership);
        String wsId = createWorkspace(orgId);
        String boardId = createBoard(wsId);

        AddBoardMemberRequest req = new AddBoardMemberRequest();
        req.setUserId("member-user");

        mockMvc.perform(post("/api/boards/{id}/members", boardId)
                        .with(user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_boardMembers_returnsMembers() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("admin@example.com"));
        makeOrgAdmin(orgId, userId);
        String wsId = createWorkspace(orgId);
        String boardId = createBoard(wsId);

        // The listing user must be a board member (isBoardMember check)
        boardMembershipRepository.save(com.omada.realtime.entity.BoardMembership.builder()
                .id(IdGenerator.uuid())
                .boardId(boardId)
                .userId(userId)
                .joinedAt(Instant.now())
                .build());
        boardMembershipRepository.save(com.omada.realtime.entity.BoardMembership.builder()
                .id(IdGenerator.uuid())
                .boardId(boardId)
                .userId("member-1")
                .joinedAt(Instant.now())
                .build());
        boardMembershipRepository.save(com.omada.realtime.entity.BoardMembership.builder()
                .id(IdGenerator.uuid())
                .boardId(boardId)
                .userId("member-2")
                .joinedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/boards/{id}/members", boardId)
                        .with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void remove_boardMember_deletes_forWorkspaceAdmin() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("admin@example.com"));
        makeOrgAdmin(orgId, userId);
        String wsId = createWorkspace(orgId);
        String boardId = createBoard(wsId);

        boardMembershipRepository.save(com.omada.realtime.entity.BoardMembership.builder()
                .id(IdGenerator.uuid())
                .boardId(boardId)
                .userId("member-user")
                .joinedAt(Instant.now())
                .build());

        mockMvc.perform(delete("/api/boards/{id}/members/{userId}", boardId, "member-user")
                        .with(user(userId)))
                .andExpect(status().isOk());

        assertThat(boardMembershipRepository.findByBoardIdAndUserId(boardId, "member-user"))
                .isEmpty();
    }

    @Test
    void remove_boardMember_rejected_forNonAdmin() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("viewer@example.com"));
        Membership membership = Membership.builder()
                .id(IdGenerator.uuid())
                .organizationId(orgId)
                .userId(userId)
                .role("VIEWER")
                .joinedAt(Instant.now())
                .build();
        membershipRepository.save(membership);
        String wsId = createWorkspace(orgId);
        String boardId = createBoard(wsId);

        boardMembershipRepository.save(com.omada.realtime.entity.BoardMembership.builder()
                .id(IdGenerator.uuid())
                .boardId(boardId)
                .userId("member-user")
                .joinedAt(Instant.now())
                .build());

        mockMvc.perform(delete("/api/boards/{id}/members/{userId}", boardId, "member-user")
                        .with(user(userId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void add_duplicate_boardMember_rejected() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("admin@example.com"));
        makeOrgAdmin(orgId, userId);
        String wsId = createWorkspace(orgId);
        String boardId = createBoard(wsId);

        boardMembershipRepository.save(com.omada.realtime.entity.BoardMembership.builder()
                .id(IdGenerator.uuid())
                .boardId(boardId)
                .userId("member-user")
                .joinedAt(Instant.now())
                .build());

        AddBoardMemberRequest req = new AddBoardMemberRequest();
        req.setUserId("member-user");

        mockMvc.perform(post("/api/boards/{id}/members", boardId)
                        .with(user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(req)))
                .andExpect(status().isBadRequest());
    }
}
