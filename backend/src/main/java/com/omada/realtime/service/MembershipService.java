package com.omada.realtime.service;

import com.omada.realtime.dto.request.AddMembershipRequest;
import com.omada.realtime.dto.response.MembershipResponse;

import java.util.List;

/**
 * Organization membership management. Roles: OWNER > ADMIN > MEMBER > VIEWER.
 */
public interface MembershipService {

    /** Adds a user (by email) to an organization with the given role. */
    MembershipResponse addMember(String organizationId, AddMembershipRequest req);

    /** Lists all members of an organization. */
    List<MembershipResponse> listMembers(String organizationId);

    /** Changes a member's role. */
    MembershipResponse updateRole(String organizationId, String userId, String role);

    /** Removes a member from an organization. */
    void removeMember(String organizationId, String userId);

    /** True if the user holds at least the given role in the organization. */
    boolean hasRole(String organizationId, String userId, String minRole);
}
