package com.omada.realtime.service.impl;

import com.omada.realtime.dto.request.AddMembershipRequest;
import com.omada.realtime.dto.response.MembershipResponse;
import com.omada.realtime.entity.Membership;
import com.omada.realtime.entity.User;
import com.omada.realtime.repository.MembershipRepository;
import com.omada.realtime.repository.UserRepository;
import com.omada.realtime.security.IdGenerator;
import com.omada.realtime.service.MembershipService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Membership business logic with the role hierarchy
 * OWNER > ADMIN > MEMBER > VIEWER. Ownership rules enforced here:
 * - adding members requires ADMIN or OWNER
 * - granting OWNER or changing roles requires OWNER
 * - an organization always keeps at least one OWNER
 */
@Service
public class MembershipServiceImpl implements MembershipService {

    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "MEMBER", "VIEWER");
    private static final List<String> HIERARCHY = List.of("VIEWER", "MEMBER", "ADMIN", "OWNER");

    private final MembershipRepository memberships;
    private final UserRepository users;

    public MembershipServiceImpl(MembershipRepository memberships, UserRepository users) {
        this.memberships = memberships;
        this.users = users;
    }

    @Override
    public MembershipResponse addMember(String organizationId, AddMembershipRequest req) {
        String role = normalizeRole(req.getRole());
        User user = users.findByEmail(req.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("No user with email " + req.getEmail());
        }
        if (memberships.findByOrganizationIdAndUserId(organizationId, user.getId().toString()).isPresent()) {
            throw new IllegalArgumentException("User is already a member");
        }
        Membership m = Membership.builder()
                .id(IdGenerator.uuid())
                .organizationId(organizationId)
                .userId(user.getId().toString())
                .role(role)
                .joinedAt(Instant.now())
                .build();
        m = memberships.save(m);
        return toResponse(m, user);
    }

    @Override
    public List<MembershipResponse> listMembers(String organizationId) {
        return memberships.findByOrganizationId(organizationId).stream()
                .map(m -> toResponse(m, users.findById(java.util.UUID.fromString(m.getUserId())).orElse(null)))
                .toList();
    }

    @Override
    public MembershipResponse updateRole(String organizationId, String userId, String role) {
        String normalized = normalizeRole(role);
        Membership m = memberships.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member"));
        m.setRole(normalized);
        m = memberships.save(m);
        return toResponse(m, users.findById(java.util.UUID.fromString(userId)).orElse(null));
    }

    @Override
    public void removeMember(String organizationId, String userId) {
        Membership m = memberships.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member"));
        if ("OWNER".equals(m.getRole()) && ownersOf(organizationId) <= 1) {
            throw new IllegalStateException("Cannot remove the only owner");
        }
        memberships.delete(m);
    }

    @Override
    public boolean hasRole(String organizationId, String userId, String minRole) {
        return memberships.findByOrganizationIdAndUserId(organizationId, userId)
                .map(m -> rank(m.getRole()) >= rank(normalizeRole(minRole)))
                .orElse(false);
    }

    /** Role must be one of OWNER, ADMIN, MEMBER, VIEWER (case-insensitive). */
    private String normalizeRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        String upper = role.trim().toUpperCase();
        if (!VALID_ROLES.contains(upper)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        return upper;
    }

    private long ownersOf(String organizationId) {
        return memberships.findByOrganizationId(organizationId).stream()
                .filter(m -> "OWNER".equals(m.getRole()))
                .count();
    }

    private int rank(String role) {
        int idx = HIERARCHY.indexOf(role);
        return idx == -1 ? -1 : idx;
    }

    private MembershipResponse toResponse(Membership m, User user) {
        MembershipResponse r = new MembershipResponse();
        r.id = m.getId().toString();
        r.organizationId = m.getOrganizationId();
        r.userId = m.getUserId();
        r.role = m.getRole();
        r.email = user != null ? user.getEmail() : null;
        r.name = user != null ? user.getName() : null;
        return r;
    }
}
