package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateBoardRequest;
import com.omada.realtime.dto.response.BoardResponse;
import com.omada.realtime.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/boards")
public class BoardController {
    private final BoardService service;
    public BoardController(BoardService service){ this.service = service; }

    @PreAuthorize("@workspaceSecurity.hasBoardWrite(#workspaceId, authentication.name)")
    @PostMapping
    public BoardResponse create(@PathVariable String workspaceId, @Valid @RequestBody CreateBoardRequest req){
        req.workspaceId = workspaceId;
        return service.create(req);
    }

    @PreAuthorize("@workspaceSecurity.canView(#workspaceId, authentication.name)")
    @GetMapping
    public List<BoardResponse> byWorkspace(@PathVariable String workspaceId){
        return service.findByWorkspace(UUID.fromString(workspaceId));
    }

    @PreAuthorize("@workspaceSecurity.canView(#workspaceId, authentication.name)")
    @GetMapping("/{id}")
    public BoardResponse get(@PathVariable String workspaceId, @PathVariable UUID id){
        return service.getById(id);
    }

    @PreAuthorize("@workspaceSecurity.hasBoardWrite(#workspaceId, authentication.name)")
    @PatchMapping("/{id}")
    public BoardResponse update(@PathVariable String workspaceId, @PathVariable UUID id, @Valid @RequestBody CreateBoardRequest req){
        return service.update(id, req);
    }

    @PreAuthorize("@workspaceSecurity.hasWorkspaceAdmin(#workspaceId, authentication.name)")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String workspaceId, @PathVariable UUID id){
        service.delete(id);
    }
}
