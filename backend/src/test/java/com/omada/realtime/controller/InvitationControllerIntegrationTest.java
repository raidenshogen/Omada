package com.omada.realtime.controller;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.dto.request.CreateInvitationRequest;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.response.AuthResponse;
import com.omada.realtime.entity.Invitation;
import com.omada.realtime.entity.Membership;
import com.omada.realtime.entity.Organization;
import com.omada.realtime.entity.User;
import com.omada.realtime.entity.Workspace;
import com.omada.realtime.repository.InvitationRepository;
import com.omada.realtime.repository.MembershipRepository;
import com.omada.realtime.repository.OrganizationRepository;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.repository.WorkspaceRepository;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.service.InvitationService;
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
 * Integration tests for the invitation controller endpoints:
 * create/list (workspace admin), accept/decline/revoke by token (permitAll),
 * and authorization boundaries.
 */
@AutoConfigureMockMvc
public class InvitationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvitationRepository invitationRepository;

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

    private User createUserWithEmail(String email) {
        User user = User.builder()
                .id(IdGenerator.uuid())
                .email(email)
                .name(email.split("@")[0])
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return userRepository.save(user);
    }

    private String asJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void create_invitation_returnsCreated_forWorkspaceAdmin() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("admin@example.com"));
        makeOrgAdmin(orgId, userId);
        String wsId = createWorkspace(orgId);

        CreateInvitationRequest req = new CreateInvitationRequest();
        req.email = "alice@example.com";
        req.role = "MEMBER";
        req.boardId = null;

        mockMvc.perform(post("/api/workspaces/{workspaceId}/invitations", wsId)
                        .with(user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void create_invitation_rejected_forNonAdmin() throws Exception {
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

        CreateInvitationRequest req = new CreateInvitationRequest();
        req.email = "alice@example.com";
        req.role = "MEMBER";

        mockMvc.perform(post("/api/workspaces/{workspaceId}/invitations", wsId)
                        .with(user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_invitations_returnsThatWorkspace() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("admin2@example.com"));
        makeOrgAdmin(orgId, userId);
        String ws1 = createWorkspace(orgId);
        String ws2 = createWorkspace(orgId);

        createUserWithEmail("a@example.com");
        createUserWithEmail("b@example.com");
        invitationService.create(ws1, "a@example.com", "MEMBER", null);
        invitationService.create(ws1, "b@example.com", "ADMIN", null);
        invitationService.create(ws2, "c@example.com", "MEMBER", null);

        mockMvc.perform(get("/api/workspaces/{workspaceId}/invitations", ws1)
                        .with(user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void accept_invitation_createsMembership() throws Exception {
        String wsId = createWorkspace(createOrg());
        createUserWithEmail("alice-accept@example.com");
        Invitation inv = invitationService.create(wsId, "alice-accept@example.com", "MEMBER", null);

        mockMvc.perform(post("/api/invitations/{token}/accept", inv.getToken())
                        .with(user("anyone")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void accept_unknownToken_returns400() throws Exception {
        mockMvc.perform(post("/api/invitations/{token}/accept", UUID.randomUUID())
                        .with(user("anyone")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void decline_invitation_marksDeclined() throws Exception {
        String wsId = createWorkspace(createOrg());
        Invitation inv = invitationService.create(wsId, "alice@example.com", "MEMBER", null);

        mockMvc.perform(post("/api/invitations/{token}/decline", inv.getToken())
                        .with(user("anyone")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void revoke_invitation_marksRevoked_forWorkspaceAdmin() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("admin3@example.com"));
        makeOrgAdmin(orgId, userId);
        String wsId = createWorkspace(orgId);
        Invitation inv = invitationService.create(wsId, "alice@example.com", "MEMBER", null);

        mockMvc.perform(delete("/api/invitations/{id}", inv.getToken())
                        .with(user(userId)))
                .andExpect(status().isOk());

        assertThat(invitationRepository.findByToken(inv.getToken()))
                .get()
                .extracting(Invitation::getStatus)
                .isEqualTo("REVOKED");
    }

    @Test
    void revoke_rejected_forNonAdmin() throws Exception {
        String orgId = createOrg();
        String userId = registerUser(uniqueEmail("viewer2@example.com"));
        Membership membership = Membership.builder()
                .id(IdGenerator.uuid())
                .organizationId(orgId)
                .userId(userId)
                .role("VIEWER")
                .joinedAt(Instant.now())
                .build();
        membershipRepository.save(membership);
        String wsId = createWorkspace(orgId);
        Invitation inv = invitationService.create(wsId, "alice@example.com", "MEMBER", null);

        mockMvc.perform(delete("/api/invitations/{id}", inv.getToken())
                        .with(user(userId)))
                .andExpect(status().isForbidden());
    }

}
