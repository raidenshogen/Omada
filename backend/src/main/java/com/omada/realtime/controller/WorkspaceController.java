package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateWorkspaceRequest;
import com.omada.realtime.dto.response.WorkspaceResponse;
import com.omada.realtime.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceService service;
    public WorkspaceController(WorkspaceService service){ this.service = service; }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.isOrgWriter(#req.organizationId, authentication.name)")
    public WorkspaceResponse create(@Valid @RequestBody CreateWorkspaceRequest req){
        return service.create(req);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.isOrgMember(#orgId, authentication.name)")
    public List<WorkspaceResponse> byOrg(@RequestParam String orgId){
        return service.findByOrg(orgId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canViewWorkspace(#id, authentication.name)")
    public WorkspaceResponse get(@PathVariable String id){
        return service.findById(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canWriteWorkspace(#id, authentication.name)")
    public WorkspaceResponse update(@PathVariable String id, @Valid @RequestBody CreateWorkspaceRequest req){
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.isWorkspaceOwner(#id, authentication.name)")
    public void delete(@PathVariable String id){
        service.delete(id);
    }
}
