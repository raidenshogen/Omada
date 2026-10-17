package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateListRequest;
import com.omada.realtime.dto.response.ListResponse;
import com.omada.realtime.service.TaskListService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/lists")
public class ListController {
    private final TaskListService service;
    public ListController(TaskListService service){ this.service = service; }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.canEditBoard(#boardId, authentication.name)")
    public ListResponse create(@PathVariable String boardId, @Valid @RequestBody CreateListRequest req){
        req.boardId = boardId;
        return service.create(req);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.canViewBoard(#boardId, authentication.name)")
    public List<ListResponse> byBoard(@PathVariable String boardId){
        return service.findByBoard(boardId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canViewBoard(#boardId, authentication.name)")
    public ListResponse get(@PathVariable String boardId, @PathVariable String id){
        return service.getById(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canEditBoard(#boardId, authentication.name)")
    public ListResponse update(@PathVariable String boardId, @PathVariable String id, @Valid @RequestBody CreateListRequest req){
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.canEditBoard(#boardId, authentication.name)")
    public void delete(@PathVariable String boardId, @PathVariable String id){
        service.delete(id);
    }
}
