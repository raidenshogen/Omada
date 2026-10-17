package com.omada.realtime.controller;

import com.omada.realtime.dto.response.ActivityResponse;
import com.omada.realtime.dto.response.PageResponse;
import com.omada.realtime.service.ActivityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private static final int MAX_PAGE_SIZE = 100;

    private final ActivityService service;
    public ActivityController(ActivityService service){ this.service = service; }

    /**
     * Paginated activity feed, scoped to a workspace or a board the caller can view.
     * {@code page} is 0-based; {@code size} is clamped to [1, 100].
     */
    @GetMapping
    @PreAuthorize("(#workspaceId != null and @workspaceSecurity.canViewWorkspace(#workspaceId, authentication.name))"
            + " or (#boardId != null and @workspaceSecurity.canViewBoard(#boardId, authentication.name))")
    public PageResponse<ActivityResponse> get(
            @RequestParam(required=false) String workspaceId,
            @RequestParam(required=false) String boardId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        int p = Math.max(0, page);
        int s = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        if(workspaceId != null) return service.findByWorkspace(workspaceId, p, s);
        if(boardId != null) return service.findByBoard(boardId, p, s);
        return PageResponse.empty(p, s);
    }
}
