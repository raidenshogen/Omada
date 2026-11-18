package com.omada.realtime.service;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.entity.Workspace;
import com.omada.realtime.entity.WorkspaceMembership;
import com.omada.realtime.repository.WorkspaceMembershipRepository;
import com.omada.realtime.repository.WorkspaceRepository;
import com.omada.realtime.security.IdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for workspace membership: create/list/change-role/remove,
 * duplicate-member rejection, and role validation.
 */
public class WorkspaceMembershipServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private WorkspaceMembershipService membershipService;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

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

    @Test
    void create_storesRole_andListsMembers() {
        String wsId = createWorkspace();
        WorkspaceMembership m = membershipService.create(wsId, "user-1", "MEMBER");

        assertThat(m.getRole()).isEqualTo("MEMBER");
        assertThat(m.getJoinedAt()).isNotNull();
        assertThat(m.getWorkspaceId()).isEqualTo(wsId);
        assertThat(m.getUserId()).isEqualTo("user-1");

        List<WorkspaceMembership> members = membershipService.findByWorkspace(wsId);
        assertThat(members).extracting(WorkspaceMembership::getUserId)
                .containsExactly("user-1");
    }

    @Test
    void changeRole_updatesAndPersists() {
        String wsId = createWorkspace();
        membershipService.create(wsId, "user-2", "MEMBER");

        WorkspaceMembership updated = membershipService.changeRole(wsId, "user-2", "ADMIN");
        assertThat(updated.getRole()).isEqualTo("ADMIN");

        assertThat(membershipRepository.findByWorkspaceIdAndUserId(wsId, "user-2"))
                .map(WorkspaceMembership::getRole)
                .isPresent()
                .get()
                .isEqualTo("ADMIN");
    }

    @Test
    void remove_deletesMembership() {
        String wsId = createWorkspace();
        membershipService.create(wsId, "user-3", "MEMBER");
        membershipService.remove(wsId, "user-3");

        assertThat(membershipService.findByWorkspace(wsId)).isEmpty();
    }

    @Test
    void duplicateMemberAndInvalidRole_throw() {
        String wsId = createWorkspace();
        membershipService.create(wsId, "user-4", "OWNER");

        assertThatThrownBy(() -> membershipService.create(wsId, "user-4", "MEMBER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already a member");

        assertThatThrownBy(() -> membershipService.create(wsId, "user-5", "SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role");

        assertThatThrownBy(() -> membershipService.changeRole(wsId, "unknown", "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a member");
    }
}
