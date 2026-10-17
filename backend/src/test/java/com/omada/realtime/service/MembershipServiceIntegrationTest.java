package com.omada.realtime.service;

import com.omada.realtime.AbstractIntegrationTest;
import com.omada.realtime.dto.request.AddMembershipRequest;
import com.omada.realtime.dto.response.MembershipResponse;
import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.response.AuthResponse;
import com.omada.realtime.entity.Membership;
import com.omada.realtime.repository.MembershipRepository;
import com.omada.realtime.repository.OrganizationRepository;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.entity.Organization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for membership RBAC: role hierarchy, add/list members,
 * duplicate + unknown user handling, and the owner-protection rule.
 */
public class MembershipServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MembershipService membershipService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.omada.realtime.service.UserService userService;

    private String createOrg() {
        Organization org = Organization.builder()
                .id(IdGenerator.uuid())
                .name("RBAC Org")
                .slug("rbac-org-" + java.util.UUID.randomUUID())
                .createdAt(Instant.now())
                .build();
        return organizationRepository.save(org).getId().toString();
    }

    private String registerUser(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setName(email.split("@")[0]);
        req.setPassword("password123");
        AuthResponse auth = userService.register(req);
        return auth.getUser().getId();
    }

    @Test
    void addMember_storesRole_andListsMembers() {
        String orgId = createOrg();
        String ownerId = registerUser("owner-rbac@example.com");
        String memberId = registerUser("member-rbac@example.com");

        membershipRepository.save(Membership.builder()
                .id(IdGenerator.uuid()).organizationId(orgId).userId(ownerId)
                .role("OWNER").joinedAt(Instant.now()).build());

        AddMembershipRequest add = new AddMembershipRequest();
        add.setEmail("member-rbac@example.com");
        add.setRole("MEMBER");
        MembershipResponse resp = membershipService.addMember(orgId, add);

        assertThat(resp.getRole()).isEqualTo("MEMBER");
        assertThat(resp.getEmail()).isEqualTo("member-rbac@example.com");

        var members = membershipService.listMembers(orgId);
        assertThat(members).extracting(MembershipResponse::getUserId)
                .containsExactlyInAnyOrder(ownerId, memberId);
    }

    @Test
    void hasRole_enforcesHierarchy() {
        String orgId = createOrg();
        String userId = registerUser("hierarchy@example.com");
        membershipRepository.save(Membership.builder()
                .id(IdGenerator.uuid()).organizationId(orgId).userId(userId)
                .role("MEMBER").joinedAt(Instant.now()).build());

        assertThat(membershipService.hasRole(orgId, userId, "VIEWER")).isTrue();
        assertThat(membershipService.hasRole(orgId, userId, "MEMBER")).isTrue();
        assertThat(membershipService.hasRole(orgId, userId, "ADMIN")).isFalse();
        assertThat(membershipService.hasRole(orgId, userId, "OWNER")).isFalse();
        // non-member is denied even for VIEWER
        String outsider = registerUser("outsider-rbac@example.com");
        assertThat(membershipService.hasRole(orgId, outsider, "VIEWER")).isFalse();
    }

    @Test
    void addMember_duplicateOrUnknownUser_throws() {
        String orgId = createOrg();
        String userId = registerUser("dup-rbac@example.com");
        membershipRepository.save(Membership.builder()
                .id(IdGenerator.uuid()).organizationId(orgId).userId(userId)
                .role("ADMIN").joinedAt(Instant.now()).build());

        AddMembershipRequest dup = new AddMembershipRequest();
        dup.setEmail("dup-rbac@example.com");
        dup.setRole("MEMBER");
        assertThatThrownBy(() -> membershipService.addMember(orgId, dup))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already a member");

        AddMembershipRequest unknown = new AddMembershipRequest();
        unknown.setEmail("ghost@example.com");
        unknown.setRole("MEMBER");
        assertThatThrownBy(() -> membershipService.addMember(orgId, unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No user with email");
    }

    @Test
    void removeMember_cannotRemoveLastOwner() {
        String orgId = createOrg();
        String ownerId = registerUser("lastowner-rbac@example.com");
        membershipRepository.save(Membership.builder()
                .id(IdGenerator.uuid()).organizationId(orgId).userId(ownerId)
                .role("OWNER").joinedAt(Instant.now()).build());

        assertThatThrownBy(() -> membershipService.removeMember(orgId, ownerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only owner");
    }
}
