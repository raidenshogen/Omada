package com.omada.realtime.service.impl;
import com.omada.realtime.dto.request.CreateOrganizationRequest;
import com.omada.realtime.dto.response.OrganizationResponse;
import com.omada.realtime.entity.Membership;
import com.omada.realtime.entity.Organization;
import com.omada.realtime.mapper.OrganizationMapper;
import com.omada.realtime.repository.MembershipRepository;
import com.omada.realtime.repository.OrganizationRepository;
import com.omada.realtime.service.OrganizationService;
import com.omada.realtime.security.IdGenerator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class OrganizationServiceImpl implements OrganizationService {
    private final OrganizationRepository repo;
    private final OrganizationMapper mapper;
    private final MembershipRepository memberships;
    public OrganizationServiceImpl(OrganizationRepository repo, OrganizationMapper mapper, MembershipRepository memberships){
        this.repo=repo; this.mapper=mapper; this.memberships=memberships;
    }
    @Override public OrganizationResponse create(CreateOrganizationRequest req){
        Organization org = mapper.toEntity(req);
        org.setId(IdGenerator.uuid());
        org.setCreatedAt(Instant.now());
        // stamp the creator so audit trails and RBAC bootstrapping have an anchor
        org.setCreatedBy(currentUserId());
        org = repo.save(org);
        // bootstrap RBAC: the creator becomes the organization's first OWNER
        memberships.save(Membership.builder()
                .id(IdGenerator.uuid())
                .organizationId(org.getId().toString())
                .userId(org.getCreatedBy())
                .role("OWNER")
                .joinedAt(Instant.now())
                .build());
        return mapper.toResponse(org);
    }
    @Override public List<OrganizationResponse> findAll(){
        // only organizations the caller is a member of (tenant isolation);
        // unauthenticated callers (should not happen behind the filter) get nothing
        String userId = currentUserId();
        if (userId == null) return List.of();
        return memberships.findByUserId(userId).stream()
                .map(m -> repo.findById(UUID.fromString(m.getOrganizationId())).orElse(null))
                .filter(o -> o != null)
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
    @Override public OrganizationResponse findById(String id){
        UUID uuid = UUID.fromString(id);
        Organization org = repo.findById(uuid).orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        return mapper.toResponse(org);
    }
    @Override public OrganizationResponse update(String id, CreateOrganizationRequest req){
        UUID uuid = UUID.fromString(id);
        Organization org = repo.findById(uuid).orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        org.setName(req.name);
        org.setSlug(req.slug);
        org = repo.save(org);
        return mapper.toResponse(org);
    }
    @Override public void delete(String id){
        UUID uuid = UUID.fromString(id);
        if(!repo.existsById(uuid)) throw new IllegalArgumentException("Organization not found");
        repo.deleteById(uuid);
    }

    /** Authenticated user id from the security context (null if anonymous). */
    static String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
