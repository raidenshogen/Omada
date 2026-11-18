package com.omada.realtime.service;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.entity.Invitation;
import com.omada.realtime.entity.User;
import com.omada.realtime.entity.Workspace;
import com.omada.realtime.entity.WorkspaceMembership;
import com.omada.realtime.repository.InvitationRepository;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.repository.WorkspaceMembershipRepository;
import com.omada.realtime.repository.WorkspaceRepository;
import com.omada.realtime.security.IdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for workspace invitations: create/list/accept/decline/revoke,
 * idempotent acceptance, and expiry handling.
 */
public class InvitationServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private WorkspaceMembershipService membershipService;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
        invitationRepository.deleteAll();
    }

    private String createWorkspace() {
        Workspace ws = Workspace.builder()
                .id(IdGenerator.uuid())
                .organizationId("org-" + UUID.randomUUID())
                .name("Test Workspace")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return workspaceRepository.save(ws).getId().toString();
    }

    private User createUser(String email) {
        User user = User.builder()
                .id(IdGenerator.uuid())
                .email(email)
                .name("Test User")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return userRepository.save(user);
    }

    private Invitation createInvitation(String workspaceId, String email, String role) {
        return invitationService.create(workspaceId, email, role, null);
    }

    @Test
    void create_storesPendingInvitationWithTokenAndExpiry() {
        String wsId = createWorkspace();

        Invitation inv = invitationService.create(wsId, "alice@example.com", "MEMBER", "board-1");

        assertThat(inv.getId()).isNotNull();
        assertThat(inv.getToken()).isNotNull();
        assertThat(inv.getEmail()).isEqualTo("alice@example.com");
        assertThat(inv.getWorkspaceId()).isEqualTo(wsId);
        assertThat(inv.getBoardId()).isEqualTo("board-1");
        assertThat(inv.getRole()).isEqualTo("MEMBER");
        assertThat(inv.getStatus()).isEqualTo("PENDING");
        Instant expiresAt = inv.getExpiresAt();
        assertThat(expiresAt).isAfter(Instant.now().plus(Duration.ofHours(23)));
        assertThat(expiresAt).isBefore(Instant.now().plus(Duration.ofHours(25)));
    }

    @Test
    void listByWorkspace_returnsOnlyThatWorkspace() {
        String ws1 = createWorkspace();
        String ws2 = createWorkspace();
        invitationService.create(ws1, "alice@example.com", "MEMBER", null);
        invitationService.create(ws1, "bob@example.com", "ADMIN", null);
        invitationService.create(ws2, "carol@example.com", "MEMBER", null);

        List<Invitation> ws1Invites = invitationService.listByWorkspace(ws1);

        assertThat(ws1Invites).extracting(Invitation::getEmail)
                .containsExactlyInAnyOrder("alice@example.com", "bob@example.com");
        assertThat(invitationService.listByWorkspace(ws2))
                .extracting(Invitation::getEmail)
                .containsExactly("carol@example.com");
    }

    @Test
    void accept_createsMembershipAndMarksAccepted() {
        String wsId = createWorkspace();
        User user = createUser("alice-accept@example.com");
        Invitation inv = createInvitation(wsId, "alice-accept@example.com", "MEMBER");

        Invitation accepted = invitationService.accept(inv.getToken());

        assertThat(accepted.getStatus()).isEqualTo("ACCEPTED");
        assertThat(invitationRepository.findByToken(inv.getToken()))
                .map(Invitation::getStatus)
                .get()
                .isEqualTo("ACCEPTED");

        WorkspaceMembership membership = membershipRepository.findByWorkspaceIdAndUserId(wsId, user.getId().toString())
                .orElseThrow(() -> new AssertionError("Expected membership to be created"));
        assertThat(membership.getRole()).isEqualTo("MEMBER");
    }

    @Test
    void acceptTwice_doesNotDuplicateMembership() {
        String wsId = createWorkspace();
        User user = createUser("alice-twice@example.com");
        Invitation inv = createInvitation(wsId, "alice-twice@example.com", "MEMBER");

        invitationService.accept(inv.getToken());
        invitationService.accept(inv.getToken());

        assertThat(invitationService.listByWorkspace(wsId))
                .filteredOn(i -> i.getStatus().equals("ACCEPTED"))
                .hasSize(1);
        assertThat(membershipRepository.findByWorkspaceId(wsId))
                .filteredOn(m -> m.getUserId().equals(user.getId().toString()))
                .hasSize(1);
    }

    @Test
    void acceptUnknownToken_throws() {
        assertThatThrownBy(() -> invitationService.accept(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invitation not found");
    }

    @Test
    void acceptExpired_throwsAndMarksExpired() {
        String wsId = createWorkspace();
        User user = createUser("alice-expired@example.com");
        Invitation expired = Invitation.builder()
                .id(IdGenerator.uuid())
                .token(IdGenerator.uuid())
                .email("alice@example.com")
                .workspaceId(wsId)
                .role("MEMBER")
                .status("PENDING")
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        invitationRepository.save(expired);

        assertThatThrownBy(() -> invitationService.accept(expired.getToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        assertThat(invitationRepository.findByToken(expired.getToken()))
                .map(Invitation::getStatus)
                .get()
                .isEqualTo("EXPIRED");
        assertThat(membershipService.findByWorkspace(wsId)).isEmpty();
    }

    @Test
    void decline_marksDeclinedAndCannotBeAccepted() {
        String wsId = createWorkspace();
        Invitation inv = createInvitation(wsId, "alice@example.com", "MEMBER");

        Invitation declined = invitationService.decline(inv.getToken());

        assertThat(declined.getStatus()).isEqualTo("DECLINED");
        assertThatThrownBy(() -> invitationService.accept(inv.getToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not pending");
    }

    @Test
    void revoke_marksInvitationRevokedAndHidesItFromPendingList() {
        String wsId = createWorkspace();
        Invitation inv = createInvitation(wsId, "alice@example.com", "MEMBER");

        invitationService.revoke(inv.getToken());

        assertThat(invitationRepository.findByToken(inv.getToken()))
                .get()
                .extracting(Invitation::getStatus)
                .isEqualTo("REVOKED");
        assertThat(invitationService.listByWorkspace(wsId)).hasSize(1);
    }
}
