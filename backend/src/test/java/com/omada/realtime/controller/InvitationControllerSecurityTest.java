package com.omada.realtime.controller;

import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.entity.Invitation;
import com.omada.realtime.repository.BoardMembershipRepository;
import com.omada.realtime.repository.BoardRepository;
import com.omada.realtime.repository.CardRepository;
import com.omada.realtime.repository.InvitationRepository;
import com.omada.realtime.security.JwtAuthenticationFilter;
import com.omada.realtime.security.JwtUtil;
import com.omada.realtime.security.WorkspaceSecurity;
import com.omada.realtime.service.InvitationService;
import com.omada.realtime.service.MembershipService;
import com.omada.realtime.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Container-free RBAC + mapping test for InvitationController.
 * create/list/revoke require OWNER or ADMIN of the workspace; get/accept/decline are open
 * to any holder of a valid (unguessable) token — the service enforces token validity.
 */
@WebMvcTest(controllers = InvitationController.class)
@Import({com.omada.realtime.config.SecurityConfig.class, JwtAuthenticationFilter.class, WorkspaceSecurity.class})
class InvitationControllerSecurityTest {

    @Autowired
    MockMvc mvc;
    @MockBean
    InvitationService invitationService;
    @MockBean
    WorkspaceService workspaceService;
    @MockBean
    MembershipService membershipService;
    @MockBean
    BoardRepository boardRepository;
    @MockBean
    CardRepository cardRepository;
    @MockBean
    StringRedisTemplate redis;
    @MockBean
    InvitationRepository invitationRepository;
    @MockBean
    BoardMembershipRepository boardMembershipRepository;

    static final UUID TOKEN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final String INVITATION_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    static final String WS = "33333333-3333-3333-3333-333333333333";
    static final String ORG = "org-1";
    static final String USER = "user-1";

    private final Invitation inv = Invitation.builder()
            .id(UUID.fromString(INVITATION_ID))
            .token(TOKEN)
            .email("invitee@example.com")
            .workspaceId(WS)
            .role("MEMBER")
            .status("PENDING")
            .expiresAt(Instant.now().plusSeconds(86400))
            .build();

    @BeforeEach
    void setUp() {
        when(workspaceService.findById(WS)).thenReturn(new WorkspaceResponse(WS, ORG, "ws", ""));
        // Negative cache miss by default: workspaceOrg falls through to the (mocked) workspace service.
        var ops = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);
        // Deny by default: no OWNER/ADMIN membership unless a test grants it.
        when(membershipService.hasRole(eq(ORG), eq(USER), anyString())).thenReturn(false);
        when(invitationRepository.findByToken(TOKEN)).thenReturn(Optional.of(inv));
    }

    private String token() {
        return "Bearer " + JwtUtil.generateAccess(USER);
    }

    private void givenAdmin() {
        when(membershipService.hasRole(eq(ORG), eq(USER), eq("ADMIN"))).thenReturn(true);
    }

    @Test
    void createInvitationIsRejectedWhenNotAdmin() throws Exception {
        mvc.perform(post("/api/workspaces/{workspaceId}/invitations", WS)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invitee@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInvitationIsAllowedWhenAdmin() throws Exception {
        givenAdmin();
        when(invitationService.create(eq(WS), anyString(), anyString(), isNull())).thenReturn(inv);
        mvc.perform(post("/api/workspaces/{workspaceId}/invitations", WS)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invitee@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void listInvitationsIsRejectedWhenNotAdmin() throws Exception {
        mvc.perform(get("/api/workspaces/{workspaceId}/invitations", WS)
                        .header("Authorization", token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listInvitationsIsAllowedWhenAdmin() throws Exception {
        givenAdmin();
        when(invitationService.listByWorkspace(WS)).thenReturn(List.of(inv));
        mvc.perform(get("/api/workspaces/{workspaceId}/invitations", WS)
                        .header("Authorization", token()))
                .andExpect(status().isOk());
    }

    @Test
    void getInvitationByTokenIsOpenToTokenHolder() throws Exception {
        when(invitationService.getByToken(TOKEN)).thenReturn(inv);
        mvc.perform(get("/api/invitations/{token}", TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void acceptInvitationIsOpenToTokenHolder() throws Exception {
        when(invitationService.accept(TOKEN)).thenReturn(inv);
        mvc.perform(post("/api/invitations/{token}/accept", TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void declineInvitationIsOpenToTokenHolder() throws Exception {
        when(invitationService.decline(TOKEN)).thenReturn(inv);
        mvc.perform(post("/api/invitations/{token}/decline", TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void revokeInvitationIsRejectedWhenNotAdmin() throws Exception {
        mvc.perform(delete("/api/invitations/{id}", TOKEN)
                        .header("Authorization", token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokeInvitationIsAllowedWhenAdmin() throws Exception {
        givenAdmin();
        doNothing().when(invitationService).revoke(TOKEN);
        mvc.perform(delete("/api/invitations/{id}", TOKEN)
                        .header("Authorization", token()))
                .andExpect(status().isOk());
    }
}
