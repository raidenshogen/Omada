package com.omada.realtime.service.impl;
import com.omada.realtime.dto.request.CreateWorkspaceRequest;
import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.entity.Workspace;
import com.omada.realtime.mapper.WorkspaceMapper;
import com.omada.realtime.repository.WorkspaceRepository;
import com.omada.realtime.repository.WorkspaceMembershipRepository;
import com.omada.realtime.service.WorkspaceService;
import com.omada.realtime.security.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class WorkspaceServiceImpl implements WorkspaceService {
    private final WorkspaceRepository repo;
    private final WorkspaceMembershipRepository workspaceMemberships;
    private final WorkspaceMapper mapper;
    public WorkspaceServiceImpl(WorkspaceRepository repo, WorkspaceMapper mapper, WorkspaceMembershipRepository workspaceMemberships){ this.repo=repo; this.mapper=mapper; this.workspaceMemberships = workspaceMemberships; }
    @Override public WorkspaceResponse create(CreateWorkspaceRequest req){
        Workspace ws = mapper.toEntity(req);
        ws.setId(IdGenerator.uuid());
        ws.setOrganizationId(req.organizationId);
        ws.setCreatedAt(Instant.now());
        ws.setUpdatedAt(Instant.now());
        ws = repo.save(ws);
        String creatorId = currentUserId();
        if (creatorId != null && !workspaceMemberships.existsByWorkspaceIdAndUserId(ws.getId().toString(), creatorId)) {
            workspaceMemberships.save(com.omada.realtime.entity.WorkspaceMembership.builder()
                    .id(IdGenerator.uuid())
                    .workspaceId(ws.getId().toString())
                    .userId(creatorId)
                    .role("OWNER")
                    .joinedAt(Instant.now())
                    .build());
        }
        return mapper.toResponse(ws);
    }

    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
    @Override public List<WorkspaceResponse> findByOrg(String orgId){
            return repo.findByOrganizationId(orgId).stream().map(mapper::toResponse).collect(Collectors.toList());
        }
        @Override public List<WorkspaceResponse> findForUser(String userId){
            return workspaceMemberships.findByUserId(userId).stream()
                    .map(m -> {
                        try {
                            return repo.findById(UUID.fromString(m.getWorkspaceId())).orElse(null);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(ws -> ws != null)
                    .distinct()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
        }
        @Override public WorkspaceResponse findById(String id){
            Workspace ws = repo.findById(UUID.fromString(id)).orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
            return mapper.toResponse(ws);
        }
        @Override public WorkspaceResponse update(String id, CreateWorkspaceRequest req){
            Workspace ws = repo.findById(UUID.fromString(id)).orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
            ws.setName(req.name);
            ws.setDescription(req.description);
            ws.setUpdatedAt(Instant.now());
            ws = repo.save(ws);
            return mapper.toResponse(ws);
        }
        @Override public void delete(String id){
            if(!repo.existsById(UUID.fromString(id))) throw new IllegalArgumentException("Workspace not found");
            repo.deleteById(UUID.fromString(id));
        }
}
