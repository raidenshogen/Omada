package com.omada.realtime.controller;

import com.omada.realtime.dto.request.AddMembershipRequest;
import com.omada.realtime.dto.request.UpdateRoleRequest;
import com.omada.realtime.dto.response.MembershipResponse;
import com.omada.realtime.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Organization membership API. Every endpoint requires an authenticated user
 * holding at least ADMIN (OWNER for role changes); the checks run through
 * workspaceSecurity so RBAC lives in one place.
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/members")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /** Adds a member (ADMIN+). */
    @PostMapping
    @PreAuthorize("@workspaceSecurity.isOrgAdmin(#orgId, authentication.name)")
    public MembershipResponse add(@PathVariable String orgId, @Valid @RequestBody AddMembershipRequest req) {
        return membershipService.addMember(orgId, req);
    }

    /** Lists members (any member can view). */
    @GetMapping
    @PreAuthorize("@workspaceSecurity.isOrgMember(#orgId, authentication.name)")
    public List<MembershipResponse> list(@PathVariable String orgId) {
        return membershipService.listMembers(orgId);
    }

    /** Changes a member's role (OWNER only). */
    @PatchMapping("/{userId}")
    @PreAuthorize("@workspaceSecurity.isOrgOwner(#orgId, authentication.name)")
    public MembershipResponse updateRole(@PathVariable String orgId, @PathVariable String userId,
                                         @Valid @RequestBody UpdateRoleRequest req) {
        return membershipService.updateRole(orgId, userId, req.getRole());
    }

    /** Removes a member (ADMIN+; owners protected in the service). */
    @DeleteMapping("/{userId}")
    @PreAuthorize("@workspaceSecurity.isOrgAdmin(#orgId, authentication.name)")
    public void remove(@PathVariable String orgId, @PathVariable String userId) {
        membershipService.removeMember(orgId, userId);
    }
}
